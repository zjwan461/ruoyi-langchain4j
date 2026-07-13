package com.ruoyi.ai.component.memory.dto;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.data.message.ImageContent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JImageContent implements Content, LangChain4jMemoryAdaptor<ImageContent> {

    private JImage image;
    private ImageContent.DetailLevel detailLevel;

    @Override
    public ContentType type() {
        return ContentType.IMAGE;
    }

    @Override
    public ImageContent toLangChain4jMemoryComponent() {
        return ImageContent.from(image.toLangChain4jMemoryComponent());
    }

    @Override
    public void fromLangChain4jMemoryComponent(ImageContent imageContent) {
        JImage jImage = new JImage();
        jImage.fromLangChain4jMemoryComponent(imageContent.image());
        this.image = jImage;
        this.detailLevel = imageContent.detailLevel();
    }
}
