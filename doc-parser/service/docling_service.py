# app/service/doc_service.py
import asyncio
import io
import os
from pathlib import Path

from docling.datamodel.base_models import ConversionStatus
from docling.datamodel.base_models import InputFormat
from docling.datamodel.pipeline_options import (
    PdfPipelineOptions,
    VlmPipelineOptions,
    RapidOcrOptions
)
from docling.datamodel.pipeline_options import PictureDescriptionApiOptions
from docling.datamodel.pipeline_options_vlm_model import ApiVlmOptions, ResponseFormat
from docling.document_converter import (
    DocumentConverter,
    PdfFormatOption,
    WordFormatOption,
    ExcelFormatOption,
    CsvFormatOption,
    MarkdownFormatOption,
    PowerpointFormatOption,
    HTMLFormatOption,
)
from docling_core.types.io import DocumentStream
from huggingface_hub import snapshot_download
from logger_config import logger
from dotenv import load_dotenv

load_dotenv()

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "").strip()
OPENAI_BASE_URL = os.getenv("OPENAI_BASE_URL", "").strip()
OCR_MODEL = os.getenv("OCR_MODEL", "qwen-vl-max")
OCR_MAX_CONCURRENT = int(os.getenv("OCR_MAX_CONCURRENT", "5"))
IMG_DESC_CONCURRENT = int(os.getenv("IMG_DESC_CONCURRENT", "5"))
DESCRIBE_DOC_IMG = os.getenv("DESCRIBE_DOC_IMG", "false") == "true"
OCR_PROMPT = os.getenv(
    "OCR_PROMPT",
    "仅提取图片中的原始文字，保留表格、代码块等markdown格式语法。不要额外添加任何解释、标题、注释、格式说明、无关内容，只返回markdown文本",
)
IMG_DESC_PROMPT = os.getenv(
    "IMG_DESC_PROMPT",
    "Describe the image",
)

pic_desc_api_options = PictureDescriptionApiOptions(
    url=f"{OPENAI_BASE_URL}/chat/completions",
    headers={"Authorization": f"Bearer {OPENAI_API_KEY}"},
    params=dict(model=OCR_MODEL, chat_template_kwargs={"enable_thinking": False}),
    prompt=IMG_DESC_PROMPT,
    timeout=60,
    concurrency=IMG_DESC_CONCURRENT,
)

# 全局单例初始化，避免重复加载模型
_converter = None
vlm_pipeline_options = VlmPipelineOptions()
vlm_pipeline_options.do_picture_description = DESCRIBE_DOC_IMG
vlm_pipeline_options.enable_remote_services = True
vlm_pipeline_options.picture_description_options = pic_desc_api_options
vlm_pipeline_options.vlm_options = ApiVlmOptions(
    url=f"{OPENAI_BASE_URL}/chat/completions",
    headers={"Authorization": f"Bearer {OPENAI_API_KEY}"},
    params=dict(model=OCR_MODEL, chat_template_kwargs={"enable_thinking": False}),
    prompt=OCR_PROMPT,
    timeout=60,
    temperature=0.1,
    response_format=ResponseFormat.MARKDOWN,
    concurrency=OCR_MAX_CONCURRENT,
)

# pdf_pipeline_options = PdfPipelineOptions()
# pdf_pipeline_options.do_picture_description = True
# pdf_pipeline_options.enable_remote_services = True
# pdf_pipeline_options.picture_description_options = pic_desc_api_options

print("Loading RapidOCR models")
download_path = snapshot_download(repo_id="SWHL/RapidOCR")

# Setup RapidOcrOptions for english detection
det_model_path = os.path.join(
    download_path, "PP-OCRv4", "en_PP-OCRv3_det_infer.onnx"
)
rec_model_path = os.path.join(
    download_path, "PP-OCRv4", "ch_PP-OCRv4_rec_server_infer.onnx"
)
cls_model_path = os.path.join(
    download_path, "PP-OCRv3", "ch_ppocr_mobile_v2.0_cls_train.onnx"
)
ocr_options = RapidOcrOptions(
    det_model_path=det_model_path,
    rec_model_path=rec_model_path,
    cls_model_path=cls_model_path,
)

pdf_pipeline_options = PdfPipelineOptions(
    ocr_options=ocr_options,
)

pdf_pipeline_options.do_ocr = True
pdf_pipeline_options.do_table_structure = True
pdf_pipeline_options.table_structure_options.do_cell_matching = True
pdf_pipeline_options.enable_remote_services = True
pdf_pipeline_options.do_picture_description = DESCRIBE_DOC_IMG
pdf_pipeline_options.picture_description_options = pic_desc_api_options


def get_doc_converter():
    global _converter
    if _converter is None:
        logger.info("初始化 Docling 转换器（全局单例）")
        _converter = DocumentConverter(
            format_options={
                InputFormat.PDF: PdfFormatOption(pipeline_options=pdf_pipeline_options),
                InputFormat.DOCX: WordFormatOption(
                    pipeline_options=vlm_pipeline_options
                ),
                InputFormat.XLSX: ExcelFormatOption(
                    pipeline_options=vlm_pipeline_options
                ),
                InputFormat.CSV: CsvFormatOption(pipeline_options=vlm_pipeline_options),
                InputFormat.MD: MarkdownFormatOption(
                    pipeline_options=vlm_pipeline_options
                ),
                InputFormat.PPTX: PowerpointFormatOption(
                    pipeline_options=vlm_pipeline_options
                ),
                InputFormat.HTML: HTMLFormatOption(
                    pipeline_options=vlm_pipeline_options
                ),
            }
        )
    return _converter


async def docling_to_markdown(file_bytes: bytes, filename: str, trace_id: str):
    try:
        logger.info(f"Docling 处理文件: {filename} | trace_id={trace_id}")
        conv = get_doc_converter()
        path = Path(filename.lower())
        input_io = io.BytesIO(file_bytes)

        result = await asyncio.to_thread(
            conv.convert, source=DocumentStream(name=filename, stream=input_io)
        )

        # 状态判断
        if result.status != ConversionStatus.SUCCESS:
            logger.error(f"Docling 转换失败: {result.errors}")
            raise RuntimeError(f"Docling conversion failed: {result.errors}")

        # 导出 Markdown
        md_text = result.document.export_to_markdown()
        logger.info(f"Docling 处理完成 | 字符数: {len(md_text)} | trace_id={trace_id}")

        return {
            "page_content": md_text.strip(),
            "metadata": {
                "trace_id": trace_id,
                "filename": filename,
                "ext": path.suffix,
                "engine": "docling==2.85.0",
                "status": "success",
            },
        }

    except Exception as e:
        logger.error(f"Docling 处理异常: {str(e)} | trace_id={trace_id}", exc_info=True)
        raise
