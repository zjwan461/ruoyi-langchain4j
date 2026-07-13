package com.ruoyi.ai.component.memory.dto;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.CustomMessage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class JCustomMessage extends JChatMessage implements ChatMessage,LangChain4jMemoryAdaptor<CustomMessage> {

    private Map<String, Object> attributes;

    @Override
    public CustomMessage toLangChain4jMemoryComponent() {
        return CustomMessage.customMessage(attributes);
    }

    @Override
    public void fromLangChain4jMemoryComponent(CustomMessage customMessage) {
        this.attributes = customMessage.attributes();
    }

    @Override
    public ChatMessageType type() {
        return ChatMessageType.CUSTOM;
    }
}
