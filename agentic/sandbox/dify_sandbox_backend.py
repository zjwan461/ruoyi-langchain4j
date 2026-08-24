"""Dify Sandbox backend.

Provides a sandbox backend using the Dify Sandbox SDK.
"""

from __future__ import annotations

import io
import os
import shlex
from uuid import uuid4

from deepagents.backends.protocol import (
    ExecuteResponse,
    FileDownloadResponse,
    FileUploadResponse,
    WriteResult,
)
from deepagents.backends.sandbox import BaseSandbox
from dify_sandbox import DifySandboxClient
from dify_sandbox.models import RunCommandResponse

_backend: DifySandboxBackend


class DifySandboxBackend(BaseSandbox):
    """Dify Sandbox backend using the Dify Sandbox SDK.

    Implements sync variants (execute/write/download_files/upload_files) using
    the SDK. The async variants raise NotImplementedError since the framework
    always calls the sync versions.
    """

    def __init__(self, dify_sandbox_client: DifySandboxClient) -> None:
        self.dify_sandbox_sdk = dify_sandbox_client
        self._id = str(uuid4())

    @property
    def id(self) -> str:
        return self._id

    @staticmethod
    def _parse_command(command: str) -> tuple[str, list[str]]:
        """Parse a command string into command and arguments.

        Uses shlex to properly split shell commands, handling quoted arguments,
        escaped characters, etc. Filters out shell-specific syntax like
        redirections and pipes that are not supported by run_command.

        Args:
            command: Full command string (e.g., "python3 script.py --arg value")

        Returns:
            Tuple of (command, args) where command is the executable and args
            is a list of arguments.
        """
        try:
            parts = shlex.split(command)
        except ValueError:
            # Fallback to simple split if shlex fails (e.g. unmatched quotes)
            parts = command.split()
        if not parts:
            return "", []

        # Filter out shell-specific syntax (redirections, pipes, etc.)
        # that are not supported by run_command's args parameter
        filtered_args = []
        skip_next = False
        for i, part in enumerate(parts[1:], start=1):
            if skip_next:
                skip_next = False
                continue

            # Skip redirections: >, >>, <, 2>, 2>>, &>, &>>
            if part in (">", ">>", "<", "2>", "2>>", "&>", "&>>"):
                skip_next = True  # Skip the next argument (filename)
                continue

            # Skip arguments containing redirection syntax (e.g., "2>/dev/null")
            if ">" in part or "<" in part:
                continue

            # Skip pipes and logical operators
            if part in ("|", "||", "&&", ";"):
                continue

            filtered_args.append(part)

        return parts[0], filtered_args

    def execute(self, command: str, *, timeout: int | None = None) -> ExecuteResponse:
        try:
            cmd, args = self._parse_command(command)
            resp: RunCommandResponse = self.dify_sandbox_sdk.run_command(
                command=cmd, args=args
            )
            if resp.is_success:
                return ExecuteResponse(output=resp.stdout, exit_code=resp.exit_code)
            else:
                return ExecuteResponse(output=resp.stderr, exit_code=resp.exit_code)
        except Exception as e:
            return ExecuteResponse(output=str(e), exit_code=1)

    async def aexecute(
        self, command: str, *, timeout: int | None = None
    ) -> ExecuteResponse:
        raise NotImplementedError("Use execute()")

    def write(self, file_path: str, content: str) -> WriteResult:
        try:
            file_obj = io.BytesIO(content.encode("utf-8"))
            self.dify_sandbox_sdk.upload_file(file=file_obj, filename=file_path)
            return WriteResult(path=file_path)
        except Exception as e:
            return WriteResult(path=file_path, error=str(e))

    async def awrite(self, file_path: str, content: str) -> WriteResult:
        raise NotImplementedError("Use write()")

    def download_files(self, paths: list[str]) -> list[FileDownloadResponse]:
        results = []
        for path in paths:
            try:
                content = self.dify_sandbox_sdk.download_file(filename=path)
                results.append(
                    FileDownloadResponse(path=path, content=content, error=None)
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
                file_obj = io.BytesIO(content)
                self.dify_sandbox_sdk.upload_file(file=file_obj, filename=filename)
                results.append(FileUploadResponse(path=filename, error=None))
            except Exception as e:
                results.append(FileUploadResponse(path=filename, error=str(e)))
        return results

    async def aupload_files(
        self, files: list[tuple[str, bytes]]
    ) -> list[FileUploadResponse]:
        raise NotImplementedError("Use upload_files()")


def get_or_create_sandbox() -> DifySandboxBackend:
    global _backend
    client = DifySandboxClient(
        base_url=os.getenv("SANDBOX_BASE_URL", "http://sandbox:8194"),
        api_key=os.getenv("SANDBOX_API_KEY", "dify-sandbox"),
        timeout=int(os.getenv("SANDBOX_TIMEOUT", "30")),
    )
    _backend = DifySandboxBackend(dify_sandbox_client=client)
    return _backend
