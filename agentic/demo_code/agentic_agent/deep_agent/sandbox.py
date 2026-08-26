"""Dify Sandbox backend.

Provides a sandbox backend using the Dify Sandbox SDK.
"""

from __future__ import annotations

import asyncio
import os

from deepagents.backends.protocol import (
    ExecuteResponse,
    FileDownloadResponse,
    FileUploadResponse,
    WriteResult,
)
from deepagents.backends.sandbox import BaseSandbox
from python_sandbox_sdk import AsyncSandboxClient, CommandResult
import logging

logger = logging.getLogger(__file__)

_backends: dict[str, PythonSandboxBackend] = {}


class PythonSandboxBackend(BaseSandbox):
    """Dify Sandbox backend using the Dify Sandbox SDK.

    Implements sync variants (execute/write/download_files/upload_files) using
    the SDK. The async variants raise NotImplementedError since the framework
    always calls the sync versions.
    """

    def __init__(self, sandbox_client: AsyncSandboxClient) -> None:
        self.sandbox_client = sandbox_client
        self._id: str | None = None

    async def init_session(self) -> None:
        """异步初始化 session，必须在首次使用前调用。"""
        self._id = await self.sandbox_client.create_session()

    @property
    def id(self) -> str:
        return self._id

    def execute(self, command: str, *, timeout: int | None = None) -> ExecuteResponse:
        raise NotImplementedError("Use aexecute()")

    async def aexecute(
        self, command: str, *, timeout: int | None = None
    ) -> ExecuteResponse:
        try:
            cmd_result: CommandResult = await self.sandbox_client.exec_shell(
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

    def write(self, file_path: str, content: str) -> WriteResult:
        raise NotImplementedError("Use awrite()")

    async def awrite(self, file_path: str, content: str) -> WriteResult:
        try:
            await self.sandbox_client.write_file(
                self.id, container_path=file_path, content=content
            )
            return WriteResult(path=file_path)
        except Exception as e:
            return WriteResult(path=file_path, error=str(e))

    def download_files(self, paths: list[str]) -> list[FileDownloadResponse]:
        raise NotImplementedError("Use adownload_files()")

    async def adownload_files(self, paths: list[str]) -> list[FileDownloadResponse]:
        results = []
        for path in paths:
            try:
                data: bytes = await self.sandbox_client.download_file(
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

    def upload_files(self, files: list[tuple[str, bytes]]) -> list[FileUploadResponse]:
        raise NotImplementedError("Use aupload_files()")

    async def aupload_files(
        self, files: list[tuple[str, bytes]]
    ) -> list[FileUploadResponse]:
        results = []
        for filename, content in files:
            try:
                await self.sandbox_client.upload_file(
                    session_id=self.id, container_path=filename, data=content
                )
                results.append(FileUploadResponse(path=filename, error=None))
            except Exception as e:
                results.append(FileUploadResponse(path=filename, error=str(e)))
        return results


skill_idr = os.path.realpath(
    os.path.join(os.path.dirname(__file__), "../../../..", "skills")
)
print(f"skill_dir={skill_idr}")


async def get_or_create_sandbox(session_id: str) -> PythonSandboxBackend:
    global _backends
    _backend = _backends.get(session_id)
    if _backend:
        return _backend
    else:
        client = AsyncSandboxClient(
            base_url=os.getenv("SANDBOX_BASE_URL", "http://agentic-sandbox:8080/"),
            api_key=os.getenv("SANDBOX_API_KEY", "sandbox-secret-key"),
        )
        _backend = PythonSandboxBackend(sandbox_client=client)
        await _backend.init_session()
        _backends[session_id] = _backend
        await upload_skills_to_sandbox(client, _backend.id, skill_idr)
        return _backend


def _collect_files_to_upload(local_skill_dir: str) -> list[tuple[str, str, bytes]]:
    """收集需要上传的文件列表（同步阻塞操作，需在线程中执行）"""
    files_to_upload = []
    for root, dirs, files in os.walk(local_skill_dir):
        for file in files:
            file_path = os.path.join(root, file)
            rel_path = os.path.relpath(file_path, local_skill_dir)
            container_path = f"/skills/{rel_path}".replace("\\", "/")
            with open(file_path, "rb") as f:
                data = f.read()
            files_to_upload.append((file_path, container_path, data))
    return files_to_upload


async def upload_skills_to_sandbox(
    client: AsyncSandboxClient,
    session_id: str,
    local_skill_dir: str,
    skill_white_list: list[str] = None,
):
    skills_dir = f"/skills"
    try:
        # 将阻塞的文件系统操作包装到线程中执行
        files_to_upload = await asyncio.to_thread(
            _collect_files_to_upload, local_skill_dir
        )
        
        # 异步上传文件
        for file_path, container_path, data in files_to_upload:
            # 提取目标目录路径
            container_dir = os.path.dirname(container_path).replace("\\", "/")
            # 在 sandbox 中创建目录（如果不存在）
            await client.exec_shell(session_id, f"mkdir -p {container_dir}")
            # 上传文件
            await client.upload_file(session_id, container_path, data)
            logger.info(f"已上传: {file_path} -> {container_path}")
    finally:
        await client.close()
