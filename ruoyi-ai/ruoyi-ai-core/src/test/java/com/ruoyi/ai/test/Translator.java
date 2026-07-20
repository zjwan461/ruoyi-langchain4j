package com.ruoyi.ai.test;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.V;

public interface Translator {

    @Agent
    TokenStream translate(@V("language") String language, @V("story") String story);
}
