package com.ruoyi.ai.component.flow.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * X6 流程连线，起止节点 ID 分别位于 source.cell 和 target.cell。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("edge")
public class FlowEdge extends FlowCell {
    private EdgeEndpoint source;
    private EdgeEndpoint target;

    @JsonIgnore
    public String getEdgeId() {
        return getId();
    }

    @JsonIgnore
    public String getSourceNodeId() {
        return source == null ? null : source.getCell();
    }

    @JsonIgnore
    public String getTargetNodeId() {
        return target == null ? null : target.getCell();
    }

    public EdgeEndpoint getSource() {
        return source;
    }

    public void setSource(EdgeEndpoint source) {
        this.source = source;
    }

    public EdgeEndpoint getTarget() {
        return target;
    }

    public void setTarget(EdgeEndpoint target) {
        this.target = target;
    }
}
