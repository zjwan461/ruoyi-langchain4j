import os
from typing import Literal
from tavily import TavilyClient
from deepagents import create_deep_agent
from constant import llm

tavily_client = TavilyClient(api_key="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")


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


# 系统提示，引导Agent成为专家级研究员
research_instructions = """您是一位专家级研究员。您的工作是进行彻底的研究，然后撰写一份精美的报告。

您可以访问互联网搜索工具，作为收集信息的主要方式。

## `internet_search`

使用此工具对给定查询进行互联网搜索。您可以指定要返回的最大结果数、主题以及是否应包含原始内容。
"""

agent = create_deep_agent(
    model=llm, tools=[internet_search], system_prompt=research_instructions
)


result = agent.invoke({"messages": [{"role": "user", "content": "什么是 langgraph？"}]})

# 打印Agent的响应
print(result["messages"][-1].content)
