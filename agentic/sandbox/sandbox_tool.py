"""
Sandbox SDK 的 LangChain Tool 封装。

将 dify-sandbox 的代码执行能力封装为 LangChain 可调用的 Tool，
供 Agent 在对话中安全执行 Python 代码。

典型用法：
    from sandbox_tool import run_python_code_tool, check_sandbox_health_tool

    agent = create_agent(
        model=llm,
        tools=[run_python_code_tool, check_sandbox_health_tool],
        system_prompt="你是一个乐于助人的AI助手，在适当时候可以调用沙箱执行Python代码",
    )
"""
import logging
from langchain.tools import tool

from .sandbox_sdk import get_sandbox_sdk

logger = logging.getLogger(__name__)


@tool(description="执行python代码")
def run_python_code(python_code: str) -> str:
    """在安全沙箱中执行 Python 代码并返回结果。
    适用于需要进行数学计算、数据处理、代码验证等场景。
    输入参数为完整的 Python 代码字符串，返回标准输出结果。
    如果执行失败，会返回错误信息。"""
    try:
        sdk = get_sandbox_sdk()
        result = sdk.run_python_and_get_stdout(python_code)
        return result if result else "(沙箱执行无输出)"
    except Exception as e:
        logger.error(f"sandbox tool execution error: {e}")
        return f"沙箱执行出错: {e}"


@tool(description="检查python沙箱环境监控状态")
def check_sandbox_health() -> str:
    """检查代码执行沙箱服务的健康状态。
    返回沙箱服务是否可用。"""
    try:
        sdk = get_sandbox_sdk()
        ok = sdk.health()
        return "沙箱服务正常运行" if ok else "沙箱服务不可用"
    except Exception as e:
        return f"沙箱健康检查失败: {e}"
