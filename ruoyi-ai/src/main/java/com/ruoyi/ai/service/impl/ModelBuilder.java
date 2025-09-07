package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.domain.Model;
import com.ruoyi.ai.enums.ModelProvider;
import com.ruoyi.common.exception.ServiceException;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
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
}
