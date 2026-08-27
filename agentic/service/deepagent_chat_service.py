"""
DeepAgent Chat Service - 基于 TreeNode 构建嵌套 Deep Agents 的对话服务。

根据流程树的父子关系构建嵌套的 deep agents，父节点作为 supervisor，
子节点作为其下属 agents。每个 agent 从 gRPC 获取配置信息（agent 详情、model 信息），
并将 tools/skills 转换为 langchain 可调用的工具。

主要功能：
- 解析 TreeNode 结构，递归构建嵌套 agents（通过 subagents 参数）
- 从 gRPC 获取 agent 和 model 配置
- 工具转换（仅 sandbox 类型）
- 支持 SSE 流式输出（每个 chunk 包含 agentId 和 agentName）
- 始终使用 InMemorySaver 进行对话记忆
"""

import json
from collections import OrderedDict
from typing import Any, AsyncGenerator, Dict, List, Optional, Tuple, Literal, Type

from langchain.agents.middleware import AgentMiddleware
from langchain.tools import tool
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver
from langgraph.graph.state import CompiledStateGraph
from deepagents import create_deep_agent
from deepagents.graph import DeepAgentState
from agentic.sandbox.python_sandbox_backend import get_or_create_sandbox

from grpc_client import get_grpc_client
from util.flow_tree_parser import TreeNode
from sandbox.sandbox_tool import run_python_code, check_sandbox_health, get_sandbox_dependencies, run_sandbox_command
from logger_config import logger
from pydantic import create_model, Field
import os
from pathlib import Path

BACKEND_DIR = os.getenv("BACKEND_DIR", str(Path.home() / ".workspaces"))
RECURSION_LIMIT = int(os.getenv("RECURSION_LIMIT", "50"))
# 全局记忆存储（进程级别共享）
_memory_store: Any = None
DB_URI = os.getenv("DB_URI")

# 保存 PostgresSaver 上下文管理器对象，用于关闭时释放连接
_postgres_cm: Any = None


async def connect_db():
    """启动时连接数据库，创建表结构并初始化记忆存储"""
    global _memory_store, _postgres_cm
    if DB_URI:
        _postgres_cm = AsyncPostgresSaver.from_conn_string(DB_URI)
        _memory_store = await _postgres_cm.__aenter__()
        await _memory_store.setup()
        logger.info("PostgreSQL 记忆存储已连接")
    else:
        _memory_store = InMemorySaver()
        logger.info("使用 InMemorySaver 作为记忆存储")


async def disconnect_db():
    """关闭时断开数据库连接"""
    global _memory_store, _postgres_cm
    if _postgres_cm is not None:
        await _postgres_cm.__aexit__(None, None, None)
        _postgres_cm = None
        _memory_store = None
        logger.info("PostgreSQL 记忆存储已断开")


def get_memory_store() -> Any:
    """获取全局记忆存储实例"""
    return _memory_store


class JAgentState(DeepAgentState):
    """扩展 DeepAgentState，添加 agent_id 字段。

    注意：JAgentState 是 TypedDict 子类，不能被实例化。
    作为 state_schema 传给 create_deep_agent 时必须传类本身，
    否则会被当作普通 dict，无法作为 dict 的 key 哈希。

    字段语义：
      agent_id: 当前 LangGraph graph state 中的 agent_id。
      AgentIdMiddleware 会在每次模型调用前将其覆盖为
      "当前正在执行的 agent" 的 id —— 子 agent 会覆盖父 agent，
      这样任意中间件 / 工具可通过 runtime.state["agent_id"]
      读到当前正在运行的是哪个 agent。
    """

    agent_id: int | None


# ---------------------------------------------------------------------------
# agent_name 与 agent_id 的编码 / 解码辅助
# ---------------------------------------------------------------------------
# 构建时把 id 拼到 name 末尾（"name#id"），astream 外部循环从
# metadata["lc_agent_name"] 中 split 得到当前正在输出的 agent 的 id。
# 这样做避开了 ContextVar / Task 边界 / state 等复杂路径，最简单可靠。
_AGENT_NAME_SEP = "#"


def _encode_agent_name(name: str, agent_id: int) -> str:
    """构建时把 agent_id 拼到 name 末尾，astream 中可 split 取回 id。"""
    return f"{name}{_AGENT_NAME_SEP}{agent_id}"


