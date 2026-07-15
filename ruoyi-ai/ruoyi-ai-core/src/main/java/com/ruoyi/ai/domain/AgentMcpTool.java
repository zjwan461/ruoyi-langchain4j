package com.ruoyi.ai.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * agent_mcp_tool对象 agent_mcp_tool
 * 
 * @author Jerry
 * @date 2026-07-15
 */
public class AgentMcpTool extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** mcp工具名称 */
    @Excel(name = "mcp工具名称")
    private String mcpToolName;

    /** mcp工具描述 */
    @Excel(name = "mcp工具描述")
    private String mcpToolDescription;

    /** mcp工具参数 */
    @Excel(name = "mcp工具参数")
    private String mcpToolParameters;

    /** 所属mcp service id */
    @Excel(name = "所属mcp service id")
    private Long pid;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }
    public void setMcpToolName(String mcpToolName) 
    {
        this.mcpToolName = mcpToolName;
    }

    public String getMcpToolName() 
    {
        return mcpToolName;
    }
    public void setMcpToolDescription(String mcpToolDescription) 
    {
        this.mcpToolDescription = mcpToolDescription;
    }

    public String getMcpToolDescription() 
    {
        return mcpToolDescription;
    }
    public void setMcpToolParameters(String mcpToolParameters) 
    {
        this.mcpToolParameters = mcpToolParameters;
    }

    public String getMcpToolParameters() 
    {
        return mcpToolParameters;
    }
    public void setPid(Long pid) 
    {
        this.pid = pid;
    }

    public Long getPid() 
    {
        return pid;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("mcpToolName", getMcpToolName())
            .append("mcpToolDescription", getMcpToolDescription())
            .append("mcpToolParameters", getMcpToolParameters())
            .append("pid", getPid())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("createBy", getCreateBy())
            .append("updateBy", getUpdateBy())
            .toString();
    }
}
