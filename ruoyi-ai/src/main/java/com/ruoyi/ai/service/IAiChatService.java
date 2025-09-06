package com.ruoyi.ai.service;

import com.ruoyi.ai.domain.AiAgent;
import reactor.core.publisher.Flux;

public interface IAiChatService {

  Flux<String> chat(AiAgent aiAgent, String prompt, String sessionId);

}
