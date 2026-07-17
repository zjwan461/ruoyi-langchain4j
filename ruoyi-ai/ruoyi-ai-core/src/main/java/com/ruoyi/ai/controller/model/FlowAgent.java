package com.ruoyi.ai.controller.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FlowAgent {

    private Long id;
    private String name;
    private String description;
    private Boolean showReasoning;
}
