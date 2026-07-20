package com.ruoyi.ai.tool;

import com.ruoyi.ai.component.flow.dto.AgentSpec;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.List;

public class FlowTool {

    @Tool("选择使用哪一个子智能体")
    public AgentSpec selectSubAgent(@P("子智能体列表") List<AgentSpec> subAgents) {
        return AgentSpec.builder().build();
    }
}
