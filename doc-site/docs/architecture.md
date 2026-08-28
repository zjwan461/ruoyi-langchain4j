# 架构设计

本文档详细介绍 RuoYi-Langchain4j 的系统架构、服务组件和调用链路。

---

## 🏗️ 整体架构

```mermaid
graph TB
    subgraph "前端层"
        UI[ruoyi-ui<br/>Vue3 + Element Plus<br/>端口:80]
    end

    subgraph "API网关层"
        NGINX[Nginx<br/>反向代理 + 静态资源]
    end

    subgraph "应用服务层"
        subgraph "Java后端服务"
            ADMIN[ruoyi-admin<br/>Spring Boot 2.x<br/>端口:8080<br/>gRPC:9090]
            ORIGIN[AgentFlowChatServiceImpl<br/>origin模式<br/>LangChain4j原生执行]
        end
        
        subgraph "Python AI服务"
            AGENTIC[agentic<br/>FastAPI + DeepAgents<br/>端口:8142]
            DOC_PARSER[doc-parser<br/>文档解析服务<br/>端口:8143]
        end
        
        subgraph "沙箱服务"
            SANDBOX[agentic-sandbox<br/>自研Python沙箱<br/>端口:8194<br/>Docker-in-Docker]
        end
    end

    subgraph "数据存储层"
        MYSQL[(MySQL 8.0<br/>业务数据<br/>端口:3306)]
        REDIS[(Redis<br/>缓存 + 会话<br/>端口:6379)]
        PGVECTOR[(PostgreSQL 15<br/>+ pgvector<br/>向量数据库<br/>端口:5432)]
    end

    subgraph "外部服务"
        LLM[大模型 API<br/>OpenAI/通义千问等]
        SEARCH[搜索 API<br/>Tavily等]
    end

    UI --> NGINX
    NGINX --> ADMIN
    
    ADMIN -->|impl_type=origin| ORIGIN
    ADMIN -->|impl_type=agentic<br/>HTTP SSE| AGENTIC
    ADMIN -->|DocumentParseClient<br/>HTTP REST API| DOC_PARSER
    
    AGENTIC -->|gRPC| ADMIN
    AGENTIC -->|python-sandbox-sdk<br/>异步HTTP| SANDBOX
    
    ORIGIN -->|SandboxClient<br/>HTTP REST API| SANDBOX
    ADMIN -->|SandboxClient<br/>HTTP REST API| SANDBOX
    ADMIN --> MYSQL
    ADMIN --> REDIS
    ADMIN --> PGVECTOR
    
    AGENTIC --> PGVECTOR
    AGENTIC --> LLM
    AGENTIC --> SEARCH
    
    DOC_PARSER --> LLM
    
    SANDBOX -->|Docker API<br/>容器隔离执行| DOCKER[Docker Engine<br/>动态创建Python容器]
```

---

## 📦 服务组件

### ruoyi-admin (Java 后端服务)

**技术栈**：
- Spring Boot 2.x
- MyBatis + Druid 连接池
- LangChain4j (AI 能力集成)
- gRPC Server (端口 9090)

**核心职责**：
- 用户认证与权限管理
- AI Agent 配置管理
- 模型管理
- 知识库管理
- 工具管理
- MCP 服务管理
- 对话记录存储
- 提供 gRPC 服务供 Python 服务调用
- 文档解析服务调用（通过 DocumentParseClient）

**gRPC 服务接口**：
- `ModelGrpcServer`: 模型信息查询
- `AiAgentGrpcServer`: AI Agent 信息查询
- `AgentFlowGrpcServer`: Agent Flow 配置查询
- `AgentMcpGrpcServer`: MCP 服务配置查询

### agentic (Python AI Agent 服务)

**技术栈**：
- FastAPI
- LangChain / DeepAgents
- gRPC Client
- SQLAlchemy (异步)

**核心职责**：
- Agent Flow 执行引擎（agentic 模式）
- 多 Agent 协作编排
- 工具调用管理
- 联网搜索集成
- 代码执行沙箱集成
- 流式响应输出 (SSE)

**核心 API**：
- `POST /chat-agentic`: Agent 对话接口 (流式响应)
- `GET /health`: 健康检查

### doc-parser (文档解析服务)

