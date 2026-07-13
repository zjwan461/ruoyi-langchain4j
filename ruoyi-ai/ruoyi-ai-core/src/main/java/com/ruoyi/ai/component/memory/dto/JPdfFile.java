package com.ruoyi.ai.component.memory.dto;

import dev.langchain4j.data.pdf.PdfFile;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URI;

@Getter
@Setter
@NoArgsConstructor
public class JPdfFile implements LangChain4jMemoryAdaptor<PdfFile> {

    private URI url;
    private String base64Data;
    private String mimeType;

    @Override
    public PdfFile toLangChain4jMemoryComponent() {
        return PdfFile.builder()
                .base64Data(base64Data)
                .mimeType(mimeType)
                .url(url)
                .build();
    }

    @Override
    public void fromLangChain4jMemoryComponent(PdfFile pdfFile) {
        this.url = pdfFile.url();
        this.base64Data = pdfFile.base64Data();
        this.mimeType = pdfFile.mimeType();
    }
}
