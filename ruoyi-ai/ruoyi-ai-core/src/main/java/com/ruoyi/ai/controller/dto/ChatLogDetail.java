package com.ruoyi.ai.controller.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 对话日志详情：单次对话全部消息 + 参与者列表
 *
 * @author ruoyi
 */
public class ChatLogDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * type: agent | agentFlow
     */
    private String type;

    /**
     * 单 Agent 模式下：智能体ID
     */
    private Long agentId;

    /**
     * Agent Flow 模式下：智能体Flow ID
     */
    private Long agentFlowId;

    /**
     * 当前对话的 traceId
     */
    private String traceId;

    /**
     * 当前对话的会话ID
     */
    private String sessionId;

    /**
     * 消息列表（按 create_time asc, id asc 排序）
     */
    private List<?> messages;

    /**
     * Agent Flow 模式下：参与本次对话的 Agent 列表
     */
    private List<Map<String, Object>> participantAgents;

    public ChatLogDetail() {
        this.messages = new ArrayList<>();
        this.participantAgents = new ArrayList<>();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public Long getAgentFlowId() {
        return agentFlowId;
    }

    public void setAgentFlowId(Long agentFlowId) {
        this.agentFlowId = agentFlowId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<?> getMessages() {
        return messages;
    }

    public void setMessages(List<?> messages) {
        this.messages = messages;
    }

    public List<Map<String, Object>> getParticipantAgents() {
        return participantAgents;
    }

    public void setParticipantAgents(List<Map<String, Object>> participantAgents) {
        this.participantAgents = participantAgents;
    }
}
