package com.ruoyi.ai.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 聊天信息对象 chat_message
 * 
 * @author ruoyi
 * @date 2025-09-08
 */
public class ChatMessage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 角色 */
    @Excel(name = "角色")
    private String role;

    /** 消息内容 */
    @Excel(name = "消息内容")
    private String content;

    /** 客户端ID */
    @Excel(name = "客户端ID")
    private String clientId;

    /** 会话ID */
    @Excel(name = "会话ID")
    private String sessionId;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setRole(String role) 
    {
        this.role = role;
    }

    public String getRole() 
    {
        return role;
    }

    public void setContent(String content) 
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public void setClientId(String clientId) 
    {
        this.clientId = clientId;
    }

    public String getClientId() 
    {
        return clientId;
    }

    public void setSessionId(String sessionId) 
    {
        this.sessionId = sessionId;
    }

    public String getSessionId() 
    {
        return sessionId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("role", getRole())
            .append("content", getContent())
            .append("clientId", getClientId())
            .append("sessionId", getSessionId())
            .append("createTime", getCreateTime())
            .toString();
    }
}
