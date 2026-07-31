package com.ruoyi.ai.component.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.guardrail.ChatExecutor;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServiceContext;
import dev.langchain4j.service.tool.ToolServiceContext;

import java.util.List;
import java.util.function.Function;

public class JToolAwareRepromptExecutor {
    private JToolAwareRepromptExecutor() {
    }

    static ChatExecutor wrap(
            ChatExecutor rawChatExecutor,
            AiServiceContext context,
            Object memoryId,
            ChatRequestParameters parameters,
            InvocationContext invocationContext,
            ToolServiceContext toolServiceContext,
            Function<ChatRequest, ChatResponse> chatModelInvoker) {
        return new ChatExecutor() {
            @Override
            public ChatResponse execute() {
                return rawChatExecutor.execute();
            }

            @Override
            public ChatResponse execute(List<ChatMessage> chatMessages) {
                ChatResponse initialResponse = rawChatExecutor.execute(chatMessages);

                if (!initialResponse.aiMessage().hasToolExecutionRequests()) {
                    return initialResponse;
                }

                return context.toolService
                        .executeInferenceAndToolsLoop(
                                context,
                                memoryId,
                                initialResponse,
                                parameters,
                                chatMessages,
                                null,
                                invocationContext,
                                toolServiceContext,
                                chatModelInvoker)
                        .aggregateResponse();
            }
        };
    }
}
