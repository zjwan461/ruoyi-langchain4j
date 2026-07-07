package com.ruoyi.ai.service;

import dev.langchain4j.service.TokenStream;
import reactor.core.publisher.Flux;

public interface AIAssistant {

    TokenStream chatStream(String message);

    Flux<String> chat(String message);
}
