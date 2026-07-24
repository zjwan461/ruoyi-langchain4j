package com.ruoyi.ai.component.flow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentSpec {

    private Long agentId;

    private String agentName;

    private String description;

    private String nodeId;

    private Boolean showReasoning;

    private Boolean flowMemory;
}
