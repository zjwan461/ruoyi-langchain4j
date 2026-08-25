from deepagents import create_deep_agent
from langchain.messages import AIMessageChunk
from langchain.tools import tool
from deepagents.backends import FilesystemBackend, CompositeBackend
from .constant import llm
from datetime import datetime

from sandbox.j_sandbox_backend import get_or_create_sandbox
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


backend = CompositeBackend(
    default=sandbox_backend,  # 原有业务后端不变
    routes={
        "/large_tool_results/": FilesystemBackend(
            root_dir="/data/deepagents_large_results"
        ),
        # "/skills/": FilesystemBackend(root_dir=local_skill_dir),
    },
)

agent = create_deep_agent(
    model=llm,
    skills=["/skills"],
    tools=[get_date_time],
    # backend=FilesystemBackend(root_dir=work_dir),
    backend=sandbox_backend,
    system_prompt="你是一个AI助手，你需要帮助用户解决实际问题。在有必要的时候调用skill完成工作。",
)


# resp = agent.invoke({"messages": [{"role": "user", "content": "hi"}]})
# print(resp)

user_prompt = "查询一下深圳明天的天气，并给出穿衣意见"
for chunk, _ in agent.stream(
    {"messages": [{"role": "user", "content": user_prompt}]},
    config={"configurable": {"thread_id": "1"}},
    stream_mode="messages",
):
    cb = chunk.content_blocks
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
