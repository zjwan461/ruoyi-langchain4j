package com.ruoyi.ai.test;

import dev.langchain4j.service.UserMessage;

public interface Assistant {

  String chat(@UserMessage String userMessage);

}
