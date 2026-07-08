package com.ruoyi.ai.tool;

import com.ruoyi.ai.domain.AgentTool;
import com.ruoyi.ai.domain.AiAgent;
import com.ruoyi.ai.service.IAgentToolService;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.*;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
        List<AgentTool> agentTools = agentToolService.selectAgentToolByAgentId(aiAgent.getId());
        Map<ToolSpecification, ToolExecutor> toolsProvider = new LinkedHashMap<>();
        String sandboxDict = DictUtils.getDictValue("ai_tool_type", "sandbox");
        String builtinDict = DictUtils.getDictValue("ai_tool_type", "builtin");
        for (AgentTool tool : agentTools) {
            if (tool.getToolType().equals(sandboxDict)) {
                SandboxToolExecutor sandboxToolExecutor = new SandboxToolExecutor(tool.getCode());
                ToolSpecification toolSpecification = ToolSpecification.builder()
                        .name(tool.getToolName())
                        //todo handle params
                        .parameters(JsonObjectSchema.builder().build())
                        .description(tool.getDescription())
                        .build();
                toolsProvider.put(toolSpecification, sandboxToolExecutor);
            } else if (tool.getToolType().equals(builtinDict)) {
                String clazz = tool.getClazz();
                Object obj = null;
                List<Method> methods = new ArrayList<>();
                try {
                    Class<?> aClass = Class.forName(clazz);
                    obj = aClass.getDeclaredConstructor().newInstance();
                    for (Method method : aClass.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(Tool.class)) {
                            methods.add(method);
                        }
                    }
                } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                         InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                for (Method method : methods) {
                    String toolName = method.getAnnotation(Tool.class).name();
                    DefaultToolExecutor defaultToolExecutor = new DefaultToolExecutor(obj, method);
                    ToolSpecification toolSpecification = ToolSpecification.builder()
                            .name(toolName)
                            //todo handle params
                            .parameters(JsonObjectSchema.builder().build())
                            .description(tool.getDescription())
                            .build();
                    toolsProvider.put(toolSpecification, defaultToolExecutor);
                }
            } else {
                throw new IllegalArgumentException("not support: " + tool.getToolType());
            }
        }
        return new ToolProviderResult(toolsProvider);
    }
}
