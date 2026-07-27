import os
import pandas as pd  # 轻量读取表格
from io import BytesIO


async def xls_to_markdown(file_bytes: bytes, filename, trace_id):
    ext = os.path.splitext(filename)[-1]

    # ===================== 核心：Excel → 标准 Markdown 表格 =====================
    markdown_content_list = []
    excel_file = pd.ExcelFile(BytesIO(file_bytes))  # 直接从内存读取，更快

    for sheet_name in excel_file.sheet_names:
        df = pd.read_excel(
            BytesIO(file_bytes), sheet_name=sheet_name, dtype=str
        ).fillna("")

        if not df.empty or df.columns is not None:
            # 转成标准 Markdown 表格（| 姓名 | 年龄 | 这样）
            md_table = df.to_markdown(index=False)
            markdown_content = f"## {sheet_name}\n\n{md_table}\n\n"
            markdown_content_list.append(markdown_content)
    # ==========================================================================

    return [
        {
            "page_content": markdown_content.strip(),
            "metadata": {
                "trace_id": trace_id,
                "filename": filename,
                "ext": ext,
                "engine": "pandas",
                "status": "success",
            },
        }
        for markdown_content in markdown_content_list
    ]
