package com.ruoyi.ai.service;

import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.domain.AgentFlow;
import reactor.core.publisher.FluxSink;

public interface AgentFlowService {

    void init(String sessionId, AgentFlow agentFlow);

    void invoke(String sessionId, String clientId, String request, FluxSink<String> fluxSink);

    void registerSubAgent();

    AIAgent nextAIAssistant();
}
