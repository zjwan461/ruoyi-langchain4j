package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.domain.AiAgent;
import com.ruoyi.ai.domain.Model;
import com.ruoyi.ai.enums.ModelProvider;
import com.ruoyi.ai.service.IModelService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.spring.SpringUtils;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaChatRequestParameters;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.stereotype.Service;

@Service
public class ModelBuilder {

    public EmbeddingModel getEmbeddingModel(Model model) {
        EmbeddingModel embeddingModel;
        ModelProvider provider = ModelProvider.fromValue(model.getProvider());
        if (provider == ModelProvider.OLLAMA) {
            embeddingModel = OllamaEmbeddingModel.builder()
                    .baseUrl(model.getBaseUrl())
                    .modelName(model.getName())
                    .build();
        } else if (provider == ModelProvider.OPEN_AI) {
            embeddingModel = OpenAiEmbeddingModel.builder()
                    .baseUrl(model.getBaseUrl())
                    .modelName(model.getName())
                    .build();
        } else if (provider == ModelProvider.LOCAL) {
            String saveDir = model.getSaveDir();
            embeddingModel = new OnnxEmbeddingModel(saveDir + "/onnx/model.onnx", saveDir + "/onnx/tokenizer.json", PoolingMode.MEAN);
        } else {
            throw new ServiceException("不支持的模型提供商");
        }
        return embeddingModel;
    }

    public StreamingChatModel getStreamingLLM(Model model) {
        ModelProvider provider = ModelProvider.fromValue(model.getProvider());
        StreamingChatModel llm = null;
        if (provider == ModelProvider.OLLAMA) {
            llm = OllamaStreamingChatModel.builder()
                    .baseUrl(model.getBaseUrl())
                    .modelName(model.getName())
                    .think(true)
                    .returnThinking(true)
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        } else {
            llm = OpenAiStreamingChatModel.builder()
                    .baseUrl(model.getBaseUrl())
                    .modelName(model.getName())
                    .returnThinking(true)
                    .apiKey(model.getApiKey())
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        }
        return llm;
    }

    public ChatModel getBlockingLLM(Model model) {
        ModelProvider provider = ModelProvider.fromValue(model.getProvider());
        ChatModel llm = null;
        if (provider == ModelProvider.OLLAMA) {
            llm = OllamaChatModel.builder()
                    .baseUrl(model.getBaseUrl())
                    .modelName(model.getName())
                    .build();
        } else {
            llm = OpenAiChatModel.builder()
                    .baseUrl(model.getBaseUrl())
                    .modelName(model.getName())
                    .apiKey(model.getApiKey())
                    .build();
        }
        return llm;
    }

    public ChatRequestParameters getParameters(Model model) {
        ModelProvider provider = ModelProvider.fromValue(model.getProvider());
        ChatRequestParameters parameters = null;
        if (provider == ModelProvider.OLLAMA) {
            parameters = OpenAiChatRequestParameters.builder()
                    .modelName(model.getName())
                    .temperature(model.getTemperature())
                    .maxOutputTokens(model.getMaxOutputToken())
                    .build();
        } else {
            parameters = OllamaChatRequestParameters.builder()
                    .modelName(model.getName())
                    .temperature(model.getTemperature())
                    .maxOutputTokens(model.getMaxOutputToken())
                    .build();
        }

        return parameters;
    }

    public ChatRequestParameters getParameters(AiAgent aiAgent) {
        Model model = SpringUtils.getBean(IModelService.class).selectModelById(aiAgent.getModelId());
        ModelProvider provider = ModelProvider.fromValue(model.getProvider());
        ChatRequestParameters parameters = null;
        if (provider == ModelProvider.OLLAMA) {
            parameters = OpenAiChatRequestParameters.builder()
                    .modelName(model.getName())
                    .temperature(aiAgent.getTemperature())
                    .maxOutputTokens(aiAgent.getMaxOutputToken())
                    .build();
        } else {
            parameters = OllamaChatRequestParameters.builder()
                    .modelName(model.getName())
                    .temperature(aiAgent.getTemperature())
                    .maxOutputTokens(aiAgent.getMaxOutputToken())
                    .build();
        }

        return parameters;
    }
}
