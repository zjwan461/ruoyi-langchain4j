"""Deep Agent graph for deployment."""

from __future__ import annotations

import contextlib
import os
from datetime import datetime, timezone

from langchain_core.runnables import RunnableConfig

from deepagents import create_deep_agent
from langchain_core.tools import tool
from langgraph_sdk.runtime import ServerRuntime

from deepagents.backends import FilesystemBackend

from langchain_openai import ChatOpenAI
from .sandbox import (
    get_or_create_sandbox,
)

DEFAULT_MODEL = os.getenv("DEEP_AGENT_MODEL", "qwen3.7-plus")

llm = ChatOpenAI(
    base_url="https://dashscope-intl.aliyuncs.com/compatible-mode/v1",
    api_key="sk-0543377485b74778b3728e250df4e7d1",
    model=DEFAULT_MODEL,
    extra_body={"chat_template_kwargs": {"enable_thinking": False}},
)

SYSTEM_PROMPT = """
You are a deep agent.

Workflow:
1. Write and maintain a todo list for non-trivial requests.
2. Delegate focused fact-finding to subagents when helpful.
3. Store intermediate drafts in files when the task is long.
4. Before finalizing, critique your work for risks, gaps, and missing constraints.
5. Return concise, actionable output.

- Prefer concrete evidence over assumptions.
- State unresolved uncertainty explicitly.
- Keep output compact unless the user asks for depth.
""".strip()


@tool
def utc_now() -> str:
    """Return the current UTC timestamp in ISO format."""
    return datetime.now(tz=timezone.utc).isoformat()


SUBAGENTS = [
    {
        "name": "researcher",
        "description": "Use for evidence collection and source-grounded fact finding.",
        "system_prompt": (
            "You are a focused researcher. Gather evidence, list assumptions, and "
            "report contradictions clearly."
        ),
        "tools": [utc_now],
    },
    {
        "name": "critic",
        "description": "Use for adversarial review of drafts and plans.",
        "system_prompt": (
            "You are a critical reviewer. Find weak logic, untested assumptions, and "
            "missing constraints."
        ),
        "tools": [utc_now],
    },
]


def _build_agent(backend=None):
    return create_deep_agent(
        model=llm,
        tools=[utc_now],
        backend=backend,
        system_prompt=SYSTEM_PROMPT,
        subagents=SUBAGENTS,
        skills=["/skills"],
        # You can disable these if you want to run without interrupts
        interrupt_on={"execute": True, "write_file": True},
        name="deep_agent",
    )


RO_AGENT = _build_agent()


# backend = FilesystemBackend(root_dir=".")


@contextlib.asynccontextmanager
async def get_agent(config: RunnableConfig, runtime: ServerRuntime):
    ert = runtime.execution_runtime
    if ert:
        thread_id = config.get("configurable", {}).get("thread_id", "default")
        print(f"thread_id={thread_id}")
        session_id = "default"
        backend = await get_or_create_sandbox(session_id)
        yield _build_agent(backend=backend)
    else:
        yield RO_AGENT
