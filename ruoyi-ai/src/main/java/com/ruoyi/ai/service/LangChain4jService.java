package com.ruoyi.ai.service;

import com.ruoyi.ai.enums.ModelProvider;
import com.ruoyi.ai.enums.ModelType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import java.util.List;
import java.util.Map;

public interface LangChain4jService {

  boolean checkModelConfig(String baseUrl, String apiKey, String modelName, ModelProvider provider,
      ModelType type);

  List<TextSegment> splitDocument(String docFile, int maxSegmentSize, int maxOverlapSize);

  List<String> embedTextSegments(EmbeddingModel embeddingModel, List<TextSegment> textSegments);

  List<Map<String, Object>> querySegmentTextEqualsByMetaData(Map<String, Object> metadata);

  void removeSegment(List<String> ids);

  void updateSegment(EmbeddingModel embeddingModel, TextSegment textSegment, String embeddingId);

  List<EmbeddingMatch<TextSegment>> search(EmbeddingModel embeddingModel, String query,
      int maxResult,
      double minScore, Map<String, Object> metadata);

  boolean checkLocalEmbeddingModel(String saveDir);
}
