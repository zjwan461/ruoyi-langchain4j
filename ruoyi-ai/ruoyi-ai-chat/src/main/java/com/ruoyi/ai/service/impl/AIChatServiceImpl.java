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
import com.ruoyi.ai.enums.ModelType;
import com.ruoyi.ai.service.*;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysConfigService;
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
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@Primary
public class AIChatServiceImpl extends AIBaseChatServiceImpl {

    @Resource
    private LangChain4jService langChain4jService;

    @Resource
    private IModelService modelService;

    @Resource
    private IKnowledgeBaseService knowledgeBaseService;

    @Resource
    private ModelBuilder modelBuilder;

    public AIChatServiceImpl(IChatMessageService chatMessageService,
                             InMemoryChatMemoryStore inMemoryChatMemoryStore,
                             RedisMemoryChatMemoryStore redisMemoryChatMemoryStore,
                             AiConfig aiConfig,
                             ObjectMapper objectMapper,
                             RedisTemplate<Object, Object> redisTemplate,
                             RedisScript<Long> limitScript,
                             ISysConfigService sysConfigService) {
        super(chatMessageService,
                inMemoryChatMemoryStore,
                redisMemoryChatMemoryStore,
                aiConfig,
                objectMapper,
                redisTemplate,
                limitScript,
                sysConfigService);
    }


    @Override
    public Flux<String> chat(AIAgent aiAgent, String prompt, String clientId, String sessionId) {
        Model model = modelService.selectModelById(aiAgent.getModelId());

        StreamingChatModel llm = modelBuilder.getStreamingLLM(model);

        String promptTemplate = aiAgent.getPromptTemplate();
        promptTemplate = promptTemplate.replace(Constants.USER_MSG_TEMPLATE, prompt);

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
                    embBuilder.append(text);
                });
                promptTemplate = promptTemplate.replace(Constants.KNOWLEDGE_BASE_TEMPLATE, embBuilder.toString());
            }
        } else if (promptTemplate.contains(Constants.KNOWLEDGE_BASE_TEMPLATE) && StringUtils.isBlank(aiAgent.getKbIds())) {
            promptTemplate = promptTemplate.replace(Constants.KNOWLEDGE_BASE_TEMPLATE, "");
        }

        saveChatMessage(prompt, clientId, sessionId, aiAgent.getId(), ChatMessageType.USER);

        ChatMemory chatMemory = null;
        if (aiAgent.getMemoryCount() != null && aiAgent.getMemoryCount() > 0) {
            ChatMemoryStore memoryStore = aiConfig.getAgent().getMemoryStrategy() == AiConfig.Agent.MemoryStrategy.REDIS ?
                    redisMemoryChatMemoryStore : inMemoryChatMemoryStore;
            chatMemory = MessageWindowChatMemory.builder()
                    .chatMemoryStore(memoryStore)
                    .maxMessages(aiAgent.getMemoryCount())
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
            ThreadUtil.execute(() -> {
                AtomicInteger thinkCount = new AtomicInteger(0);
                AtomicBoolean thinkFinish = new AtomicBoolean(false);
                llm.chat(chatRequest, new StreamingChatResponseHandler() {

                    @Override
                    public void onPartialThinking(PartialThinking partialThinking) {
                        String text = partialThinking.text();
                        if (thinkCount.incrementAndGet() == 1) {
                            text = Constants.THINK_PREFIX_TAG + text;
                        }
                        Map<String, String> msg = MapUtil.<String, String>builder()
                                .put("msg", text)
                                .build();
                        try {
                            fluxSink.next(objectMapper.writeValueAsString(msg));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void onPartialResponse(String partialResponse) {
                        if (thinkCount.intValue() > 0 && !thinkFinish.get()) {
                            partialResponse = Constants.THINK_SUFFIX_TAG + partialResponse;
                            thinkFinish.set(true);
                        }
                        Map<String, String> msg = MapUtil.<String, String>builder()
                                .put("msg", partialResponse)
                                .build();
                        try {
                            fluxSink.next(objectMapper.writeValueAsString(msg));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
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
                        saveChatMessage(content.toString(), clientId, sessionId, aiAgent.getId(), ChatMessageType.AI);
                    }

                    @Override
                    public void onError(Throwable error) {
                        fluxSink.error(error);
                    }
                });

            });
        });

    }

    private EmbeddingModel getEmbeddingModel() {
        Model query = new Model();
        query.setType(ModelType.EMBEDDING.getValue());
        List<Model> models = modelService.selectModelList(query);
        if (models == null) {
            throw new ServiceException("尚未配置embedding模型,无法进行知识库查询");
        }
        String value = sysConfigService.selectConfigByKey("ai.model.embedding");
        if (StringUtils.isBlank(value)) {
            log.warn("not configure default embedding model, will use the first one embedding model save in db");
            Model first = models.get(0);
            return modelBuilder.getEmbeddingModel(first);
        } else {
            long embId = Long.parseLong(value);
            Model model = models.stream().filter(x -> embId == x.getId()).findFirst().orElse(null);
            if (model == null) {
                throw new ServiceException("已配置的默认向量模型id未配置");
            }
            return modelBuilder.getEmbeddingModel(model);
        }

    }


}
