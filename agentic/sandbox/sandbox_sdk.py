"""
dify-sandbox 轻量级 SDK。

dify-sandbox 是 Dify 团队开源的代码执行沙箱（基于 gVisor + seccomp），
提供 HTTP API 接收 Python 代码并安全执行。本 SDK 负责与该服务进行交互。

对应 docker-compose 中的 sandbox 服务（端口 8194）。
配置通过环境变量或默认值注入：
- SANDBOX_BASE_URL = http://sandbox:8194
- SANDBOX_API_KEY = dify-sandbox
- SANDBOX_TIMEOUT = 30 秒

典型用法：
    sandbox_sdk = SandboxSdk()
    
    # 1) 直接运行 Python 代码并获取 stdout
    output = sandbox_sdk.run_python_and_get_stdout("print(1 + 1)")
    
    # 2) 获取完整响应（含 stdout / stderr / error）
    resp = sandbox_sdk.run_python("import sys; print(sys.version)")
    
    # 3) 健康检查
    ok = sandbox_sdk.health()

对应接口：
- GET  /health           — 健康检查
- POST /v1/sandbox/run   — 运行代码
"""
import os
import logging
from typing import Optional

import requests

from .sandbox_request import SandboxRequest
from .sandbox_response import SandboxResponse

logger = logging.getLogger(__name__)


