"""
dify-sandbox 执行请求体。

对应 dify-sandbox 的 POST /v1/sandbox/run 接口，
详见官方文档：https://github.com/langgenius/dify-sandbox。

典型用法：
    request = SandboxRequest(code="print('hello world')")
    response = sandbox_sdk.run(request)
"""
from dataclasses import dataclass
from typing import Optional


@dataclass
class SandboxRequest:
    """dify-sandbox 执行请求"""
    
    # 要在沙箱中执行的源代码
    code: str
    
    # 语言类型。当前 dify-sandbox 仅支持 python，此处保留扩展能力。
    language: str = "python3"
    
    def __post_init__(self):
        """验证必填字段"""
        if not self.code:
            raise ValueError("code 不能为空")
