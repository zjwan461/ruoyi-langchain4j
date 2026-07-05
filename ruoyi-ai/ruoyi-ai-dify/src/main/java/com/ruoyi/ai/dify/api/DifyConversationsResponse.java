package com.ruoyi.ai.dify.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Dify /v1/conversations 接口响应体。
 *
 * <p>对应接口示例：</p>
 * <pre>
 * curl -X GET 'http://localhost/v1/conversations?user=abc-123&last_id=&limit=20' \
 *   --header 'Authorization: Bearer {api_key}'
 * </pre>
 *
 * <p>响应示例：</p>
 * <pre>
 * {
 *   "limit": 20,
 *   "has_more": false,
 *   "data": [
 *     {
 *       "id": "10799fb8-64f7-4296-bbf7-b42bfbe0ae54",
 *       "name": "New chat",
 *       "inputs": { "book": "book", "myName": "Lucy" },
 *       "status": "normal",
 *       "created_at": 1679667915,
 *       "updated_at": 1679667915
 *     }
 *   ]
 * }
 * </pre>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DifyConversationsResponse {

    /** 单页条数。 */
    private Integer limit;

    /** 是否还有更多分页数据。 */
    @JsonProperty("has_more")
    private Boolean hasMore;

    /** 会话列表。 */
    private List<Conversation> data;

    /**
     * 单个会话条目。
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Conversation {
        /** 会话 ID。 */
        private String id;
        /** 会话名称（默认 "New chat"，可被工作流改名）。 */
        private String name;
        /** 用户上传的输入变量。 */
        private Map<String, Object> inputs;
        /** 会话状态，例如 normal。 */
        private String status;
        /** 创建时间（秒级时间戳）。 */
        @JsonProperty("created_at")
        private Long createdAt;
        /** 更新时间（秒级时间戳）。 */
        @JsonProperty("updated_at")
        private Long updatedAt;
    }
}
