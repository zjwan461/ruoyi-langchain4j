package com.ruoyi.ai.component.flow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowExport {
    private String version;
    private String exportTime;
    private Integer nodeCount;
    private Integer edgeCount;
    private List<AgentNode> nodes;
    private List<FlowEdge> edges;

    // getter & setter
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getExportTime() {
        return exportTime;
    }

    public void setExportTime(String exportTime) {
        this.exportTime = exportTime;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Integer getEdgeCount() {
        return edgeCount;
    }

    public void setEdgeCount(Integer edgeCount) {
        this.edgeCount = edgeCount;
    }

    public List<AgentNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<AgentNode> nodes) {
        this.nodes = nodes;
    }

    public List<FlowEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<FlowEdge> edges) {
        this.edges = edges;
    }
}