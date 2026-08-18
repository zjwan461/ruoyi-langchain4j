package com.ruoyi.ai.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * agent_kb_invoke对象 agent_kb_invoke
 *
 * @author Jerry
 * @date 2026-07-07
 */
public class AgentKbInvoke extends BaseEntity {
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
     * 文件名
     */
    @Excel(name = "文件名")
    private String fileName;

    /**
     * 文本召回的分段
     */
    @Excel(name = "文本召回的分段")
    private String text;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    private String url;

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

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    @Override
    public Date getCreateTime() {
        return createTime;
    }

    @Override
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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
                .append("fileName", getFileName())
                .append("text", getText())
                .append("createTime", getCreateTime())
                .append("url", getUrl())
                .toString();
    }
}
