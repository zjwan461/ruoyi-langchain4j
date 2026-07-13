package com.ruoyi.ai.component.memory.dto;

import cn.hutool.core.collection.CollUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class JAiMessage extends JChatMessage implements ChatMessage, LangChain4jMemoryAdaptor<AiMessage> {

    private String text;
    private String thinking;
    private List<JToolExecutionRequest> toolExecutionRequests;
    private Map<String, Object> attributes;

    @Override
    public AiMessage toLangChain4jMemoryComponent() {
        List<ToolExecutionRequest> originToolExecutionRequests = null;
        if (CollUtil.isNotEmpty(toolExecutionRequests)) {
            originToolExecutionRequests = toolExecutionRequests.stream()
                    .map(JToolExecutionRequest::toLangChain4jMemoryComponent)
                    .toList();
        }
        return AiMessage.builder()
                .text(text)
                .thinking(thinking)
                .toolExecutionRequests(originToolExecutionRequests)
                .attributes(attributes)
                .build();
    }

    @Override
    public void fromLangChain4jMemoryComponent(AiMessage aiMessage) {
        this.text = aiMessage.text();
        this.thinking = aiMessage.thinking();
        List<ToolExecutionRequest> ters = aiMessage.toolExecutionRequests();
        List<JToolExecutionRequest> jters = null;

        if (CollUtil.isNotEmpty(ters)) {
            jters = new ArrayList<>();
            for (ToolExecutionRequest toolExecutionRequest : ters) {
                JToolExecutionRequest jter = new JToolExecutionRequest();
                jter.fromLangChain4jMemoryComponent(toolExecutionRequest);
                jters.add(jter);
            }
        }
        this.toolExecutionRequests = jters;
        this.attributes = aiMessage.attributes();
    }

    @Override
    public ChatMessageType type() {
        return ChatMessageType.AI;
    }
}
