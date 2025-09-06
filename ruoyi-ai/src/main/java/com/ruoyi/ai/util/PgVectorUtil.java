package com.ruoyi.ai.util;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static dev.langchain4j.internal.Utils.getOrDefault;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.config.AiConfig.PgVector;
import com.ruoyi.common.exception.ServiceException;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PgVectorUtil {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .enable(INDENT_OUTPUT);

  private static final Logger logger = LoggerFactory.getLogger(PgVectorUtil.class);

  private PGSimpleDataSource dataSource;

  @Resource
  private AiConfig aiConfig;


  @PostConstruct
  public void initDataSource() {
    PgVector pgVector = aiConfig.getPgVector();
    dataSource = new PGSimpleDataSource();
    dataSource.setServerNames(new String[]{pgVector.getHost()});
    dataSource.setPortNumbers(new int[]{pgVector.getPort()});
    dataSource.setDatabaseName(pgVector.getDatabase());
    dataSource.setUser(pgVector.getUser());
    dataSource.setPassword(pgVector.getPassword());
  }

  static final Map<Class<?>, String> SQL_TYPE_MAP = Stream.of(
          new SimpleEntry<>(Integer.class, "int"),
          new SimpleEntry<>(Long.class, "bigint"),
          new SimpleEntry<>(Float.class, "float"),
          new SimpleEntry<>(Double.class, "float8"),
          new SimpleEntry<>(String.class, "text"),
          new SimpleEntry<>(UUID.class, "uuid"),
          new SimpleEntry<>(Boolean.class, "boolean"),
          // Default
          new SimpleEntry<>(Object.class, "text"))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

  public Connection getConnection() throws SQLException {
    Connection connection = dataSource.getConnection();
    // Find a way to do the following code in connection initialization.
    // Here we assume the datasource could handle a connection pool
    // and we should add the vector type on each connection
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate("CREATE EXTENSION IF NOT EXISTS vector");
    }
    PGvector.addVectorType(connection);
    return connection;
  }

  static final String metadata_sql_template = "AND (metadata->>'%s')::%s is not null and (metadata->>'%s')::bigint = %s";

  public List<Map<String, Object>> selectByMetadata(Map<String, Object> metadata) {
    StringBuilder sql = new StringBuilder();
    sql.append("select * from embedding where 1=1 ");
    for (Entry<String, Object> entry : metadata.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      String columnTyp = SQL_TYPE_MAP.get(value.getClass());
      String sqlTmp = String.format(metadata_sql_template, key, columnTyp, key, value);
      sql.append(sqlTmp);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("generate sql: {}", sql);
    }
    List<Map<String, Object>> result = new ArrayList<>();
    try (Connection connection = this.getConnection()) {
      try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
        try (ResultSet resultSet = ps.executeQuery()) {
          while (resultSet.next()) {
            Map<String, Object> item = new HashMap<>();
            String embeddingId = resultSet.getString("embedding_id");
            PGvector vector = (PGvector) resultSet.getObject("embedding");
            Embedding embedding = new Embedding(vector.toArray());
            String text = resultSet.getString("text");
            String metadataJson = getOrDefault(resultSet.getString("metadata"), "{}");
            Metadata md = new Metadata(OBJECT_MAPPER.readValue(metadataJson, Map.class));
            item.put("embeddingId", embeddingId);
            item.put("embedding", embedding);
            item.put("text", text);
            item.put("metadata", md);
            result.add(item);
          }
        }
      }
    } catch (SQLException | JsonProcessingException e) {
      logger.error(e.getMessage(), e);
      throw new ServiceException("知识库查询失败");
    }
    return result;
  }

  public Map<String, Object> selectById(String embeddingId) {
    String sql = "select * from embedding where embedding_id = ?";
    Map<String, Object> result = null;
    try (Connection connection = this.getConnection()) {
      try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setObject(1, UUID.fromString(embeddingId));
        try (ResultSet resultSet = ps.executeQuery()) {
          if (resultSet.next()) {
            result = new HashMap<>();
            PGvector vector = (PGvector) resultSet.getObject("embedding");
            Embedding embedding = new Embedding(vector.toArray());
            String text = resultSet.getString("text");
            String metadataJson = getOrDefault(resultSet.getString("metadata"), "{}");
            Metadata md = new Metadata(OBJECT_MAPPER.readValue(metadataJson, Map.class));
            result.put("embeddingId", embeddingId);
            result.put("embedding", embedding);
            result.put("text", text);
            result.put("metadata", md);
          }
        }
      }
    } catch (SQLException | JsonProcessingException e) {
      logger.error(e.getMessage(), e);
      throw new ServiceException("知识库查询失败");
    }
    return result;
  }

}
