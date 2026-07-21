package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.domain.ChatFlowMessage;
import com.ruoyi.ai.mapper.ChatFlowMessageMapper;
import com.ruoyi.ai.service.IChatFlowMessageService;
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
public class ChatFlowMessageServiceImpl implements IChatFlowMessageService {
    @Autowired
    private ChatFlowMessageMapper chatFlowMessageMapper;

    /**
     * 查询聊天信息
     *
     * @param id 聊天信息主键
     * @return 聊天信息
     */
    @Override
    public ChatFlowMessage selectChatMessageById(Long id) {
        return chatFlowMessageMapper.selectChatMessageById(id);
    }

    /**
     * 查询聊天信息列表
     *
     * @param chatFlowMessage 聊天信息
     * @return 聊天信息
     */
    @Override
    public List<ChatFlowMessage> selectChatMessageList(ChatFlowMessage chatFlowMessage) {
        return chatFlowMessageMapper.selectChatMessageList(chatFlowMessage);
    }

    /**
     * 新增聊天信息
     *
     * @param chatFlowMessage 聊天信息
     * @return 结果
     */
    @Override
    public int insertChatMessage(ChatFlowMessage chatFlowMessage) {
        chatFlowMessage.setCreateTime(DateUtils.getNowDate());
        return chatFlowMessageMapper.insertChatMessage(chatFlowMessage);
    }

    /**
     * 修改聊天信息
     *
     * @param chatFlowMessage 聊天信息
     * @return 结果
     */
    @Override
    public int updateChatMessage(ChatFlowMessage chatFlowMessage) {
        return chatFlowMessageMapper.updateChatMessage(chatFlowMessage);
    }

    /**
     * 批量删除聊天信息
     *
     * @param ids 需要删除的聊天信息主键
     * @return 结果
     */
    @Override
    public int deleteChatMessageByIds(Long[] ids) {
        return chatFlowMessageMapper.deleteChatMessageByIds(ids);
    }

    /**
     * 删除聊天信息信息
     *
     * @param id 聊天信息主键
     * @return 结果
     */
    @Override
    public int deleteChatMessageById(Long id) {
        return chatFlowMessageMapper.deleteChatMessageById(id);
    }

    @Override
    public List<Map<String, String>> selectSessionList(String clientId, Long agentId, Integer limit) {
        return chatFlowMessageMapper.selectSessionList(clientId, agentId, limit);
    }

    @Override
    public List<ChatFlowMessage> selectAgentChatMessageBySessionId(String sessionId, Long agentId) {
//        return chatMessageMapper.selectAgentChatMessageBySessionId(sessionId, agentId);
        return chatFlowMessageMapper.selectAgentChatMessageBySessionIdWithInvokeData(sessionId, agentId);
    }

    @Override
    public List<ChatFlowMessage> selectAgentChatMessageBySessionIdOrderDesc(String sessionId, Long agentId) {
        return chatFlowMessageMapper.selectAgentChatMessageBySessionIdWithInvokeDataOrderDesc(sessionId, agentId);
    }

    @Override
    public int deleteBySessionId(String sessionId) {
        return chatFlowMessageMapper.deleteBySessionId(sessionId);
    }
}
