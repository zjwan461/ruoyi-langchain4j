package com.ruoyi.ai.dify.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class DifyChatRequest {
    private Map<String, Object> inputs;
    private String query;
    @JsonProperty("response_mode")
    private ResponseModel responseMode;
    @JsonProperty("conversation_id")
    private String conversationId;
    private String user;
    private List<FileInfo> files;

    public static class FileInfo {
        private FileType type;
        @JsonProperty("transfer_method")
        private String transferMethod = "remote_url";
        private String url;

        public FileType getType() {
            return type;
        }

        public void setType(FileType type) {
            this.type = type;
        }

        public String getTransferMethod() {
            return transferMethod;
        }

        public void setTransferMethod(String transferMethod) {
            this.transferMethod = transferMethod;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    // RequestData的getter和setter
    public Map<String, Object> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Object> inputs) {
        this.inputs = inputs;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public ResponseModel getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(ResponseModel responseMode) {
        this.responseMode = responseMode;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public List<FileInfo> getFiles() {
        return files;
    }

    public void setFiles(List<FileInfo> files) {
        this.files = files;
    }


    public enum ResponseModel {
        streaming, blocking
    }

    public enum FileType {
        document, image, audio, video, custom
    }
}