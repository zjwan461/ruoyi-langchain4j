"""LangGraph single-node graph template.

Returns a predefined response. Replace logic and configuration as needed.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Dict

from langgraph.graph import StateGraph
from langgraph.runtime import Runtime
from typing_extensions import TypedDict


class Context(TypedDict):
    """Context parameters for the agent.

    Set these when creating assistants OR when invoking the graph.
    See: https://langchain-ai.github.io/langgraph/cloud/how-tos/configuration_cloud/
    """

    my_configurable_param: str


@dataclass
class State:
    """Input state for the agent.

    Defines the initial structure of incoming data.
    See: https://langchain-ai.github.io/langgraph/concepts/low_level/#state
    """

    style: str = "幽默"
    prompt: str = ""
    response: str = ""


from langchain_openai import ChatOpenAI

llm = ChatOpenAI(
    base_url="https://dashscope-intl.aliyuncs.com/compatible-mode/v1",
    api_key="sk-0543377485b74778b3728e250df4e7d1",
    model="qwen3.7-plus",
    extra_body={"chat_template_kwargs": {"enable_thinking": False}},
)


async def call_model(state: State, runtime: Runtime[Context]) -> Dict[str, Any]:
    """调用 LLM 模型，根据 prompt 和 style 生成回复。

    Can use runtime context to alter behavior.
    """
    prompt = state.prompt
    style = state.style

    # 构建带风格要求的提示词
    system_message = f"请用{style}的风格回答用户的问题。"
    messages = [
        ("system", system_message),
        ("human", prompt),
    ]

    # 调用模型获取响应
    result = await llm.ainvoke(messages)
    response_text = result.content

    return {
        "response": response_text,
    }


# Define the graph
graph = (
    StateGraph(State, context_schema=Context)
    .add_node(call_model)
    .add_edge("__start__", "call_model")
    .compile(name="New Graph")
)
