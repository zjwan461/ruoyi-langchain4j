package com.ruoyi.ai.component.flow.dto;

import cn.hutool.core.collection.CollUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRoundOut {
    // 当前轮输出文本
    private String content;
    // 是否需要跳转下一个 Agent（用于驱动 expand 链路）
    private boolean needNext;
    // 传递给下一轮的请求文本（包含父 Agent 本轮输出 + 原始用户请求）
    private String transferRequest;
    /**
     * 当前批次需要顺次执行的子 Agent 列表（有序）。
     * <p>
     * 父 Agent 调用 {@code agent_selector} 时返回的 agentIds 数组会按顺序填充到这里。
     * 调度层会按返回顺序严格串行执行；批次内不会做并行/插队调度。
     */
    private List<AgentSpec> nextAgentSpecs;

    /**
     * 判断当前轮是否包含下一批待执行子 Agent。
     */
    public boolean hasNextBatch() {
        return CollUtil.isNotEmpty(nextAgentSpecs);
    }

    /**
     * 追加一个子 Agent 到有序批次列表中，并按需设置 needNext=true。
     */
    public void addNextSpec(AgentSpec spec) {
        if (spec == null) {
            return;
        }
        if (nextAgentSpecs == null) {
            nextAgentSpecs = new ArrayList<>();
        }
        nextAgentSpecs.add(spec);
        this.needNext = true;
    }
}
