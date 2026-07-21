package com.ruoyi.ai.component.flow.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentSpec {

    private Long agentId;

    private String agentName;

    private String description;

    private String nodeId;
}
