from deepagents import create_deep_agent
from langchain.messages import AIMessageChunk
from langchain.tools import tool
from deepagents.backends import FilesystemBackend, CompositeBackend
from .constant import llm
from datetime import datetime

from agentic.sandbox.python_sandbox_backend import get_or_create_sandbox
import os

work_dir = os.path.join(os.path.dirname(__file__), "../..")
print(f"工作目录：{work_dir}")

local_skill_dir = work_dir + "/skills"

# 上传 skills 目录下的所有文件到 sandbox（保持目录结构）
UPLOAD_DIR = ""
SKILLS_DIR = f"/skills"


sandbox_backend = get_or_create_sandbox(session_id="123")


def upload_skills_to_sandbox():
    client = sandbox_backend.sandbox_client
    # 创建 sandbox 会话
    session_id = sandbox_backend.id
    try:
        # 递归遍历 local_skill_dir 下的所有文件
        for root, dirs, files in os.walk(local_skill_dir):
            for file in files:
                file_path = os.path.join(root, file)
                # 计算相对于 local_skill_dir 的相对路径
                rel_path = os.path.relpath(file_path, local_skill_dir)
                # 构建 sandbox 中的目标路径（保持 /skills 前缀）
                container_path = f"{SKILLS_DIR}/{rel_path}".replace("\\", "/")
                # 提取目标目录路径
                container_dir = os.path.dirname(container_path).replace("\\", "/")
                # 在 sandbox 中创建目录（如果不存在）
                client.exec_shell(session_id, f"mkdir -p {container_dir}")
                # 读取文件内容并上传
                with open(file_path, "rb") as f:
                    data = f.read()
                client.upload_file(session_id, container_path, data)
                print(f"已上传: {file_path} -> {container_path}")
    finally:
        client.close()


upload_skills_to_sandbox()


@tool(description="获取当前时间日期")
def get_date_time():
    now = datetime.now()
    print(now)
    # 输出示例：2026-08-21 15:30:22.123456

    # 转字符串格式化
    return now.strftime("%Y-%m-%d %H:%M:%S")


from typing import Literal
from tavily import TavilyClient

tavily_client = TavilyClient(api_key="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")


@tool
def internet_search(
    query: str,
    max_results: int = 5,
    topic: Literal["general", "news", "finance"] = "general",
    include_raw_content: bool = False,
):
    """运行网络搜索"""
    return tavily_client.search(
        query,
        max_results=max_results,
        include_raw_content=include_raw_content,
        topic=topic,
    )


# 在需要多场景backend需要使用
# backend = CompositeBackend(
#     default=sandbox_backend,  # 原有业务后端不变
#     routes={
#         "/large_tool_results/": FilesystemBackend(
#             root_dir="/data/deepagents_large_results"
#         ),
#         # "/skills/": FilesystemBackend(root_dir=local_skill_dir),
#     },
# )

from langgraph.checkpoint.memory import MemorySaver

agent = create_deep_agent(
    name="Jarvis",
    model=llm,
    # skills=["/skills"],
    # tools=[get_date_time],
    subagents=[
        {
            "name": "websearch_agent",
            "description": "进行网络搜索，总结归纳",
            "system_prompt": "你是一个网络搜索专家，你负责调用工具从互联网上搜索答案，汇总后结合问题进行回答。如需获取天气，请调用对应的skill来说实现",
            "tools": [internet_search, get_date_time],
            "skills": ["/skills"],
        }
    ],
    backend=sandbox_backend,
    checkpointer=MemorySaver(),
    system_prompt="你是一个主管Agent助手，你需要对任务进行拆解，分为多个子任务调用子Agent来处理。最终给我最终答案。",
)


# resp = agent.invoke({"messages": [{"role": "user", "content": "hi"}]})
# print(resp)

current_agent: str = ""
user_prompt = "查询一下深圳明天的天气，并给出穿衣意见"
for _, chunk in agent.stream(
    {"messages": [{"role": "user", "content": user_prompt}]},
    config={"configurable": {"thread_id": "1"}},
    stream_mode="messages",
    subgraphs=True,
):
    msg_chunk = chunk[0]
    metadata = chunk[1]
    cb = msg_chunk.content_blocks
    lc_agent_name = metadata.get("lc_agent_name", "")
    if current_agent != lc_agent_name:
        current_agent = lc_agent_name
        print(f"\nAgent: {current_agent}开始发言：")

    if cb:
        item = cb[0]
        if item.get("type") == "text":
            print(item.get("text"), end="")
        elif item.get("type") == "tool_call_chunk":
            tool_call_id = item.get("id")
            tool_name = item.get("name")
            if tool_call_id:
                print(f"正在调用工具：{tool_name}")
            # else:
            #     print(item)

sandbox_backend.sandbox_client.delete_session(sandbox_backend.id)
