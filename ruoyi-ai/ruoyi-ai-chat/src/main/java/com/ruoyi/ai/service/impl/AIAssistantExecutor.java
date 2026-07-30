package com.ruoyi.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.component.memory.RedisMemoryChatMemoryStore;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.domain.Attachment;
import com.ruoyi.ai.domain.ChatMessage;
import com.ruoyi.ai.service.*;
import com.ruoyi.ai.util.Base64DataUriUtil;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.system.service.ISysConfigService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AIAssistantExecutor extends AIBaseChatServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(AIAssistantExecutor.class);

    public AIAssistantExecutor(IChatMessageService chatMessageService,
                               InMemoryChatMemoryStore inMemoryChatMemoryStore,
                               RedisMemoryChatMemoryStore redisMemoryChatMemoryStore,
                               AiConfig aiConfig,
                               ObjectMapper objectMapper,
                               RedisTemplate<Object, Object> redisTemplate,
                               RedisScript<Long> limitScript,
                               ISysConfigService sysConfigService,
                               IKnowledgeBaseService knowledgeBaseService,
                               IModelService modelService,
                               ModelBuilder modelBuilder,
                               IAIAgentService aiAgentService,
                               PgVectorEmbeddingStore pgVectorEmbeddingStore,
                               IAttachmentService attachmentService) {
        super(chatMessageService,
                inMemoryChatMemoryStore,
                redisMemoryChatMemoryStore,
                aiConfig,
                objectMapper,
                redisTemplate,
                limitScript,
                sysConfigService,
                knowledgeBaseService,
                modelService,
                modelBuilder,
                aiAgentService,
                pgVectorEmbeddingStore,
                attachmentService);
    }

    @Override
    public Flux<String> chat(AIAgent aiAgent, String prompt, String clientId, String sessionId, List<Long> attachmentIds) {
        ChatMessage userMessage = saveChatMessage(prompt, clientId, sessionId, aiAgent.getId(), ChatMessageType.USER, attachmentIds);
        String promptTemplate = aiAgent.getPromptTemplate();
        if (promptTemplate.contains(Constants.USER_MSG_TEMPLATE)) {
            promptTemplate = promptTemplate.replace(Constants.USER_MSG_TEMPLATE, prompt);
        } else {
            promptTemplate = promptTemplate + prompt;
        }
        if (promptTemplate.contains(Constants.KNOWLEDGE_BASE_TEMPLATE)) {
            promptTemplate = promptTemplate.replace(Constants.KNOWLEDGE_BASE_TEMPLATE, "");
        }
        String finalPromptTemplate = promptTemplate;
        AIAssistant assistant = this.buildAIAssistant(aiAgent, sessionId, false, false);
        return Flux.create(fluxSink -> {
            SseEmitterContext sseEmitterContext = this.createSseEmitterContext(fluxSink);
            AtomicReference<List<Map<String, Object>>> retrieveList = new AtomicReference<>();
            AtomicReference<List<Map<String, Object>>> toolCallList = new AtomicReference<>();
            AtomicInteger thinkCount = new AtomicInteger(0);
            AtomicBoolean thinkFinish = new AtomicBoolean(false);

            List<Content> messages = new ArrayList<>();
            messages.add(TextContent.from(finalPromptTemplate));
            if (CollUtil.isNotEmpty(userMessage.getAttachments())) {
                for (Attachment attachment : userMessage.getAttachments()) {
                    if (FileUtils.isImageBySuffix(attachment.getFileName())) {
                        Base64DataUriUtil.Base64ParseResult parseResult = Base64DataUriUtil.parse(attachment.getFileContent());
                        messages.add(ImageContent.from(parseResult.getPureBase64(), parseResult.getMimeType()));
                    } else {
                        TextContent attachContent = TextContent.from(String.format(Constants.DEFAULT_ATTACHMENT_FILE_PROMPT, attachment.getFileName(), attachment.getFileContent()));
                        messages.add(attachContent);
                    }
                }
            }
            TokenStream tokenStream = assistant.chatStream(messages);

            // 【重点】前端主动断开连接时立刻标记关闭
            fluxSink.onDispose(sseEmitterContext::markClosed);

            tokenStream
                    .onPartialThinking(thinkingChunk -> {
                        String text = thinkingChunk.text();
                        if (thinkCount.incrementAndGet() == 1) {
                            text = Constants.THINK_PREFIX_TAG + text;
                        }
                        emitFluxEvent(Constants.SSE_EVENT_MSG, text, aiAgent.getId(), aiAgent.getName(), sseEmitterContext.safeEmitter);
                    })
                    .onPartialResponse(chunk -> {
                        if (thinkCount.intValue() > 0 && !thinkFinish.get()) {
                            chunk = Constants.THINK_SUFFIX_TAG + chunk;
                            thinkFinish.set(true);
                        }
                        emitFluxEvent(Constants.SSE_EVENT_MSG, chunk, aiAgent.getId(), aiAgent.getName(), sseEmitterContext.safeEmitter);
                    })
                    .onCompleteResponse(resp -> {
                        String content = getAIResponse(resp);
                        ChatMessage assistantMessage = saveChatMessage(content, clientId, sessionId, aiAgent.getId(), ChatMessageType.AI, null);
                        if (CollUtil.isNotEmpty(retrieveList.get())) {
                            for (Map<String, Object> item : retrieveList.get()) {
                                saveKBRetrieved(aiAgent, sessionId, clientId, assistantMessage.getId(),
                                        String.valueOf(item.get("fileName")), String.valueOf(item.get("text")));
                            }
                        }
                        if (CollUtil.isNotEmpty(toolCallList.get())) {
                            for (Map<String, Object> item : toolCallList.get()) {
                                saveToolCallRecord(aiAgent, sessionId, clientId, assistantMessage.getId(),
                                        String.valueOf(item.get("toolName")), String.valueOf(item.get("toolInvokeResult")));
                            }
                        }
                        // 所有业务保存完成后，发起Flux正常结束
                        if (sseEmitterContext.markClosed()) {
                            fluxSink.complete();
                        }
                    })
                    .onToolExecuted(toolExecuted -> {
                        String result = toolExecuted.result();
                        ToolExecutionRequest request = toolExecuted.request();
                        log.info("调用工具完成{}, result={}", request.name(), result);
                        Map<String, Object> value = MapUtil.<String, Object>builder()
                                .put("toolName", request.name())
                                .put("toolInvokeResult", result)
                                .build();
                        emitFluxEvent(Constants.SSE_EVENT_TOOL_CALLED, value, aiAgent.getId(), aiAgent.getName(), sseEmitterContext.safeEmitter);
                        List<Map<String, Object>> toolCalls = toolCallList.get() == null ? new ArrayList<>() : toolCallList.get();
                        toolCalls.add(value);
                        toolCallList.set(toolCalls);
                    })
                    .beforeToolExecution(beforeToolExecution -> {
                        ToolExecutionRequest request = beforeToolExecution.request();
                        log.info("调用工具{}", request.name());
                        Map<String, Object> value = MapUtil.<String, Object>builder()
                                .put("toolName", request.name())
                                .build();
                        emitFluxEvent(Constants.SSE_EVENT_TOOL_CALL, value, aiAgent.getId(), aiAgent.getName(), sseEmitterContext.safeEmitter);
                    })
                    .onRetrieved(contents -> {
                        log.info("知识库查询完成，共{}条", contents.size());
                        handleRetrieveRecord(retrieveList, contents);
                        if (CollUtil.isNotEmpty(retrieveList.get())) {
                            emitFluxEvent(Constants.SSE_EVENT_RETRIEVED, retrieveList.get(), aiAgent.getId(), aiAgent.getName(), sseEmitterContext.safeEmitter);
                        }
                    })
                    .onError(error -> {
                        log.error("AI流式对话异常", error);
                        if (sseEmitterContext.markClosed()) {
                            fluxSink.error(error);
                        }
                    })
                    .start();
        });
    }


}