**技术栈**：
- FastAPI
- Docling (文档解析)
- OCR 模型集成

**核心职责**：
- PDF/Word/Excel 文档解析
- 图片 OCR 识别
- 文档内容结构化
- 向量化预处理

**调用方式**：
- 由 ruoyi-admin 通过 HTTP REST API 调用
- 前端上传文件 → ruoyi-admin 调用 doc-parser → 返回解析内容 → 前端携带 attachmentId 发起对话

### agentic-sandbox (自研 Python 代码执行沙箱)

**技术栈**：
- Docker-in-Docker (DinD)
- Python 3.12
- 自研 HTTP REST API 服务

**核心职责**：
- 安全执行用户提交的 Python 代码和 Shell 命令
- 资源限制 (CPU/内存)
- 会话管理 (Session)
- 容器生命周期管理

**双路径调用**：
- **ruoyi-admin (Java)**: 通过 `SandboxClient.java` 调用（origin 模式）
- **agentic (Python)**: 通过 `python-sandbox-sdk` 调用（agentic 模式）

---

## 🔗 调用链路

### Agent Flow 对话主链路（双模式路由）

Agent Flow 对话请求统一由前端发送到 `ruoyi-admin`，后端根据 `agent_flow.impl_type` 字段决定执行路径：

- **origin 模式**：`ruoyi-admin` 自身使用 LangChain4j 原生执行 Flow
- **agentic 模式**：`ruoyi-admin` 通过 `AgenticServiceImpl` → `AgenticRequestUtil` 将请求转发给 Python `agentic` 服务

```mermaid
sequenceDiagram
    participant U as 用户
    participant UI as ruoyi-ui
    participant ADMIN as ruoyi-admin<br/>(Java)
    participant AG as agentic<br/>(Python)
    participant GRPC as gRPC
    participant DB as MySQL
    participant REDIS as Redis
    participant PG as PostgreSQL<br/>(pgvector)
    participant LLM as 大模型 API
    participant SB as Sandbox

    U->>UI: 发送消息
    UI->>ADMIN: POST /api/ai/flow/chat

    ADMIN->>DB: 查询 AgentFlow 配置
    DB-->>ADMIN: 返回 agent_flow (含 impl_type)

    alt impl_type = origin（LangChain4j 原生执行）
        ADMIN->>ADMIN: AgentFlowChatServiceImpl<br/>FlowTreeParser 解析 Flow
        ADMIN->>PG: 检索知识库向量
        PG-->>ADMIN: 返回相关文档
        ADMIN->>REDIS: 加载对话记忆
        REDIS-->>ADMIN: 对话历史
        ADMIN->>LLM: 调用大模型
        LLM-->>ADMIN: 流式响应
        alt 需要代码执行
            ADMIN->>SB: SandboxClient.execPython()
            SB-->>ADMIN: 执行结果
        end
        ADMIN->>REDIS: 保存对话记录
        ADMIN-->>UI: SSE 流式响应
    else impl_type = agentic（DeepAgents 执行）
        ADMIN->>AG: AgenticRequestUtil<br/>POST /chat-agentic (HTTP SSE)
        AG->>GRPC: 获取 Agent 配置
        GRPC->>ADMIN: getAiAgentWithToolAndSkillById
        ADMIN-->>GRPC: 返回 Agent 信息
        GRPC-->>AG: Agent 配置
        AG->>GRPC: 获取模型配置
        GRPC->>ADMIN: getModelById
        ADMIN-->>GRPC: 返回模型
        GRPC-->>AG: 模型配置
        AG->>PG: 检索知识库向量
        PG-->>AG: 返回相关文档
        AG->>LLM: 调用大模型
        LLM-->>AG: 流式响应
        alt 需要代码执行
            AG->>SB: python-sandbox-sdk.exec_shell()
            SB-->>AG: 执行结果
        end
        AG->>PG: 保存 checkpoint
        AG-->>ADMIN: SSE 流式响应
        ADMIN->>REDIS: 保存对话记录
        ADMIN-->>UI: SSE 流式响应
    end

    UI-->>U: 显示回复
```

### gRPC 调用链路

