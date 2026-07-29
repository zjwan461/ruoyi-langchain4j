package com.ruoyi.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.component.memory.RedisMemoryChatMemoryStore;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.domain.ChatMessage;
import com.ruoyi.ai.domain.KnowledgeBase;
import com.ruoyi.ai.domain.Model;
import com.ruoyi.ai.service.*;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysConfigService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@Primary
public class AIChatServiceImpl extends AIBaseChatServiceImpl {

    @Resource
    private LangChain4jService langChain4jService;

    public AIChatServiceImpl(IChatMessageService chatMessageService,
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
        Model model = modelService.selectModelById(aiAgent.getModelId());

        StreamingChatModel llm = modelBuilder.getStreamingLLM(model);

        String promptTemplate = aiAgent.getPromptTemplate();
        if (promptTemplate.contains(Constants.USER_MSG_TEMPLATE)) {
            promptTemplate = promptTemplate.replace(Constants.USER_MSG_TEMPLATE, prompt);
        } else {
            promptTemplate = promptTemplate + prompt;
        }

        List<Map<String, Object>> retrievedContents = new ArrayList<>();
        if (promptTemplate.contains(Constants.KNOWLEDGE_BASE_TEMPLATE) && StringUtils.isNotBlank(aiAgent.getKbIds())) {
            List<Long> ids = Arrays.stream(aiAgent.getKbIds().split(",")).map(Long::valueOf)
                    .collect(Collectors.toList());
            List<KnowledgeBase> kbs = knowledgeBaseService.selectKnowledgeBaseByIds(ids);
            EmbeddingModel embeddingModel;
            if (kbs != null) {
                embeddingModel = getEmbeddingModel();

                List<EmbeddingMatch<TextSegment>> searchRes = langChain4jService.search(embeddingModel, prompt,
                        aiAgent.getMaxResult() == null ? 3 : aiAgent.getMaxResult(), aiAgent.getMinScore() == null ? 0.7 : aiAgent.getMinScore(),
                        new IsIn("kb_id", ids));
                StringBuilder embBuilder = new StringBuilder();
                searchRes.stream().map(EmbeddingMatch::embedded).forEach(embedded -> {
                    String text = embedded.text();
                    Metadata metadata = embedded.metadata();
                    Long kbId = metadata.getLong("kb_id");
                    String fileName = metadata.getString("file_name");
                    Map<String, Object> retrievedMetadata = MapUtil.<String, Object>builder()
                            .put("kbId", kbId)
                            .put("fileName", fileName)
                            .put("text", text)
                            .build();
                    retrievedContents.add(retrievedMetadata);
                    embBuilder.append(text);
                });
                promptTemplate = promptTemplate.replace(Constants.KNOWLEDGE_BASE_TEMPLATE, embBuilder.toString());
            }
        } else if (promptTemplate.contains(Constants.KNOWLEDGE_BASE_TEMPLATE) && StringUtils.isBlank(aiAgent.getKbIds())) {
            promptTemplate = promptTemplate.replace(Constants.KNOWLEDGE_BASE_TEMPLATE, "");
        }

        saveChatMessage(prompt, clientId, sessionId, aiAgent.getId(), ChatMessageType.USER, attachmentIds);

        ChatMemory chatMemory = null;
        if (aiAgent.getMemoryCount() != null && aiAgent.getMemoryCount() > 0) {
            ChatMemoryStore memoryStore = aiConfig.getAgent().getMemoryStrategy() == AiConfig.Agent.MemoryStrategy.REDIS ?
                    redisMemoryChatMemoryStore : inMemoryChatMemoryStore;
            chatMemory = MessageWindowChatMemory.builder()
                    .chatMemoryStore(memoryStore)
                    .maxMessages(aiAgent.getMemoryCount() * 2)
                    .id(sessionId)
                    .build();
        }

        SystemMessage systemMessage = null;
        String sysMsgContent = aiAgent.getSystemMessage();
        if (StringUtils.isNotBlank(sysMsgContent)) {
            systemMessage = SystemMessage.from(sysMsgContent);
        }

        UserMessage userMessage = UserMessage.from(promptTemplate);

        if (chatMemory != null) {
            if (systemMessage != null) {
                chatMemory.add(systemMessage);
            }
            chatMemory.add(userMessage);
        }

        ChatRequestParameters parameters = modelBuilder.getParameters(aiAgent);

        final ChatRequest chatRequest = ChatRequest.builder()
                .parameters(parameters)
                .messages(
                        chatMemory != null ? chatMemory.messages() : systemMessage != null ? Arrays.asList(systemMessage, userMessage) : Collections.singletonList(userMessage))
                .build();

        return Flux.create(fluxSink -> {
            if (CollUtil.isNotEmpty(retrievedContents)) {
                this.emitFluxEvent(Constants.SSE_EVENT_RETRIEVED, retrievedContents, fluxSink);

            }
            AtomicInteger thinkCount = new AtomicInteger(0);
            AtomicBoolean thinkFinish = new AtomicBoolean(false);
            AIBaseChatServiceImpl root = this;

            llm.chat(chatRequest, new StreamingChatResponseHandler() {

                @Override
                public void onPartialThinking(PartialThinking partialThinking) {
                    String text = partialThinking.text();
                    if (thinkCount.incrementAndGet() == 1) {
                        text = Constants.THINK_PREFIX_TAG + text;
                    }
                    root.emitFluxEvent(Constants.SSE_EVENT_MSG, text, fluxSink);
                }

                @Override
                public void onPartialResponse(String partialResponse) {
                    if (thinkCount.intValue() > 0 && !thinkFinish.get()) {
                        partialResponse = Constants.THINK_SUFFIX_TAG + partialResponse;
                        thinkFinish.set(true);
                    }
                    root.emitFluxEvent(Constants.SSE_EVENT_MSG, partialResponse, fluxSink);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    fluxSink.complete();
                    StringBuilder content = new StringBuilder();
                    String thinking = completeResponse.aiMessage().thinking();
                    if (StringUtils.isNotBlank(thinking)) {
                        content.append(Constants.THINK_PREFIX_TAG)
                                .append(thinking)
                                .append(Constants.THINK_SUFFIX_TAG);
                    }
                    String aiText = completeResponse.aiMessage().text();
                    content.append(aiText);
                    ChatMessage assistantMessage = saveChatMessage(content.toString(), clientId, sessionId, aiAgent.getId(), ChatMessageType.AI, null);
                    for (Map<String, Object> item : retrievedContents) {
                        root.saveKBRetrieved(aiAgent, sessionId, clientId, assistantMessage.getId(),
                                String.valueOf(item.get("fileName")), String.valueOf(item.get("text")));
                    }
                }

                @Override
                public void onError(Throwable error) {
                    fluxSink.error(error);
                    fluxSink.complete();
                }
            });

        });

    }


}
