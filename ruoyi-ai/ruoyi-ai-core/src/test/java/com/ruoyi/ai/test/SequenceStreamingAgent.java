package com.ruoyi.ai.test;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.V;

public interface SequenceStreamingAgent {

    @Agent
    TokenStream sequenceAgent(@V("topic") String topic, @V("language") String language);
}
