package com.ruoyi.ai.controller.dto;

import java.io.Serializable;

/**
 * 对话日志分页查询条件
 * <p>
 * 用于前端 /ai/chatLog 接口入参。
 * type 决定查询 chat_message（单 Agent）还是 chat_flow_message（Agent Flow）。
 *
 * @author ruoyi
 */
public class ChatLogQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * agent | agentFlow
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
     * 会话ID（可选，精确匹配）
     */
    private String sessionId;

    /**
     * 追踪ID（可选，模糊匹配）
     */
    private String traceId;

    /**
     * 页码，从 1 开始
     */
    private Integer pageNum;

    /**
     * 每页条数
     */
    private Integer pageSize;

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

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
