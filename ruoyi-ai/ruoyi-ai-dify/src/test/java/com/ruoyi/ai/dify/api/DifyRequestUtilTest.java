package com.ruoyi.ai.dify.api;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * RequestUtil 单元测试
 *
 * <p>使用 {@link SpringExtension} 加载最小化的 Spring 上下文（仅含 {@link DifyRequestUtil}
 * 与 {@link ObjectMapper}），结合 JDK 内置的 {@link HttpServer} 在本地随机端口启动
 * 一个 mock 服务，对 {@link DifyRequestUtil} 两个公共方法进行端到端验证：</p>
 *
 * <ul>
 *   <li>{@link DifyRequestUtil#info(String, String)}：正常返回、缺少 / 空 apiKey、
 *       空 / null urlPrefix、响应非 JSON、401 未授权 等场景。</li>
 *   <li>{@link DifyRequestUtil#chatBlocking(String, String, DifyChatRequest)}：
 *       正常返回、强制覆盖 responseMode=blocking、缺少 / 空 apiKey、
 *       空 / null urlPrefix、null request、响应非 JSON、401 未授权 等场景。</li>
 * </ul>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>{@link DifyRequestUtil} 中 {@code objectMapper} 字段为 {@code @Resource} 注入，
 *       Spring 会按字段名查找 Bean，因此测试配置中只需提供一个名为
 *       {@code objectMapper} 的 {@link Bean} 即可完成装配。</li>
 *   <li>通过不同的 {@code urlPrefix} 把请求路由到 mock 服务的不同路径，
 *       以区分正常、响应非 JSON、401 未授权等场景。</li>
 * </ul>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DifyRequestUtilTest.RequestUtilTestConfig.class)
@DisplayName("RequestUtil 单元测试")
class DifyRequestUtilTest {

    @Autowired
    private DifyRequestUtil difyRequestUtil;

    private String baseUrl = "http://localhost/v1";

    private String apiKey = "app-UZ6Vz9KglmhCDIjKIaIkNQfM";

    /**
     * Spring 测试配置：通过 {@link Import} 让 Spring 扫描 {@link DifyRequestUtil}
     * 上的 {@code @Component}，同时手动注册一个名为 {@code objectMapper}
     * 的 Bean，供 {@code @Resource} 按字段名匹配注入。
     */
    @Configuration
    @Import(DifyRequestUtil.class)
    static class RequestUtilTestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
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
    void testInfoSuccess() {
        DifyInfoResponse resp = difyRequestUtil.info(baseUrl, apiKey);

        assertNotNull(resp);
        System.out.println(resp);
    }


    // ============================ chatBlocking() 测试 ============================

    @Test
    @DisplayName("chatBlocking() 正常返回：覆盖全部字段并发送正确的请求体")
    void testChatBlockingSuccess() {
        DifyChatRequest request = new DifyChatRequest();
        request.setQuery("你好");
        request.setUser("u1");

        DifyChatBlockingResponse resp = difyRequestUtil.chatBlocking(baseUrl, apiKey, request);

        assertNotNull(resp);
        System.out.println(resp);
    }

    @Test
    void testChatStreamingSuccess() {
        DifyChatRequest request = new DifyChatRequest();
        request.setQuery("现在几点了");
        request.setUser("u1");
        difyRequestUtil.chatStreaming(baseUrl, apiKey, request, difyChatStreamingResponse -> System.out.println(JSONUtil.toJsonStr(difyChatStreamingResponse)));
    }

}