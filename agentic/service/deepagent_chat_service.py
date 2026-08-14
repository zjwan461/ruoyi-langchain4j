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
import logging
import re
from typing import Any, AsyncGenerator, Dict, List, Optional, Tuple, Literal

from langchain.tools import tool
from langgraph.checkpoint.memory import InMemorySaver
from deepagents import create_deep_agent

from grpc_client import get_grpc_client
from util.flow_tree_parser import TreeNode
from sandbox.sandbox_tool import run_python_code, check_sandbox_health
from logger_config import logger

# 全局记忆存储（进程级别共享），始终启用
_memory_store = InMemorySaver()


def get_memory_store() -> InMemorySaver:
    """获取全局记忆存储实例"""
    return _memory_store


class DeepAgentChatService:
    """
    DeepAgent 对话服务

    负责根据 TreeNode 结构构建嵌套的 deep agents，并处理对话请求。
    通过 create_deep_agent 的 subagents 参数实现父子 agent 嵌套。
    """

    def __init__(self):
        self.grpc_client = get_grpc_client()

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
            # 收集所有 agent 的 name -> (agentId, agentInfo) 映射
            agent_name_map = self._collect_agent_name_map(tree_node)

            # 构建根 agent（含子 agents）
            root_agent, root_agent_info = self._build_agent_from_tree_node(
                tree_node, web_search
            )

            if root_agent is None:
                yield self._format_sse_error("无法构建根 Agent")
                return

            # 根 agent 始终显示推理内容给前端
            show_reasoning = True

            # 当前 Agent 信息，初始为根 Agent
            current_agent_id = root_agent_info.get("id") if root_agent_info else None
            current_agent_name = (
                root_agent_info.get("name", "主 Agent")
                if root_agent_info
                else "主 Agent"
            )

            # 用于累积 task 工具的 args（解析 subagent_type）
            task_args_buffer = ""
            in_task_call = False

            # 构建输入消息
            messages = self._build_messages(prompt, images)

            # 配置记忆
            config = {"configurable": {"thread_id": session_id}}

            # 流式调用 agent
            async for msg_chunk, metadata in root_agent.astream(
                {"messages": messages},
                config=config,
                stream_mode="messages",
            ):
                # 获取 content_blocks
                content_blocks = getattr(msg_chunk, "content_blocks", [])

                # 检查是否进入 tools 节点（子 Agent 执行）
                langgraph_node = metadata.get("langgraph_node", "")

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

                        if tool_name == "task":
                            # 开始调用 task 工具（切换到子 Agent）
                            in_task_call = True
                            task_args_buffer = ""
                            logger.info("开始调用 task 工具，准备切换到子 Agent")

                        if in_task_call and tool_args:
                            # 累积 task 工具的 args
                            task_args_buffer += tool_args

                            # 尝试从片段中提取 subagent_type
                            match = re.search(
                                r'"subagent_type":\s*"([^"]*)"', task_args_buffer
                            )
                            if match:
                                subagent_type = match.group(1)
                                if (
                                    subagent_type
                                    and subagent_type != current_agent_name
                                ):
                                    # 从映射中查找 agentId
                                    agent_info = agent_name_map.get(subagent_type)
                                    if agent_info:
                                        current_agent_id = agent_info.get("id")
                                        current_agent_name = subagent_type
                                        logger.info(
                                            f"切换到子 Agent: {subagent_type} (ID: {current_agent_id})"
                                        )

                        if tool_name and tool_name != "task":
                            # 其他工具调用
                            logger.info(f"工具调用: {tool_name}")
                            if show_reasoning:
                                yield self._format_sse_tool_call(
                                    tool_name,
                                    agent_id=current_agent_id,
                                    agent_name=current_agent_name,
                                )

                # 检查是否从 tools 节点回到 model 节点（子 Agent 执行完成）
                if langgraph_node == "model" and in_task_call:
                    # 子 Agent 执行完成，恢复为主 Agent
                    in_task_call = False
                    task_args_buffer = ""
                    current_agent_id = (
                        root_agent_info.get("id") if root_agent_info else None
                    )
                    current_agent_name = (
                        root_agent_info.get("name", "主 Agent")
                        if root_agent_info
                        else "主 Agent"
                    )
                    logger.info(
                        f"子 Agent 执行完成，恢复为主 Agent: {current_agent_name}"
                    )

                # 处理工具执行结果（ToolMessage）
                if hasattr(msg_chunk, "name") and hasattr(msg_chunk, "content"):
                    if msg_chunk.name:  # ToolMessage
                        logger.info(f"工具 {msg_chunk.name} 执行完成")
                        if show_reasoning:
                            yield self._format_sse_tool_result(
                                msg_chunk.name,
                                msg_chunk.content,
                                agent_id=current_agent_id,
                                agent_name=current_agent_name,
                            )

            # 流结束标记
            yield "data: [DONE]\n\n"

        except Exception as e:
            logger.error(f"对话流处理异常: {e}", exc_info=True)
            yield self._format_sse_error(str(e))

    def _collect_agent_name_map(self, tree_node: TreeNode) -> Dict[str, Dict[str, Any]]:
        """
        遍历 TreeNode，收集所有 agent 的 name -> agentInfo 映射

        Args:
            tree_node: 树节点

        Returns:
            {agent_name: {id, name, ...}} 映射
        """
        name_map = {}

        def _collect(node: TreeNode):
            agent_id = node.node.agent_id
            if agent_id is not None:
                agent_info = self.grpc_client.get_ai_agent_with_tool_and_skill_by_id(
                    agent_id
                )
                if agent_info:
                    agent_name = agent_info.get("name", "")
                    if agent_name:
                        name_map[agent_name] = agent_info
            for child in node.children:
                _collect(child)

        _collect(tree_node)
        return name_map

    def _build_agent_from_tree_node(
        self, tree_node: TreeNode, web_search: bool = False
    ) -> Tuple[Optional[Any], Optional[Dict[str, Any]]]:
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
        tools = self._build_tools(agent_info, web_search)

        # 构建 system prompt
        system_prompt = agent_info.get("systemMessage", "")
        if not system_prompt:
            system_prompt = (
                f"你是一个名为 {agent_info.get('name', 'AI助手')} 的智能助手。"
            )

        # 递归构建子 agents 为 SubAgent spec dicts 或 CompiledSubAgent dicts
        subagents = []
        for child in tree_node.children:
            child_spec = self._build_subagent_spec(child, web_search)
            if child_spec is not None:
                subagents.append(child_spec)

        # 创建 deep agent，通过 subagents 参数实现嵌套
        try:
            agent = create_deep_agent(
                model=llm,
                tools=tools,
                system_prompt=system_prompt,
                subagents=subagents if subagents else None,
                checkpointer=_memory_store,
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

    def _build_subagent_spec(
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

        agent_name = agent_info.get("name", f"agent_{agent_id}")
        agent_description = agent_info.get("description", f"子助手 {agent_name}")

        if not tree_node.children:
            # 叶子节点：构建 SubAgent spec dict，由父级的 create_deep_agent 自动编译
            llm = self._build_llm(model_info, agent_info)
            if llm is None:
                return None
            tools = self._build_tools(agent_info, web_search)

            spec: Dict[str, Any] = {
                "name": agent_name,
                "description": agent_description,
                "system_prompt": agent_info.get("systemMessage", "")
                or f"你是一个名为 {agent_name} 的智能助手。",
                "tools": tools,
                "model": llm,
            }
            logger.info(f"构建 SubAgent spec (叶子): {agent_name} (ID: {agent_id})")
            return spec
        else:
            # 非叶子节点：先递归编译为 deep agent，再包装为 CompiledSubAgent
            child_agent, _ = self._build_agent_from_tree_node(tree_node, web_search)
            if child_agent is None:
                return None

            compiled_spec: Dict[str, Any] = {
                "name": agent_name,
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

    def _build_tools(
        self, agent_info: Dict[str, Any], web_search: bool = False
    ) -> List[Any]:
        """
        根据 agent 信息构建工具列表

        仅处理 sandbox 类型的工具，过滤掉 builtin 类型。

        Args:
            agent_info: Agent 配置信息
            web_search: 是否启用联网搜索

        Returns:
            langchain 工具列表
        """
        tools = []

        # 添加内置沙箱工具
        tools.append(run_python_code)
        tools.append(check_sandbox_health)

        # 处理 agent 配置的工具（仅 sandbox 类型）
        agent_tools = agent_info.get("agentTools", [])
        for tool_info in agent_tools:
            tool_type = tool_info.get("toolType", "")
            # 只处理 sandbox 类型的工具
            if tool_type == "sandbox":
                langchain_tool = self._convert_sandbox_tool_to_langchain(tool_info)
                if langchain_tool:
                    tools.append(langchain_tool)
            else:
                logger.debug(
                    f"跳过非 sandbox 类型工具: {tool_info.get('toolName', 'unknown')} (type={tool_type})"
                )

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

        if not code:
            logger.warning(f"sandbox 工具 {tool_name} 缺少代码")
            return None

        # 动态创建工具函数，通过闭包捕获变量
        def _make_sandbox_tool(name: str, desc: str, src_code: str):
            @tool(name=name, description=desc or f"执行 {name} 工具")
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
