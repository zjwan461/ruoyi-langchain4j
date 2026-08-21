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


@tool(description="在沙箱中执行python代码。沙箱中并不会保存任何代码、文件，只能将代码当成参数传入执行。")
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


@tool(description="查询python沙箱当前已安装的依赖列表")
def get_sandbox_dependencies() -> str:
    """查询沙箱环境中已安装的依赖包列表。
    返回每个依赖的名称和版本信息。
    输入参数为编程语言，默认 python3。"""
    try:
        sdk = get_sandbox_sdk()
        deps = sdk.get_dependencies()
        if not deps:
            return "当前沙箱环境无已安装的依赖"
        lines = [
            f"- {d.get('name', 'unknown')}: {d.get('version', '') or '未指定版本'}"
            for d in deps
        ]
        return f"沙箱依赖列表（共 {len(deps)} 个）:\n" + "\n".join(lines)
    except Exception as e:
        logger.error(f"sandbox get dependencies error: {e}")
        return f"查询沙箱依赖失败: {e}"


@tool(description="更新python沙箱环境中的依赖包")
def update_sandbox_dependencies(dependencies: str) -> str:
    """更新沙箱环境中的依赖包。
    输入参数为依赖列表字符串，多个依赖用逗号分隔，
    格式支持 '包名==版本' 或 '包名'，例如 'requests==2.31.0,pillow'。
    language 参数指定编程语言，默认 python3。"""
    try:
        dep_list = [d.strip() for d in dependencies.split(",") if d.strip()]
        if not dep_list:
            return "依赖列表为空，请提供至少一个依赖"
        sdk = get_sandbox_sdk()
        success = sdk.update_dependencies(dep_list)
        if success:
            return f"依赖更新成功: {', '.join(dep_list)}"
        else:
            return f"依赖更新失败: {', '.join(dep_list)}"
    except Exception as e:
        logger.error(f"sandbox update dependencies error: {e}")
        return f"更新沙箱依赖失败: {e}"
