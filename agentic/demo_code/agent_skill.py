from langchain.agents import create_agent
from deepagents import create_deep_agent
from langchain.messages import AIMessageChunk
from langchain.tools import tool
from deepagents.backends import FilesystemBackend
from .constant import llm
from datetime import datetime
from sandbox.sandbox_tool import run_python_code, check_sandbox_health
import os

work_dir = os.path.join(os.path.dirname(__file__), "../..")
print(f"工作目录：{work_dir}")

# work_dir = r"D:/workspaces/ruoyi-langchain4j"


@tool(description="获取当前时间日期")
def get_date_time():
    now = datetime.now()
    print(now)
    # 输出示例：2026-08-21 15:30:22.123456

    # 转字符串格式化
    return now.strftime("%Y-%m-%d %H:%M:%S")


agent = create_deep_agent(
    model=llm,
    skills=["./skills/"],
    tools=[get_date_time, run_python_code, check_sandbox_health],
    backend=FilesystemBackend(root_dir=work_dir),
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
