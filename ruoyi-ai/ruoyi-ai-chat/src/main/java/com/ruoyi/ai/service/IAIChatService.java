package com.ruoyi.ai.service;

import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.domain.AgentFlow;
import com.ruoyi.ai.domain.ChatFlowMessage;
import com.ruoyi.ai.domain.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public interface IAIChatService {

    Flux<String> chat(AIAgent aiAgent, String prompt, String clientId, String sessionId);

    Flux<String> chat(AgentFlow agentFlow, String prompt, String clientId, String sessionId);

    String createSession(String clientId);

    boolean checkClientSession(String clientId, String sessionId);

    boolean checkIfOverLmtRequest(Long agentId, Integer dayLmtPerClient, String clientId);

    List<Map<String, String>> listClientSession(String clientId, Long agentId);

    List<ChatMessage> listAgentChatMessageBySessionId(String sessionId, Long agentId);

    List<ChatFlowMessage> listAgentFlowChatMessageBySessionId(String sessionId, Long agentId);

    /**
     * 按时间倒序分页查询聊天记录（需在调用前使用 PageHelper.startPage 启动分页）
     *
     * @param sessionId 会话ID
     * @param agentId 智能体ID
     * @return 当前页聊天记录（按 create_time desc, id desc）
     */
    List<ChatMessage> listAgentChatMessageBySessionIdOrderDesc(String sessionId, Long agentId);

    List<ChatFlowMessage> listAgentFlowChatMessageBySessionIdOrderDesc(String sessionId, Long agentId);

    void deleteSession(String sessionId);

    ChatMessage saveChatMessage(String content, String clientId, String sessionId, Long agentId,
                         ChatMessageType chatMessageType);
}
