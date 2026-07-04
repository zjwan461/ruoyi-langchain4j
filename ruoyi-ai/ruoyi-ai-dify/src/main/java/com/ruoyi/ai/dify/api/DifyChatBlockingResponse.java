package com.ruoyi.ai.dify.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DifyChatBlockingResponse {
    private String event;
    @JsonProperty("task_id")
    private String taskId;
    private String id;
    @JsonProperty("message_id")
    private String messageId;
    @JsonProperty("conversation_id")
    private String conversationId;
    private String mode;
    private String answer;
    private Metadata metadata;
    @JsonProperty("created_at")
    private Long createdAt;

    public static class Metadata {
        @JsonProperty("annotation_reply")
        private String annotationReply;
        private Usage usage;
        @JsonProperty("retriever_resources")
        private List<RetrieverResource> retrieverResources;

        public String getAnnotationReply() {
            return annotationReply;
        }

        public void setAnnotationReply(String annotationReply) {
            this.annotationReply = annotationReply;
        }

        // Getters and Setters
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

        @Override
        public String toString() {
            return "Metadata{" +
                    "annotationReply='" + annotationReply + '\'' +
                    ", usage=" + usage +
                    ", retrieverResources=" + retrieverResources +
                    '}';
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

        // Getters and Setters
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

        @Override
        public String toString() {
            return "RetrieverResource{" +
                    "position=" + position +
                    ", datasetId='" + datasetId + '\'' +
                    ", datasetName='" + datasetName + '\'' +
                    ", documentId='" + documentId + '\'' +
                    ", documentName='" + documentName + '\'' +
                    ", segmentId='" + segmentId + '\'' +
                    ", score=" + score +
                    ", content='" + content + '\'' +
                    '}';
        }
    }

    // Getters and Setters for EventMessage
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

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "DifyChatBlockingResponse{" +
                "event='" + event + '\'' +
                ", taskId='" + taskId + '\'' +
                ", id='" + id + '\'' +
                ", messageId='" + messageId + '\'' +
                ", conversationId='" + conversationId + '\'' +
                ", mode='" + mode + '\'' +
                ", answer='" + answer + '\'' +
                ", metadata=" + metadata +
                ", createdAt=" + createdAt +
                '}';
    }
}