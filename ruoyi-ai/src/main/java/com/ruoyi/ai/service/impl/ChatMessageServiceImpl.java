package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.domain.ChatMessage;
import com.ruoyi.ai.mapper.ChatMessageMapper;
import com.ruoyi.ai.service.IChatMessageService;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 聊天信息Service业务层处理
 *
 * @author ruoyi
 * @date 2025-09-08
 */
@Service
public class ChatMessageServiceImpl implements IChatMessageService {
    @Autowired
    private ChatMessageMapper chatMessageMapper;

    /**
     * 查询聊天信息
     *
     * @param id 聊天信息主键
     * @return 聊天信息
     */
    @Override
    public ChatMessage selectChatMessageById(Long id) {
        return chatMessageMapper.selectChatMessageById(id);
    }

    /**
     * 查询聊天信息列表
     *
     * @param chatMessage 聊天信息
     * @return 聊天信息
     */
    @Override
    public List<ChatMessage> selectChatMessageList(ChatMessage chatMessage) {
        return chatMessageMapper.selectChatMessageList(chatMessage);
    }

    /**
     * 新增聊天信息
     *
     * @param chatMessage 聊天信息
     * @return 结果
     */
    @Override
    public int insertChatMessage(ChatMessage chatMessage) {
        chatMessage.setCreateTime(DateUtils.getNowDate());
        return chatMessageMapper.insertChatMessage(chatMessage);
    }

    /**
     * 修改聊天信息
     *
     * @param chatMessage 聊天信息
     * @return 结果
     */
    @Override
    public int updateChatMessage(ChatMessage chatMessage) {
        return chatMessageMapper.updateChatMessage(chatMessage);
    }

    /**
     * 批量删除聊天信息
     *
     * @param ids 需要删除的聊天信息主键
     * @return 结果
     */
    @Override
    public int deleteChatMessageByIds(Long[] ids) {
        return chatMessageMapper.deleteChatMessageByIds(ids);
    }

    /**
     * 删除聊天信息信息
     *
     * @param id 聊天信息主键
     * @return 结果
     */
    @Override
    public int deleteChatMessageById(Long id) {
        return chatMessageMapper.deleteChatMessageById(id);
    }

    @Override
    public List<Map<String, String>> selectSessionList(String clientId, Long agentId) {
        return chatMessageMapper.selectSessionList(clientId, agentId);
    }

    @Override
    public List<ChatMessage> selectAgentChatMessageBySessionId(String sessionId, Long agentId) {
        return chatMessageMapper.selectAgentChatMessageBySessionId(sessionId, agentId);
    }
}
