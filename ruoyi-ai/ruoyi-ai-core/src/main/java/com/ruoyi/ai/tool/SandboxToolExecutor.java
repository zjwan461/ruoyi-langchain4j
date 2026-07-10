package com.ruoyi.ai.tool;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.domain.AgentTool;
import com.ruoyi.ai.sandbox.SandboxSdk;
import com.ruoyi.ai.util.SimplePythonFuncParser;
import com.ruoyi.common.utils.spring.SpringUtils;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;

import java.util.List;
import java.util.Map;

public class SandboxToolExecutor implements ToolExecutor {

    private final AgentTool agentTool;

    public SandboxToolExecutor(AgentTool agentTool) {
        this.agentTool = agentTool;
    }

    public AgentTool getAgentTool() {
        return agentTool;
    }

    @Override
    public String execute(ToolExecutionRequest toolExecutionRequest, Object memoryId) {
        SandboxSdk sandboxSdk = SpringUtils.getBean(SandboxSdk.class);
        ObjectMapper objectMapper = SpringUtils.getBean(ObjectMapper.class);
        String toolName = agentTool.getToolName();
        String parameters = agentTool.getParameters();
        String code = agentTool.getCode();
        String arguments = toolExecutionRequest.arguments();

        List<SimplePythonFuncParser.Param> paramList = null;
        try {
            paramList = objectMapper.readValue(
                    parameters,
                    new TypeReference<>() {
                    }
            );
        } catch (JsonProcessingException ignored) {
        }
        Map<String, Object> invokedParams = null;
        try {
            invokedParams = objectMapper.readValue(
                    arguments,
                    new TypeReference<>() {
                    }
            );
        } catch (JsonProcessingException ignored) {
        }

        if (CollUtil.isNotEmpty(paramList) && MapUtil.isNotEmpty(invokedParams)) {
            code = code + "\nresult = " + toolName + "(";
            for (SimplePythonFuncParser.Param param : paramList) {
                String type = param.getType();
                Object value = invokedParams.get(param.getName());
                if ("str".equals(type)) {
                    code = code + param.getName() + "='" + value + "',";
                } else {
                    code = code + param.getName() + "=" + value + ",";
                }
            }
            code += ")";
        } else {
            code = code + "\nresult = " + toolName + "()";
        }
        code += "\nprint(result)";
        return sandboxSdk.runPythonAndGetStdout(code);
    }
}
