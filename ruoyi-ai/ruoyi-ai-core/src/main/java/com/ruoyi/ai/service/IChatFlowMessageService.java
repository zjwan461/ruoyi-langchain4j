package com.ruoyi.ai.service;

import com.ruoyi.ai.domain.ChatFlowMessage;

import java.util.List;
import java.util.Map;

/**
 * 聊天信息Service接口
 *
 * @author ruoyi
 * @date 2025-09-08
 */
public interface IChatFlowMessageService {
    /**
     * 查询聊天信息
     *
     * @param id 聊天信息主键
     * @return 聊天信息
     */
    public ChatFlowMessage selectChatMessageById(Long id);

    /**
     * 查询聊天信息列表
     *
     * @param chatFlowMessage 聊天信息
     * @return 聊天信息集合
     */
    public List<ChatFlowMessage> selectChatMessageList(ChatFlowMessage chatFlowMessage);

    /**
     * 新增聊天信息
     *
     * @param chatFlowMessage 聊天信息
     * @return 结果
     */
    public int insertChatMessage(ChatFlowMessage chatFlowMessage);

    /**
     * 修改聊天信息
     *
     * @param chatFlowMessage 聊天信息
     * @return 结果
     */
    public int updateChatMessage(ChatFlowMessage chatFlowMessage);

    /**
     * 批量删除聊天信息
     *
     * @param ids 需要删除的聊天信息主键集合
     * @return 结果
     */
    public int deleteChatMessageByIds(Long[] ids);

    /**
     * 删除聊天信息信息
     *
     * @param id 聊天信息主键
     * @return 结果
     */
    public int deleteChatMessageById(Long id);

    public List<Map<String, String>> selectSessionList(String clientId, Long agentId, Integer limit);


    public List<ChatFlowMessage> selectAgentChatMessageBySessionId(String sessionId, Long agentId);

    /**
     * 按时间倒序分页查询（需配合 PageHelper.startPage 使用）
     *
     * @param sessionId 会话ID
     * @param agentId 智能体ID
     * @return 当前页的聊天信息（倒序）
     */
    public List<ChatFlowMessage> selectAgentChatMessageBySessionIdOrderDesc(String sessionId, Long agentId);

    public int deleteBySessionId(String sessionId);
}
