package com.ruoyi.ai.service;

import com.ruoyi.ai.component.flow.dto.AgentRoundOut;
import com.ruoyi.ai.component.flow.dto.AgentSpec;
import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.domain.AgentFlow;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

public interface AgentFlowService {

    void init(String sessionId, AgentFlow agentFlow);

    Mono<AgentRoundOut> invoke(String sessionId, String clientId, String request, FluxSink<String> fluxSink, AIAgent aiAgent);

    Mono<AgentRoundOut> nextRound(AgentSpec agentSpec, String sessionId, String clientId, String request, FluxSink<String> fluxSink);

    String attachSubAgentPrompt(String prompt);

}
