package com.ruoyi.ai.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

/**
 * mcp对象 agent_mcp_service
 * 
 * @author Jerry
 * @date 2026-07-15
 */
public class AgentMcpService extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** mcp服务名称 */
    @Excel(name = "mcp服务名称")
    private String mcpServiceName;

    /** mcp链接 */
    @Excel(name = "mcp链接")
    private String mcpUrl;

    /** mcp transport */
    @Excel(name = "mcp transport")
    private String mcpTransport;

    @Excel(name = "auth token")
    private String authToken;

    /** agent_mcp_tool信息 */
    private List<AgentMcpTool> agentMcpToolList;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setMcpServiceName(String mcpServiceName) 
    {
        this.mcpServiceName = mcpServiceName;
    }

    public String getMcpServiceName() 
    {
        return mcpServiceName;
    }

    public void setMcpUrl(String mcpUrl) 
    {
        this.mcpUrl = mcpUrl;
    }

    public String getMcpUrl() 
    {
        return mcpUrl;
    }

    public void setMcpTransport(String mcpTransport) 
    {
        this.mcpTransport = mcpTransport;
    }

    public String getMcpTransport() 
    {
        return mcpTransport;
    }

    public List<AgentMcpTool> getAgentMcpToolList()
    {
        return agentMcpToolList;
    }

    public void setAgentMcpToolList(List<AgentMcpTool> agentMcpToolList)
    {
        this.agentMcpToolList = agentMcpToolList;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("mcpServiceName", getMcpServiceName())
            .append("mcpUrl", getMcpUrl())
            .append("mcpTransport", getMcpTransport())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("createBy", getCreateBy())
            .append("updateBy", getUpdateBy())
            .append("agentMcpToolList", getAgentMcpToolList())
            .append("authToken", getAuthToken())
            .toString();
    }
}
