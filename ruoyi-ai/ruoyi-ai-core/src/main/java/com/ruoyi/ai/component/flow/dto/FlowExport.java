package com.ruoyi.ai.component.flow.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * X6 graph.toJSON() 的导出结构。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowExport {
    private List<FlowCell> cells = Collections.emptyList();

    public List<FlowCell> getCells() {
        return cells;
    }

    public void setCells(List<FlowCell> cells) {
        this.cells = cells == null ? Collections.emptyList() : cells;
    }

    @JsonIgnore
    public List<AgentNode> getNodes() {
        return cells.stream()
                .filter(AgentNode.class::isInstance)
                .map(AgentNode.class::cast)
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public List<FlowEdge> getEdges() {
        return cells.stream()
                .filter(FlowEdge.class::isInstance)
                .map(FlowEdge.class::cast)
                .collect(Collectors.toList());
    }
}