def _decode_agent_name(encoded: str) -> Tuple[str, Optional[int]]:
    """astream 中从 lc_agent_name 还原出 (name, id)。"""
    if _AGENT_NAME_SEP not in encoded:
        return encoded, None
    name_part, _, id_part = encoded.rpartition(_AGENT_NAME_SEP)
    try:
        return name_part, int(id_part)
    except (TypeError, ValueError):
        return encoded, None


class AgentIdMiddleware(AgentMiddleware[Any, Any, Any]):
    """在每次模型调用前，把当前 agent 的 id 写入 graph state 的 agent_id 字段。

    使用场景：
      构建 agent / subagent 时通过闭包传入该 agent 的 agent_id，
      这样运行期间（工具 / middleware / 其他节点）可通过
      `runtime.state["agent_id"]` 读到当前正在执行的是哪个 agent。

    由于中间件在模型调用前执行，在子 agent 重新进入 agent 循环时，
      子 agent 的 AgentIdMiddleware 会覆盖父 agent 留下的 agent_id，
      所以每个 agent 在自己的范围内读到的是自己的 id。
    """

    def __init__(self, agent_id: int) -> None:
        super().__init__()
        self._agent_id = agent_id

    def before_model(self, state, runtime):  # type: ignore[override]
        #    供工具 / middleware 通过 runtime.state["agent_id"] 读取
        return {"agent_id": self._agent_id}


