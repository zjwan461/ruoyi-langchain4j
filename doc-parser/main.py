# app/main.py
import os
import uuid
from fastapi import FastAPI, Request, HTTPException, Header
from typing import Optional

from logger_config import logger, trace_id_ctx
from service.docling_service import docling_to_markdown
from service.other_doc_service import other_to_markdown
from service.xls_service import xls_to_markdown
from service.doc_service import doc_to_markdown

app = FastAPI(title="Document OCR & Parsing Service")

SERVER_API_KEY = os.getenv("SERVER_API_KEY", "").strip()

DOCLING_EXTS = {
    ".pdf",
    ".docx",
    ".xlsx",
    ".pptx",
    ".ppt",
    ".md",
    ".markdown",
    ".html",
    ".xhtml",
    ".csv",
}

TEXT_EXTS = {
    ".json",
    ".txt",
    ".go",
    ".py",
    ".java",
    ".sh",
    ".bat",
    ".ps1",
    ".cmd",
    ".js",
    ".ts",
    ".css",
    ".cpp",
    ".hpp",
    ".h",
    ".c",
    ".cs",
    ".sql",
    ".log",
    ".ini",
    ".pl",
    ".pm",
    ".r",
    ".dart",
    ".dockerfile",
    ".env",
    ".php",
    ".hs",
    ".hsc",
    ".lua",
    ".nginxconf",
    ".conf",
    ".m",
    ".mm",
    ".plsql",
    ".perl",
    ".rb",
    ".rs",
    ".db2",
    ".scala",
    ".bash",
    ".swift",
    ".vue",
    ".svelte",
    ".ex",
    ".exs",
    ".erl",
    ".tsx",
    ".jsx",
    ".hs",
    ".lhs",
}


@app.put("/process")
async def process(
        request: Request,
        authorization: Optional[str] = Header(None),
        x_filename: Optional[str] = Header(None),
):
    trace_id = str(uuid.uuid4())
    trace_id_ctx.set(trace_id)

    logger.info("=" * 60)
    logger.info(f"请求 | filename={x_filename}")

    try:
        # 鉴权
        if SERVER_API_KEY:
            if not authorization or not authorization.startswith("Bearer "):
                logger.warning("未授权访问")
                raise HTTPException(status_code=401, detail="Unauthorized")
            token = authorization.split()[-1].strip()
            if token != SERVER_API_KEY:
                logger.warning("API Key 不匹配")
                raise HTTPException(status_code=401, detail="Invalid API Key")

        # 读取文件
        file_bytes = await request.body()
        if not file_bytes:
            raise HTTPException(status_code=400, detail="empty file content")

        filename = (x_filename or "").strip().lower()
        ext = os.path.splitext(filename)[-1]
        logger.info(f"文件类型: {ext}")

        # docling支持的文档走 Docling
        if ext in DOCLING_EXTS:
            return await docling_to_markdown(file_bytes, filename, trace_id)
        # xls格式处理
        elif ext == ".xls":
            return await xls_to_markdown(file_bytes, filename, trace_id)
        # doc格式处理
        elif ext == ".doc":
            return await doc_to_markdown(file_bytes, filename, trace_id)
        # 其它格式走手动处理
        elif ext in TEXT_EXTS or is_text_file(file_bytes):
            return await other_to_markdown(file_bytes, filename, trace_id)

        # 不支持
        else:
            raise HTTPException(status_code=415, detail=f"Unsupported file type: {ext}")

    except HTTPException:
        raise
    except Exception as e:
        err = f"{type(e).__name__}: {str(e)}"
        logger.error(f"处理失败: {err}", exc_info=True)
        raise HTTPException(status_code=500, detail=err)


def is_text_file(data: bytes) -> bool:
    """
    判断二进制数据是否为 文本文件（纯文本）
    标准通用方法，适用于绝大多数场景
    """
    if not data:
        return False

    # 文本文件允许的字符：可打印字符 + 换行 / 制表符
    # 排除 0x00-0x08、0x0B-0x0C、0x0E-0x1F 这些二进制控制字符
    text_characters = bytearray(
        {9, 10, 13} | set(range(32, 127)) | set(range(128, 256))
    )

    # 只检查前 8192 字节，避免大文件慢
    check_data = data[:8192]

    # 只要出现非文本字符，就不是文本文件
    for byte in check_data:
        if byte not in text_characters:
            return False
    return True
