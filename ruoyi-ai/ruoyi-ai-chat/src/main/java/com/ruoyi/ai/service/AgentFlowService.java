package com.ruoyi.ai.service;

import com.ruoyi.ai.component.flow.dto.AgentRoundOut;
import com.ruoyi.ai.component.flow.dto.AgentSpec;
import com.ruoyi.ai.domain.AgentFlow;
import com.ruoyi.ai.dto.FlowRoundContext;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AgentFlowService {

    void init(String sessionId, AgentFlow agentFlow);

    Mono<AgentRoundOut> invoke(String sessionId, String clientId, String request, FlowRoundContext ctx);

    /**
     * 多 Agent 串行调度：按 {@code specs} 入参顺序严格串行执行。
     * <p>
     * 语义：
     * <ul>
     *     <li>每个 Agent 执行完成后再启动下一个，绝不并行/插队；底层使用 Reactor
     *         {@code flatMapSequential}（{@code concatMap} 的等价串行背压版本）。</li>
     *     <li>前一个 Agent 的输出会作为下一个 Agent 的 transferRequest 一部分，
     *         同时保留原始用户请求，便于链式上下文。</li>
     *     <li>{@code specs} 中的元素为空、{@code agentId} 为 null 或数量超过批次上限时，
     *         沿用项目当前的 fail-fast 异常策略。</li>
     * </ul>
     *
     * @param specs           当前批次有序的子 Agent 规格
     * @param sessionId       会话 ID
     * @param clientId        客户端 ID
     * @param transferRequest 来自上一轮父 Agent 的累积请求（含历史输出与原始用户请求）
     * @param ctx             当前 FlowRoundContext（用于复用 SSE sink 等）
     * @return 最终一轮 AgentRoundOut（批次结束时的最后输出）
     */
    Mono<AgentRoundOut> nextRoundBatch(List<AgentSpec> specs, String sessionId, String clientId,
                                      String transferRequest, FlowRoundContext ctx);

    String attachSubAgentPrompt(String prompt, String currentNodeId);

}
