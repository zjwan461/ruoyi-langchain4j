package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.service.TranslateService;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class TranslateServiceImpl implements TranslateService {

  @Resource
  private AiConfig aiConfig;

  @Override
  public String translateBlock(String origin, String targetLang) {
//    OpenAiChatModel openAiChatModel = buildBlockModel();
//    ChatRequestParameters chatRequestParameters = buildParameters();
    OllamaChatModel ollamaChatModel = buildOllamaChatModel();
    OllamaChatRequestParameters ollamaChatRequestParameters = buildOllamaParameters();
    SystemMessage sysMsg = SystemMessage.from(
        String.format(aiConfig.getTranslate().getSystemMessage(), targetLang));
    ChatRequest request = ChatRequest.builder()
        .parameters(ollamaChatRequestParameters)
        .messages(sysMsg, UserMessage.from(origin))
        .build();

    ChatResponse chatResponse = ollamaChatModel.doChat(request);
    return chatResponse.aiMessage().text();
  }

  private OllamaChatModel buildOllamaChatModel() {
    return OllamaChatModel.builder()
        .baseUrl(aiConfig.getTranslate().getBaseUri())
        .modelName(aiConfig.getTranslate().getModelName())
        .build();
  }

  private OllamaChatRequestParameters buildOllamaParameters() {
    return OllamaChatRequestParameters.builder()
        .modelName(aiConfig.getTranslate().getModelName())
        .temperature(aiConfig.getTranslate().getTemperature())
        .build();
  }


  private OpenAiChatModel buildBlockModel() {
    return OpenAiChatModel.builder()
        .baseUrl(aiConfig.getTranslate().getBaseUri())
        .modelName(aiConfig.getTranslate().getModelName())
        .build();
  }

  private ChatRequestParameters buildParameters() {
    return OpenAiChatRequestParameters.builder()
        .modelName(aiConfig.getTranslate().getModelName())
        .temperature(aiConfig.getTranslate().getTemperature())
//        .topK(40)
//        .topP(0.9)
        .build();
  }
}
