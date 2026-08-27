from langchain.agents import create_agent
from langchain.messages import AIMessageChunk, ToolMessage
from langchain.tools import tool

from .constant import llm

@tool
def get_weather(city: str) -> str:
    """获取指定城市的天气。"""
    return f"{city}总是阳光明媚！"


agent = create_agent(
    model=llm,
    tools=[get_weather],
    system_prompt="你是一个乐于助人的AI助手，在适当的时候调用工具",
)

# result = agent.invoke({"messages": [{"role": "user", "content": "旧金山的天气怎么样"}]})
# print(result.get("messages"))

for chunk in agent.stream(
    {"messages": [{"role": "user", "content": "帮我写一个python抽奖函数，并执行一次看看我有没有中奖。你可以参考双色球的玩法。另外在最后请给我你写的代码。"}]},
    stream_mode="messages",
):
    msg_chunk = chunk[0]
    # print(msg_chunk)
    if isinstance(msg_chunk, AIMessageChunk):
        if msg_chunk.tool_calls:
            for tc in msg_chunk.tool_calls:
                if tc["name"]:
                    print(f"正在调用工具{tc['name']}")
                else:
                    print(f"工具参数：{tc['args']}")
        else:
            reason_chunk = msg_chunk.additional_kwargs.get("reasoning_content")
            if reason_chunk:
                print(
                    reason_chunk, end="", flush=True
                )
            print(msg_chunk.content, end="", flush=True)
    elif isinstance(msg_chunk, ToolMessage):
        print(f"工具{msg_chunk.name}执行结果：{msg_chunk.content}")

    # latest_message = chunk["messages"][-1]
    # if latest_message.content:
    #     print(f"智能体：{latest_message.content}")
    # elif latest_message.tool_calls:
    #     print(f"正在调用工具：{[tc['name'] for tc in latest_message.tool_calls]}")
