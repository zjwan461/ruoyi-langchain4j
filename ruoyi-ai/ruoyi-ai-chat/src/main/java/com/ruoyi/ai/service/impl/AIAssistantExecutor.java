package com.ruoyi.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.component.memory.RedisMemoryChatMemoryStore;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.domain.ChatMessage;
import com.ruoyi.ai.service.*;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysConfigService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessageType;
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
                               PgVectorEmbeddingStore pgVectorEmbeddingStore) {
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
                pgVectorEmbeddingStore);
    }

    @Override
    public Flux<String> chat(AIAgent aiAgent, String prompt, String clientId, String sessionId) {
        saveChatMessage(prompt, clientId, sessionId, aiAgent.getId(), ChatMessageType.USER);
        String promptTemplate = aiAgent.getPromptTemplate();
        if (promptTemplate.contains(Constants.USER_MSG_TEMPLATE)) {
            promptTemplate = promptTemplate.replace(Constants.USER_MSG_TEMPLATE, prompt);
        } else {
            promptTemplate = promptTemplate + prompt;
        }
        if (promptTemplate.contains(Constants.KNOWLEDGE_BASE_TEMPLATE) && StringUtils.isNotBlank(aiAgent.getKbIds())) {
            promptTemplate = promptTemplate.replace(Constants.KNOWLEDGE_BASE_TEMPLATE, "");
        }
        String finalPromptTemplate = promptTemplate;
        AIAssistant assistant = this.buildAIAssistant(aiAgent, sessionId, false);
        return Flux.create(fluxSink -> {
            AtomicReference<List<Map<String, Object>>> retrieveList = new AtomicReference<>();
            AtomicReference<List<Map<String, Object>>> toolCallList = new AtomicReference<>();
            TokenStream tokenStream = assistant.chatStream(finalPromptTemplate);
            AtomicInteger thinkCount = new AtomicInteger(0);
            AtomicBoolean thinkFinish = new AtomicBoolean(false);
            tokenStream.onPartialThinking(thinkingChunk -> {
                        String text = thinkingChunk.text();
                        if (thinkCount.incrementAndGet() == 1) {
                            text = Constants.THINK_PREFIX_TAG + text;
                        }
                        this.emitFluxEvent(Constants.SSE_EVENT_MSG, text, fluxSink);
                    }).onPartialResponse(chunk -> {
                        if (thinkCount.intValue() > 0 && !thinkFinish.get()) {
                            chunk = Constants.THINK_SUFFIX_TAG + chunk;
                            thinkFinish.set(true);
                        }
                        this.emitFluxEvent(Constants.SSE_EVENT_MSG, chunk, fluxSink);
                    }).onCompleteResponse(resp -> {
                        fluxSink.complete();
                        StringBuilder content = new StringBuilder();
                        String thinking = resp.aiMessage().thinking();
                        if (StringUtils.isNotBlank(thinking)) {
                            content.append(Constants.THINK_PREFIX_TAG)
                                    .append(thinking)
                                    .append(Constants.THINK_SUFFIX_TAG);
                        }
                        String aiText = resp.aiMessage().text();
                        content.append(aiText);
                        ChatMessage assistantMessage = saveChatMessage(content.toString(), clientId, sessionId, aiAgent.getId(), ChatMessageType.AI);
                        if (CollUtil.isNotEmpty(retrieveList.get())) {
                            for (Map<String, Object> item : retrieveList.get()) {
                                this.saveKBRetrieved(aiAgent, sessionId, clientId, assistantMessage.getId(),
                                        String.valueOf(item.get("fileName")), String.valueOf(item.get("text")));
                            }
                        }
                        if (CollUtil.isNotEmpty(toolCallList.get())) {
                            for (Map<String, Object> item : toolCallList.get()) {
                                this.saveToolCallRecord(aiAgent, sessionId, clientId, assistantMessage.getId(),
                                        String.valueOf(item.get("toolName")), String.valueOf(item.get("toolInvokeResult")));
                            }
                        }
                    }).onToolExecuted(toolExecuted -> {
                        String result = toolExecuted.result();
                        ToolExecutionRequest request = toolExecuted.request();
                        log.info("调用工具完成{}, result={}", request.name(), result);
                        Map<String, Object> value = MapUtil.<String, Object>builder().put("toolName", request.name()).put("toolInvokeResult", result).build();
                        this.emitFluxEvent(Constants.SSE_EVENT_TOOL_CALLED, value, fluxSink);
                        List<Map<String, Object>> toolCalls = toolCallList.get() == null ? new ArrayList<>() : toolCallList.get();
                        toolCalls.add(value);
                        toolCallList.set(toolCalls);
                    }).beforeToolExecution(beforeToolExecution -> {
                        ToolExecutionRequest request = beforeToolExecution.request();
                        log.info("调用工具{}", request.name());
                        Map<String, Object> value = MapUtil.<String, Object>builder().put("toolName", request.name()).build();
                        this.emitFluxEvent(Constants.SSE_EVENT_TOOL_CALL, value, fluxSink);
                    }).onRetrieved(contents -> {
                        log.info("知识库查询完成，共{}条", contents.size());
                        retrieveList.set(contents.stream().map(content -> {
                            String text = content.textSegment().text();
                            Long kbId = content.textSegment().metadata().getLong("kb_id");
                            String fileName = content.textSegment().metadata().getString("file_name");
                            return MapUtil.<String, Object>builder()
                                    .put("kbId", kbId)
                                    .put("fileName", fileName)
                                    .put("text", text)
                                    .build();
                        }).toList());
                        if (CollUtil.isNotEmpty(retrieveList.get())) {
                            this.emitFluxEvent(Constants.SSE_EVENT_RETRIEVED, retrieveList.get(), fluxSink);
                        }
                    })
                    .onError(fluxSink::error)
                    .start();
        });
    }

}
