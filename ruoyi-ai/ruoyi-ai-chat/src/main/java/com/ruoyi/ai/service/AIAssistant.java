package com.ruoyi.ai.service;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.service.TokenStream;

import java.util.List;

public interface AIAssistant {

    TokenStream chatStream(List<Content> messages);
}
