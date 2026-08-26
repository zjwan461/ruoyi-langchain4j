"""Dify Sandbox backend.

Provides a sandbox backend using the Dify Sandbox SDK.
"""

from __future__ import annotations

import os

from deepagents.backends.protocol import (
    ExecuteResponse,
    FileDownloadResponse,
    FileUploadResponse,
    WriteResult,
)
from deepagents.backends.sandbox import BaseSandbox
from python_sandbox_sdk import SandboxClient, CommandResult
from agentic.logger_config import logger

_backends: dict[str, PythonSandboxBackend] = {}


class PythonSandboxBackend(BaseSandbox):
    """Dify Sandbox backend using the Dify Sandbox SDK.

    Implements sync variants (execute/write/download_files/upload_files) using
    the SDK. The async variants raise NotImplementedError since the framework
    always calls the sync versions.
    """

    def __init__(self, sandbox_client: SandboxClient) -> None:
        self.sandbox_client = sandbox_client
        self._id = self.sandbox_client.create_session()

    @property
    def id(self) -> str:
        return self._id

    def execute(self, command: str, *, timeout: int | None = None) -> ExecuteResponse:
        try:
            cmd_result: CommandResult = self.sandbox_client.exec_shell(
                command=command, session_id=self.id
            )
            if cmd_result.success:
                return ExecuteResponse(
                    output=cmd_result.stdout, exit_code=cmd_result.exit_code
                )
            else:
                return ExecuteResponse(
                    output=cmd_result.stderr, exit_code=cmd_result.exit_code
                )
        except Exception as e:
            return ExecuteResponse(output=str(e), exit_code=1)

    async def aexecute(
        self, command: str, *, timeout: int | None = None
    ) -> ExecuteResponse:
        raise NotImplementedError("Use execute()")

    def write(self, file_path: str, content: str) -> WriteResult:
        try:
            self.sandbox_client.write_file(
                self.id, container_path=file_path, content=content
            )
            return WriteResult(path=file_path)
        except Exception as e:
            return WriteResult(path=file_path, error=str(e))

    async def awrite(self, file_path: str, content: str) -> WriteResult:
        raise NotImplementedError("Use write()")

    def download_files(self, paths: list[str]) -> list[FileDownloadResponse]:
        results = []
        for path in paths:
            try:
                data: bytes = self.sandbox_client.download_file(
                    session_id=self.id, container_path=path
                )
                results.append(
                    FileDownloadResponse(path=path, content=data, error=None)
                )
            except Exception as e:
                results.append(
                    FileDownloadResponse(path=path, content=None, error=str(e))
                )
        return results

    async def adownload_files(self, paths: list[str]) -> list[FileDownloadResponse]:
        raise NotImplementedError("Use download_files()")

    def upload_files(self, files: list[tuple[str, bytes]]) -> list[FileUploadResponse]:
        results = []
        for filename, content in files:
            try:
                self.sandbox_client.upload_file(
                    session_id=self.id, container_path=filename, data=content
                )
                results.append(FileUploadResponse(path=filename, error=None))
            except Exception as e:
                results.append(FileUploadResponse(path=filename, error=str(e)))
        return results

    async def aupload_files(
        self, files: list[tuple[str, bytes]]
    ) -> list[FileUploadResponse]:
        raise NotImplementedError("Use upload_files()")


async def get_or_create_sandbox(session_id: str) -> PythonSandboxBackend:
    global _backends
    _backend = _backends.get(session_id)
    if _backend:
        return _backend
    else:
        client = SandboxClient(
            base_url=os.getenv("SANDBOX_BASE_URL", "http://agentic-sandbox:8080/"),
            api_key=os.getenv("SANDBOX_API_KEY", "sandbox-secret-key"),
        )
        _backend = PythonSandboxBackend(sandbox_client=client)
        _backends[session_id] = _backend
        return _backend


async def upload_skills_to_sandbox(
    session_id: str, local_skill_dir: str, skill_white_list: list[str] = None
):
    skills_dir = f"/skills"
    sandbox_backend = get_or_create_sandbox(session_id=session_id)
    client = sandbox_backend.sandbox_client
    # 创建 sandbox 会话
    session_id = sandbox_backend.id
    try:
        # 递归遍历 local_skill_dir 下的所有文件
        for root, dirs, files in os.walk(local_skill_dir):
            for file in files:
                file_path = os.path.join(root, file)
                # 计算相对于 local_skill_dir 的相对路径
                rel_path = os.path.relpath(file_path, local_skill_dir)
                # 构建 sandbox 中的目标路径（保持 /skills 前缀）
                container_path = f"{skills_dir}/{rel_path}".replace("\\", "/")
                # 提取目标目录路径
                container_dir = os.path.dirname(container_path).replace("\\", "/")
                # 在 sandbox 中创建目录（如果不存在）
                client.exec_shell(session_id, f"mkdir -p {container_dir}")
                # 读取文件内容并上传
                with open(file_path, "rb") as f:
                    data = f.read()
                client.upload_file(session_id, container_path, data)
                logger.info(f"已上传: {file_path} -> {container_path}")
    finally:
        client.close()
