package com.ruoyi.ai.service;

import java.util.List;

import com.ruoyi.ai.domain.AgentFlow;

import reactor.core.publisher.Flux;

public interface AgenticService {
    
    Flux<String> chat(AgentFlow agentFlow, String prompt, String clientId, String sessionId, List<Long> attachmentIds, boolean enableWebSearch);
}
