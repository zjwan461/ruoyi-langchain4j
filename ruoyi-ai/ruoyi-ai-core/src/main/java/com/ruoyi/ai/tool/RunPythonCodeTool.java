package com.ruoyi.ai.tool;

import com.ruoyi.ai.sandbox.SandboxClient;
import com.ruoyi.ai.sandbox.dto.SandboxResponse;
import com.ruoyi.common.utils.ExceptionUtil;
import com.ruoyi.common.utils.spring.SpringUtils;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RunPythonCodeTool {

    @Tool("执行python代码并获取结果。不可在python代码中写入文件。")
    public String runPythonCode(@P("要执行的python代码") String pythonCode) {
        try {
            SandboxClient client = SpringUtils.getBean(SandboxClient.class);
            String sessionId = client.createDefaultSession();
            SandboxResponse resp = client.execPython(sessionId, pythonCode);
            if (resp.isSuccess()) {
                return resp.getStdout();
            } else {
                return resp.getStderr();
            }
        } catch (Exception e) {
            return ExceptionUtil.getExceptionMessage(e);
        }
    }

    @Tool("检查python的sandbox环境是否正常")
    public boolean checkSandboxHealth() {
        SandboxClient client = SpringUtils.getBean(SandboxClient.class);
        return client.isHealth();
    }

    @Tool("查看python安装的依赖包")
    public String getPythonDependencies() {
        try {
            SandboxClient client = SpringUtils.getBean(SandboxClient.class);
            String sessionId = client.createDefaultSession();
            return client.pipList(sessionId);
        } catch (Exception e) {
            return ExceptionUtil.getExceptionMessage(e);
        }
    }

    @Tool("安装python依赖")
    public String updatePythonDependencies(
            @P("依赖列表。格式支持 '包名==版本' 或 '包名'，例如 'requests==2.31.0,pillow'") String dependencies) {
        try {
            SandboxClient client = SpringUtils.getBean(SandboxClient.class);
            String sessionId = client.createDefaultSession();
            SandboxResponse response = client.pipInstall(sessionId, dependencies);
            if (response.isSuccess()) {
                return response.getStdout();
            } else {
                return response.getStderr();
            }
        } catch (Exception e) {
            return ExceptionUtil.getExceptionMessage(e);
        }
    }
}
