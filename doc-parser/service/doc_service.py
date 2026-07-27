import subprocess
import os
from logger_config import logger
from service.docling_service import get_doc_converter
from docling.datamodel.base_models import ConversionStatus
import tempfile

DOCX_FILE_DIR = "tmp/"


async def doc_to_markdown(file_bytes: bytes, filename, trace_id):
    with tempfile.NamedTemporaryFile(
        mode="wb", delete=False, suffix=".doc"
    ) as temp_file:
        temp_file.write(file_bytes)
        file_path = temp_file.name

    output_dir = DOCX_FILE_DIR
    docx_file = await doc_to_docx(input_doc=file_path, output_dir=output_dir)
    conv = get_doc_converter()
    result = conv.convert(source=docx_file)
    # 状态判断
    if result.status != ConversionStatus.SUCCESS:
        logger.error(f"Docling 转换失败: {result.errors}")
        raise RuntimeError(f"Docling conversion failed: {result.errors}")

        # 导出 Markdown
    md_text = result.document.export_to_markdown()
    logger.info(f"Docling 处理完成 | 字符数: {len(md_text)} | trace_id={trace_id}")

    # 用完一定要删除临时文件，避免垃圾文件
    if file_path and os.path.exists(file_path):
        os.unlink(file_path)

    if docx_file and os.path.exists(docx_file):
        os.remove(docx_file)

    return {
        "page_content": md_text.strip(),
        "metadata": {
            "trace_id": trace_id,
            "filename": filename,
            "ext": ".doc",
            "engine": "docling==2.85.0",
            "status": "success",
        },
    }


async def doc_to_docx(input_doc: str, output_dir: str = None) -> str:
    """
    调用 LibreOffice (soffice) 将 doc 转换为 docx
    :param input_doc: 输入 .doc 文件路径
    :param output_dir: 输出目录，默认与原文件同目录
    :return: 生成的 docx 路径
    """
    # 检查文件是否存在
    if not os.path.exists(input_doc):
        raise FileNotFoundError(f"文件不存在：{input_doc}")

    # 如果没指定输出目录，就用原文件所在目录
    if output_dir is None:
        output_dir = os.path.dirname(input_doc)
        if output_dir == "":  # 当前目录
            output_dir = "."

    # 创建输出目录
    os.makedirs(output_dir, exist_ok=True)

    # 拼接转换命令（关键！）
    cmd = [
        "soffice",
        "--headless",  # 无界面模式
        "--convert-to",
        "docx",  # 目标格式
        input_doc,  # 输入文件
        "--outdir",
        output_dir,  # 输出目录
    ]

    try:
        # 执行命令
        result = subprocess.run(
            cmd, check=True, capture_output=True, text=True, encoding="utf-8"
        )
        logger.info(f"✅ 转换成功：{input_doc}, result: {result}")

        # 生成输出文件名
        base_name = os.path.splitext(os.path.basename(input_doc))[0]
        output_file = os.path.join(output_dir, f"{base_name}.docx")
        return output_file

    except subprocess.CalledProcessError as e:
        logger.exception(f"❌ 转换失败：{e.stderr}")
        raise
