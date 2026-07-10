package com.ruoyi.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
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

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AIAssistantExecutor extends AIBaseChatServiceImpl {

    @Resource
    private PgVectorEmbeddingStore pgVectorEmbeddingStore;

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
        AIAssistant assistant = this.buildAIAssistant(aiAgent);
        return Flux.create(fluxSink -> {
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
                        saveChatMessage(content.toString(), clientId, sessionId, aiAgent.getId(), ChatMessageType.AI);
                    }).onToolExecuted(toolExecuted -> {
                        String result = toolExecuted.result();
                        ToolExecutionRequest request = toolExecuted.request();
                        log.info("调用工具完成{}, result={}", request.name(), result);
                        this.emitFluxEvent(Constants.SSE_EVENT_TOOL_CALLED, request.name(), fluxSink);
                    }).beforeToolExecution(beforeToolExecution -> {
                        ToolExecutionRequest request = beforeToolExecution.request();
                        log.info("调用工具{}", request.name());
                        this.emitFluxEvent(Constants.SSE_EVENT_TOOL_CALL, request.name(), fluxSink);
                    }).onRetrieved(contents -> {
                        log.info("知识库查询完成，共{}条", contents.size());
                        List<Map<String, Object>> retrieveList = contents.stream().map(content -> {
                            String text = content.textSegment().text();
                            Long kbId = content.textSegment().metadata().getLong("kb_id");
                            String fileName = content.textSegment().metadata().getString("file_name");
                            return MapUtil.<String, Object>builder()
                                    .put("kbId", kbId)
                                    .put("fileName", fileName)
                                    .put("text", text)
                                    .build();
                        }).toList();
                        if (CollUtil.isNotEmpty(retrieveList)) {
                            this.emitFluxEvent(Constants.SSE_EVENT_RETRIEVED, retrieveList, fluxSink);
                        }
                    })
                    .onError(fluxSink::error)
                    .start();
        });
    }


    protected AIAssistant buildAIAssistant(AIAgent aiAgent) {
        Long modelId = aiAgent.getModelId();
        Model model = modelService.selectModelById(modelId);
        StreamingChatModel llm = modelBuilder.getStreamingLLM(model);
        ChatMemoryStore memoryStore = aiConfig.getAgent().getMemoryStrategy() == AiConfig.Agent.MemoryStrategy.REDIS ?
                redisMemoryChatMemoryStore : inMemoryChatMemoryStore;
        AiServices<AIAssistant> builder = AiServices.builder(AIAssistant.class);
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
                    .embeddingStore(pgVectorEmbeddingStore)
                    .filter(new IsIn("kb_id", kbIdList))
                    .build();
            builder.contentRetriever(contentRetriever);
        }

        return builder.streamingChatModel(llm)
                .systemMessageProvider(x -> aiAgent.getSystemMessage())
                .chatMemory(MessageWindowChatMemory.builder()
                        .chatMemoryStore(memoryStore)
                        .maxMessages(aiAgent.getMemoryCount())
                        .build())
                .toolProvider(new JToolProvider(aiAgent))
                .maxSequentialToolsInvocations(aiConfig.getAgent().getMaxSequentialToolsInvocations())
                .build();
    }


}
