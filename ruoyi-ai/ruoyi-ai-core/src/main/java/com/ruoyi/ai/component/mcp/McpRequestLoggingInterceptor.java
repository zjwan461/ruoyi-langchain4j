package com.ruoyi.ai.component.mcp;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * OkHttp 拦截器，用于在 MCP HTTP 传输中记录请求日志。
 * <p>
 * 当 {@code JHttpMcpTransport.Builder#logRequests(boolean)} 设置为 {@code true} 时，
 * 会被注册到 {@link okhttp3.OkHttpClient}，在每个请求发出前打印请求方法、URL、请求头以及请求体，
 * 方便排查 MCP 服务调用过程中的问题。
 * <p>
 * 默认使用名称为 {@code MCP} 的 SLF4J Logger，也可以通过
 * {@link JHttpMcpTransport.Builder#logger(Logger)} 自定义日志记录器。
 */
@Slf4j
public class McpRequestLoggingInterceptor implements Interceptor {

    /** 默认流量日志 Logger 名称，对应 {@code dev.langchain4j.mcp.client.logging.McpLoggers} 的默认值。 */
    public static final String DEFAULT_TRAFFIC_LOGGER_NAME = "MCP";

    private static final Logger DEFAULT_TRAFFIC_LOGGER = LoggerFactory.getLogger(DEFAULT_TRAFFIC_LOGGER_NAME);

    /** 本类自身的日志（用于在日志记录本身出错时输出告警）。 */
    private static final Logger SELF_LOG = LoggerFactory.getLogger(McpRequestLoggingInterceptor.class);

    /** 实际写入请求日志的 Logger。 */
    private final Logger trafficLog;

    /**
     * 构造一个请求日志拦截器。
     *
     * @param logger 自定义流量日志 Logger；若为 {@code null}，则使用默认的 {@value #DEFAULT_TRAFFIC_LOGGER_NAME} Logger。
     */
    public McpRequestLoggingInterceptor(Logger logger) {
        this.trafficLog = logger != null ? logger : DEFAULT_TRAFFIC_LOGGER;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        logRequest(request);
        return chain.proceed(request);
    }

    /**
     * 打印请求方法、URL、请求头和请求体。打印过程中任何异常都会被捕获并通过自身 Logger 记录，
     * 不会影响实际请求的发送。
     *
     * @param request 当前需要发送的 OkHttp 请求。
     */
    private void logRequest(Request request) {
        try {
            trafficLog.debug(
                    "Request:\n- method: {}\n- url: {}\n- headers: {}\n- body: {}",
                    request.method(),
                    request.url(),
                    formatHeaders(request.headers()),
                    readBody(request));
        } catch (Exception e) {
            SELF_LOG.warn("Error while logging MCP request: {}", e.getMessage());
        }
    }

    /**
     * 读取请求体内容。请求体为空时返回空字符串；读取过程中出现异常时返回错误描述字符串，
     * 以避免日志记录过程影响正常请求流程。
     *
     * @param request OkHttp 请求。
     * @return 请求体的 UTF-8 字符串表示。
     */
    private static String readBody(Request request) {
        try {
            if (request.body() == null) {
                return "";
            }
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (Exception e) {
            SELF_LOG.warn("Exception while reading MCP request body", e);
            return "Exception while getting body: " + e.getMessage();
        }
    }

    /**
     * 将 OkHttp 请求头格式化为 "[key: value]" 形式，并以逗号分隔。
     *
     * @param headers OkHttp 请求头集合。
     * @return 格式化后的请求头字符串。
     */
    static String formatHeaders(Headers headers) {
        return StreamSupport.stream(headers.spliterator(), false)
                .map(header -> {
                    String headerKey = header.component1();
                    String headerValue = header.component2();
                    return String.format("[%s: %s]", headerKey, headerValue);
                })
                .collect(Collectors.joining(", "));
    }
}