package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.config.AiConfig.PgVector;
import com.ruoyi.ai.enums.ModelProvider;
import com.ruoyi.ai.enums.ModelType;
import com.ruoyi.ai.service.LangChain4jService;
import com.ruoyi.ai.util.PgVectorUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest.EmbeddingSearchRequestBuilder;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LangChain4jServiceImpl implements LangChain4jService {

    @Resource
    private AiConfig aiConfig;

    @Resource
    private PgVectorUtil pgVectorUtil;

    @Override
    public boolean checkModelConfig(String baseUrl, String apiKey, String modelName,
                                    ModelProvider provider, ModelType type) {
        if (type == ModelType.LLM) {
            ChatModel model = null;
            if (provider == ModelProvider.OPEN_AI) {
                model = OpenAiChatModel.builder()
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .logRequests(true)
                        .logResponses(true)
                        .apiKey(apiKey)
                        .build();
            } else/* if (provider == ModelProvider.OLLAMA)*/ {
                model = OllamaChatModel.builder()
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .logRequests(true)
                        .logResponses(true)
                        .build();
            }

            try {
                model.chat("hello");
            } catch (Exception e) {
                log.error("check model config error", e);
                return false;
            }
        } else {
            EmbeddingModel model = null;
            if (provider == ModelProvider.OPEN_AI) {
                model = OpenAiEmbeddingModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .logRequests(true)
                        .logResponses(true)
                        .build();
            } else {
                model = OllamaEmbeddingModel.builder()
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .logRequests(true)
                        .logResponses(true)
                        .build();
            }

            try {
                model.embed("hello world");
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    @Override
    public List<TextSegment> splitDocument(String docFile, int maxSegmentSize, int maxOverlapSize) {
        Document document = FileSystemDocumentLoader.loadDocument(docFile);
        DocumentSplitter splitter = DocumentSplitters.recursive(maxSegmentSize, maxOverlapSize);
        return splitter.split(document);
    }

    @Override
    public List<String> embedTextSegments(EmbeddingModel embeddingModel,
                                          List<TextSegment> textSegments) {
        Response<List<Embedding>> response = embeddingModel.embedAll(textSegments);
        PgVector pgVector = aiConfig.getPgVector();
        PgVectorEmbeddingStore embeddingStore = buildPgEmbeddingStore(
                embeddingModel, pgVector);
        List<Embedding> embeddings = response.content();
        return embeddingStore.addAll(embeddings, textSegments);
    }

    public PgVectorEmbeddingStore buildPgEmbeddingStore(EmbeddingModel embeddingModel,
                                                        PgVector pgVector) {
        return PgVectorEmbeddingStore.builder()
                .host(pgVector.getHost())
                .port(pgVector.getPort())
                .database(pgVector.getDatabase())
                .user(pgVector.getUser())
                .password(pgVector.getPassword())
                .table(pgVector.getTable())
                .dimension(embeddingModel.dimension())
                .build();
    }

    public PgVectorEmbeddingStore buildPgEmbeddingStore(
            PgVector pgVector) {
        return PgVectorEmbeddingStore.builder()
                .host(pgVector.getHost())
                .port(pgVector.getPort())
                .database(pgVector.getDatabase())
                .user(pgVector.getUser())
                .password(pgVector.getPassword())
                .table(pgVector.getTable())
                .dimension(1)
                .build();
    }


    @Override
    public List<Map<String, Object>> querySegmentTextEqualsByMetaData(
            Map<String, Object> metadata) {
        return pgVectorUtil.selectByMetadata(metadata);
    }

    @Override
    public void removeSegment(List<String> ids) {
        PgVectorEmbeddingStore embeddingStore = buildPgEmbeddingStore(aiConfig.getPgVector());
        embeddingStore.removeAll(ids);
    }

    @Override
    public void updateSegment(EmbeddingModel embeddingModel, TextSegment textSegment,
                              String embeddingId) {
        Response<Embedding> response = embeddingModel.embed(textSegment);
        Embedding embedding = response.content();
        PgVectorEmbeddingStore embeddingStore = this.buildPgEmbeddingStore(embeddingModel,
                aiConfig.getPgVector());
        embeddingStore.removeAll(Collections.singletonList(embeddingId));
        embeddingStore.addAll(Collections.singletonList(embeddingId),
                Collections.singletonList(embedding), Collections.singletonList(textSegment));
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> search(EmbeddingModel embeddingModel, String query,
                                                    int maxResult,
                                                    double minScore, Map<String, Object> metadata) {
        Response<Embedding> response = embeddingModel.embed(query);
        PgVectorEmbeddingStore embeddingStore = buildPgEmbeddingStore(aiConfig.getPgVector());

        EmbeddingSearchRequestBuilder searchBuilder = EmbeddingSearchRequest.builder();
        searchBuilder.queryEmbedding(response.content())
                .maxResults(maxResult)
                .minScore(minScore);
        if (metadata != null && !metadata.isEmpty()) {
            metadata.forEach((k, v) -> {
                searchBuilder.filter(new IsEqualTo(k, v));
            });
        }
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchBuilder.build());
        return result.matches();
    }

    @Override
    public boolean checkLocalEmbeddingModel(String saveDir) {
        try {
            String pathToModel = saveDir + "/onnx/model.onnx";
            String pathToTokenizer = saveDir + "/onnx/tokenizer.json";
            PoolingMode poolingMode = PoolingMode.MEAN;
            EmbeddingModel embeddingModel = new OnnxEmbeddingModel(pathToModel, pathToTokenizer, poolingMode);
            embeddingModel.embed("test");
        } catch (Exception e) {
            log.error("检查本地模型失败", e);
            return false;
        }
        return true;
    }
}
