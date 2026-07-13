package com.ruoyi.ai.component.memory.dto;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JSystemMessage extends JChatMessage implements ChatMessage, LangChain4jMemoryAdaptor<SystemMessage> {

    private String text;

    @Override
    public SystemMessage toLangChain4jMemoryComponent() {
        return SystemMessage.systemMessage(text);
    }

    @Override
    public void fromLangChain4jMemoryComponent(SystemMessage systemMessage) {
        this.text = systemMessage.text();
    }

    @Override
    public ChatMessageType type() {
        return ChatMessageType.SYSTEM;
    }
}
