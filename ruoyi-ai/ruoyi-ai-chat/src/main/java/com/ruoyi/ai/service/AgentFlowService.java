package com.ruoyi.ai.service;

import com.ruoyi.ai.component.flow.dto.AgentRoundOut;
import com.ruoyi.ai.component.flow.dto.AgentSpec;
import com.ruoyi.ai.domain.AgentFlow;
import com.ruoyi.ai.dto.FlowRoundContext;
import reactor.core.publisher.Mono;

public interface AgentFlowService {

    void init(String sessionId, AgentFlow agentFlow);

    Mono<AgentRoundOut> invoke(String sessionId, String clientId, String request, FlowRoundContext ctx);

    Mono<AgentRoundOut> nextRound(AgentSpec agentSpec, String sessionId, String clientId, String request, FlowRoundContext ctx);

    String attachSubAgentPrompt(String prompt, String currentNodeId);

}
