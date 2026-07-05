package com.ruoyi.ai.dify.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Dify Chat Streaming API（SSE）事件响应。
 *
 * <p>Dify 的 streaming 模式下响应是 Server-Sent Events 格式，每条事件形如：</p>
 * <pre>
 * data: {"event":"message","id":"...","task_id":"...","answer":"...","created_at":...}
 *
 * data: {"event":"agent_thought", ...}
 *
 * data: {"event":"message_end","metadata":{...}}
 *
 * data: {"event":"error","status":...,"code":"...","message":"..."}
 * </pre>
 *
 * <p>不同 {@code event} 字段会携带不同的负载，因此本类把全部可能的字段都列出来，
 * 调用方可根据 {@link #getEvent()} 做分支处理。</p>
 *
 * <p>常见事件类型：</p>
 * <ul>
 *     <li>{@code message} / {@code agent_message}：增量回答片段，{@link #getAnswer()} 携带本次增量</li>
 *     <li>{@code message_replace} / {@code agent_message_replace}：覆盖式回答，{@link #getAnswer()} 携带完整内容</li>
 *     <li>{@code message_end} / {@code agent_message_end}：消息结束，{@link #getMetadata()} 携带完整元数据</li>
 *     <li>{@code message_file}：附带文件，{@link #getFiles()} 携带文件信息</li>
 *     <li>{@code agent_thought}：Agent 思考步骤，{@link #getThought()} / {@link #getObservation()} 等</li>
 *     <li>{@code error}：错误事件，{@link #getStatus()} / {@link #getCode()} / {@link #getMessage()}</li>
 *     <li>{@code ping}：心跳事件，无业务字段</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DifyChatStreamingResponse {

    /** 事件类型，如 message / agent_message / message_end / agent_thought / error 等 */
    private String event;

    @JsonProperty("task_id")
    private String taskId;

    /** 消息唯一 ID（message / agent_message / message_end 等事件携带） */
    private String id;

    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("conversation_id")
    private String conversationId;

    /** 模式，固定为 streaming */
    private String mode;

    /** 本次增量或完整的回答内容（message / agent_message / message_replace 等事件携带） */
    private String answer;

    @JsonProperty("created_at")
    private Long createdAt;

    /** 元数据（仅 message_end / agent_message_end 事件携带） */
    private Metadata metadata;

    // ------------------ agent_thought 事件字段 ------------------

    /** Agent 思考步骤的位置 */
    private Integer position;

    /** Agent 思考内容 */
    private String thought;

    /** Agent 工具调用的观察结果 */
    private String observation;

    /** Agent 调用的工具名 */
    private String tool;

    /** Agent 工具输入（JSON 字符串） */
    @JsonProperty("tool_input")
    private String toolInput;

    /** Agent 思考步骤中产生的文件 */
    @JsonProperty("message_files")
    private List<String> messageFiles;

    // ------------------ message_file 事件字段 ------------------

    /** message_file 事件携带的文件信息 */
    private List<FileRef> files;

    // ------------------ error 事件字段 ------------------

    /** HTTP 状态码（错误事件携带） */
    private Integer status;

    /** 错误码 */
    private String code;

    /** 错误描述 */
    private String message;

    /**
     * 消息结束事件携带的元数据，与 {@link DifyChatBlockingResponse.Metadata} 字段保持一致。
     */
    public static class Metadata {

        @JsonProperty("annotation_reply")
        private String annotationReply;
        /**
         * 当前消息使用的模型信息
         */
        @JsonProperty("current_slice")
        private Object currentSlice;

        /**
         * 使用量
         */
        private Usage usage;

        /**
         * 检索引用的资源
         */
        @JsonProperty("retriever_resources")
        private List<RetrieverResource> retrieverResources;

        public Object getCurrentSlice() {
            return currentSlice;
        }

        public void setCurrentSlice(Object currentSlice) {
            this.currentSlice = currentSlice;
        }

        public Usage getUsage() {
            return usage;
        }

        public void setUsage(Usage usage) {
            this.usage = usage;
        }

        public List<RetrieverResource> getRetrieverResources() {
            return retrieverResources;
        }

        public void setRetrieverResources(List<RetrieverResource> retrieverResources) {
            this.retrieverResources = retrieverResources;
        }

        public String getAnnotationReply() {
            return annotationReply;
        }

        public void setAnnotationReply(String annotationReply) {
            this.annotationReply = annotationReply;
        }
    }

    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        @JsonProperty("prompt_unit_price")
        private String promptUnitPrice;
        @JsonProperty("prompt_price_unit")
        private String promptPriceUnit;
        @JsonProperty("prompt_price")
        private String promptPrice;
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        @JsonProperty("completion_unit_price")
        private String completionUnitPrice;
        @JsonProperty("completion_price_unit")
        private String completionPriceUnit;
        @JsonProperty("completion_price")
        private String completionPrice;
        @JsonProperty("total_tokens")
        private Integer totalTokens;
        @JsonProperty("total_price")
        private String totalPrice;
        private String currency;
        private Double latency;
        @JsonProperty("time_to_first_token")
        private Double timeToFirstToken;
        @JsonProperty("time_to_generate")
        private Double timeToGenerate;

        // Getters and Setters
        public Integer getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }

        public String getPromptUnitPrice() {
            return promptUnitPrice;
        }

        public void setPromptUnitPrice(String promptUnitPrice) {
            this.promptUnitPrice = promptUnitPrice;
        }

        public String getPromptPriceUnit() {
            return promptPriceUnit;
        }

        public void setPromptPriceUnit(String promptPriceUnit) {
            this.promptPriceUnit = promptPriceUnit;
        }

        public String getPromptPrice() {
            return promptPrice;
        }

        public void setPromptPrice(String promptPrice) {
            this.promptPrice = promptPrice;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
        }

        public String getCompletionUnitPrice() {
            return completionUnitPrice;
        }

        public void setCompletionUnitPrice(String completionUnitPrice) {
            this.completionUnitPrice = completionUnitPrice;
        }

        public String getCompletionPriceUnit() {
            return completionPriceUnit;
        }

        public void setCompletionPriceUnit(String completionPriceUnit) {
            this.completionPriceUnit = completionPriceUnit;
        }

        public String getCompletionPrice() {
            return completionPrice;
        }

        public void setCompletionPrice(String completionPrice) {
            this.completionPrice = completionPrice;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }

        public String getTotalPrice() {
            return totalPrice;
        }

        public void setTotalPrice(String totalPrice) {
            this.totalPrice = totalPrice;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public Double getLatency() {
            return latency;
        }

        public void setLatency(Double latency) {
            this.latency = latency;
        }

        public Double getTimeToFirstToken() {
            return timeToFirstToken;
        }

        public void setTimeToFirstToken(Double timeToFirstToken) {
            this.timeToFirstToken = timeToFirstToken;
        }

        public Double getTimeToGenerate() {
            return timeToGenerate;
        }

        public void setTimeToGenerate(Double timeToGenerate) {
            this.timeToGenerate = timeToGenerate;
        }

        @Override
        public String toString() {
            return "Usage{" +
                    "promptTokens=" + promptTokens +
                    ", promptUnitPrice='" + promptUnitPrice + '\'' +
                    ", promptPriceUnit='" + promptPriceUnit + '\'' +
                    ", promptPrice='" + promptPrice + '\'' +
                    ", completionTokens=" + completionTokens +
                    ", completionUnitPrice='" + completionUnitPrice + '\'' +
                    ", completionPriceUnit='" + completionPriceUnit + '\'' +
                    ", completionPrice='" + completionPrice + '\'' +
                    ", totalTokens=" + totalTokens +
                    ", totalPrice='" + totalPrice + '\'' +
                    ", currency='" + currency + '\'' +
                    ", latency=" + latency +
                    ", timeToFirstToken=" + timeToFirstToken +
                    ", timeToGenerate=" + timeToGenerate +
                    '}';
        }
    }

    public static class RetrieverResource {
        private Integer position;

        @JsonProperty("dataset_id")
        private String datasetId;

        @JsonProperty("dataset_name")
        private String datasetName;

        @JsonProperty("document_id")
        private String documentId;

        @JsonProperty("document_name")
        private String documentName;

        @JsonProperty("segment_id")
        private String segmentId;

        private Double score;

        private String content;

        public Integer getPosition() {
            return position;
        }

        public void setPosition(Integer position) {
            this.position = position;
        }

        public String getDatasetId() {
            return datasetId;
        }

        public void setDatasetId(String datasetId) {
            this.datasetId = datasetId;
        }

        public String getDatasetName() {
            return datasetName;
        }

        public void setDatasetName(String datasetName) {
            this.datasetName = datasetName;
        }

        public String getDocumentId() {
            return documentId;
        }

        public void setDocumentId(String documentId) {
            this.documentId = documentId;
        }

        public String getDocumentName() {
            return documentName;
        }

        public void setDocumentName(String documentName) {
            this.documentName = documentName;
        }

        public String getSegmentId() {
            return segmentId;
        }

        public void setSegmentId(String segmentId) {
            this.segmentId = segmentId;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    /**
     * message_file 事件携带的文件引用。
     */
    public static class FileRef {
        private String id;
        private String type;
        private String url;
        @JsonProperty("belongs_to")
        private String belongsTo;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getBelongsTo() {
            return belongsTo;
        }

        public void setBelongsTo(String belongsTo) {
            this.belongsTo = belongsTo;
        }
    }

    // ==================== Getters & Setters ====================

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getThought() {
        return thought;
    }

    public void setThought(String thought) {
        this.thought = thought;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public String getToolInput() {
        return toolInput;
    }

    public void setToolInput(String toolInput) {
        this.toolInput = toolInput;
    }

    public List<String> getMessageFiles() {
        return messageFiles;
    }

    public void setMessageFiles(List<String> messageFiles) {
        this.messageFiles = messageFiles;
    }

    public List<FileRef> getFiles() {
        return files;
    }

    public void setFiles(List<FileRef> files) {
        this.files = files;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}