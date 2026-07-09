package com.ruoyi.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.component.RedisMemoryChatMemoryStore;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.domain.ChatMessage;
import com.ruoyi.ai.service.IAIChatService;
import com.ruoyi.ai.service.IChatMessageService;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.system.service.ISysConfigService;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
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

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    public AIBaseChatServiceImpl(IChatMessageService chatMessageService,
                                 InMemoryChatMemoryStore inMemoryChatMemoryStore,
                                 RedisMemoryChatMemoryStore redisMemoryChatMemoryStore,
                                 AiConfig aiConfig,
                                 ObjectMapper objectMapper,
                                 RedisTemplate<Object, Object> redisTemplate,
                                 RedisScript<Long> limitScript,
                                 ISysConfigService sysConfigService) {
        this.chatMessageService = chatMessageService;
        this.inMemoryChatMemoryStore = inMemoryChatMemoryStore;
        this.redisMemoryChatMemoryStore = redisMemoryChatMemoryStore;
        this.aiConfig = aiConfig;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.limitScript = limitScript;
        this.sysConfigService = sysConfigService;
    }

    @Override
    public Flux<String> chat(AIAgent aiAgent, String prompt, String clientId, String sessionId) {
        throw new ServiceException("not supported");
    }

    @Override
    public void saveChatMessage(String content, String clientId, String sessionId, Long agentId,
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
    public void deleteSession(String sessionId) {
        if (chatMessageService.deleteBySessionId(sessionId) > 0) {
//            chatMemories.remove(Constants.MEMORY_CACHE_KEY_PREFIX + sessionId);
            log.debug("remove session id:{}", sessionId);
        }
    }
}
