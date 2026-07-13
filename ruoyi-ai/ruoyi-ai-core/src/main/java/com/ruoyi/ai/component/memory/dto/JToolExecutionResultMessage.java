package com.ruoyi.ai.component.memory.dto;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JToolExecutionResultMessage extends JChatMessage implements ChatMessage, LangChain4jMemoryAdaptor<ToolExecutionResultMessage> {

    private String id;
    private String toolName;
    private String text;

    @Override
    public ToolExecutionResultMessage toLangChain4jMemoryComponent() {
        return new ToolExecutionResultMessage(id, toolName, text);
    }

    @Override
    public void fromLangChain4jMemoryComponent(ToolExecutionResultMessage toolExecutionResultMessage) {
        this.id = toolExecutionResultMessage.id();
        this.toolName = toolExecutionResultMessage.toolName();
        this.text = toolExecutionResultMessage.text();
    }

    @Override
    public ChatMessageType type() {
        return ChatMessageType.TOOL_EXECUTION_RESULT;
    }
}
