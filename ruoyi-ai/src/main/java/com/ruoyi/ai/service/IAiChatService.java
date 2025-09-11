package com.ruoyi.ai.service;

import com.ruoyi.ai.domain.AiAgent;
import com.ruoyi.ai.domain.ChatMessage;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public interface IAiChatService {

    Flux<String> chat(AiAgent aiAgent, String prompt, String clientId, String sessionId);

    String createSession(String clientId);

    boolean checkClientSession(String clientId, String sessionId);

    boolean checkIfOverLmtRequest(Long agentId, Integer dayLmtPerClient, String clientId);

    List<Map<String, String>> listClientSession(String clientId, Long agentId);

    List<ChatMessage> listAgentChatMessageBySessionId(String sessionId, Long agentId);

    void deleteSession(String sessionId);
}
