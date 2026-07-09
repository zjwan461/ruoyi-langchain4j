package com.ruoyi.ai.service;

import dev.langchain4j.service.TokenStream;

public interface AIAssistant {

    TokenStream chatStream(String message);
}
