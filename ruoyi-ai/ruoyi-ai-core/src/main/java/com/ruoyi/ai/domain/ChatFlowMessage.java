package com.ruoyi.ai.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

/**
 * 聊天信息对象 chat_flow_message
 *
 * @author Jerry
 * @date 2026-07-21
 */
public class ChatFlowMessage extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 角色
     */
    @Excel(name = "角色")
    private String role;

    /**
     * 消息内容
     */
    @Excel(name = "消息内容")
    private String content;

    /**
     * 客户端ID
     */
    @Excel(name = "客户端ID")
    private String clientId;

    /**
     * 会话ID
     */
    @Excel(name = "会话ID")
    private String sessionId;

    @Excel(name = "智能体ID")
    private Long agentId;

    private Long agentFlowId;

    private String agentName;

    private List<AgentKbInvoke> retrievedList;

    private List<AgentToolInvoke> toolCalledList;

    private List<Attachment> attachments;

    /**
     * 追踪ID
     */
    private String traceId;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public List<AgentKbInvoke> getRetrievedList() {
        return retrievedList;
    }

    public void setRetrievedList(List<AgentKbInvoke> retrievedList) {
        this.retrievedList = retrievedList;
    }

    public List<AgentToolInvoke> getToolCalledList() {
        return toolCalledList;
    }

    public void setToolCalledList(List<AgentToolInvoke> toolCalledList) {
        this.toolCalledList = toolCalledList;
    }

    public Long getAgentFlowId() {
        return agentFlowId;
    }

    public void setAgentFlowId(Long agentFlowId) {
        this.agentFlowId = agentFlowId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("role", getRole())
                .append("content", getContent())
                .append("clientId", getClientId())
                .append("sessionId", getSessionId())
                .append("createTime", getCreateTime())
                .append("agentId", getAgentId())
                .append("agentFlowId", getAgentFlowId())
                .append("agentName", getAgentName())
                .append("retrievedList", getRetrievedList())
                .append("toolCalledList", getToolCalledList())
                .append("attachments", getAttachments())
                .append("traceId", getTraceId())
                .toString();
    }
}
