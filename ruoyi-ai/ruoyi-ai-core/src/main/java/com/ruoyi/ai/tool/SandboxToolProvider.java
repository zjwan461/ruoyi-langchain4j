package com.ruoyi.ai.tool;

import com.ruoyi.ai.domain.AgentTool;
import com.ruoyi.ai.service.IAgentToolService;
import com.ruoyi.common.utils.DictUtils;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//todo impl this provider service
@Slf4j
@Component
public class SandboxToolProvider implements ToolProvider {

    @Resource
    private IAgentToolService agentToolService;

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        AgentTool agentTool = new AgentTool();
        agentTool.setToolType(DictUtils.getDictValue("ai_tool_type", "sandbox"));
        List<AgentTool> agentTools = agentToolService.selectAgentToolList(agentTool);
        Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
        for (AgentTool tool : agentTools) {
            SandboxToolExecutor sandboxToolExecutor = new SandboxToolExecutor(tool.getCode());
            ToolSpecification toolSpecification = ToolSpecification.builder()
                    .name(tool.getToolName())
                    //todo
                    .parameters(JsonObjectSchema.builder().build())
                    .description("")
                    .build();
            tools.put(toolSpecification, sandboxToolExecutor);
        }
        return new ToolProviderResult(tools);
    }
}
