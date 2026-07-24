package com.ruoyi.ai.component.flow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * X6 连线的 source/target 端点。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EdgeEndpoint {
    private String cell;
    private String port;

    public String getCell() {
        return cell;
    }

    public void setCell(String cell) {
        this.cell = cell;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
