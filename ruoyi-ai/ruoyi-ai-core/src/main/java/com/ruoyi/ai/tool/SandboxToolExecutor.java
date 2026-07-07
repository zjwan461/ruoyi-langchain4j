package com.ruoyi.ai.tool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;

// todo impl this executor
public class SandboxToolExecutor implements ToolExecutor {

    private final String code;

    public SandboxToolExecutor(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String execute(ToolExecutionRequest toolExecutionRequest, Object memoryId) {
        return "";
    }
}
