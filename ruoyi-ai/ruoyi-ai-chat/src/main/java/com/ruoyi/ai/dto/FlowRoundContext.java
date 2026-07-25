package com.ruoyi.ai.dto;


import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.service.AIAssistant;
import lombok.Data;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

@Data
public class FlowRoundContext {
    // 当前节点ID
    private String currentNodeId;
    // 当前执行智能体
    private AIAgent currentAgent;
    // 当前智能体Assistant
    private AIAssistant currentAssistant;

    // SSE链路全局对象（一次对话全程复用）
    private FluxSink<String> rawSink;
    private BiConsumer<String, String> safeEmitter;
    private AtomicBoolean sinkClosed;
}
