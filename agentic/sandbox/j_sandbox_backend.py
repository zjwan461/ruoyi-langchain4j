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

_backends: dict[str, JSandboxBackend] = {}


class JSandboxBackend(BaseSandbox):
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


def get_or_create_sandbox(session_id: str) -> JSandboxBackend:
    global _backends
    _backend = _backends.get(session_id)
    if _backend:
        return _backend
    else:
        client = SandboxClient(
            base_url=os.getenv("SANDBOX_BASE_URL", "http://sandbox-api:8080/"),
            api_key=os.getenv("SANDBOX_API_KEY", "sandbox-secret-key"),
        )
        _backend = JSandboxBackend(sandbox_client=client)
        _backends[session_id] = _backend
        return _backend
