package com.ruoyi.ai.component;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.util.Constants;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisMemoryChatMemoryStore implements ChatMemoryStore {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 读取会话全部历史消息
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        // 1. 空值拦截
        if (memoryId == null) {
            return new ArrayList<>();
        }
        String key = Constants.MEMORY_CACHE_KEY_PREFIX + String.valueOf(memoryId);
        try {
            // 读取全量JSON字符串
            List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
            if (CollUtil.isEmpty(jsonList)) {
                return new ArrayList<>();
            }
            List<ChatMessage> messageList = new ArrayList<>(jsonList.size());
            TypeReference<ChatMessage> typeRef = new TypeReference<>() {
            };
            for (String json : jsonList) {
                ChatMessage message = objectMapper.readValue(json, typeRef);
                messageList.add(message);
            }
            return messageList;
        } catch (Exception e) {
            log.error("read memory message from redis error", e);
            // 序列化失败/Redis异常 返回空会话，避免程序崩溃
            return new ArrayList<>();
        }
    }

    /**
     * 更新会话消息（全量覆盖）
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        if (memoryId == null || CollUtil.isEmpty(messages)) {
            return;
        }
        String key = Constants.MEMORY_CACHE_KEY_PREFIX + String.valueOf(memoryId);
        try {
            // 1. 先清空旧数据，保证全量覆盖
            redisTemplate.delete(key);
            List<String> jsonMessages = new ArrayList<>(messages.size());
            for (ChatMessage msg : messages) {
                String json = objectMapper.writeValueAsString(msg);
                jsonMessages.add(json);
            }
            // 2. 尾部插入，保证对话时序正确
            redisTemplate.opsForList().rightPushAll(key, jsonMessages);
            // 3. 限制最大消息条数，截断头部旧消息
            long currentSize = redisTemplate.opsForList().size(key);
            if (currentSize > Constants.MAX_MEMORY_MESSAGE_COUNT) {
                // 保留后N条最新消息
                redisTemplate.opsForList().trim(key, currentSize - Constants.MAX_MEMORY_MESSAGE_COUNT, -1);
            }
            // 4. 设置过期时间
            if (Constants.MEMORY_TTL_SECONDS > 0) {
                redisTemplate.expire(key, Constants.MEMORY_TTL_SECONDS, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            // 可增加日志记录异常
            log.error("update memory message from redis error", e);
        }
    }

    /**
     * 清空指定会话记忆
     */
    @Override
    public void deleteMessages(Object memoryId) {
        if (memoryId == null) {
            return;
        }
        String key = Constants.MEMORY_CACHE_KEY_PREFIX + String.valueOf(memoryId);
        redisTemplate.delete(key);
    }
}