```mermaid
graph LR
    A[agentic<br/>Python Client] -->|gRPC| B[ruoyi-admin<br/>gRPC Server :9090]
    
    B --> C[ModelGrpcServer]
    B --> D[AiAgentGrpcServer]
    B --> E[AgentFlowGrpcServer]
    B --> F[AgentMcpGrpcServer]
    
    C --> G[IModelService]
    D --> H[IAIAgentService]
    E --> I[IAgentFlowService]
    F --> J[IAgentMcpService]
    
    G --> K[MySQL]
    H --> K
    I --> K
    J --> K
```

### Agent Flow 执行链路（双模式对比）

```mermaid
graph TD
    A[前端请求] --> B[ruoyi-admin<br/>AIFlowChatController]
    B --> C{agent_flow.impl_type?}
    
    C -->|origin| D[AgentFlowChatServiceImpl<br/>LangChain4j 原生执行]
    C -->|agentic| E[AgenticServiceImpl<br/>转发到 Python agentic 服务]
    
    D --> D1[FlowTreeParser<br/>解析 Flow 结构]
    D1 --> D2[构建执行树 TreeNode]
    D2 --> D3{节点类型?}
    
    D3 -->|LLM节点| D4[LangChain4j<br/>调用大模型]
    D3 -->|工具节点| D5[JToolProvider<br/>执行工具]
    D3 -->|代码节点| D6[SandboxClient<br/>沙箱执行]
    D3 -->|知识库节点| D7[PgVectorUtil<br/>向量检索]
    
    D4 --> D8[流式输出]
    D5 --> D8
    D6 --> D8
    D7 --> D8
    
    D8 --> D9{是否有下一节点?}
    D9 -->|是| D3
    D9 -->|否| D10[返回最终结果]
    
    E --> E1[AgenticRequestUtil<br/>HTTP SSE 转发]
    E1 --> E2[agentic 服务<br/>FastAPI /chat-agentic]
    E2 --> E3[flow_tree_parser<br/>解析 Flow 结构]
    E3 --> E4[构建 TreeNode<br/>嵌套 Deep Agent]
    E4 --> E5{节点类型?}
    
    E5 -->|LLM节点| E6[LangChain<br/>调用大模型]
    E5 -->|工具节点| E7[MCP / 工具调用]
    E5 -->|代码节点| E8[python-sandbox-sdk<br/>沙箱执行]
    E5 -->|知识库节点| E9[pgvector<br/>向量检索]
    
    E6 --> E10[流式输出]
    E7 --> E10
    E8 --> E10
    E9 --> E10
    
    E10 --> E11{是否有下一节点?}
    E11 -->|是| E5
    E11 -->|否| E12[SSE 流式返回<br/>ruoyi-admin → 前端]
```

---

## 🗄️ 中间件架构

### MySQL 8.0

**用途**：
- 业务数据持久化存储
- 用户、角色、权限数据
- AI Agent 配置数据
- 对话记录存储

**关键表**：
- `sys_user`, `sys_role`, `sys_menu`: 系统权限
- `ai_agent`: AI Agent 配置
- `ai_model`: 模型配置
- `ai_agent_flow`: Agent Flow 定义
- `ai_tool`, `ai_mcp_service`: 工具与 MCP 服务
- `chat_message`, `chat_flow_message`: 对话记录
- `knowledge_base`: 知识库配置

### Redis

**用途**：
- 会话缓存
- 验证码存储
- 对话记忆 (Chat Memory)
- 令牌管理
- 在线用户管理

### PostgreSQL 15 + pgvector

**用途**：
- 向量数据库
- 知识库 Embedding 存储
- 语义检索

**关键特性**：
- pgvector 扩展: 向量相似度计算
- pgvectorscale: 高性能向量检索
- 向量维度: 768 (text2vec-base-chinese)

---

## 🔐 安全机制

### 认证授权
- JWT Token
- 验证码 (滑块/文字点选)
- XSS 防护
- SQL 注入防护 (Druid Wall)

### 沙箱安全
- 容器隔离
- 资源限制
- 超时控制
- API Key 认证

---

## 📊 性能优化

### 数据库优化
- 连接池配置 (Druid)
- 向量索引: pgvector HNSW
- 业务索引优化

### 缓存策略
- Caffeine 本地缓存 (热点数据)
- Redis 分布式缓存 (会话、对话记忆)

### 异步处理
- FastAPI 异步
- Spring @Async
- 消息队列 (规划中)
