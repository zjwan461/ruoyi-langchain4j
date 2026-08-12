"""
dify-sandbox 执行响应体。

对应 dify-sandbox POST /v1/sandbox/run 的返回结构：
{
  "code": 0,            // 0 表示执行成功，非 0 表示失败
  "data": {
    "stdout": "...",
    "stderr": "..."
  },
  "error": null
}
"""
from dataclasses import dataclass
from typing import Optional, Dict, Any


@dataclass
class SandboxData:
    """dify-sandbox 执行成功时的输出载荷"""
    
    # Python 进程的标准输出
    stdout: Optional[str] = None
    
    # Python 进程的标准错误输出
    error: Optional[str] = None
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "SandboxData":
        """从字典创建实例"""
        return cls(
            stdout=data.get("stdout"),
            error=data.get("error")
        )


@dataclass
class SandboxResponse:
    """dify-sandbox 执行响应"""
    
    # 业务状态码：0 表示执行成功，非 0 表示执行失败
    # 与 HTTP 状态码不同，dify-sandbox 即便在沙箱内 Python 抛异常时，
    # HTTP 仍返回 200，由该字段标识脚本是否正常运行
    code: int
    
    # 成功时的标准输出 / 标准错误输出
    data: Optional[SandboxData] = None
    
    # 错误消息
    message: Optional[str] = None
    
    @classmethod
    def from_dict(cls, response: Dict[str, Any]) -> "SandboxResponse":
        """从字典创建实例"""
        data_dict = response.get("data")
        data = SandboxData.from_dict(data_dict) if data_dict else None
        
        return cls(
            code=response.get("code", -1),
            data=data,
            message=response.get("message")
        )
    
    def is_success(self) -> bool:
        """是否执行成功（业务码为 0）"""
        return self.code == 0
    
    def get_stdout(self) -> Optional[str]:
        """获取标准输出（stdout），若执行失败返回 None"""
        return self.data.stdout if self.data else None
    
    def get_stderr(self) -> Optional[str]:
        """获取标准错误输出（stderr），若执行失败返回 None"""
        return self.data.error if self.data else None
