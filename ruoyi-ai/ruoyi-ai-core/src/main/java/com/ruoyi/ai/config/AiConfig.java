package com.ruoyi.ai.config;

import com.ruoyi.ai.util.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    private PgVector pgVector;

    private ModelScope modelScope;

    private Agent agent;

    private Sandbox sandbox;

    private Skill skill;

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

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public Sandbox getSandbox() {
        return sandbox;
    }

    public void setSandbox(Sandbox sandbox) {
        this.sandbox = sandbox;
    }

    public Skill getSkill() {
        return skill;
    }

    public void setSkill(Skill skill) {
        this.skill = skill;
    }

    public static class PgVector {

        private String host;
        private int port = 5432;
        private String database;
        private String user;
        private String password;
        private String table;
        private int dimension = Constants.EMBEDDING_DIMENSION;
        private Boolean dropTableFirst = false;

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

        public int getDimension() {
            return dimension;
        }

        public void setDimension(int dimension) {
            this.dimension = dimension;
        }

        public Boolean getDropTableFirst() {
            return dropTableFirst;
        }

        public void setDropTableFirst(Boolean dropTableFirst) {
            this.dropTableFirst = dropTableFirst;
        }
    }

    public static class ModelScope {
        private int downloadThreadNum = 10;

        private String embeddingModelRepoId = "zjwan461/shibing624_text2vec-base-chinese";

        public int getDownloadThreadNum() {
            return downloadThreadNum;
        }

        public void setDownloadThreadNum(int downloadThreadNum) {
            this.downloadThreadNum = downloadThreadNum;
        }

        public String getEmbeddingModelRepoId() {
            return embeddingModelRepoId;
        }

        public void setEmbeddingModelRepoId(String embeddingModelRepoId) {
            this.embeddingModelRepoId = embeddingModelRepoId;
        }
    }

    public static class Agent {
        private MemoryStrategy memoryStrategy;

        private int maxSequentialToolsInvocations = 10;

        public int getMaxSequentialToolsInvocations() {
            return maxSequentialToolsInvocations;
        }

        public void setMaxSequentialToolsInvocations(int maxSequentialToolsInvocations) {
            this.maxSequentialToolsInvocations = maxSequentialToolsInvocations;
        }

        public MemoryStrategy getMemoryStrategy() {
            return memoryStrategy;
        }

        public void setMemoryStrategy(MemoryStrategy memoryStrategy) {
            this.memoryStrategy = memoryStrategy;
        }

        public enum MemoryStrategy {
            MEMORY, REDIS
        }
    }

    /**
     * dify-sandbox 代码执行沙箱配置，对应 docker-compose 中的 sandbox 服务。
     */
    public static class Sandbox {

        /**
         * dify-sandbox 服务地址。
         *
         * <p>容器内通过 compose 服务名 {@code sandbox} 直连，
         * 宿主机本地调试可改为 {@code http://localhost:8194}。</p>
         */
        private String baseUrl = "http://sandbox:8194";

        /**
         * dify-sandbox 鉴权 Key，对应 docker-compose 中 {@code API_KEY}。
         */
        private String apiKey = "dify-sandbox";

        /**
         * 请求超时时间（秒）。dify-sandbox 默认 {@code WORKER_TIMEOUT=-15}（15 秒），
         * 此处取稍大的 30 秒以兼容网络开销。
         */
        private int timeout = 30;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }

    public static class Skill {
        private Path localSkillPath = Path.of("skills");

        public Path getLocalSkillPath() {
            return localSkillPath;
        }

        public void setLocalSkillPath(Path localSkillPath) {
            this.localSkillPath = localSkillPath;
        }


    }
}
