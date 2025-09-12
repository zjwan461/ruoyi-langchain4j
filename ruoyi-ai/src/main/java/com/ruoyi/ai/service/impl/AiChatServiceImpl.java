package com.ruoyi.ai.service.impl;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.domain.AiAgent;
import com.ruoyi.ai.domain.ChatMessage;
import com.ruoyi.ai.domain.KnowledgeBase;
import com.ruoyi.ai.domain.Model;
import com.ruoyi.ai.enums.ModelType;
import com.ruoyi.ai.service.*;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.system.service.ISysConfigService;
import dev.langchain4j.data.message.ChatMessageType;
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
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiChatServiceImpl implements IAiChatService {

    @Resource
    private LangChain4jService langChain4jService;

    @Resource
    private IModelService modelService;

    @Resource
    private IKnowledgeBaseService knowledgeBaseService;

    @Resource
    private ModelBuilder modelBuilder;

    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    @Resource
    private RedisScript<Long> limitScript;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private IChatMessageService chatMessageService;

    @Resource
    private ISysConfigService sysConfigService;

    private static final TimedCache<String, ChatMemory> chatMemories = new TimedCache<>(
            TimeUnit.DAYS.toMillis(1));


    @Override
    public Flux<String> chat(AiAgent aiAgent, String prompt, String clientId, String sessionId) {
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

                List<EmbeddingMatch<TextSegment>> searchRes = langChain4jService.search(embeddingModel,
                        prompt, 3, 0.70, new IsIn("kb_id", ids));
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
            chatMemory = chatMemories.get(Constants.MEMORY_CACHE_KEY_PREFIX + sessionId);
            if (chatMemory == null) {
                chatMemory = MessageWindowChatMemory.builder()
                        .chatMemoryStore(new InMemoryChatMemoryStore())
                        .maxMessages(aiAgent.getMemoryCount())
                        .id(sessionId)
                        .build();
                chatMemories.put(Constants.MEMORY_CACHE_KEY_PREFIX + sessionId, chatMemory);
            }
        }
        UserMessage userMessage = UserMessage.from(promptTemplate);
        if (chatMemory != null) {
            chatMemory.add(userMessage);
        }

        ChatRequestParameters parameters = modelBuilder.getParameters(aiAgent);

        final ChatRequest chatRequest = ChatRequest.builder()
                .parameters(parameters)
                .messages(
                        chatMemory != null ? chatMemory.messages() : Collections.singletonList(userMessage))
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

    private void saveChatMessage(String content, String clientId, String sessionId, Long agentId,
                                 ChatMessageType chatMessageType) {
        AsyncManager.me().execute(new TimerTask() {
            @Override
            public void run() {
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setAgentId(agentId);
                chatMessage.setContent(content);
                if (chatMessageType == ChatMessageType.AI) {
                    chatMessage.setRole("assistant");
                } else if (chatMessageType == ChatMessageType.USER) {
                    chatMessage.setRole("user");
                } else {
                    chatMessage.setRole("system");
                }
                chatMessage.setClientId(clientId);
                chatMessage.setSessionId(sessionId);
                chatMessage.setCreateBy("system");
                chatMessageService.insertChatMessage(chatMessage);
            }
        });

    }

    @Override
    public String createSession(String clientId) {
        String sessionId = IdUtils.fastSimpleUUID();
        redisTemplate.opsForValue().set(Constants.AI_CHAT_CLIENT_SESSION + clientId, sessionId);
        return sessionId;
    }

    @Override
    public boolean checkClientSession(String clientId, String sessionId) {
        return sessionId.equals(redisTemplate.opsForValue().get(Constants.AI_CHAT_CLIENT_SESSION + clientId));
    }

    @Override
    public boolean checkIfOverLmtRequest(Long agentId, Integer dayLmtPerClient, String clientId) {
        Long number = redisTemplate.execute(limitScript,
                Collections.singletonList(Constants.AI_AGENT_CHAT_LMT + agentId + ":" + clientId),
                dayLmtPerClient,
                (int) TimeUnit.DAYS.toSeconds(1));
        return !StringUtils.isNull(number) && number.intValue() <= dayLmtPerClient;
    }

    @Override
    public List<Map<String, String>> listClientSession(String clientId, Long agentId) {
        String value = sysConfigService.selectConfigByKey("ai.agent.sessionNum");
        Integer limit = null;
        if (StringUtils.isNotEmpty(value)) {
            limit = Integer.valueOf(value);
        }
        List<Map<String, String>> res = chatMessageService.selectSessionList(clientId, agentId, limit);
        res.forEach(x -> {
            String title = x.get("title");
            if (title.length() > 5) {
                title = title.substring(0, 5) + "...";
            }
            x.put("title", title);
        });
        return res;
    }

    @Override
    public List<ChatMessage> listAgentChatMessageBySessionId(String sessionId, Long agentId) {
        return chatMessageService.selectAgentChatMessageBySessionId(sessionId, agentId);
    }

    @Override
    public void deleteSession(String sessionId) {
        if (chatMessageService.deleteBySessionId(sessionId) > 0) {
            chatMemories.remove(Constants.MEMORY_CACHE_KEY_PREFIX + sessionId);
        }
    }
}
