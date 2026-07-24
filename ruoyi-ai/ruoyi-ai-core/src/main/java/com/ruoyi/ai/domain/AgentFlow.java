package com.ruoyi.ai.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * agent_flow对象 agent_flow
 * 
 * @author Jerry
 * @date 2026-07-17
 */
public class AgentFlow extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** agent flow名称 */
    @Excel(name = "agent flow名称")
    private String agentFlowName;

    /** agent flow的json */
    private String flowJson;

    /** 状态 */
    @Excel(name = "状态")
    private Integer status;

    @Excel(name = "单个Client每日访问限制次数")
    private Integer dayLmtPerClient;

    private String visitUrl;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setAgentFlowName(String agentFlowName) 
    {
        this.agentFlowName = agentFlowName;
    }

    public String getAgentFlowName() 
    {
        return agentFlowName;
    }

    public void setFlowJson(String flowJson) 
    {
        this.flowJson = flowJson;
    }

    public String getFlowJson() 
    {
        return flowJson;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getVisitUrl() {
        return visitUrl;
    }

    public void setVisitUrl(String visitUrl) {
        this.visitUrl = visitUrl;
    }

    public Integer getDayLmtPerClient() {
        return dayLmtPerClient;
    }

    public void setDayLmtPerClient(Integer dayLmtPerClient) {
        this.dayLmtPerClient = dayLmtPerClient;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("agentFlowName", getAgentFlowName())
            .append("flowJson", getFlowJson())
            .append("visitUrl", getVisitUrl())
            .append("status", getStatus())
            .append("dayLmtPerClient", getDayLmtPerClient())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("createBy", getCreateBy())
            .append("updateBy", getUpdateBy())
            .toString();
    }
}
