package com.ruoyi.ai.component.service;

import dev.langchain4j.service.AiServiceContext;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.spi.services.AiServicesFactory;

public class JAiServicesFactory implements AiServicesFactory {
    @Override
    public <T> AiServices<T> create(AiServiceContext context) {
        return new JAiServices<>(context);
    }
}
