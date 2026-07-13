package com.ruoyi.ai.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

import javax.validation.constraints.NotBlank;

/**
 * agent_skill对象 agent_skill
 * 
 * @author Jerry
 * @date 2026-07-13
 */
public class AgentSkill extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** skill名称 */
    @Excel(name = "skill名称")
    @NotBlank(message = "skill名称不能为空")
    private String skillName;

    /** skill描述 */
    @Excel(name = "skill描述")
    @NotBlank(message = "skill描述不能为空")
    private String skillDescription;

    /** skill内容 */
    @Excel(name = "skill内容")
    @NotBlank(message = "skill内容不能为空")
    private String skillContent;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setSkillName(String skillName) 
    {
        this.skillName = skillName;
    }

    public String getSkillName() 
    {
        return skillName;
    }

    public void setSkillDescription(String skillDescription) 
    {
        this.skillDescription = skillDescription;
    }

    public String getSkillDescription() 
    {
        return skillDescription;
    }

    public void setSkillContent(String skillContent) 
    {
        this.skillContent = skillContent;
    }

    public String getSkillContent() 
    {
        return skillContent;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("skillName", getSkillName())
            .append("skillDescription", getSkillDescription())
            .append("skillContent", getSkillContent())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("createBy", getCreateBy())
            .append("updateBy", getUpdateBy())
            .toString();
    }
}