class DeepAgentChatService:
    """
    DeepAgent 对话服务

    负责根据 TreeNode 结构构建嵌套的 deep agents，并处理对话请求。
    通过 create_deep_agent 的 subagents 参数实现父子 agent 嵌套。
    """

    def __init__(self):
        self.grpc_client = get_grpc_client()
        # 使用 OrderedDict 实现 LRU 缓存，最大容量 128
        self.session_agent_mapping: OrderedDict[str, Any] = OrderedDict()
        self._max_cache_size = 128

    async def chat_stream(
        self,
        tree_node: TreeNode,
        prompt: str,
        session_id: str,
        images: Optional[List[str]] = None,
        web_search: bool = False,
    ) -> AsyncGenerator[str, None]:
        """
        流式对话接口

        Args:
            tree_node: 解析后的流程树根节点
            prompt: 用户输入的提示词
            session_id: 会话 ID，用于记忆管理
            images: 图片 base64 列表（可选）
            web_search: 是否启用联网搜索

        Yields:
            SSE 格式的数据流，每个 chunk 包含 agentId 和 agentName
        """
        try:
            root_agent = self.session_agent_mapping.get(session_id)
            if root_agent:
                # 访问时移动到末尾（最近使用）
                self.session_agent_mapping.move_to_end(session_id)
            else:
                # 构建根 agent（含子 agents）
                root_agent, _ = await self._build_agent_from_tree_node(
                    tree_node, web_search
                )
                # 添加新条目到末尾
                self.session_agent_mapping[session_id] = root_agent
                # 如果超过容量，移除最久未使用的条目（头部）
                if len(self.session_agent_mapping) > self._max_cache_size:
                    evicted_key, _ = self.session_agent_mapping.popitem(last=False)
                    logger.info(f"LRU 缓存已满，驱逐 session: {evicted_key}")

            if root_agent is None:
                yield self._format_sse_error("无法构建根 Agent")
                return

            # 根 agent 始终显示推理内容给前端
            show_reasoning = True

            # 当前 Agent 信息，初始为根 Agent
            current_agent_id = tree_node.node.agent_id
            current_agent_name = tree_node.node.agent_name

            # 已发送 tool_call 事件的工具调用 ID 集合，用于去重
            sent_tool_call_ids = set()

            # 构建输入消息
            messages = self._build_messages(prompt, images)

            # 配置记忆
            config = {
                "configurable": {"thread_id": session_id},
                "recursion_limit": RECURSION_LIMIT,
            }

            # 流式调用 agent
            # 初始 state 同时注入 agent_id（与 JAgentState.state_schema 对齐），
            # 这样 graph state 中始终携带当前 agent 的 id 供工具 / middleware 读取
            async for item, chunk in root_agent.astream(
                {"messages": messages, "agent_id": current_agent_id},
                config=config,
                stream_mode="messages",
                subgraphs=True,
            ):
                msg_chunk = chunk[0]
                metadata = chunk[1]
                # 获取 content_blocks
                content_blocks = getattr(msg_chunk, "content_blocks", [])
                # print(content_blocks, end="---")
                # print(metadata)
                # 检查是否进入 tools 节点（子 Agent 执行）
                lc_agent_name = metadata.get("lc_agent_name", "")
                if lc_agent_name:
                    # 构建时把 agent_id 编码进了 name（"name#id"），
                    # astream 中 split 得到当前正在输出的 agent 的 id。
                    decoded_name, decoded_id = _decode_agent_name(lc_agent_name)
                    current_agent_name = decoded_name
                    if decoded_id is not None:
                        current_agent_id = decoded_id

                # 判断消息类型：ToolMessage 有 name 属性，AIMessage 没有
                is_tool_message = hasattr(msg_chunk, "name") and msg_chunk.name

                if is_tool_message:
                    # ToolMessage：工具执行结果，通过 tool_called 输出
                    tool_id = msg_chunk.tool_call_id
                    tool_name = msg_chunk.name
                    tool_content = (
                        msg_chunk.content if hasattr(msg_chunk, "content") else ""
                    )
                    logger.info(f"工具： {tool_name}, tool_id: {tool_id} 执行完成")
                    if tool_id in sent_tool_call_ids:
                        if show_reasoning:
                            yield self._format_sse_tool_result(
                                tool_name,
                                tool_content,
                                agent_id=current_agent_id,
                                agent_name=current_agent_name,
                            )
                            sent_tool_call_ids.remove(tool_id)
                else:
                    # AIMessage：正常推理输出
                    # 遍历 content_blocks
                    for block in content_blocks:
                        if not isinstance(block, dict):
                            continue

                        block_type = block.get("type")

                        if block_type == "text":
                            # 文本内容块
                            text = block.get("text", "")
                            if text and show_reasoning:
                                yield self._format_sse_text(
                                    text,
                                    agent_id=current_agent_id,
                                    agent_name=current_agent_name,
                                )

                        elif block_type == "tool_call_chunk":
                            # 工具调用分片
                            tool_name = block.get("name")
                            tool_args = block.get("args", "")
                            tool_id = block.get("id", "")

                            if tool_id and tool_name:
                                # 其他工具调用，使用 tool_name 去重
                                # tool_call_chunk 流式传输时，后续 chunk 的 id 为空字符串，导致 id 去重失效
                                # 改用 name 去重，确保每个工具名只发送一次 tool_call 事件
                                if tool_id not in sent_tool_call_ids:
                                    sent_tool_call_ids.add(tool_id)
                                    logger.info(
                                        f"工具调用：{tool_name}, tool_id: {tool_id}"
                                    )
                                    if show_reasoning:
                                        yield self._format_sse_tool_call(
                                            tool_name,
                                            agent_id=current_agent_id,
                                            agent_name=current_agent_name,
                                        )

            # 流结束标记
            yield "data: [DONE]\n\n"

        except Exception as e:
            logger.error(f"对话流处理异常: {e}", exc_info=True)
            yield self._format_sse_error(str(e))

    async def _build_agent_from_tree_node(
        self, tree_node: TreeNode, web_search: bool = False
    ) -> Tuple[Optional[CompiledStateGraph], Optional[Dict[str, Any]]]:
        """
        根据 TreeNode 递归构建 deep agent

        递归处理子节点，将子 agent 构建为 CompiledSubAgent dict
        （包含 name、description、runnable），通过 subagents 参数传入父 agent。
        始终使用 InMemorySaver 作为 checkpointer，忽略 flow_memory 字段。

        Args:
            tree_node: 当前树节点
            web_search: 是否启用联网搜索

        Returns:
            元组 (构建好的 deep agent, agent 配置信息)，失败返回 (None, None)
        """
        node = tree_node.node
        agent_id = node.agent_id

        if agent_id is None:
            logger.error(f"节点 {node.node_id} 缺少 agent_id")
            return None, None

        # 从 gRPC 获取 agent 详情（包含 tools 和 skills）
        agent_info = self.grpc_client.get_ai_agent_with_tool_and_skill_by_id(agent_id)
        if agent_info is None:
            logger.error(f"无法获取 agent 信息，agent_id={agent_id}")
            return None, None

        # 获取 model 信息
        model_id = agent_info.get("modelId")
        if model_id is None:
            logger.error(f"agent {agent_id} 未配置 modelId")
            return None, None

        model_info = self.grpc_client.get_model_by_id(model_id)
        if model_info is None:
            logger.error(f"无法获取 model 信息，model_id={model_id}")
            return None, None

        # 构建 LLM
        llm = self._build_llm(model_info, agent_info)
        if llm is None:
            logger.error(f"构建 LLM 失败，model_id={model_id}")
            return None, None

        # 构建工具列表
        tools = await self._build_tools(agent_info, web_search)

        # 构建 system prompt
        system_prompt = agent_info.get("systemMessage", tree_node.node.description)
        if not system_prompt:
            system_prompt = f"你是一个名为 {tree_node.node.agent_name} 的智能助手。"

        # 递归构建子 agents 为 SubAgent spec dicts 或 CompiledSubAgent dicts
        subagents = []
        for child in tree_node.children:
            child_spec = await self._build_subagent_spec(child, web_search)
            if child_spec is not None:
                subagents.append(child_spec)

        # 创建 deep agent，通过 subagents 参数实现嵌套
        # 注意：
        #   1. state_schema 必须传入 TypedDict 类本身，不能传其实例（实例为 dict，不可哈希）
        #   2. middleware 挂载 AgentIdMiddleware，在每次模型调用前把本 agent 的 id
        #      写入 state["agent_id"]，供工具 / middleware / subagent 调用时读取
        try:
            agent = create_deep_agent(
                model=llm,
                tools=tools,
                system_prompt=system_prompt,
                subagents=subagents if subagents else None,
                middleware=[AgentIdMiddleware(agent_id)],
                checkpointer=_memory_store,
                name=_encode_agent_name(tree_node.node.agent_name, agent_id),
                state_schema=JAgentState,
                backend=await get_or_create_sandbox(session_id="default")
            )
            # show_reasoning 从 TreeNode 的 AgentNodeData 获取，而非 gRPC agent 信息
            node_show_reasoning = tree_node.node.show_reasoning
            logger.info(
                f"成功构建 agent: {agent_info.get('name')} (ID: {agent_id}), "
                f"子 agent 数量: {len(subagents)}, "
                f"show_reasoning: {node_show_reasoning}"
            )
            return agent, agent_info
        except Exception as e:
            logger.error(f"创建 deep agent 失败: {e}", exc_info=True)
            return None, None

    async def _build_subagent_spec(
        self, tree_node: TreeNode, web_search: bool = False
    ) -> Optional[Dict[str, Any]]:
        """
        为子节点构建 SubAgent spec dict 或 CompiledSubAgent dict。

        如果子节点没有自己的子节点（叶子节点），构建 SubAgent spec dict，
        让 create_deep_agent 自动处理。

        如果子节点有自己的子节点（非叶子节点），先递归编译为 deep agent，
        再包装为 CompiledSubAgent dict（包含 name、description、runnable）。

        Args:
            tree_node: 子节点
            web_search: 是否启用联网搜索

        Returns:
            SubAgent dict 或 CompiledSubAgent dict，失败返回 None
        """
        node = tree_node.node
        agent_id = node.agent_id

        if agent_id is None:
            logger.error(f"子节点 {node.node_id} 缺少 agent_id")
            return None

        agent_info = self.grpc_client.get_ai_agent_with_tool_and_skill_by_id(agent_id)
        if agent_info is None:
            logger.error(f"无法获取子 agent 信息，agent_id={agent_id}")
            return None

        model_id = agent_info.get("modelId")
        if model_id is None:
            logger.error(f"子 agent {agent_id} 未配置 modelId")
            return None

        model_info = self.grpc_client.get_model_by_id(model_id)
        if model_info is None:
            logger.error(f"无法获取子 agent model 信息，model_id={model_id}")
            return None

        agent_name = node.agent_name
        agent_description = node.description

        if not tree_node.children:
            # 叶子节点：构建 SubAgent spec dict，由父级的 create_deep_agent 自动编译
            llm = self._build_llm(model_info, agent_info)
            if llm is None:
                return None
            tools = await self._build_tools(agent_info, web_search)

            spec: Dict[str, Any] = {
                "name": _encode_agent_name(agent_name, agent_id),
                "description": agent_description,
                "system_prompt": agent_info.get("systemMessage", agent_description)
                or f"你是一个名为 {agent_name} 的智能助手。",
                "tools": tools,
                "model": llm,
                # 挂载 AgentIdMiddleware，在子 agent 被调用时把其 agent_id 写入 state
                "middleware": [AgentIdMiddleware(agent_id)],
            }
            logger.info(f"构建 SubAgent spec (叶子): {agent_name} (ID: {agent_id})")
            return spec
        else:
            # 非叶子节点：先递归编译为 deep agent，再包装为 CompiledSubAgent
            child_agent, _ = await self._build_agent_from_tree_node(
                tree_node, web_search
            )
            if child_agent is None:
                return None

            compiled_spec: Dict[str, Any] = {
                "name": _encode_agent_name(agent_name, agent_id),
                "description": agent_description,
                "runnable": child_agent,
            }
            logger.info(
                f"构建 CompiledSubAgent (非叶子): {agent_name} (ID: {agent_id})"
            )
            return compiled_spec

    def _build_llm(
        self, model_info: Dict[str, Any], agent_info: Dict[str, Any]
    ) -> Optional[Any]:
        """
        根据 model 和 agent 信息构建 LLM

        Args:
            model_info: 模型配置信息
            agent_info: Agent 配置信息

        Returns:
            构建好的 LLM 实例
        """
        try:
            base_url = model_info.get("baseUrl")
            api_key = model_info.get("apiKey")
            model_name = model_info.get("name")

            if not base_url or not api_key or not model_name:
                logger.error("模型配置不完整")
                return None

            # 从 agent 获取温度参数（优先使用 agent 配置，否则使用 model 配置）
            temperature = agent_info.get("temperature") or model_info.get(
                "temperature", 0.7
            )

            # maxOutputToken: -1 表示不限制，其他正值表示限制
            max_output_token = agent_info.get("maxOutputToken") or model_info.get(
                "maxOutputToken"
            )

            # 构建 LLM 参数
            llm_kwargs = {
                "base_url": base_url,
                "api_key": api_key,
                "model": model_name,
                "temperature": temperature,
            }

            # maxOutputToken 为 -1 表示不限制，不传 max_tokens；正值则传入
            if max_output_token and max_output_token > 0:
                llm_kwargs["max_tokens"] = max_output_token

            # 默认关闭深度思考（show_reasoning 字段用于控制是否输出内容给前端，与模型推理无关）
            llm_kwargs["extra_body"] = {
                "chat_template_kwargs": {"enable_thinking": False}
            }

            # 使用 ChatOpenAI 创建 LLM
            from langchain_openai import ChatOpenAI

            llm = ChatOpenAI(**llm_kwargs)

            logger.info(f"成功构建 LLM: {model_name}")
            return llm

        except Exception as e:
            logger.error(f"构建 LLM 失败: {e}", exc_info=True)
            return None

    async def _build_tools(
        self, agent_info: Dict[str, Any], web_search: bool = False
    ) -> List[Any]:
        """
        根据 agent 信息构建工具列表

        处理 sandbox 类型的工具和 MCP 工具。

        Args:
            agent_info: Agent 配置信息
            web_search: 是否启用联网搜索

        Returns:
            langchain 工具列表
        """
        tools = []

        # 处理 agent 配置的工具（仅 sandbox 类型）
        agent_tools = agent_info.get("agentTools", [])
        for tool_info in agent_tools:
            tool_type = tool_info.get("toolType", "")
            # 只处理 sandbox 类型的工具
            if tool_type == "sandbox":
                langchain_tool = self._convert_sandbox_tool_to_langchain(tool_info)
                if langchain_tool:
                    tools.append(langchain_tool)
            elif (
                tool_type == "builtin"
                and tool_info.get("toolName") == "run_python_code"
            ):
                # 内置沙箱工具
                tools.append(run_python_code)
                tools.append(check_sandbox_health)
                tools.append(get_sandbox_dependencies)
                tools.append(run_sandbox_command)
            else:
                logger.debug(
                    f"跳过非 sandbox 类型工具: {tool_info.get('toolName', 'unknown')} (type={tool_type})"
                )

        # 处理 MCP 工具
        mcp_tools = await self._build_mcp_tools(agent_info)
        tools.extend(mcp_tools)

        # 如果启用联网搜索，添加搜索工具
        if web_search:
            search_tool = self._create_web_search_tool()
            if search_tool:
                tools.append(search_tool)

        return tools

    def _convert_sandbox_tool_to_langchain(
        self, tool_info: Dict[str, Any]
    ) -> Optional[Any]:
        """
        将 sandbox 类型的 agent tool 转换为 langchain tool

        Args:
            tool_info: 工具配置信息（sandbox 类型）

        Returns:
            langchain 工具，转换失败返回 None
        """
        tool_name = tool_info.get("toolName", "")
        description = tool_info.get("description", "")
        code = tool_info.get("code", "")
        parameters_raw = tool_info.get("parameters", "")

        param_schema: Dict[str, Any] = {}
        if parameters_raw:
            param_schema = json.loads(parameters_raw)

        if not code:
            logger.warning(f"sandbox 工具 {tool_name} 缺少代码")
            return None

        def build_pydantic_model_from_json_schema(schema: Any) -> Type:
            """
            简易转换: JSON Schema / 参数列表 → Pydantic Model

            支持两种输入格式:
              1. 标准 JSON Schema:
                 {"type": "object", "properties": {...}, "required": [...]}
              2. 简化参数列表（前端常见）:
                 [{"name": "city", "type": "str", "description": "...", "required": true}, ...]

            只处理最常用场景（string/integer/number/boolean/array/object，
            以及对应的 Python 类型名 str/int/float/bool/list/dict）。
            """
            fields: Dict[str, Any] = {}

            # 格式 2: 简化参数列表 → 归一化为标准 properties/required
            if isinstance(schema, list):
                properties: Dict[str, Dict[str, Any]] = {}
                required_list: List[str] = []
                for item in schema:
                    if not isinstance(item, dict):
                        continue
                    param_name = item.get("name")
                    if not param_name:
                        continue
                    properties[param_name] = item
                    # 兼容多种必填字段命名
                    if item.get("required") or item.get("isRequired"):
                        required_list.append(param_name)
            elif isinstance(schema, dict):
                properties = schema.get("properties", {}) or {}
                required_list = schema.get("required", []) or []
            else:
                logger.warning(
                    f"无法识别的 parameters schema 类型: {type(schema).__name__}, "
                    f"工具 {tool_name} 将不创建参数 schema"
                )
                return create_model(f"SandboxTool_{tool_name}_Args")

            # 同时支持 JSON Schema 类型名 与 Python 类型名
            type_mapping: Dict[str, Type] = {
                # JSON Schema 类型
                "string": str,
                "integer": int,
                "number": float,
                "boolean": bool,
                "array": list,
                "object": dict,
                # Python 类型名（前端可能直接传）
                "str": str,
                "int": int,
                "float": float,
                "bool": bool,
                "list": list,
                "dict": dict,
            }

            for param_name, prop in properties.items():
                json_type = prop.get("type", "string")
                py_type = type_mapping.get(json_type, str)
                desc = prop.get("description", "")
                is_required = param_name in required_list

                if is_required:
                    fields[param_name] = (py_type, Field(description=desc))
                else:
                    fields[param_name] = (
                        Optional[py_type],
                        Field(default=None, description=desc),
                    )

            return create_model(f"SandboxTool_{tool_name}_Args", **fields)

        args_schema_cls = build_pydantic_model_from_json_schema(param_schema)

        # 动态创建工具函数，通过闭包捕获变量
        def _make_sandbox_tool(name: str, desc: str, src_code: str):
            # langchain 1.3.x 中 tool() 装饰器的签名：
            #   tool(name_or_callable, runnable=None, *args, description=None, ...)
            # name 仅支持作为第一个位置参数传入，不支持作为关键字参数。
            @tool(
                name,
                description=desc or f"执行 {name} 工具",
                args_schema=args_schema_cls,
            )
            def sandbox_tool(**kwargs) -> str:
                """执行沙箱代码"""
                try:
                    exec_code = src_code
                    for key, value in kwargs.items():
                        exec_code = exec_code.replace(f"${{{key}}}", str(value))

                    from sandbox.sandbox_sdk import get_sandbox_sdk

                    sdk = get_sandbox_sdk()
                    result = sdk.run_python_and_get_stdout(exec_code)
                    return result if result else "(执行无输出)"
                except Exception as e:
                    logger.error(f"沙箱工具 {name} 执行失败: {e}")
                    return f"执行失败: {e}"

            return sandbox_tool

        return _make_sandbox_tool(tool_name, description, code)

    async def _build_mcp_tools(self, agent_info: Dict[str, Any]) -> List[Any]:
        """
        根据 agent 信息构建 MCP 工具列表

        从 agentMcpTools 中提取 MCP 工具，按 MCP service (pid) 分组，
        每个 service 只建立一次 MCP 连接，获取该 service 提供的所有工具后，
        按名称匹配 agent 实际配置的 MCP 工具。

        Args:
            agent_info: Agent 配置信息

        Returns:
            langchain MCP 工具列表
        """
        mcp_tools = []

        # 获取 agent 的 MCP 工具列表
        agent_mcp_tools = agent_info.get("agentMcpTools", [])
        if not agent_mcp_tools:
            return mcp_tools

        # 提取所有唯一的 MCP service ID (pid)
        mcp_service_ids = list(
            set(tool.get("pid") for tool in agent_mcp_tools if tool.get("pid"))
        )
        if not mcp_service_ids:
            logger.warning("MCP 工具缺少 pid 信息")
            return mcp_tools

        # 通过 gRPC 获取 MCP service 详情
        try:
            mcp_services = self.grpc_client.get_mcp_service_by_ids(mcp_service_ids)
        except Exception as e:
            logger.error(f"获取 MCP service 信息失败: {e}", exc_info=True)
            return mcp_tools

        # 构建 MCP service ID 到 service 信息的映射
        mcp_service_map = {service["id"]: service for service in mcp_services}

        # 按 pid 分组：{pid: [agent 配置的该 service 下的 mcpToolName 集合]}
        pid_to_tool_names: Dict[str, set] = {}
        for mcp_tool_info in agent_mcp_tools:
            pid = mcp_tool_info.get("pid")
            tool_name = mcp_tool_info.get("mcpToolName", "")
            if pid and tool_name:
                pid_to_tool_names.setdefault(pid, set()).add(tool_name)

        # 按 service 分组连接 MCP server，每个 service 只连接一次
        for pid, needed_tool_names in pid_to_tool_names.items():
            if pid not in mcp_service_map:
                logger.warning(f"MCP service pid={pid} 无效，跳过")
                continue

            mcp_service = mcp_service_map[pid]
            tools_from_service = await self._fetch_mcp_service_tools(mcp_service)

            if not tools_from_service:
                logger.warning(
                    f"MCP service {mcp_service.get('mcpServiceName', pid)} 未返回任何工具"
                )
                continue

            # 按名称匹配 agent 实际需要的 MCP 工具
            # tools_from_service 是 dict: {tool_name: langchain_tool}
            for tool_name in needed_tool_names:
                if tool_name in tools_from_service:
                    mcp_tools.append(tools_from_service[tool_name])
                    logger.info(f"成功匹配 MCP 工具: {tool_name}")
                else:
                    available = list(tools_from_service.keys())
                    logger.warning(
                        f"MCP 工具 {tool_name} 在 service 中未找到，"
                        f"可用工具: {available}"
                    )

        return mcp_tools

    async def _fetch_mcp_service_tools(
        self, mcp_service: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        连接一个 MCP service，获取该 service 提供的所有工具。

        每个 MCP service 只建立一次连接，返回工具名到 langchain 工具的映射。

        Args:
            mcp_service: MCP service 信息（包含 mcpUrl、mcpTransport、authToken 等）

        Returns:
            dict: {tool_name: langchain_tool}，连接失败返回空 dict
        """
        mcp_url = mcp_service.get("mcpUrl", "")
        mcp_transport = mcp_service.get("mcpTransport", "sse")
        auth_token = mcp_service.get("authToken", "")
        mcp_service_name = mcp_service.get("mcpServiceName", "mcp_server")

        if not mcp_url:
            logger.warning(f"MCP service {mcp_service_name} 缺少 mcpUrl")
            return {}

        try:
            from langchain_mcp_adapters.client import MultiServerMCPClient

            # 构建 MCP 服务器配置
            server_config = {
                "transport": mcp_transport,
                "url": mcp_url,
            }

            # 如果有认证令牌，添加到 headers
            if auth_token:
                server_config["headers"] = {"Authorization": f"Bearer {auth_token}"}

            # 创建 MCP 客户端，每个 service 只连接一次
            client = MultiServerMCPClient({mcp_service_name: server_config})

            # 异步获取该 service 提供的所有工具
            all_tools = await client.get_tools()

            # 构建工具名到工具实例的映射
            tools_map = {}
            for t in all_tools:
                tools_map[t.name] = t

            logger.info(
                f"MCP service {mcp_service_name} 提供了 {len(all_tools)} 个工具: "
                f"{list(tools_map.keys())}"
            )
            return tools_map

        except Exception as e:
            logger.error(
                f"连接 MCP service {mcp_service_name} 失败: {e}", exc_info=True
            )
            return {}

    def _create_web_search_tool(self) -> Optional[Any]:
        """创建联网搜索工具"""
        try:
            from tavily import TavilyClient
            import os

            api_key = os.getenv("TAVILY_API_KEY")
            if not api_key:
                logger.warning("未配置 TAVILY_API_KEY，无法启用联网搜索")
                return None

            tavily_client = TavilyClient(api_key=api_key)

            @tool(description="搜索互联网获取最新信息")
            def web_search(
                query: str,
                max_results: int = 5,
                topic: Literal["general", "news", "finance"] = "general",
                include_raw_content: bool = False,
            ) -> str:
                """执行网络搜索"""
                return tavily_client.search(
                    query,
                    max_results=max_results,
                    include_raw_content=include_raw_content,
                    topic=topic,
                )

            return web_search
        except ImportError:
            logger.warning("tavily-python 未安装，无法启用联网搜索")
            return None
        except Exception as e:
            logger.error(f"创建搜索工具失败: {e}")
            return None

    def _build_messages(
        self,
        prompt: str,
        images: Optional[List[str]] = None,
    ) -> List[Dict[str, Any]]:
        """
        构建消息列表

        Args:
            prompt: 用户输入
            images: 图片 base64 列表

        Returns:
            消息列表
        """
        if images:
            # 多模态消息格式
            content = [{"type": "text", "text": prompt}]
            for img_base64 in images:
                content.append(
                    {
                        "type": "image_url",
                        "image_url": {"url": f"data:image/jpeg;base64,{img_base64}"},
                    }
                )
            return [{"role": "user", "content": content}]
        else:
            return [{"role": "user", "content": prompt}]

    @staticmethod
    def _format_sse_text(
        content: str, agent_id: Optional[int] = None, agent_name: str = "主 Agent"
    ) -> str:
        """
        格式化文本 SSE 数据块
        格式: {"msg": "内容", "agentId": 1, "agentName": "Jarvis"}
        """
        chunk_obj = {"msg": content}
        if agent_id is not None:
            chunk_obj["agentId"] = agent_id
        chunk_obj["agentName"] = agent_name
        chunk_str = json.dumps(chunk_obj, ensure_ascii=False)
        return f"data: {chunk_str}\n\n"

    @staticmethod
    def _format_sse_tool_call(
        tool_name: str, agent_id: Optional[int] = None, agent_name: str = "主 Agent"
    ) -> str:
        """
        格式化工具调用开始 SSE 消息
        格式: {"tool_call": {"toolName": "工具名"}, "agentId": 1, "agentName": "Jarvis"}
        """
        chunk_obj = {"tool_call": {"toolName": tool_name}}
        if agent_id is not None:
            chunk_obj["agentId"] = agent_id
        chunk_obj["agentName"] = agent_name
        chunk_str = json.dumps(chunk_obj, ensure_ascii=False)
        return f"data: {chunk_str}\n\n"

    @staticmethod
    def _format_sse_tool_result(
        tool_name: str,
        result: str,
        agent_id: Optional[int] = None,
        agent_name: str = "主 Agent",
    ) -> str:
        """
        格式化工具调用完成 SSE 消息
        格式: {"tool_called": {"toolName": "工具名", "toolInvokeResult": "结果"}, "agentId": 1, "agentName": "Jarvis"}
        """
        chunk_obj = {"tool_called": {"toolName": tool_name, "toolInvokeResult": result}}
        if agent_id is not None:
            chunk_obj["agentId"] = agent_id
        chunk_obj["agentName"] = agent_name
        chunk_str = json.dumps(chunk_obj, ensure_ascii=False)
        return f"data: {chunk_str}\n\n"

    @staticmethod
    def _format_sse_error(error_msg: str) -> str:
        """格式化错误 SSE 消息"""
        chunk_obj = {"error": {"message": error_msg}}
        chunk_str = json.dumps(chunk_obj, ensure_ascii=False)
        return f"data: {chunk_str}\n\n"


# 全局服务实例
_chat_service: Optional[DeepAgentChatService] = None


def get_deep_agent_chat_service() -> DeepAgentChatService:
    """获取全局 DeepAgentChatService 实例"""
    global _chat_service
    if _chat_service is None:
        _chat_service = DeepAgentChatService()
    return _chat_service
