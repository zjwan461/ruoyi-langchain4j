from langchain.agents import create_agent
from langchain.tools import tool
from langgraph.checkpoint.memory import InMemorySaver
from constant import llm
from langchain_mcp_adapters.client import MultiServerMCPClient


async def chat():

    @tool
    def get_weather(city: str) -> str:
        """获取指定城市的天气。"""
        return f"{city}总是阳光明媚！"

    client = MultiServerMCPClient(
        {
            "websearch": {
                "transport": "sse",  # 基于 HTTP 的远程服务器
                # 确保您在 8000 端口启动了您的天气服务器
                "url": "https://dashscope.aliyuncs.com/api/v1/mcps/WebSearch/sse",
                "headers": {
                    "Authorization": "Bearer sk-93d67e61482e4d29818ca45a23c69d98"
                },
            }
        }
    )

    tools = await client.get_tools()
    # print(tools)

    tools.append(get_weather)

    agent = create_agent(
        model=llm,
        tools=tools,
        system_prompt="你是一个乐于助人的AI助手，在适当的时候调用工具",
        checkpointer=InMemorySaver(),
    )

    result = await agent.ainvoke(
        {
            "messages": [
                {
                    "role": "user",
                    "content": "帮我查一下ruoyi-langchain4j。 一个开源gitee上的项目。然后帮我查一下深圳的天气。",
                }
            ]
        },
        config={"configurable": {"thread_id": "1"}},
    )

    print(result)


if __name__ == "__main__":
    import asyncio

    asyncio.run(chat())
