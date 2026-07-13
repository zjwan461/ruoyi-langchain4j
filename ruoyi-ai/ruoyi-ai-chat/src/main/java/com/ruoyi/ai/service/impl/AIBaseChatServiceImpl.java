package com.ruoyi.ai.service.impl;

import cn.hutool.core.map.MapUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.component.memory.RedisMemoryChatMemoryStore;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.domain.*;
import com.ruoyi.ai.enums.ModelType;
import com.ruoyi.ai.service.*;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.service.ISysConfigService;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class AIBaseChatServiceImpl implements IAIChatService {

    protected final IChatMessageService chatMessageService;

    protected final RedisMemoryChatMemoryStore redisMemoryChatMemoryStore;

    protected final InMemoryChatMemoryStore inMemoryChatMemoryStore;

    protected final AiConfig aiConfig;

    protected final ObjectMapper objectMapper;

    protected final RedisTemplate<Object, Object> redisTemplate;

    protected final RedisScript<Long> limitScript;

    protected final ISysConfigService sysConfigService;

    protected final IKnowledgeBaseService knowledgeBaseService;

    protected final IModelService modelService;

    protected final ModelBuilder modelBuilder;

    protected final IAIAgentService aiAgentService;

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    public AIBaseChatServiceImpl(IChatMessageService chatMessageService,
                                 InMemoryChatMemoryStore inMemoryChatMemoryStore,
                                 RedisMemoryChatMemoryStore redisMemoryChatMemoryStore,
                                 AiConfig aiConfig,
                                 ObjectMapper objectMapper,
                                 RedisTemplate<Object, Object> redisTemplate,
                                 RedisScript<Long> limitScript,
                                 ISysConfigService sysConfigService,
                                 IKnowledgeBaseService knowledgeBaseService,
                                 IModelService modelService,
                                 ModelBuilder modelBuilder, IAIAgentService aiAgentService) {
        this.chatMessageService = chatMessageService;
        this.inMemoryChatMemoryStore = inMemoryChatMemoryStore;
        this.redisMemoryChatMemoryStore = redisMemoryChatMemoryStore;
        this.aiConfig = aiConfig;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.limitScript = limitScript;
        this.sysConfigService = sysConfigService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.modelService = modelService;
        this.modelBuilder = modelBuilder;
        this.aiAgentService = aiAgentService;
    }

    @Override
    public Flux<String> chat(AIAgent aiAgent, String prompt, String clientId, String sessionId) {
        throw new ServiceException("not supported");
    }

    @Override
    public ChatMessage saveChatMessage(String content, String clientId, String sessionId, Long agentId,
                                       ChatMessageType chatMessageType) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setAgentId(agentId);
        chatMessage.setContent(content);
        if (chatMessageType == ChatMessageType.AI) {
            chatMessage.setRole("assistant");
        } else if (chatMessageType == ChatMessageType.USER) {
            chatMessage.setRole("user");
        } else if (chatMessageType == ChatMessageType.SYSTEM) {
            chatMessage.setRole("system");
        } else if (chatMessageType == ChatMessageType.TOOL_EXECUTION_RESULT) {
            chatMessage.setRole("tool_execution_result");
        } else {
            chatMessage.setRole(chatMessageType.name());
        }
        chatMessage.setClientId(clientId);
        chatMessage.setSessionId(sessionId);
        chatMessage.setCreateBy("system");
        chatMessageService.insertChatMessage(chatMessage);
        return chatMessage;
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
        return number.intValue() <= dayLmtPerClient;
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
    public List<ChatMessage> listAgentChatMessageBySessionIdOrderDesc(String sessionId, Long agentId) {
        return chatMessageService.selectAgentChatMessageBySessionIdOrderDesc(sessionId, agentId);
    }

    @Override
    public void deleteSession(String sessionId) {
        if (chatMessageService.deleteBySessionId(sessionId) > 0) {
//            chatMemories.remove(Constants.MEMORY_CACHE_KEY_PREFIX + sessionId);
            log.debug("remove session id:{}", sessionId);
        }
    }

    protected EmbeddingModel getEmbeddingModel() {
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

    protected void emitFluxEvent(String event, Object value, FluxSink<String> fluxSink) {
        Map<String, Object> msg = MapUtil.<String, Object>builder()
                .put(event, value)
                .build();

        try {
            fluxSink.next(objectMapper.writeValueAsString(msg));
        } catch (JsonProcessingException ignored) {
        }
    }

    protected void saveToolCallRecord(AIAgent aiAgent, String sessionId, String clientId, Long messageId,
                                      String toolName, String toolInvokeResult) {
        try {
            AgentToolInvoke invoke = new AgentToolInvoke();
            invoke.setAgentId(aiAgent.getId());
            invoke.setToolName(toolName);
            invoke.setClientId(clientId);
            invoke.setSessionId(sessionId);
            invoke.setMessageId(messageId);
            invoke.setToolInvokeResult(toolInvokeResult);
            aiAgentService.insertAgentToolInvoked(invoke);
        } catch (Exception e) {
            log.error("save tool call record error:", e);
        }
    }

    protected void saveKBRetrieved(AIAgent aiAgent, String sessionId, String clientId, Long messageId,
                                   String fileName, String text) {
        try {
            AgentKbInvoke invoke = new AgentKbInvoke();
            invoke.setAgentId(aiAgent.getId());
            invoke.setClientId(clientId);
            invoke.setSessionId(sessionId);
            invoke.setMessageId(messageId);
            invoke.setFileName(fileName);
            invoke.setText(text);
            aiAgentService.insertAgentKBInvoked(invoke);
        } catch (Exception e) {
            log.error("save kb retrieved error:", e);
        }
    }
}
