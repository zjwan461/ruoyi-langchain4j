package com.ruoyi.ai.sandbox;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * dify-sandbox 轻量级 SDK。
 *
 * <p>dify-sandbox 是 Dify 团队开源的代码执行沙箱（基于 gVisor + seccomp），
 * 提供 HTTP API 接收 Python 代码并安全执行。本 SDK 负责与该服务进行交互。</p>
 *
 * <p>对应 docker-compose 中的 {@code sandbox} 服务（端口 {@code 8194}）。
 * 配置统一通过 {@link AiConfig#getSandbox()} 注入（{@code ai.sandbox.*}）：</p>
 * <ul>
 *     <li>{@code ai.sandbox.base-url} = {@code http://sandbox:8194}</li>
 *     <li>{@code ai.sandbox.api-key}  = {@code dify-sandbox}</li>
 *     <li>{@code ai.sandbox.timeout}  = {@code 30} 秒</li>
 * </ul>
 *
 * <p>典型用法：</p>
 * <pre>{@code
 * @Resource
 * private SandboxSdk sandboxSdk;
 *
 * // 1) 直接运行 Python 代码并获取 stdout
 * String output = sandboxSdk.runPythonAndGetStdout("print(1 + 1)");
 *
 * // 2) 获取完整响应（含 stdout / stderr / error）
 * SandboxResponse resp = sandboxSdk.runPython("import sys; print(sys.version)");
 *
 * // 3) 健康检查
 * boolean ok = sandboxSdk.health();
 * }</pre>
 *
 * <p>对应接口：</p>
 * <ul>
 *     <li>{@code GET  /health}           — 健康检查</li>
 *     <li>{@code POST /v1/sandbox/run}   — 运行代码</li>
 * </ul>
 */
@Slf4j
@Component
public class SandboxSdk {

    /**
     * 运行代码路径（不含域名）。
     */
    private static final String PATH_RUN = "/v1/sandbox/run";

    /**
     * 健康检查路径（不含域名）。
     */
    private static final String PATH_HEALTH = "/health";

    /**
     * dify-sandbox 鉴权头名称。
     */
    private static final String HEADER_API_KEY = "X-Api-Key";

    /**
     * dify-sandbox 业务失败时的兜底 baseUrl（仅当 {@code ai.sandbox.base-url} 未配置时使用）。
     */
    private static final String DEFAULT_BASE_URL = "http://sandbox:8194";

    /**
     * dify-sandbox 业务失败时的兜底 apiKey。
     */
    private static final String DEFAULT_API_KEY = "dify-sandbox";

    /**
     * dify-sandbox 业务失败时的兜底超时秒数。
     */
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    @Resource
    private AiConfig aiConfig;

    @Resource
    private ObjectMapper objectMapper;

    // ------------------------------------------------------------------
    // 公开 API：便捷方法
    // ------------------------------------------------------------------

    /**
     * 健康检查。直接请求 {@code GET /health}，状态码 200 即视为健康。
     *
     * @return true-服务可用；false-服务不可用或网络异常
     */
    public boolean health() {
        String url = buildUrl(PATH_HEALTH);
        try (HttpResponse response = HttpUtil.createRequest(Method.GET, url)
                .timeout(getTimeoutMs())
                .header(HEADER_API_KEY, getApiKey())
                .execute()) {
            return response.isOk();
        } catch (Exception e) {
            log.warn("dify-sandbox health check fail, url={}, message={}", url, e.getMessage());
            return false;
        }
    }

    /**
     * 运行 Python 代码并返回完整响应。
     *
     * <p>即使 HTTP 返回 2xx，{@link SandboxResponse#getCode()} 非 0 也表示
     * 沙箱内 Python 执行失败，可通过 {@link SandboxResponse#getStderr()} 获取错误信息。</p>
     *
     * @param pythonCode Python 源码
     * @return Sandbox 响应（永远非空）
     * @throws ServiceException 当 HTTP 非 2xx 或响应反序列化失败时抛出
     */
    public SandboxResponse runPython(String pythonCode) {
        SandboxRequest request = new SandboxRequest(pythonCode, "python3");
        return run(request);
    }

    /**
     * 运行 Python 代码并仅返回 {@code stdout}。
     *
     * <p>若执行失败（业务码非 0）抛出 {@link ServiceException}，
     * 错误信息合并 {@code error}、{@code stderr}、{@code stdout} 三者便于排查。</p>
     *
     * @param pythonCode Python 源码
     * @return {@code stdout} 内容
     * @throws ServiceException 当 sandbox 返回业务错误时抛出
     */
    public String runPythonAndGetStdout(String pythonCode) {
        SandboxResponse response = runPython(pythonCode);
        if (!response.isSuccess()) {
            String message = buildFailureMessage(response);
            log.error("dify-sandbox run python fail, code={}, message={}", response.getCode(), message);
            throw new ServiceException(message);
        }
        return response.getStdout();
    }

    /**
     * 执行通用 Sandbox 请求。
     *
     * <p>底层封装：序列化请求体 → 设置 {@code X-Api-Key} 头与 JSON Content-Type
     * → 调用 {@code POST /v1/sandbox/run} → 反序列化响应。</p>
     *
     * @param request 请求体（不能为空）
     * @return Sandbox 响应（永远非空）
     * @throws ServiceException 当参数为空、HTTP 非 2xx 或响应反序列化失败时抛出
     */
    public SandboxResponse run(SandboxRequest request) {
        if (request == null) {
            throw new ServiceException("sandbox request is null");
        }
        if (StringUtils.isBlank(request.getCode())) {
            throw new ServiceException("sandbox code is empty");
        }
        if (StringUtils.isBlank(request.getLanguage())) {
            request.setLanguage("python3");
        }

        String url = buildUrl(PATH_RUN);
        String body;
        try {
            body = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("format sandbox request to json fail...", e);
            throw new ServiceException("format sandbox request to json fail...");
        }

        log.debug("dify-sandbox run, url={}, language={}, code-len={}",
                url, request.getLanguage(), request.getCode().length());

        HttpRequest httpRequest = HttpUtil.createRequest(Method.POST, url)
                .timeout(getTimeoutMs())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .header(HEADER_API_KEY, getApiKey())
                .body(body);

        try (HttpResponse response = httpRequest.execute()) {
            if (!response.isOk()) {
                throw new ServiceException("request dify-sandbox return http status="
                        + response.getStatus() + ", response=" + response.body());
            }
            String respBody = response.body();
            try {
                return objectMapper.readValue(respBody, SandboxResponse.class);
            } catch (JsonProcessingException e) {
                log.error("format sandbox response to json fail, body={}", respBody, e);
                throw new ServiceException("format sandbox response to json fail...");
            }
        }
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /**
     * 拼接 URL，自动处理末尾斜杠。
     */
    private String buildUrl(String path) {
        String prefix = getBaseUrl();
        prefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        String suffix = path.startsWith("/") ? path : "/" + path;
        return prefix + suffix;
    }

    /**
     * 构造 sandbox 业务失败时的错误信息，合并 error/stderr/stdout 三段便于排查。
     */
    private String buildFailureMessage(SandboxResponse response) {
        StringBuilder sb = new StringBuilder("dify-sandbox run fail, code=").append(response.getCode());
        if (StringUtils.isNotBlank(response.getStderr())) {
            sb.append(", error=").append(response.getStderr());
        }
        if (response.getData() != null) {
            if (StringUtils.isNotBlank(response.getStderr())) {
                sb.append(", stderr=").append(response.getStderr());
            }
            if (StringUtils.isNotBlank(response.getStdout())) {
                sb.append(", stdout=").append(response.getStdout());
            }
        }
        return sb.toString();
    }

    /**
     * 从 {@link AiConfig} 读取 {@code base-url}，缺失时使用兜底值。
     */
    private String getBaseUrl() {
        AiConfig.Sandbox sandbox = aiConfig == null ? null : aiConfig.getSandbox();
        if (sandbox == null || StringUtils.isBlank(sandbox.getBaseUrl())) {
            return DEFAULT_BASE_URL;
        }
        return sandbox.getBaseUrl();
    }

    /**
     * 从 {@link AiConfig} 读取 {@code api-key}，缺失时使用兜底值。
     */
    private String getApiKey() {
        AiConfig.Sandbox sandbox = aiConfig == null ? null : aiConfig.getSandbox();
        if (sandbox == null || StringUtils.isBlank(sandbox.getApiKey())) {
            return DEFAULT_API_KEY;
        }
        return sandbox.getApiKey();
    }

    /**
     * 从 {@link AiConfig} 读取 {@code timeout}（秒），缺失或非法时使用兜底值。
     */
    private int getTimeoutSeconds() {
        AiConfig.Sandbox sandbox = aiConfig == null ? null : aiConfig.getSandbox();
        if (sandbox == null || sandbox.getTimeout() <= 0) {
            return DEFAULT_TIMEOUT_SECONDS;
        }
        return sandbox.getTimeout();
    }

    /**
     * 将秒转换为 Hutool 所需的毫秒。
     */
    private int getTimeoutMs() {
        return getTimeoutSeconds() * 1000;
    }
}