class SandboxSdk:
    """dify-sandbox 轻量级 SDK"""
    
    # 运行代码路径（不含域名）
    PATH_RUN = "/v1/sandbox/run"
    
    # 健康检查路径（不含域名）
    PATH_HEALTH = "/health"

    # 依赖查询
    PATH_DEP = "/v1/sandbox/dependencies"

    # 更新依赖
    PATH_UPDATE_DEP = "/v1/sandbox/dependencies/update"
    
    # dify-sandbox 鉴权头名称
    HEADER_API_KEY = "X-Api-Key"
    
    # dify-sandbox 业务失败时的兜底 baseUrl（仅当环境变量未配置时使用）
    DEFAULT_BASE_URL = "http://sandbox:8194"
    
    # dify-sandbox 业务失败时的兜底 apiKey
    DEFAULT_API_KEY = "dify-sandbox"
    
    # dify-sandbox 业务失败时的兜底超时秒数
    DEFAULT_TIMEOUT_SECONDS = 30
    
    def __init__(
        self,
        base_url: Optional[str] = None,
        api_key: Optional[str] = None,
        timeout: Optional[int] = None
    ):
        """
        初始化 SandboxSdk
        
        Args:
            base_url: sandbox 服务的基础 URL，默认从环境变量 SANDBOX_BASE_URL 读取，
                     若未配置则使用 DEFAULT_BASE_URL
            api_key: sandbox 服务的 API Key，默认从环境变量 SANDBOX_API_KEY 读取，
                    若未配置则使用 DEFAULT_API_KEY
            timeout: 请求超时时间（秒），默认从环境变量 SANDBOX_TIMEOUT 读取，
                    若未配置则使用 DEFAULT_TIMEOUT_SECONDS
        """
        self.base_url = base_url or os.getenv("SANDBOX_BASE_URL", self.DEFAULT_BASE_URL)
        self.api_key = api_key or os.getenv("SANDBOX_API_KEY", self.DEFAULT_API_KEY)
        self.timeout = timeout or int(os.getenv("SANDBOX_TIMEOUT", str(self.DEFAULT_TIMEOUT_SECONDS)))
    
    def health(self) -> bool:
        """
        健康检查。直接请求 GET /health，状态码 200 即视为健康。
        
        Returns:
            True-服务可用；False-服务不可用或网络异常
        """
        url = self._build_url(self.PATH_HEALTH)
        try:
            response = requests.get(
                url,
                timeout=self.timeout,
                headers={self.HEADER_API_KEY: self.api_key}
            )
            return response.status_code == 200
        except Exception as e:
            logger.warning(f"dify-sandbox health check fail, url={url}, message={e}")
            return False
    
    def run_python(self, python_code: str) -> SandboxResponse:
        """
        运行 Python 代码并返回完整响应。
        
        即使 HTTP 返回 2xx，SandboxResponse.code 非 0 也表示
        沙箱内 Python 执行失败，可通过 SandboxResponse.get_stderr() 获取错误信息。
        
        Args:
            python_code: Python 源码
            
        Returns:
            Sandbox 响应（永远非空）
            
        Raises:
            ValueError: 当参数为空时抛出
            requests.RequestException: 当 HTTP 非 2xx 时抛出
        """
        request = SandboxRequest(code=python_code, language="python3")
        return self.run(request)
    
    def run_python_and_get_stdout(self, python_code: str) -> str:
        """
        运行 Python 代码并仅返回 stdout。
        
        若执行失败（业务码非 0）返回错误信息，
        错误信息合并 error、stderr、stdout 三者便于排查。
        
        Args:
            python_code: Python 源码
            
        Returns:
            stdout 内容，若执行失败返回错误信息
        """
        response = self.run_python(python_code)
        if not response.is_success() or response.get_stderr():
            message = self._build_failure_message(response)
            logger.error(f"dify-sandbox run python fail, code={response.code}, message={message}")
            return message
        return response.get_stdout() or ""
    
    def run(self, request: SandboxRequest) -> SandboxResponse:
        """
        执行通用 Sandbox 请求。
        
        底层封装：序列化请求体 → 设置 X-Api-Key 头与 JSON Content-Type
        → 调用 POST /v1/sandbox/run → 反序列化响应。
        
        Args:
            request: 请求体（不能为空）
            
        Returns:
            Sandbox 响应（永远非空）
            
        Raises:
            ValueError: 当参数为空或代码为空时抛出
            requests.RequestException: 当 HTTP 非 2xx 时抛出
        """
        if request is None:
            raise ValueError("sandbox request is null")
        if not request.code:
            raise ValueError("sandbox code is empty")
        if not request.language:
            request.language = "python3"
        
        url = self._build_url(self.PATH_RUN)
        
        logger.debug(f"dify-sandbox run, url={url}, language={request.language}, code-len={len(request.code)}")
        
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json",
            self.HEADER_API_KEY: self.api_key
        }
        
        payload = {
            "code": request.code,
            "language": request.language
        }
        
        response = requests.post(
            url,
            json=payload,
            headers=headers,
            timeout=self.timeout
        )
        
        if response.status_code != 200:
            raise requests.RequestException(
                f"request dify-sandbox return http status={response.status_code}, response={response.text}"
            )
        
        try:
            response_data = response.json()
            return SandboxResponse.from_dict(response_data)
        except Exception as e:
            logger.error(f"format sandbox response to json fail, body={response.text}")
            raise ValueError(f"format sandbox response to json fail: {e}")
        

    def get_dependencies(self, language: str = "python3") -> list:
        """
        查询沙箱支持的依赖信息。
        
        API 返回结构：
        {
          "code": 0,
          "message": "success",
          "data": {
            "dependencies": [
              {"name": "requests", "version": ""},
              {"name": "jinja2", "version": ""},
              ...
            ]
          }
        }
        
        Args:
            language: 编程语言，默认 "python3"
            
        Returns:
            依赖列表，每个元素为 {"name": str, "version": str} 字典
            
        Raises:
            requests.RequestException: 当 HTTP 非 2xx 时抛出
            ValueError: 当业务码非 0 或响应解析失败时抛出
        """
        url = self._build_url(self.PATH_DEP)
        
        logger.debug(f"dify-sandbox dependencies query, url={url}, language={language}")
        
        headers = {
            "Accept": "application/json",
            self.HEADER_API_KEY: self.api_key
        }
        
        params = {
            "language": language
        }
        
        response = requests.get(
            url,
            params=params,
            headers=headers,
            timeout=self.timeout
        )
        
        if response.status_code != 200:
            raise requests.RequestException(
                f"request dify-sandbox dependencies return http status={response.status_code}, response={response.text}"
            )
        
        try:
            response_data = response.json()
            # 检查业务码
            if response_data.get("code") != 0:
                raise ValueError(
                    f"dify-sandbox dependencies query fail, code={response_data.get('code')}, "
                    f"message={response_data.get('message')}"
                )
            # 提取 dependencies 列表
            data = response_data.get("data", {})
            return data.get("dependencies", [])
        except ValueError:
            raise
        except Exception as e:
            logger.error(f"format dependencies response to json fail, body={response.text}")
            raise ValueError(f"format dependencies response to json fail: {e}")
    
    def update_dependencies(self, dependencies: list, language: str = "python3") -> bool:
        """
        更新沙箱依赖。
        
        请求体结构：
        {
          "dependencies": ["requests==2.31.0", "pillow"],
          "language": "python3"
        }
        
        响应结构：
        {
          "code": 0,
          "message": "success",
          "data": {}
        }
        
        Args:
            dependencies: 依赖列表，格式为 "包名==版本" 或 "包名"
            language: 编程语言，默认 "python3"
            
        Returns:
            True-更新成功；False-更新失败
            
        Raises:
            ValueError: 当参数为空时抛出
            requests.RequestException: 当 HTTP 非 2xx 时抛出
        """
        if not dependencies:
            raise ValueError("dependencies is empty")
        
        url = self._build_url(self.PATH_UPDATE_DEP)
        
        logger.debug(f"dify-sandbox update dependencies, url={url}, language={language}, deps={dependencies}")
        
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json",
            self.HEADER_API_KEY: self.api_key
        }
        
        payload = {
            "dependencies": dependencies,
            "language": language
        }
        
        response = requests.post(
            url,
            json=payload,
            headers=headers,
            timeout=self.timeout
        )
        
        if response.status_code != 200:
            raise requests.RequestException(
                f"request dify-sandbox update dependencies return http status={response.status_code}, response={response.text}"
            )
        
        try:
            response_data = response.json()
            return response_data.get("code") == 0
        except Exception as e:
            logger.error(f"format update dependencies response to json fail, body={response.text}")
            raise ValueError(f"format update dependencies response to json fail: {e}")
    
    def _build_url(self, path: str) -> str:
        """拼接 URL，自动处理末尾斜杠"""
        prefix = self.base_url.rstrip("/")
        suffix = path if path.startswith("/") else "/" + path
        return prefix + suffix
    
    def _build_failure_message(self, response: SandboxResponse) -> str:
        """构造 sandbox 业务失败时的错误信息，合并 error/stderr/stdout 三段便于排查"""
        message = f"dify-sandbox run fail, code={response.code}"
        if response.get_stderr():
            message += f", error={response.get_stderr()}"
        return message


# 便捷函数：创建全局实例
_default_sdk: Optional[SandboxSdk] = None


def get_sandbox_sdk() -> SandboxSdk:
    """获取默认的 SandboxSdk 实例（单例模式）"""
    global _default_sdk
    if _default_sdk is None:
        _default_sdk = SandboxSdk()
    return _default_sdk


def run_python(code: str) -> SandboxResponse:
    """便捷函数：运行 Python 代码并返回完整响应"""
    return get_sandbox_sdk().run_python(code)


def run_python_and_get_stdout(code: str) -> str:
    """便捷函数：运行 Python 代码并仅返回 stdout"""
    return get_sandbox_sdk().run_python_and_get_stdout(code)


def health() -> bool:
    """便捷函数：健康检查"""
    return get_sandbox_sdk().health()
