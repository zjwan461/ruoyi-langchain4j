package com.ruoyi.ai.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * agent_tool对象 agent_tool
 * 
 * @author Jerry
 * @date 2026-07-07
 */
public class AgentTool extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 工具名称 */
    @Excel(name = "工具名称")
    private String toolName;

    /** 工具类型，可选项：builtin,sandbox */
    @Excel(name = "工具类型，可选项：builtin,sandbox")
    private String toolType;

    /** 工具代码，仅sandbox类型有值 */
    private String code;

    /** 内置工具全类名，仅builtin类型有值 */
    private String clazz;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setToolName(String toolName) 
    {
        this.toolName = toolName;
    }

    public String getToolName() 
    {
        return toolName;
    }

    public void setToolType(String toolType) 
    {
        this.toolType = toolType;
    }

    public String getToolType() 
    {
        return toolType;
    }

    public void setCode(String code) 
    {
        this.code = code;
    }

    public String getCode() 
    {
        return code;
    }

    public void setClazz(String clazz) 
    {
        this.clazz = clazz;
    }

    public String getClazz() 
    {
        return clazz;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("toolName", getToolName())
            .append("toolType", getToolType())
            .append("code", getCode())
            .append("clazz", getClazz())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("createBy", getCreateBy())
            .append("updateBy", getUpdateBy())
            .toString();
    }
}
