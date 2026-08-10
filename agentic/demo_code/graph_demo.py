from typing import Annotated
from langgraph.graph import StateGraph, START, END
from pydantic import BaseModel, Field
from typing_extensions import TypedDict, Literal
from operator import add
from constant import llm
import asyncio
from langchain.messages import AIMessageChunk


# 定义图状态
class State(TypedDict):
    joke: str
    topic: str
    punchline: str
    needs_improvement: bool


# 定义用于评估笑话的结构化输出模型
class PunchlineEvaluation(BaseModel):
    needs_improvement: bool = Field(
        description="判断笑话是否需要改进。如果笑点明显或平庸，返回 true。"
    )


# 使用结构化输出增强 LLM
evaluator = llm.with_structured_output(PunchlineEvaluation)

# 节点


def generate_joke(state: State):
    """生成一个关于给定主题的笑话"""
    joke = llm.invoke(f"写一个关于 {state['topic']} 的笑话")
    return {"joke": joke.content}


def check_punchline(state: State):
    """检查笑话是否需要改进"""
    evaluation = evaluator.invoke(f"评估这个笑话的笑点: {state['joke']}")
    return {"needs_improvement": evaluation.needs_improvement}


def improve_joke(state: State):
    """改进笑话"""
    improved_joke = llm.invoke(f"改进这个笑话，使其更有趣: {state['joke']}")
    return {"joke": improved_joke.content}


def polish_joke(state: State):
    """润色笑话，确保质量"""
    polished_joke = llm.invoke(f"润色这个笑话，使其更加完美: {state['joke']}")
    return {"joke": polished_joke.content}


# 条件边函数，根据笑话是否需要改进来决定路由


def should_improve_joke(state: State):
    """根据笑话是否需要改进来决定路由"""
    if state["needs_improvement"]:
        return "improve_joke"
    else:
        return "polish_joke"


# 构建工作流
workflow_builder = StateGraph(State)

# 添加节点
workflow_builder.add_node("generate_joke", generate_joke)
workflow_builder.add_node("check_punchline", check_punchline)
workflow_builder.add_node("improve_joke", improve_joke)
workflow_builder.add_node("polish_joke", polish_joke)

# 添加边来连接节点
workflow_builder.add_edge(START, "generate_joke")
workflow_builder.add_edge("generate_joke", "check_punchline")
workflow_builder.add_conditional_edges(
    "check_punchline",
    should_improve_joke,
    {  # 由 should_improve_joke 返回的值 : 要访问的下一个节点的名称
        "improve_joke": "improve_joke",
        "polish_joke": "polish_joke",
    },
)
workflow_builder.add_edge("improve_joke", "polish_joke")
workflow_builder.add_edge("polish_joke", END)

# 编译工作流
workflow = workflow_builder.compile()

# 显示工作流
# from IPython.display import display, Image

# display(Image(workflow.get_graph().draw_mermaid_png()))
with open("./workflow.png", "w+b") as f:
    f.write(workflow.get_graph().draw_mermaid_png())

# blocking 调用
# state = workflow.invoke({"topic": "编程"})

# print(state["joke"])


async def run_workflow_stream():
    async for chunk, params in workflow.astream(
        input={"topic": "编程"}, stream_mode="messages"
    ):
        if isinstance(chunk, AIMessageChunk):
            print(chunk.content, end="", flush=True)
        else:
            print(type(chunk))
            print(chunk)


if __name__ == "__main__":
    asyncio.run(run_workflow_stream())
