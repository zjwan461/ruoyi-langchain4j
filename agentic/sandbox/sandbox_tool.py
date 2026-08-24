"""
Sandbox SDK 的 LangChain Tool 封装。

将 dify-sandbox 的代码执行能力封装为 LangChain 可调用的 Tool，
供 Agent 在对话中安全执行 Python 代码。

典型用法：
    from sandbox_tool import (
        run_python_code, check_sandbox_health, get_sandbox_dependencies,
        run_sandbox_command, upload_sandbox_file, download_sandbox_file, delete_sandbox_file,
    )

    agent = create_agent(
        model=llm,
        tools=[
            run_python_code, check_sandbox_health, get_sandbox_dependencies,
            run_sandbox_command, upload_sandbox_file, download_sandbox_file, delete_sandbox_file,
        ],
        system_prompt="你是一个乐于助人的AI助手，在适当时候可以调用沙箱执行Python代码",
    )
"""

import logging
from langchain.tools import tool

from dify_sandbox import DifySandboxClient
import os

logger = logging.getLogger(__name__)

client: DifySandboxClient = None


def get_sandbox_sdk_client():
    global client
    if client is None:
        client = DifySandboxClient(
            base_url=os.getenv("SANDBOX_BASE_URL", "http://sandbox:8194"),
            api_key=os.getenv("SANDBOX_API_KEY", "dify-sandbox"),
            timeout=int(os.getenv("SANDBOX_TIMEOUT", "30")),
        )
    return client


@tool(description="在沙箱中执行python代码。")
def run_python_code(python_code: str) -> str:
    """在安全沙箱中执行 Python 代码并返回结果。
    适用于需要进行数学计算、数据处理、代码验证等场景。
    输入参数为完整的 Python 代码字符串，返回标准输出结果。
    如果执行失败，会返回错误信息。"""
    try:
        client = get_sandbox_sdk_client()
        result = client.run_python(code=python_code)
        # RunCodeResponse 包含 stdout, stderr, error, exit_code
        if result.is_success:
            return result.stdout if result.stdout else "(沙箱执行无输出)"
        else:
            error_msg = result.error or result.stderr or "未知错误"
            return f"代码执行失败 (exit_code={result.exit_code}): {error_msg}"
    except Exception as e:
        logger.error(f"sandbox tool execution error: {e}")
        return f"沙箱执行出错: {e}"


@tool(description="检查python沙箱服务健康状态")
def check_sandbox_health() -> str:
    """检查代码执行沙箱服务的健康状态。
    返回沙箱服务是否可用。"""
    try:
        client = get_sandbox_sdk_client()
        # 使用 SDK 提供的 health_check 方法
        is_healthy = client.health_check()
        return "沙箱服务正常运行" if is_healthy else "沙箱服务不可用"
    except Exception as e:
        return f"沙箱健康检查失败: {e}"


@tool(description="查询python沙箱当前已安装的依赖列表")
def get_sandbox_dependencies(language: str = "python3") -> str:
    """查询沙箱环境中已安装的依赖包列表。
    返回每个依赖的名称和版本信息。
    输入参数为编程语言，默认 python3，可选 nodejs。"""
    try:
        client = get_sandbox_sdk_client()
        # 使用 SDK 提供的 get_dependencies 方法
        dep_info = client.get_dependencies(language=language)
        if dep_info.dependencies:
            deps_str = "\n".join([f"- {dep}" for dep in dep_info.dependencies])
            return f"语言: {dep_info.language}\n已安装依赖:\n{deps_str}"
        else:
            return f"语言: {dep_info.language}\n未查询到已安装的依赖"
    except Exception as e:
        logger.error(f"sandbox get dependencies error: {e}")
        return f"查询沙箱依赖失败: {e}"


@tool(description="在沙箱中执行shell命令。")
def run_sandbox_command(command: str, args: str = "") -> str:
    """在沙箱环境中执行 shell 命令并返回结果。
    命令在沙箱的上传目录中执行，危险命令（如 rm、shutdown）会被拦截。
    输入参数 command 为要执行的命令名称（如 "ls"、"python3"），
    args 为空格分隔的参数列表（如 "-l" 或 "script.py"），可为空。
    返回命令的标准输出结果，如果执行失败则返回错误信息。"""
    try:
        client = get_sandbox_sdk_client()
        # 将空格分隔的参数字符串转为列表
        arg_list = args.split() if args.strip() else []
        result = client.run_command(command=command, args=arg_list)
        # RunCommandResponse 包含 stdout, stderr, exit_code
        if result.is_success:
            return result.stdout if result.stdout else "(命令执行无输出)"
        else:
            error_msg = result.stderr or "未知错误"
            return f"命令执行失败 (exit_code={result.exit_code}): {error_msg}"
    except Exception as e:
        logger.error(f"sandbox run_command error: {e}")
        return f"沙箱命令执行出错: {e}"


@tool(description="上传文件到沙箱环境。")
def upload_sandbox_file(file_path: str) -> str:
    """将本地文件上传到沙箱环境中。
    上传后可在沙箱中通过文件名访问该文件（如执行脚本、读取数据等）。
    输入参数 file_path 为本地文件的完整路径。
    返回上传结果，包含沙箱中分配的文件名和文件大小。"""
    try:
        client = get_sandbox_sdk_client()
        result = client.upload_file(file=file_path)
        return f"文件上传成功: 沙箱文件名={result.filename}, 大小={result.size} 字节"
    except FileNotFoundError:
        return f"文件不存在: {file_path}"
    except Exception as e:
        logger.error(f"sandbox upload_file error: {e}")
        return f"文件上传失败: {e}"


@tool(description="从沙箱环境下载文件。")
def download_sandbox_file(filename: str, save_path: str = "") -> str:
    """从沙箱环境中下载文件到本地。
    输入参数 filename 为沙箱中的文件名。
    save_path 为本地保存路径，若为空则返回文件内容（文本文件直接返回内容，二进制文件返回 base64 编码）。
    若指定 save_path，返回保存结果信息。"""
    import base64
    try:
        client = get_sandbox_sdk_client()
        if save_path:
            saved = client.download_file(filename=filename, save_path=save_path)
            return f"文件下载成功: 已保存到 {saved}"
        else:
            content = client.download_file(filename=filename)
            # 尝试作为文本解码
            try:
                text = content.decode("utf-8")
                return text if text else "(文件内容为空)"
            except UnicodeDecodeError:
                # 二进制文件，返回 base64
                b64 = base64.b64encode(content).decode("ascii")
                return f"(二进制文件, {len(content)} 字节, base64):\n{b64}"
    except Exception as e:
        logger.error(f"sandbox download_file error: {e}")
        return f"文件下载失败: {e}"


@tool(description="删除沙箱环境中的文件。")
def delete_sandbox_file(filename: str) -> str:
    """删除沙箱环境中的指定文件。
    输入参数 filename 为沙箱中的文件名。
    返回删除结果信息。"""
    try:
        client = get_sandbox_sdk_client()
        client.delete_file(filename=filename)
        return f"文件 '{filename}' 删除成功"
    except Exception as e:
        logger.error(f"sandbox delete_file error: {e}")
        return f"文件删除失败: {e}"
