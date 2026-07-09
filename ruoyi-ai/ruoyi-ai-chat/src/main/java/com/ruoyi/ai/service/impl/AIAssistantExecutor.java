package com.ruoyi.ai.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.component.RedisMemoryChatMemoryStore;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.domain.KnowledgeBase;
import com.ruoyi.ai.domain.Model;
import com.ruoyi.ai.service.AIAssistant;
import com.ruoyi.ai.service.IChatMessageService;
import com.ruoyi.ai.service.IKnowledgeBaseService;
import com.ruoyi.ai.service.IModelService;
import com.ruoyi.ai.tool.JToolProvider;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysConfigService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
                               ModelBuilder modelBuilder) {
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
                modelBuilder);
    }

    @Override
    public Flux<String> chat(AIAgent aiAgent, String prompt, String clientId, String sessionId) {
        String promptTemplate = aiAgent.getPromptTemplate();
        saveChatMessage(prompt, clientId, sessionId, aiAgent.getId(), ChatMessageType.USER);
        AIAssistant assistant = this.buildAIAssistant(aiAgent);
        promptTemplate = promptTemplate.replace(Constants.USER_MSG_TEMPLATE, prompt);
        String finalPromptTemplate = promptTemplate;
        return Flux.create(fluxSink -> {
            ThreadUtil.execute(() -> {
                TokenStream tokenStream = assistant.chatStream(finalPromptTemplate);
                AtomicInteger thinkCount = new AtomicInteger(0);
                AtomicBoolean thinkFinish = new AtomicBoolean(false);
                tokenStream.onPartialThinking(thinkingChunk -> {
                            String text = thinkingChunk.text();
                            if (thinkCount.incrementAndGet() == 1) {
                                text = Constants.THINK_PREFIX_TAG + text;
                            }
                            Map<String, String> msg = MapUtil.<String, String>builder()
                                    .put("msg", text)
                                    .build();
                            try {
                                fluxSink.next(objectMapper.writeValueAsString(msg));
                            } catch (JsonProcessingException ignore) {
                            }
                        }).onPartialResponse(chunk -> {
                            if (thinkCount.intValue() > 0 && !thinkFinish.get()) {
                                chunk = Constants.THINK_SUFFIX_TAG + chunk;
                                thinkFinish.set(true);
                            }
                            Map<String, String> msg = MapUtil.<String, String>builder()
                                    .put("msg", chunk)
                                    .build();
                            try {
                                fluxSink.next(objectMapper.writeValueAsString(msg));
                            } catch (JsonProcessingException ignored) {
                            }
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
                            saveChatMessage(content.toString(), clientId, sessionId, aiAgent.getId(), ChatMessageType.AI);
                        }).onToolExecuted(toolExecuted -> {
                            String result = toolExecuted.result();
                            ToolExecutionRequest request = toolExecuted.request();
                            log.info("调用工具完成{}, result={}", request.name(), result);
                            Map<String, String> msg = MapUtil.<String, String>builder()
                                    .put("tool_called", request.name())
                                    .build();
                            try {
                                fluxSink.next(objectMapper.writeValueAsString(msg));
                            } catch (JsonProcessingException ignored) {
                            }
                        }).beforeToolExecution(beforeToolExecution -> {
                            ToolExecutionRequest request = beforeToolExecution.request();
                            log.info("调用工具{}", request.name());
                            Map<String, String> msg = MapUtil.<String, String>builder()
                                    .put("tool_call", request.name())
                                    .build();
                            try {
                                fluxSink.next(objectMapper.writeValueAsString(msg));
                            } catch (JsonProcessingException ignored) {
                            }
                        })
                        .onError(fluxSink::error)
                        .start();
            });
        });
    }


    protected AIAssistant buildAIAssistant(AIAgent aiAgent) {
        Long modelId = aiAgent.getModelId();
        Model model = modelService.selectModelById(modelId);
        StreamingChatModel llm = modelBuilder.getStreamingLLM(model);
        ChatMemoryStore memoryStore = aiConfig.getAgent().getMemoryStrategy() == AiConfig.Agent.MemoryStrategy.REDIS ?
                redisMemoryChatMemoryStore : inMemoryChatMemoryStore;

        EmbeddingStoreContentRetriever contentRetriever = null;
        String kbIds = aiAgent.getKbIds();
        if (StringUtils.isNotBlank(kbIds)) {
            List<Long> ids = Arrays.stream(kbIds.split(",")).map(Long::valueOf).toList();
            List<KnowledgeBase> kbs = knowledgeBaseService.selectKnowledgeBaseByIds(ids);
            List<Long> kbIdList = kbs.stream().map(KnowledgeBase::getId).toList();
            contentRetriever = EmbeddingStoreContentRetriever.builder()
                    .embeddingModel(getEmbeddingModel())
                    .minScore(aiAgent.getMinScore())
                    .maxResults(aiAgent.getMaxResult())
                    .embeddingStore(PgVectorEmbeddingStore.builder().build())
                    .filter(new IsIn("kb_id", kbIdList))
                    .build();
        }
        return AiServices.builder(AIAssistant.class)
                .streamingChatModel(llm)
                .systemMessageProvider(x -> aiAgent.getSystemMessage())
                .chatMemory(MessageWindowChatMemory.builder()
                        .chatMemoryStore(memoryStore)
                        .maxMessages(aiAgent.getMemoryCount())
                        .build())
                .toolProvider(new JToolProvider(aiAgent))
                .contentRetriever(contentRetriever)
                .maxSequentialToolsInvocations(10)
                .build();
    }


}
