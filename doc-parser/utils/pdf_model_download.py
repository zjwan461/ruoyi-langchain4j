def download_pdf_models():
    # Download RappidOCR models from HuggingFace
    print("Downloading RapidOCR models")
    download_path = snapshot_download(repo_id="SWHL/RapidOCR")
    print(f"PDF models download success {download_path}")

download_pdf_models()