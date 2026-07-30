package com.ruoyi.ai.test;


import dev.langchain4j.data.message.Content;

import java.util.List;

public interface MyAssistant {

    String chat(List<Content> messages);

}
