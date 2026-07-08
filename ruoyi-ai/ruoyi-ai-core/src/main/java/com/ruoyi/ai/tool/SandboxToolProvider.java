package com.ruoyi.ai.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.domain.AgentTool;
import com.ruoyi.ai.domain.AiAgent;
import com.ruoyi.ai.service.IAgentToolService;
import com.ruoyi.ai.util.SimplePythonFuncParser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.*;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SandboxToolProvider implements ToolProvider {

    private final AiAgent aiAgent;

    private final ToolService toolService = new ToolService();

    public SandboxToolProvider(AiAgent aiAgent) {
        this.aiAgent = aiAgent;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        IAgentToolService agentToolService = SpringUtils.getBean(IAgentToolService.class);
        ObjectMapper objectMapper = SpringUtils.getBean(ObjectMapper.class);
        List<AgentTool> agentTools = agentToolService.selectAgentToolByAgentId(aiAgent.getId());
        Map<ToolSpecification, ToolExecutor> toolsProvider = new LinkedHashMap<>();
        String sandboxDict = DictUtils.getDictValue("ai_tool_type", "sandbox");
        String builtinDict = DictUtils.getDictValue("ai_tool_type", "builtin");
        for (AgentTool tool : agentTools) {
            if (tool.getToolType().equals(sandboxDict)) {
                SandboxToolExecutor sandboxToolExecutor = new SandboxToolExecutor(tool.getCode());
                ToolSpecification.Builder builder = ToolSpecification.builder()
                        .name(tool.getToolName())
                        .description(tool.getDescription());

                String parameters = tool.getParameters();
                List<SimplePythonFuncParser.Param> paramList = null;
                try {
                    paramList = objectMapper.readValue(
                            parameters,
                            new TypeReference<>() {
                            }
                    );
                } catch (JsonProcessingException ignored) {
                }
                if (paramList != null) {
                    JsonObjectSchema.Builder paramBuilder = JsonObjectSchema.builder();
                    for (SimplePythonFuncParser.Param param : paramList) {
                        if ("str".equals(param.getType())) {
                            paramBuilder.addStringProperty(param.getName());
                        } else if ("int".equals(param.getType())) {
                            paramBuilder.addIntegerProperty(param.getName());
                        } else if ("bool".equals(param.getType())) {
                            paramBuilder.addBooleanProperty(param.getName());
                        } else if ("float".equals(param.getType())) {
                            paramBuilder.addNumberProperty(param.getName());
                        } else {
                            paramBuilder.addStringProperty(param.getName());
                        }
                    }
                    builder.parameters(paramBuilder.build());
                }
                ToolSpecification toolSpecification = builder.build();
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
                    log.error(e.getMessage(), e);
                    throw new ServiceException("初始化AgentTool方法失败");
                }
                for (Method method : methods) {
                    DefaultToolExecutor defaultToolExecutor = new DefaultToolExecutor(obj, method);
                    ToolSpecification toolSpecification = ToolSpecifications.toolSpecificationFrom(method);
                    toolsProvider.put(toolSpecification, defaultToolExecutor);
                }
            } else {
                throw new IllegalArgumentException("not support tool type: " + tool.getToolType());
            }
        }
        return new ToolProviderResult(toolsProvider);
    }
}
