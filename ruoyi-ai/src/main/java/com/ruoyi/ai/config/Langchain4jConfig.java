package com.ruoyi.ai.config;

import com.ruoyi.ai.util.Constants;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @author jerry.su
 * @date 2025/9/13 22:42
 */
@Configuration
public class Langchain4jConfig {

    @Resource
    private AiConfig aiConfig;

    @Bean
    public PgVectorEmbeddingStore embeddingStore() {
        AiConfig.PgVector pgVector = aiConfig.getPgVector();
        return PgVectorEmbeddingStore.builder()
                .host(pgVector.getHost())
                .port(pgVector.getPort())
                .database(pgVector.getDatabase())
                .user(pgVector.getUser())
                .password(pgVector.getPassword())
                .table(pgVector.getTable())
                .dimension(Constants.EMBEDDING_DIMENSION)
                .build();
    }

}
