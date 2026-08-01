package com.ruoyi.ai.mapper;

import com.ruoyi.ai.domain.ChatFlowMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * AgentFlow聊天信息Mapper接口
 *
 * @author ruoyi
 * @date 2025-09-08
 */
public interface ChatFlowMessageMapper {
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


    public List<Map<String, String>> selectSessionList(@Param("clientId") String clientId, @Param("agentFlowId") Long agentFlowId, @Param("limit") Integer limit);


    public List<ChatFlowMessage> selectAgentChatMessageBySessionId(@Param("sessionId") String sessionId, @Param("agentFlowId") Long agentFlowId);

    int deleteBySessionId(String sessionId);

    List<ChatFlowMessage> selectAgentChatMessageBySessionIdWithInvokeData(@Param("sessionId") String sessionId, @Param("agentFlowId") Long agentFlowId);

    /**
     * 按时间倒序分页查询（与 PageHelper 配合使用，由调用方 startPage 拦截）
     *
     * @param sessionId 会话ID
     * @param agentFlowId 智能体ID
     * @return 聊天信息集合（按 create_time desc, id desc）
     */
    List<ChatFlowMessage> selectAgentChatMessageBySessionIdWithInvokeDataOrderDesc(@Param("sessionId") String sessionId, @Param("agentFlowId") Long agentFlowId);

    // ========================= 对话日志（新增） =========================

    /**
     * 对话日志：按 traceId 分组聚合查询 Agent Flow 一次对话的概要
     *
     * @param agentFlowId 智能体Flow ID（必填）
     * @param sessionId   会话ID（可选）
     * @param traceId     追踪ID（可选，模糊匹配）
     * @return 每次对话一行，包含 traceId、sessionId、msgCount、agentCount、agentNames、startTime、endTime、userQuestion、lastAnswer
     */
    List<Map<String, Object>> selectChatLogTraceList(@Param("agentFlowId") Long agentFlowId,
                                                    @Param("sessionId") String sessionId,
                                                    @Param("traceId") String traceId);

    /**
     * 对话日志：根据 traceId 查询 Agent Flow 一次对话的全部消息（含 invoke 数据）
     *
     * @param traceId 追踪ID
     * @return 消息集合（按 create_time asc, id asc）
     */
    List<ChatFlowMessage> selectChatLogMessagesByTraceId(@Param("traceId") String traceId);

    /**
     * 对话日志：根据 traceId 查询 Agent Flow 一次对话的全部消息（不含 invoke 数据）
     *
     * @param traceId 追踪ID
     * @return 消息集合（按 create_time asc, id asc）
     */
    List<ChatFlowMessage> selectChatLogSimpleMessagesByTraceId(@Param("traceId") String traceId);

    /**
     * 对话日志：查询某 Agent Flow 流水中出现过的全部 sessionId
     *
     * @param agentFlowId 智能体Flow ID
     * @return sessionId 列表
     */
    List<Map<String, Object>> selectChatLogSessionIdList(@Param("agentFlowId") Long agentFlowId);

    /**
     * 对话日志：删除单个 traceId 对应的所有消息（Agent Flow）
     *
     * @param traceId 追踪ID
     * @return 删除行数
     */
    int deleteChatLogByTraceId(@Param("traceId") String traceId);

    /**
     * 对话日志：查询 Agent Flow 一次对话中参与的 Agent 列表
     *
     * @param traceId 追踪ID
     * @return 参与的 Agent 列表
     */
    List<Map<String, Object>> selectChatLogAgentsByTraceId(@Param("traceId") String traceId);
}
