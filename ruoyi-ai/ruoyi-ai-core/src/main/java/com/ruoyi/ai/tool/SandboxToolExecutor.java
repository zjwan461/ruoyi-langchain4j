package com.ruoyi.ai.tool;

import com.ruoyi.ai.sandbox.SandboxSdk;
import com.ruoyi.common.utils.spring.SpringUtils;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;

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
        SandboxSdk sandboxSdk = SpringUtils.getBean(SandboxSdk.class);
        return sandboxSdk.runPythonAndGetStdout(code);
    }
}
