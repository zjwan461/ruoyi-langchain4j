package com.ruoyi.ai.component.memory.dto;

import dev.langchain4j.data.image.Image;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URI;

@Getter
@Setter
@NoArgsConstructor
public class JImage implements LangChain4jMemoryAdaptor<Image> {

    private URI url;
    private String base64Data;
    private String mimeType;
    private String revisedPrompt;

    @Override
    public Image toLangChain4jMemoryComponent() {
        return Image.builder()
                .base64Data(this.base64Data)
                .mimeType(this.mimeType)
                .revisedPrompt(this.revisedPrompt)
                .url(this.url)
                .build();
    }

    @Override
    public void fromLangChain4jMemoryComponent(Image image) {
        this.url = image.url();
        this.base64Data = image.base64Data();
        this.mimeType = image.mimeType();
        this.revisedPrompt = image.revisedPrompt();
    }
}
