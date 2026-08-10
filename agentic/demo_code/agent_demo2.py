from langchain.messages import HumanMessage
from langchain.agents import create_agent
from constant import llm
import time

from langchain.tools import tool, ToolRuntime
from langgraph.checkpoint.memory import InMemorySaver  # [!code highlight]

"""tool call and memory
"""


@tool(description="天气查询工具")
def get_weather(city: str, runtime: ToolRuntime) -> str:
    """Get weather for a given city."""
    writer = runtime.stream_writer

    # Stream custom updates as the tool executes
    writer(f"Looking up data for city: {city}")
    time.sleep(3)
    writer(f"Acquired data for city: {city}")

    return f"It's always sunny in {city}!"


agent = create_agent(
    model=llm,
    tools=[get_weather],
    system_prompt="你是一个乐于助人的AI助手，在适当的时候调用工具",
    checkpointer=InMemorySaver(),
)

# for chunk in agent.stream(
#     {"messages": [{"role": "user", "content": "深圳的天气怎么样? 给点出行建议。"}]},
#     config={"configurable": {"thread_id": "1"}},
#     stream_mode="updates",
# ):
#     # print(chunk)
#     for step, data in chunk.items():
#         print(f"step: {step}")
#         print(f"content: {data['messages'][-1].content_blocks}")


# for token, metadata in agent.stream(  # [!code highlight]
#     {"messages": [{"role": "user", "content": "What is the weather in 深圳?"}]},
#     config={"configurable": {"thread_id": "1"}},
#     stream_mode=["messages"],
# ):
#     print(f"node: {metadata['langgraph_node']}")
#     print(f"content: {token.content_blocks}")
#     print("\n")


for stream_mode, chunk in agent.stream(  # [!code highlight]
    {"messages": [{"role": "user", "content": "What is the weather in SF?"}]},
    config={"configurable": {"thread_id": "1"}},
    stream_mode=["updates", "custom"],
):
    print(f"stream_mode: {stream_mode}")
    print(f"content: {chunk}")
    print("\n")
