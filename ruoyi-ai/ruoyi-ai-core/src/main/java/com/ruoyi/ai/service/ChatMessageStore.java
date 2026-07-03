package com.ruoyi.ai.service;

import com.ruoyi.ai.domain.AiAgent;
import dev.langchain4j.model.chat.response.ChatResponse;

public interface ChatMessageStore {

  void save(ChatResponse chatResponse, AiAgent aiAgent);


}
