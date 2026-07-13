package com.ruoyi.ai.component.memory.dto;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.data.message.TextContent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JTextContent implements Content, LangChain4jMemoryAdaptor<TextContent> {

    private String text;

    @Override
    public ContentType type() {
        return ContentType.TEXT;
    }

    @Override
    public TextContent toLangChain4jMemoryComponent() {
        return TextContent.from(text);
    }

    @Override
    public void fromLangChain4jMemoryComponent(TextContent textContent) {
        this.text = textContent.text();
    }
}
