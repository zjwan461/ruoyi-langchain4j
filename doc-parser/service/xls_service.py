import os
import pandas as pd
from io import BytesIO


async def xls_to_markdown(file_bytes: bytes, filename, trace_id):
    ext = os.path.splitext(filename)[-1]

    markdown_content_list = []
    excel_file = pd.ExcelFile(BytesIO(file_bytes))

    for sheet_name in excel_file.sheet_names:
        df = pd.read_excel(
            BytesIO(file_bytes), sheet_name=sheet_name, dtype=str
        ).fillna("")

        if not df.empty or df.columns is not None:
            md_table = df.to_markdown(index=False)
            markdown_content = f"## {sheet_name}\n\n{md_table}\n\n"
            markdown_content_list.append(markdown_content)

    # 把所有工作表内容拼接成一段完整markdown
    full_markdown = "\n".join(markdown_content_list).strip()

    # 直接返回单个字典，不再返回列表
    return {
        "page_content": full_markdown,
        "metadata": {
            "trace_id": trace_id,
            "filename": filename,
            "ext": ext,
            "engine": "pandas",
            "status": "success",
        },
    }
