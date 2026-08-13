package com.ruoyi.ai.config;

import com.ruoyi.ai.util.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    private PgVector pgVector;

    private ModelScope modelScope;

    private Agent agent;

    private Sandbox sandbox;

    private Skill skill;

    private Mcp mcp;

    private DocParser docParser;

    private TavilyWebSearch tavilyWebSearch;

    private Agentic agentic;

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

    public Mcp getMcp() {
        return mcp;
    }

    public void setMcp(Mcp mcp) {
        this.mcp = mcp;
    }

    public DocParser getDocParser() {
        return docParser;
    }

    public void setDocParser(DocParser docParser) {
        this.docParser = docParser;
    }

    public TavilyWebSearch getTavilyWebSearch() {
        return tavilyWebSearch;
    }

    public void setTavilyWebSearch(TavilyWebSearch tavilyWebSearch) {
        this.tavilyWebSearch = tavilyWebSearch;
    }

    public Agentic getAgentic() {
        return agentic;
    }

    public void setAgentic(Agentic agentic) {
        this.agentic = agentic;
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
        private boolean sync2Db = Boolean.TRUE;

        public Path getLocalSkillPath() {
            return localSkillPath;
        }

        public void setLocalSkillPath(Path localSkillPath) {
            this.localSkillPath = localSkillPath;
        }


        public boolean isSync2Db() {
            return sync2Db;
        }

        public void setSync2Db(boolean sync2Db) {
            this.sync2Db = sync2Db;
        }
    }

    public static class Mcp {
        private Duration timeout = Duration.ofSeconds(30);

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }

    public static class DocParser {
        private String serverUrl;

        private String apiKey;

        private int connectTimeout;

        private int readTimeout;

        public String getServerUrl() {
            return serverUrl;
        }

        public void setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }
    }

    public static class TavilyWebSearch {
        // check config at: https://github.com/langchain4j/langchain4j-examples/blob/main/rag-examples/src/main/java/_3_advanced/_08_Advanced_RAG_Web_Search_Example.java
        private Boolean enabled = Boolean.FALSE;

        private String apiKey;

        private Duration timeout;

        private String searchDepth;

        private Boolean includeAnswer = Boolean.TRUE;

        private Boolean includeRawContent = Boolean.TRUE;

        private List<String> includeDomains;

        private List<String> excludeDomains;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public String getSearchDepth() {
            return searchDepth;
        }

        public void setSearchDepth(String searchDepth) {
            this.searchDepth = searchDepth;
        }

        public Boolean getIncludeAnswer() {
            return includeAnswer;
        }

        public void setIncludeAnswer(Boolean includeAnswer) {
            this.includeAnswer = includeAnswer;
        }

        public Boolean getIncludeRawContent() {
            return includeRawContent;
        }

        public void setIncludeRawContent(Boolean includeRawContent) {
            this.includeRawContent = includeRawContent;
        }

        public List<String> getIncludeDomains() {
            return includeDomains;
        }

        public void setIncludeDomains(List<String> includeDomains) {
            this.includeDomains = includeDomains;
        }

        public List<String> getExcludeDomains() {
            return excludeDomains;
        }

        public void setExcludeDomains(List<String> excludeDomains) {
            this.excludeDomains = excludeDomains;
        }
    }

    /**
     * Agentic Agent 服务配置
     */
    public static class Agentic {
        /**
         * 服务完整地址: http://ip:port/chat-agentic
         */
        private String serverUrl;

        /**
         * 连接超时 ms
         */
        private int connectTimeout = 30000;

        /**
         * 读取超时 ms（SSE 流式响应需要较长超时）
         */
        private int readTimeout = 300000;

        public String getServerUrl() {
            return serverUrl;
        }

        public void setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }
    }
}
