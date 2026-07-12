package com.ruoyi.ai.mapper;

import com.ruoyi.ai.domain.ChatMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 聊天信息Mapper接口
 *
 * @author ruoyi
 * @date 2025-09-08
 */
public interface ChatMessageMapper {
    /**
     * 查询聊天信息
     *
     * @param id 聊天信息主键
     * @return 聊天信息
     */
    public ChatMessage selectChatMessageById(Long id);

    /**
     * 查询聊天信息列表
     *
     * @param chatMessage 聊天信息
     * @return 聊天信息集合
     */
    public List<ChatMessage> selectChatMessageList(ChatMessage chatMessage);

    /**
     * 新增聊天信息
     *
     * @param chatMessage 聊天信息
     * @return 结果
     */
    public int insertChatMessage(ChatMessage chatMessage);

    /**
     * 修改聊天信息
     *
     * @param chatMessage 聊天信息
     * @return 结果
     */
    public int updateChatMessage(ChatMessage chatMessage);

    /**
     * 删除聊天信息
     *
     * @param id 聊天信息主键
     * @return 结果
     */
    public int deleteChatMessageById(Long id);

    /**
     * 批量删除聊天信息
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteChatMessageByIds(Long[] ids);


    public List<Map<String, String>> selectSessionList(@Param("clientId") String clientId, @Param("agentId") Long agentId, @Param("limit") Integer limit);


    public List<ChatMessage> selectAgentChatMessageBySessionId(@Param("sessionId") String sessionId, @Param("agentId") Long agentId);

    int deleteBySessionId(String sessionId);

    List<ChatMessage> selectAgentChatMessageBySessionIdWithInvokeData(@Param("sessionId") String sessionId, @Param("agentId") Long agentId);

    /**
     * 按时间倒序分页查询（与 PageHelper 配合使用，由调用方 startPage 拦截）
     *
     * @param sessionId 会话ID
     * @param agentId 智能体ID
     * @return 聊天信息集合（按 create_time desc, id desc）
     */
    List<ChatMessage> selectAgentChatMessageBySessionIdWithInvokeDataOrderDesc(@Param("sessionId") String sessionId, @Param("agentId") Long agentId);
}
