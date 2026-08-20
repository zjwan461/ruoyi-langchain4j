package com.ruoyi.ai.tool;

import java.util.List;
import java.util.Map;

import com.ruoyi.ai.sandbox.SandboxSdk;
import com.ruoyi.common.utils.spring.SpringUtils;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RunPythonCodeTool {

    @Tool("执行python代码并获取结果")
    public String runPythonCode(@P("要执行的python代码") String pythonCode) {
        SandboxSdk sdk = SpringUtils.getBean(SandboxSdk.class);
        return sdk.runPythonAndGetStdout(pythonCode);
    }

    @Tool("检查python的sandbox环境是否正常")
    public boolean checkSandboxHealth() {
        SandboxSdk sdk = SpringUtils.getBean(SandboxSdk.class);
        return sdk.health();
    }

    @Tool("查看python依赖")
    public List<Map<String, String>> getPythonDependencies() {
        SandboxSdk sdk = SpringUtils.getBean(SandboxSdk.class);
        return sdk.getDependencies();
    }

    @Tool("安装python依赖")
    public boolean updatePythonDependencies(
            @P("依赖列表。格式支持 '包名==版本' 或 '包名'，例如 'requests==2.31.0,pillow'。多个用','拼接") List<String> dependencies) {
        SandboxSdk sdk = SpringUtils.getBean(SandboxSdk.class);
        return sdk.updateDependencies(dependencies);
    }
}
