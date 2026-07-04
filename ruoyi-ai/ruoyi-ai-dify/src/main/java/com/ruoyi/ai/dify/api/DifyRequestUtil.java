package com.ruoyi.ai.dify.api;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Dify HTTP 请求工具。
 *
 * <p>非流式方法（{@link #info(String, String)} / {@link #chatBlocking(String, String, DifyChatRequest)}）
 * 仍然使用 Hutool 的 {@link HttpRequest} 同步阻塞调用，简单稳定。</p>
 *
 * <p>{@link #chatStreaming(String, String, DifyChatRequest, Consumer)}
 * 使用 Spring {@link WebClient} + {@link Flux} 订阅 {@link DataBuffer}，<b>不会预读整个响应体</b>，
 * 每个 SSE 事件到达 socket 即可立即回调 {@code consumer}，确保真正的 Server-Sent Events 流式语义。</p>
 */
@Component
@Slf4j
public class DifyRequestUtil {

    @Resource
    private ObjectMapper objectMapper;

    /**
     * /**
     * 复用的 WebClient。
     *
     * <ul>
     *     <li>显式指定 {@link ReactorClientHttpConnector} 作为底层 HTTP 连接器，
     *         避免 {@code WebClient.builder().build()} 在 classpath 未被探测到
     *         默认实现时抛出
     *         {@code IllegalStateException("No suitable default ClientHttpConnector found")}。</li>
     *     <li>关闭默认的内存缓冲上限（{@code maxInMemorySize=-1}），
     *         避免超长 SSE 流被 WebFlux 编解码器提前缓存到堆内存。</li>
     *     <li>保留 HTTP 客户端层面的 chunked 编码，事件可按网络包粒度逐步到达。</li>
     * </ul>
     */
    private final WebClient webClient = WebClient.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(-1))
            .clientConnector(new ReactorClientHttpConnector())
            .build();

    public DifyInfoResponse info(String urlPrefix, String apiKey) {
        if (StringUtils.isBlank(urlPrefix)) {
            throw new ServiceException("dify url prefix is empty");
        }
        urlPrefix = handleUrlPrefix(urlPrefix);
        HttpRequest httpRequest = HttpUtil.createRequest(Method.GET, urlPrefix + "/info");
        if (StringUtils.isNotBlank(apiKey)) {
            httpRequest.header("Authorization", "Bearer " + apiKey);
        }
        HttpResponse response = httpRequest.execute();
        if (!response.isOk()) {
            throw new ServiceException("request dify return http status=" + response.getStatus());
        }
        String body = response.body();
        try {
            return objectMapper.readValue(body, DifyInfoResponse.class);
        } catch (JsonProcessingException e) {
            log.error("format dify response to json fail...", e);
            throw new ServiceException("format dify response to json fail...");
        }
    }

    private String handleUrlPrefix(String urlPrefix) {
        urlPrefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
        urlPrefix = urlPrefix.endsWith("/v1") ? urlPrefix : urlPrefix + "/v1";
        return urlPrefix;
    }

    public DifyChatBlockingResponse chatBlocking(String urlPrefix, String apiKey, DifyChatRequest request) {
        if (StringUtils.isBlank(urlPrefix)) {
            throw new ServiceException("dify url prefix is empty");
        }
        if (Objects.isNull(request)) {
            throw new ServiceException("dify request is null");
        }
        urlPrefix = handleUrlPrefix(urlPrefix);
        request.setResponseMode(DifyChatRequest.ResponseModel.blocking);

        String content = null;
        try {
            content = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("format dify request to json fail...", e);
            throw new ServiceException("format dify request to json fail...");
        }

        HttpRequest httpRequest = HttpUtil.createRequest(Method.POST, urlPrefix + "/chat-messages")
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(content);

        if (StringUtils.isNotBlank(apiKey)) {
            httpRequest.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse response = httpRequest.execute();
        if (!response.isOk()) {
            throw new ServiceException("request dify return http status=" + response.getStatus());
        }
        String body = response.body();
        try {
            return objectMapper.readValue(body, DifyChatBlockingResponse.class);
        } catch (JsonProcessingException e) {
            log.error("format dify response to json fail...", e);
            throw new ServiceException("format dify response to json fail...");
        }
    }

    /**
     * 请求 Dify Chat Streaming API（Server-Sent Events）并逐个事件回调消费。
     *
     * <p>实现细节：</p>
     * <ul>
     *     <li>使用 Spring {@link WebClient} 发起 POST 请求，
     *         通过 {@code exchangeToFlux} + {@code bodyToFlux(DataBuffer.class)} 拿到<b>按网络包粒度</b>到达的字节流，
     *         不会被任何内存缓冲预先缓存为完整响应。</li>
     *     <li>请求头声明 {@code Accept: text/event-stream}；{@code response_mode} 被强制为 {@code streaming}。</li>
     *     <li>解析层在订阅者中维护一个跨 {@link DataBuffer} 的行缓冲（{@link StringBuilder}），
     *         每读到完整一行就调用 {@link #parseSseLine(String, Consumer)}：
     *         跳过 {@code event:}/{@code id:}/{@code :} 等非 {@code data:} 行；遇到空行作为事件分隔；
     *         {@code [DONE]} 视为流结束哨兵。</li>
     *     <li>为保持原 API 形态（{@code void} + {@link Consumer} 同步回调），方法内部用 {@link CountDownLatch}
     *         阻塞至订阅完成；任何错误都被收集到 {@link AtomicReference} 并在 finally 中以
     *         {@link ServiceException} 抛出。</li>
     * </ul>
     *
     * <p>常见事件类型参见 {@link DifyChatStreamingResponse} 类注释，回调中可通过
     * {@link DifyChatStreamingResponse#getEvent()} 判断具体事件类型并做分支处理。</p>
     *
     * @param urlPrefix Dify 服务地址前缀，例如 {@code http://localhost/v1}
     * @param apiKey    Dify API Key；为空时不会设置 {@code Authorization} 请求头
     * @param request   Chat 请求体；不能为空
     * @param consumer  每个解析出的 SSE 事件都会回调一次；不能为空
     * @throws ServiceException 当参数不合法、HTTP 状态非 2xx 或 SSE 数据解析失败时抛出
     */
    public void chatStreaming(String urlPrefix, String apiKey, DifyChatRequest request,
                              Consumer<DifyChatStreamingResponse> consumer) {
        if (StringUtils.isBlank(urlPrefix)) {
            throw new ServiceException("dify url prefix is empty");
        }
        if (Objects.isNull(request)) {
            throw new ServiceException("dify request is null");
        }
        if (Objects.isNull(consumer)) {
            throw new ServiceException("dify streaming consumer is null");
        }
        urlPrefix = handleUrlPrefix(urlPrefix);
        request.setResponseMode(DifyChatRequest.ResponseModel.streaming);

        String content;
        try {
            content = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("format dify request to json fail...", e);
            throw new ServiceException("format dify request to json fail...");
        }

        // 构造 WebClient 请求规格；用 final 变量以便 lambda 中使用
        final String finalUrl = urlPrefix + "/chat-messages";
        WebClient.RequestBodySpec spec = webClient.post()
                .uri(finalUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM);
        if (StringUtils.isNotBlank(apiKey)) {
            spec = spec.header("Authorization", "Bearer " + apiKey);
        }
        // bodyValue 放在最后：其返回类型是 RequestHeadersSpec，不再继续链式赋值
        spec.bodyValue(content);

        // 同步阻塞语义：CountDownLatch 等到 onComplete/onError；err 收集异常
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
                                // 直接返回 error Flux，比 handle + sink.error + flatMapMany(Flux::error) 更直观
                                return Flux.<DataBuffer>error(new ServiceException(
                                        "request dify return http status=" + resp.statusCode().value() + " body=" + sb));
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
            throw new ServiceException("dify streaming interrupted");
        }

        Throwable err = errRef.get();
        if (err instanceof ServiceException) {
            throw (ServiceException) err;
        }
        if (err instanceof RuntimeException) {
            throw (RuntimeException) err;
        }
        if (err != null) {
            throw new ServiceException("dify streaming error: " + err.getMessage());
        }
    }

    /**
     * 解析一行 SSE 字段：
     * <ul>
     *     <li>空行：作为事件分隔，忽略</li>
     *     <li>{@code data:} 开头：去掉前缀后若为空或 {@code [DONE]} 则跳过；否则按 JSON 反序列化为
     *         {@link DifyChatStreamingResponse} 并回调</li>
     *     <li>其它行（{@code event:}/{@code id:}/注释行等）：直接忽略</li>
     * </ul>
     */
    private void parseSseLine(String line, Consumer<DifyChatStreamingResponse> consumer) {
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
            DifyChatStreamingResponse event = objectMapper.readValue(data, DifyChatStreamingResponse.class);
            consumer.accept(event);
        } catch (JsonProcessingException e) {
            log.error("format dify streaming event to json fail... data={}", data, e);
            throw new ServiceException("format dify streaming event to json fail...");
        }
    }
}
