package com.ruoyi.ai.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * agent_tool_invoke对象 agent_tool_invoke
 *
 * @author Jerry
 * @date 2026-07-07
 */
public class AgentToolInvoke extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    private Long id;

    /**
     * Agent ID
     */
    @Excel(name = "Agent ID")
    private Long agentId;

    /**
     * 客户端id
     */
    @Excel(name = "客户端id")
    private String clientId;

    /**
     * 会话id
     */
    @Excel(name = "会话id")
    private String sessionId;

    @Excel(name = "消息id")
    private Long messageId;

    private Long messageFlowId;

    /**
     * 工具名称
     */
    @Excel(name = "工具名称")
    private String toolName;

    /**
     * 工具调用结果
     */
    @Excel(name = "工具调用结果")
    private String toolInvokeResult;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public Long getAgentId() {
        return agentId;
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

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolInvokeResult(String toolInvokeResult) {
        this.toolInvokeResult = toolInvokeResult;
    }

    public String getToolInvokeResult() {
        return toolInvokeResult;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getMessageFlowId() {
        return messageFlowId;
    }

    public void setMessageFlowId(Long messageFlowId) {
        this.messageFlowId = messageFlowId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("agentId", getAgentId())
                .append("clientId", getClientId())
                .append("sessionId", getSessionId())
                .append("messageId", getMessageId())
                .append("messageFlowId", getMessageFlowId())
                .append("toolName", getToolName())
                .append("toolInvokeResult", getToolInvokeResult())
                .append("createTime", getCreateTime())
                .toString();
    }
}
