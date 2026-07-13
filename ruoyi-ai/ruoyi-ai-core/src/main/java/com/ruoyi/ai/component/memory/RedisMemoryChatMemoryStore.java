package com.ruoyi.ai.component.memory;

import cn.hutool.core.collection.CollUtil;
import com.ruoyi.ai.component.memory.dto.*;
import com.ruoyi.ai.util.Constants;
import dev.langchain4j.data.message.*;
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
    private RedisTemplate<Object, Object> redisTemplate;

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
            List<ChatMessage> memoryMessageList = (List<ChatMessage>) redisTemplate.opsForValue().get(key);
            if (CollUtil.isEmpty(memoryMessageList)) {
                return new ArrayList<>();
            }
            List<ChatMessage> messageList = new ArrayList<>(memoryMessageList.size());
            for (Object message : memoryMessageList) {
                if (message instanceof LangChain4jMemoryAdaptor<?>) {
                    ChatMessage chatMessage = (ChatMessage) ((LangChain4jMemoryAdaptor<?>) message).toLangChain4jMemoryComponent();
                    messageList.add(chatMessage);
                }
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
            List<ChatMessage> list = new ArrayList<>(messages.size());
            for (ChatMessage msg : messages) {
                if (msg instanceof AiMessage am) {
                    JAiMessage jam = new JAiMessage();
                    jam.fromLangChain4jMemoryComponent(am);
                    list.add(jam);
                } else if (msg instanceof SystemMessage sm) {
                    JSystemMessage jsm = new JSystemMessage();
                    jsm.fromLangChain4jMemoryComponent(sm);
                    list.add(jsm);
                } else if (msg instanceof CustomMessage cm) {
                    JCustomMessage jcm = new JCustomMessage();
                    jcm.fromLangChain4jMemoryComponent(cm);
                    list.add(jcm);
                } else if (msg instanceof ToolExecutionResultMessage trm) {
                    JToolExecutionResultMessage jtrm = new JToolExecutionResultMessage();
                    jtrm.fromLangChain4jMemoryComponent(trm);
                    list.add(jtrm);
                } else if (msg instanceof UserMessage um) {
                    JUserMessage jum = new JUserMessage();
                    jum.fromLangChain4jMemoryComponent(um);
                    list.add(jum);
                }
            }
            // 2. 尾部插入，保证对话时序正确
            int currentSize = list.size();
            if (currentSize > Constants.MAX_MEMORY_MESSAGE_COUNT) {
                // 保留后N条最新消息
                list = CollUtil.sub(list, currentSize - Constants.MAX_MEMORY_MESSAGE_COUNT, list.size());
            }
            redisTemplate.opsForValue().set(key, list);
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