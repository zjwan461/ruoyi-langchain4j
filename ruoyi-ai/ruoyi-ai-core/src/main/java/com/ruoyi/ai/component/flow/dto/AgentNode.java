package com.ruoyi.ai.component.flow.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * X6 智能体节点。
 * 画布单元格 ID 位于 id，业务属性位于 data。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("agent-node")
public class AgentNode extends FlowCell {
    private Position position;
    private Boolean visible;
    private AgentNodeData data;

    @JsonIgnore
    public String getNodeId() {
        return getId();
    }

    @JsonIgnore
    public Integer getAgentId() {
        return data == null ? null : data.getAgentId();
    }

    @JsonIgnore
    public String getAgentName() {
        return data == null ? null : data.getName();
    }

    @JsonIgnore
    public String getDescription() {
        return data == null ? null : data.getDescription();
    }

    @JsonIgnore
    public Boolean getShowReasoning() {
        return data == null ? null : data.getShowReasoning();
    }

    @JsonIgnore
    public Boolean getFlowMemory() {
        return data ==null ? null : data.getFlowMemory();
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public AgentNodeData getData() {
        return data;
    }

    public void setData(AgentNodeData data) {
        this.data = data;
    }
}
