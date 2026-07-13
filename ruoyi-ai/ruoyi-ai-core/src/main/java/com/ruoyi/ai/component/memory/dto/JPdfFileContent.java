package com.ruoyi.ai.component.memory.dto;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.data.message.PdfFileContent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JPdfFileContent implements Content, LangChain4jMemoryAdaptor<PdfFileContent> {

    private JPdfFile pdfFile;

    @Override
    public ContentType type() {
        return ContentType.PDF;
    }


    @Override
    public PdfFileContent toLangChain4jMemoryComponent() {
        return PdfFileContent.from(pdfFile.toLangChain4jMemoryComponent());
    }

    @Override
    public void fromLangChain4jMemoryComponent(PdfFileContent pdfFileContent) {
        JPdfFile jPdfFile = new JPdfFile();
        jPdfFile.fromLangChain4jMemoryComponent(pdfFileContent.pdfFile());
        this.pdfFile = jPdfFile;
    }
}
