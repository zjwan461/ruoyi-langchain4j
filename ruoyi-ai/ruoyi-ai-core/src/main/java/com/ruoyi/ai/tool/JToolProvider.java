package com.ruoyi.ai.tool;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.domain.*;
import com.ruoyi.ai.service.IAgentMcpService;
import com.ruoyi.ai.service.LangChain4jService;
import com.ruoyi.ai.util.SimplePythonFuncParser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.internal.JsonSchemaElementJsonUtils;
import dev.langchain4j.mcp.McpToolExecutor;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.*;
import dev.langchain4j.skills.DefaultSkill;
import dev.langchain4j.skills.Skill;
import dev.langchain4j.skills.Skills;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class JToolProvider implements ToolProvider {

    private final AIAgent aiAgent;

    private final boolean enableFlowTool;

    public JToolProvider(AIAgent aiAgent, boolean enableFlowTool) {
        this.aiAgent = aiAgent;
        this.enableFlowTool = enableFlowTool;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        ObjectMapper objectMapper = SpringUtils.getBean(ObjectMapper.class);
        AiConfig aiConfig = SpringUtils.getBean(AiConfig.class);
        List<AgentTool> agentTools = aiAgent.getAgentTools();
        List<AgentSkill> agentSkills = aiAgent.getAgentSkills();
        List<AgentMcpTool> agentMcpTools = aiAgent.getAgentMcpTools();
        Map<ToolSpecification, ToolExecutor> toolsProvider = new LinkedHashMap<>();
        String sandboxDict = DictUtils.getDictValue("ai_tool_type", "sandbox");
        String builtinDict = DictUtils.getDictValue("ai_tool_type", "builtin");
        for (AgentTool tool : agentTools) {
            // 处理sandbox类型tool
            if (tool.getToolType().equals(sandboxDict)) {
                SandboxToolExecutor sandboxToolExecutor = new SandboxToolExecutor(tool);
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
            }
            // 处理内置java类型tool
            else if (tool.getToolType().equals(builtinDict)) {
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
        // 处理skill
        if (CollUtil.isNotEmpty(agentSkills)) {
            List<DefaultSkill> list = agentSkills.stream().map(agentSkill -> Skill.builder()
                    .name(agentSkill.getSkillName())
                    .description(agentSkill.getSkillName())
                    .content(agentSkill.getSkillContent())
                    .build()).toList();
            Skills skills = Skills.from(list);
            ToolProvider toolProvider = skills.toolProvider();
            ToolProviderResult toolProviderResult = toolProvider.provideTools(request);
            for (AiServiceTool aiServiceTool : toolProviderResult.aiServiceTools()) {
                toolsProvider.put(aiServiceTool.toolSpecification(), aiServiceTool.toolExecutor());
            }
        }
        if (CollUtil.isNotEmpty(agentMcpTools)) {
            IAgentMcpService mcpService = SpringUtils.getBean(IAgentMcpService.class);
            LangChain4jService langChain4jService = SpringUtils.getBean(LangChain4jService.class);
            Set<Long> mcpServiceIds = agentMcpTools.stream().map(AgentMcpTool::getPid).collect(Collectors.toSet());
            List<AgentMcpService> mcpServices = mcpService.selectAgentMcpServiceByIds(mcpServiceIds.toArray(Long[]::new));
            Map<Long, McpClient> mcpClientMap = new LinkedHashMap<>();
            for (AgentMcpService service : mcpServices) {
                McpTransport mcpTransport = langChain4jService.getMcpTransport(service.getMcpUrl(), service.getMcpTransport(), service.getAuthToken());
                DefaultMcpClient client = DefaultMcpClient.builder()
                        .transport(mcpTransport)
                        .toolExecutionTimeout(aiConfig.getMcp().getTimeout())
                        .build();
                mcpClientMap.put(service.getId(), client);
            }
            for (AgentMcpTool agentMcpTool : agentMcpTools) {
                McpClient mcpClient = mcpClientMap.get(agentMcpTool.getPid());
                LinkedHashMap<String, Object> map = null;
                Map<String, Object> metadata = null;
                try {
                    if (agentMcpTool.getMcpToolParameters() != null) {
                        map = objectMapper.readValue(agentMcpTool.getMcpToolParameters(), new TypeReference<>() {
                        });
                    }
                    if (agentMcpTool.getMcpToolMetadata() != null) {
                        metadata = objectMapper.readValue(agentMcpTool.getMcpToolMetadata(), new TypeReference<>() {
                        });
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
                if (map != null) {
                    JsonObjectSchema jsonObjectSchema = (JsonObjectSchema) JsonSchemaElementJsonUtils.fromMap(map);
                    McpToolExecutor mcpToolExecutor = new McpToolExecutor(mcpClient, agentMcpTool.getMcpToolName());
                    ToolSpecification toolSpecification = ToolSpecification.builder()
                            .parameters(jsonObjectSchema)
                            .name(agentMcpTool.getMcpToolName())
                            .description(agentMcpTool.getMcpToolDescription())
                            .metadata(metadata)
                            .build();
                    toolsProvider.put(toolSpecification, mcpToolExecutor);
                }
            }
        }
        return new ToolProviderResult(toolsProvider);
    }

    public AIAgent getAiAgent() {
        return aiAgent;
    }

    public boolean isEnableFlowTool() {
        return enableFlowTool;
    }
}
