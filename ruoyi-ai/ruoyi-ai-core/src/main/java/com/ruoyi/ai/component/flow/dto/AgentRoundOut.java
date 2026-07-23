package com.ruoyi.ai.component.flow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRoundOut {
    // 当前轮输出文本
    private String content;
    // 是否需要跳转下一个Agent
    private boolean needNext;
    // 下一个Agent配置
    private AgentSpec nextAgentSpec;
    // 传递给下一轮的请求文本（后续可以改成本轮输出结果）
    private String transferRequest;
}