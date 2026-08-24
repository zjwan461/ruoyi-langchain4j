from langchain.agents import create_agent
from deepagents import create_deep_agent
from langchain.messages import AIMessageChunk
from langchain.tools import tool
from deepagents.backends import FilesystemBackend, CompositeBackend
from .constant import llm
from datetime import datetime
from sandbox.sandbox_tool import (
    run_python_code,
    check_sandbox_health,
    run_sandbox_command,
    upload_sandbox_file,
)
from sandbox.dify_sandbox_backend import get_or_create_sandbox
import os

work_dir = os.path.join(os.path.dirname(__file__), "../..")
print(f"工作目录：{work_dir}")

skill_dir = work_dir + "/skills"

# 上传 skills 目录下的所有文件到 sandbox（保持目录结构）
from sandbox.sandbox_tool import get_sandbox_sdk_client

UPLOAD_DIR = "/data/uploads"
SKILLS_DIR = f"{UPLOAD_DIR}/skills"


def upload_skills_to_sandbox():
    """上传 skills 目录下的所有文件到 sandbox，保持原有目录结构和原始文件名"""
    client = get_sandbox_sdk_client()
    uploaded_files = []

    # 第一步：创建 skills 根目录
    client.run_command("mkdir", ["-p", SKILLS_DIR])

    # 第二步：上传所有文件到 sandbox 的 /data/uploads/ 目录
    # 注意：sandbox 会为每个上传的文件分配一个随机文件名，需要记录返回的实际文件名
    for root, dirs, files in os.walk(skill_dir):
        for file in files:
            file_path = os.path.join(root, file)
            rel_path = os.path.relpath(file_path, skill_dir)
            # 将路径分隔符统一为 /
            rel_path = rel_path.replace(os.sep, "/")
            try:
                result = client.upload_file(file_path, filename=file)
                # result.filename 是 sandbox 分配的实际文件名（随机字符串）
                uploaded_files.append((result.filename, file, rel_path))
                print(f"已上传: {rel_path} -> sandbox文件名: {result.filename}")
            except Exception as e:
                print(f"上传失败 {rel_path}: {e}")

    # 第三步：创建目录结构并移动文件到正确位置，同时恢复原始文件名
    for sandbox_filename, original_filename, rel_path in uploaded_files:
        # 获取文件在 sandbox 中的相对目录
        rel_dir = os.path.dirname(rel_path)
        if rel_dir:
            # 创建子目录（放在 skills 根目录下）
            target_dir = f"{SKILLS_DIR}/{rel_dir}"
            client.run_command("mkdir", ["-p", target_dir])
            # 移动文件到目标目录，同时恢复原始文件名
            client.run_command(
                "mv",
                [
                    f"{UPLOAD_DIR}/{sandbox_filename}",
                    f"{target_dir}/{original_filename}",
                ],
            )
            print(f"已移动到: {target_dir}/{original_filename}")
        else:
            # 文件在根目录，移动到 skills 根目录并恢复原始文件名
            client.run_command(
                "mv",
                [
                    f"{UPLOAD_DIR}/{sandbox_filename}",
                    f"{SKILLS_DIR}/{original_filename}",
                ],
            )
            print(f"已移动到: {SKILLS_DIR}/{original_filename}")


upload_skills_to_sandbox()


@tool(description="获取当前时间日期")
def get_date_time():
    now = datetime.now()
    print(now)
    # 输出示例：2026-08-21 15:30:22.123456

    # 转字符串格式化
    return now.strftime("%Y-%m-%d %H:%M:%S")


backend = CompositeBackend(
    default=get_or_create_sandbox(),  # 原有业务后端不变
    routes={
        "/large_tool_results/": FilesystemBackend(
            root_dir="/data/deepagents_large_results"
        ),
    },
)

agent = create_deep_agent(
    model=llm,
    skills=["/data/uploads/skills"],
    tools=[get_date_time],
    # backend=FilesystemBackend(root_dir=work_dir),
    backend=backend,
    system_prompt="你是一个AI助手，你需要帮助用户解决实际问题。在有必要的时候调用skill完成工作。工作目录在/data/uploads下，你无权限访问其它任何目录。",
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
