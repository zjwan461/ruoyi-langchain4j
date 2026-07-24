package com.ruoyi.ai.component.flow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * X6 画布中的通用单元格。
 * 根据 shape 字段将 cells 中的元素反序列化为节点或连线。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "shape", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AgentNode.class, name = "agent-node"),
        @JsonSubTypes.Type(value = FlowEdge.class, name = "edge")
})
public abstract class FlowCell {
    private String id;
    private String shape;
    private Integer zIndex;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public Integer getZIndex() {
        return zIndex;
    }

    public void setZIndex(Integer zIndex) {
        this.zIndex = zIndex;
    }
}
