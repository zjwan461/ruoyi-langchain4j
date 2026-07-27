import os
from langchain_community.document_loaders.helpers import detect_file_encodings
import tempfile
from logger_config import logger


async def other_to_markdown(file_bytes: bytes, filename, trace_id):
    ext = os.path.splitext(filename)[-1]
    content = read_text(file_bytes)
    return {
        "page_content": content.strip(),
        "metadata": {
            "trace_id": trace_id,
            "filename": filename,
            "ext": ext,
            "engine": "manual",
            "status": "success",
        },
    }


def read_text(file_bytes: bytes):
    content = ""
    file_path = None  # 提前定义
    try:
        content = file_bytes.decode(encoding="utf-8")
    except UnicodeDecodeError:
        try:
            # 关键修复：delete=False，写完立即关闭
            with tempfile.NamedTemporaryFile(mode="wb", delete=False) as temp_file:
                temp_file.write(file_bytes)
                file_path = temp_file.name

            # 文件已经关闭 → 可以安全读取
            detected_encodings = detect_file_encodings(file_path)
            for encoding in detected_encodings:
                logger.debug(f"Trying encoding: {encoding.encoding}")
                try:
                    with open(file_path, encoding=encoding.encoding) as f:
                        content = f.read()
                    break
                except UnicodeDecodeError:
                    continue
        finally:
            # 用完一定要删除临时文件，避免垃圾文件
            if file_path and os.path.exists(file_path):
                os.unlink(file_path)
    return content
