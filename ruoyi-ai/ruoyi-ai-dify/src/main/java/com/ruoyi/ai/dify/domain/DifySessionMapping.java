package com.ruoyi.ai.dify.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Dify 会话映射对象 dify_session_mapping
 *
 * @author ruoyi
 * @date 2025-10-27
 */
public class DifySessionMapping extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /**
     * agent chat session_id
     */
    @Excel(name = "agent chat session_id")
    private String sessionId;

    /**
     * agent chat client_id
     */
    @Excel(name = "agent chat client_id")
    private String clientId;

    @Excel(name = "agent_id")
    private Long agentId;

    /**
     * dify chat conversation_id
     */
    @Excel(name = "dify chat conversation_id")
    private String conversationId;

    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }

    public String getSessionId()
    {
        return sessionId;
    }

    public void setClientId(String clientId)
    {
        this.clientId = clientId;
    }

    public String getClientId()
    {
        return clientId;
    }

    public void setConversationId(String conversationId)
    {
        this.conversationId = conversationId;
    }

    public String getConversationId()
    {
        return conversationId;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("sessionId", getSessionId())
                .append("clientId", getClientId())
                .append("conversationId", getConversationId())
                .append("agentId", getAgentId())
                .append("createTime", getCreateTime())
                .toString();
    }
}