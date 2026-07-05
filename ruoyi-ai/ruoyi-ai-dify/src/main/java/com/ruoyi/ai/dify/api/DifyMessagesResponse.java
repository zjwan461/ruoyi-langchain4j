package com.ruoyi.ai.dify.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Dify /v1/messages 接口响应体。
 *
 * <p>对应接口示例：</p>
 * <pre>
 * curl -X GET 'http://localhost/v1/messages?user=abc-123&conversation_id={conversation_id}' \
 *   --header 'Authorization: Bearer {api_key}'
 * </pre>
 *
 * <p>响应示例（截取）：</p>
 * <pre>
 * {
 *   "limit": 20,
 *   "has_more": false,
 *   "data": [
 *     {
 *       "id": "a076a87f-31e5-48dc-b452-0061adbbc922",
 *       "conversation_id": "cd78daf6-f9e4-4463-9ff2-54257230a0ce",
 *       "inputs": { "name": "dify" },
 *       "query": "iphone 13 pro",
 *       "answer": "...",
 *       "message_files": [],
 *       "feedback": null,
 *       "retriever_resources": [ ... ],
 *       "created_at": 1705569239
 *     }
 *   ]
 * }
 * </pre>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DifyMessagesResponse {

    /** 单页条数。 */
    private Integer limit;

    /** 是否还有更多分页数据。 */
    @JsonProperty("has_more")
    private Boolean hasMore;

    /** 历史消息列表。 */
    private List<Message> data;

    /**
     * 单条历史消息。
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        /** 消息 ID。 */
        private String id;
        /** 所属会话 ID。 */
        @JsonProperty("conversation_id")
        private String conversationId;
        /** 用户输入变量。 */
        private Map<String, Object> inputs;
        /** 用户问题。 */
        private String query;
        /** 助手回答。 */
        private String answer;
        /** 消息附属的文件列表。 */
        @JsonProperty("message_files")
        private List<Map<String, Object>> messageFiles;
        /**
         * 用户反馈结果，可能为 {@code null} 或形如 {@code {"rating": "like" / "dislike"}} 的对象。
         * 由于结构可能变化，这里用 {@link Object} 兼容解析。
         */
        private Object feedback;
        /** 检索召回的资源（如知识库片段）。 */
        @JsonProperty("retriever_resources")
        private List<Map<String, Object>> retrieverResources;
        /** 创建时间（秒级时间戳）。 */
        @JsonProperty("created_at")
        private Long createdAt;
    }
}
