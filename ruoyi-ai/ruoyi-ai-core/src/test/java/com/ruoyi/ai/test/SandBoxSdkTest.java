package com.ruoyi.ai.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.dify.api.DifyRequestUtil;
import com.ruoyi.ai.sandbox.SandboxRequest;
import com.ruoyi.ai.sandbox.SandboxResponse;
import com.ruoyi.ai.sandbox.SandboxSdk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SandBoxSdkTest.SandboxSdkTestConfig.class)
@DisplayName("SandboxSdk 单元测试")
class SandBoxSdkTest {

    @Autowired
    private SandboxSdk sandboxSdk;

    /**
     * Spring 测试配置：通过 {@link Import} 让 Spring 扫描 {@link DifyRequestUtil}
     * 上的 {@code @Component}，同时手动注册一个名为 {@code objectMapper}
     * 的 Bean，供 {@code @Resource} 按字段名匹配注入。
     */
    @Configuration
    @Import(SandboxSdk.class)
    static class SandboxSdkTestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public AiConfig aiConfig() {
            AiConfig aiConfig = new AiConfig();
            AiConfig.Sandbox sandbox = new AiConfig.Sandbox();
            sandbox.setApiKey("dify-sandbox");
            sandbox.setTimeout(60);
            sandbox.setBaseUrl("http://223.254.129.15:38194");
            aiConfig.setSandbox(sandbox);
            return aiConfig;
        }
    }

    @BeforeEach
    void setUp() throws IOException {
    }

    @AfterEach
    void tearDown() {

    }

    // ============================ info() 测试 ============================

    @Test
    @DisplayName("info() 正常返回：解析响应并设置 Authorization 头")
    void testSandbox() {
        System.out.println(sandboxSdk.health());
        SandboxRequest req = new SandboxRequest();
        req.setLanguage("python3");
        req.setCode("print(\"Hello, Dify-Sandbox!\")\n" +
                "a = 1 + 2\n" +
                "print(f\"1 + 2 = {a}\")");
        SandboxResponse sandboxResponse = sandboxSdk.run(req);
        System.out.println(sandboxResponse);
    }


}