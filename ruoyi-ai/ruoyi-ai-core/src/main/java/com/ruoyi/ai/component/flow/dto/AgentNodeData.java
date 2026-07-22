package com.ruoyi.ai.component.flow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * agent-node 的业务数据，对应 X6 节点的 data 字段。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentNodeData {
    private Integer agentId;
    private String name;
    private String description;
    private Boolean showReasoning;

    public Integer getAgentId() {
        return agentId;
    }

    public void setAgentId(Integer agentId) {
        this.agentId = agentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getShowReasoning() {
        return showReasoning;
    }

    public void setShowReasoning(Boolean showReasoning) {
        this.showReasoning = showReasoning;
    }
}
