package com.ruoyi.ai.util;

import cn.hutool.core.lang.Assert;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Agentic Agent 服务 HTTP 请求工具。
 *
 * <p>
 * 对接 FastAPI 的 {@code POST /chat-agentic} 接口，支持 SSE 流式响应。
 * </p>
 *
 * <p>
 * 流式方法使用 Spring {@link WebClient} + {@link Flux} 订阅 {@link DataBuffer}，
 * 每个 SSE 事件到达 socket 即可立即回调 consumer，确保真正的 Server-Sent Events 流式语义。
 * </p>
 */
@Component
@Slf4j
public class AgenticRequestUtil {

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 服务完整地址
     */
    private final String serverUrl;

    /**
     * 复用的 WebClient，已配置连接超时和读取超时。
     *
     * <ul>
     * <li>通过 Reactor Netty {@link HttpClient} 配置连接超时和读取超时</li>
     * <li>关闭默认的内存缓冲上限（{@code maxInMemorySize=-1}），避免超长 SSE 流被提前缓存</li>
     * </ul>
     */
    private final WebClient webClient;

    public AgenticRequestUtil(AiConfig aiConfig) {
        AiConfig.Agentic agentic = aiConfig.getAgentic();
        Assert.notNull(agentic, "ai.agentic 配置不能为空");
        Assert.notBlank(agentic.getServerUrl(), "ai.agentic.server-url 不能为空");
        this.serverUrl = agentic.getServerUrl().trim();
        int connectTimeout = agentic.getConnectTimeout() <= 0 ? 30000 : agentic.getConnectTimeout();
        int readTimeout = agentic.getReadTimeout() <= 0 ? 300000 : agentic.getReadTimeout();

        // 配置 Reactor Netty HttpClient 超时
        HttpClient httpClient = HttpClient.create()
                // 连接超时
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                // 响应超时（从发送请求到收到第一个字节）
                .responseTimeout(Duration.ofMillis(readTimeout))
                // 读取超时：如果连续 readTimeout 毫秒没有新数据到达，则关闭连接
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS)));

        this.webClient = WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(-1))
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * SSE 流式对话。
     *
     * <p>
     * 使用 Spring {@link WebClient} 发起 POST 请求，通过 {@code exchangeToFlux} +
     * {@code bodyToFlux(DataBuffer.class)} 拿到按网络包粒度到达的字节流。
     * 解析层维护跨 {@link DataBuffer} 的行缓冲，每读到完整一行就解析 SSE data 并回调 consumer。
     * </p>
     *
     * @param token    JWT 认证令牌（Bearer token）
     * @param request  对话请求体
     * @param consumer 每个 SSE 事件都会回调一次；传入解析后的 {@link AgenticChatResponse}
     * @throws ServiceException 当参数不合法、HTTP 状态非 2xx 或 SSE 数据解析失败时抛出
     */
    public void chatStreaming(String token, AgenticChatRequest request,
            Consumer<AgenticChatResponse> consumer) {
        if (StringUtils.isBlank(token)) {
            throw new ServiceException("agentic token is empty");
        }
        if (Objects.isNull(request)) {
            throw new ServiceException("agentic request is null");
        }
        if (Objects.isNull(consumer)) {
            throw new ServiceException("agentic streaming consumer is null");
        }

        String content;
        try {
            content = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("format agentic request to json fail...", e);
            throw new ServiceException("format agentic request to json fail...");
        }

        final String finalUrl = serverUrl;
        WebClient.RequestBodySpec spec = webClient.post()
                .uri(finalUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .header("Authorization", "Bearer " + token);
        spec.bodyValue(content);

        // 同步阻塞语义：CountDownLatch 等到 onComplete/onError
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> errRef = new AtomicReference<>();
        StringBuilder lineBuf = new StringBuilder(256);

        spec.exchangeToFlux(resp -> {
            if (resp.statusCode().is2xxSuccessful()) {
                return resp.bodyToFlux(DataBuffer.class);
            }
            // 非 2xx：拼出 body 后抛 ServiceException
            return resp.bodyToFlux(DataBuffer.class)
                    .collectList()
                    .defaultIfEmpty(java.util.Collections.emptyList())
                    .flatMapMany(list -> {
                        StringBuilder sb = new StringBuilder();
                        for (DataBuffer b : list) {
                            try {
                                sb.append(StandardCharsets.UTF_8.decode(b.asByteBuffer()).toString());
                            } finally {
                                DataBufferUtils.release(b);
                            }
                        }
                        return Flux.<DataBuffer>error(new ServiceException(
                                "request agentic return http status=" + resp.statusCode().value()
                                        + " body=" + sb));
                    });
        })
                .doOnNext(buf -> {
                    try {
                        String chunk = StandardCharsets.UTF_8.decode(buf.asByteBuffer()).toString();
                        lineBuf.append(chunk);
                        int idx;
                        while ((idx = lineBuf.indexOf("\n")) >= 0) {
                            String line = lineBuf.substring(0, idx);
                            lineBuf.delete(0, idx + 1);
                            // 兼容 \r\n
                            if (line.endsWith("\r")) {
                                line = line.substring(0, line.length() - 1);
                            }
                            parseSseLine(line, consumer);
                        }
                    } finally {
                        DataBufferUtils.release(buf);
                    }
                })
                .doOnError(errRef::set)
                .doFinally(sig -> done.countDown())
                .subscribe();

        try {
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("agentic streaming interrupted");
        }

        Throwable err = errRef.get();
        if (err instanceof ServiceException) {
            throw (ServiceException) err;
        }
        if (err instanceof RuntimeException) {
            throw (RuntimeException) err;
        }
        if (err != null) {
            throw new ServiceException("agentic streaming error: " + err.getMessage());
        }
    }

    /**
     * 解析一行 SSE 字段：
     * <ul>
     * <li>空行：作为事件分隔，忽略</li>
     * <li>{@code data:} 开头：去掉前缀后若为空或 {@code [DONE]} 则跳过；否则按 JSON 反序列化</li>
     * <li>其它行（{@code event:}/{@code id:}/注释行等）：直接忽略</li>
     * </ul>
     */
    private void parseSseLine(String line, Consumer<AgenticChatResponse> consumer) {
        if (StringUtils.isBlank(line)) {
            return;
        }
        final String prefix = "data:";
        if (!line.startsWith(prefix)) {
            return;
        }
        String data = line.substring(prefix.length()).trim();
        if (StringUtils.isBlank(data) || "[DONE]".equals(data)) {
            return;
        }
        try {
            AgenticChatResponse response = objectMapper.readValue(data, AgenticChatResponse.class);
            consumer.accept(response);
        } catch (JsonProcessingException e) {
            log.error("format agentic streaming event to json fail... data={}", data, e);
            throw new ServiceException("format agentic streaming event to json fail...");
        }
    }

    // ===================== 请求 DTO =====================

    /**
     * Agentic 对话请求体，与 Python 端 ChatReq 对齐
     */
    public static class AgenticChatRequest {
        private Long agent_flow_id;
        private String prompt;
        private String client_id;
        private String session_id;
        private Boolean web_search;
        private List<String> images;

        public Long getAgent_flow_id() {
            return agent_flow_id;
        }

        public void setAgent_flow_id(Long agent_flow_id) {
            this.agent_flow_id = agent_flow_id;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public String getClient_id() {
            return client_id;
        }

        public void setClient_id(String client_id) {
            this.client_id = client_id;
        }

        public String getSession_id() {
            return session_id;
        }

        public void setSession_id(String session_id) {
            this.session_id = session_id;
        }

        public Boolean getWeb_search() {
            return web_search;
        }

        public void setWeb_search(Boolean web_search) {
            this.web_search = web_search;
        }

        public List<String> getImages() {
            return images;
        }

        public void setImages(List<String> images) {
            this.images = images;
        }

        /**
         * 便捷构造方法
         */
        public static AgenticChatRequest of(Long agentFlowId, String prompt,
                String clientId, String sessionId, Boolean webSearch, List<String> images) {
            AgenticChatRequest req = new AgenticChatRequest();
            req.setAgent_flow_id(agentFlowId);
            req.setPrompt(prompt);
            req.setClient_id(clientId);
            req.setSession_id(sessionId);
            req.setWeb_search(webSearch);
            req.setImages(images);
            return req;
        }
    }

    // ===================== 响应 DTO =====================

    /**
     * Agentic SSE 流式响应体，与 Python 端返回的 JSON 对齐。
     *
     * <p>
     * Python 端返回格式：{@code {"msg": "chunk text"}}，流结束时发送 {@code data: [DONE]}
     * </p>
     */
    public static class AgenticChatResponse {
        /**
         * 消息内容片段
         */
        private String msg;

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        @Override
        public String toString() {
            return "AgenticChatResponse [msg=" + msg + "]";
        }

    }
}
