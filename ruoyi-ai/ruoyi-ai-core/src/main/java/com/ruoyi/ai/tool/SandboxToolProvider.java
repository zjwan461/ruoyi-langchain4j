package com.ruoyi.ai.tool;

import com.ruoyi.ai.domain.AgentTool;
import com.ruoyi.ai.domain.AiAgent;
import com.ruoyi.ai.service.IAgentToolService;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//todo impl this provider service
@Slf4j
public class SandboxToolProvider implements ToolProvider {

    private final AiAgent aiAgent;

    public SandboxToolProvider(AiAgent aiAgent) {
        this.aiAgent = aiAgent;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        IAgentToolService agentToolService = SpringUtils.getBean(IAgentToolService.class);
        AgentTool agentTool = new AgentTool();
        agentTool.setToolType(DictUtils.getDictValue("ai_tool_type", "sandbox"));
        List<AgentTool> agentTools = agentToolService.selectAgentToolList(agentTool);
        Map<ToolSpecification, ToolExecutor> toolsProvider = new LinkedHashMap<>();
        for (AgentTool tool : agentTools) {
            SandboxToolExecutor sandboxToolExecutor = new SandboxToolExecutor(tool.getCode());
            ToolSpecification toolSpecification = ToolSpecification.builder()
                    .name(tool.getToolName())
                    .parameters(JsonObjectSchema.builder().build())
                    .description(tool.getDescription())
                    .build();
            toolsProvider.put(toolSpecification, sandboxToolExecutor);
        }
        return new ToolProviderResult(toolsProvider);
    }
}
