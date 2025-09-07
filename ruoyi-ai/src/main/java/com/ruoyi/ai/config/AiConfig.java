package com.ruoyi.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    private Translate translate;

    private PgVector pgVector;

    private ModelScope modelScope;

    public Translate getTranslate() {
        return translate;
    }

    public void setTranslate(Translate translate) {
        this.translate = translate;
    }

    public PgVector getPgVector() {
        return pgVector;
    }

    public void setPgVector(PgVector pgVector) {
        this.pgVector = pgVector;
    }

    public ModelScope getModelScope() {
        return modelScope;
    }

    public void setModelScope(ModelScope modelScope) {
        this.modelScope = modelScope;
    }

    public static class Translate {

        private String baseUri;

        private String modelName;

        private Double temperature;

        private String systemMessage;

        public String getBaseUri() {
            return baseUri;
        }

        public void setBaseUri(String baseUri) {
            this.baseUri = baseUri;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public String getSystemMessage() {
            return systemMessage;
        }

        public void setSystemMessage(String systemMessage) {
            this.systemMessage = systemMessage;
        }
    }

    public static class PgVector {

        private String host;
        private int port = 5432;
        private String database;
        private String user;
        private String password;
        private String table;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }
    }

    public static class ModelScope {
        private int downloadThreadNum = 10;

        public int getDownloadThreadNum() {
            return downloadThreadNum;
        }

        public void setDownloadThreadNum(int downloadThreadNum) {
            this.downloadThreadNum = downloadThreadNum;
        }
    }

}
