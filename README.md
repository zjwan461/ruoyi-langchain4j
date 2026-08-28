<div align="center">

# RuoYi-Langchain4j

**基于 RuoYi-Vue 集成 LangChain4j 的企业级 AI 快速开发平台**

[![RuoYi-Vue](https://img.shields.io/badge/Base-RuoYi--Vue-blue)](https://github.com/yangzongzhuan/RuoYi-Vue)
[![LangChain4j](https://img.shields.io/badge/AI-LangChain4j-green)](https://docs.langchain4j.info/)
[![MCP](https://img.shields.io/badge/Protocol-MCP-9cf)](https://modelcontextprotocol.io/)
[![Skills](https://img.shields.io/badge/AI-Skills-orange)]()
[![Spring Boot](https://img.shields.io/badge/SpringBoot-2.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-2.x-success)](https://vuejs.org/)
[![License](https://img.shields.io/badge/license-MIT-orange)](./LICENSE)

> 前端 Vue / Element UI · 后端 Spring Boot / Spring Security / Redis & Jwt / WebSocket / LangChain4j / PgVector / Python Sandbox / **MCP** / **Skills** · Python 微服务（**doc-parser 文档解析** / **agentic 智能体网关**）

[快速开始](#-快速开始) · [架构设计](./ARCHITECTURE.md) · [在线体验](#-在线体验) · [演示图](#-演示图) · [支持项目](#-支持项目)

</div>

---

## 📖 项目简介

RuoYi-Langchain4j 是在经典后台管理框架 **[RuoYi-Vue](https://github.com/yangzongzhuan/RuoYi-Vue)** 之上，深度集成 **[LangChain4j](https://docs.langchain4j.info/)**、向量数据库 **PgVector**、**[Dify](https://dify.ai/)** 生态与 **[MCP（Model Context Protocol）](https://modelcontextprotocol.io/)** 协议 的扩展版本。
项目在保留 RuoYi 原有权限、监控、定时任务、代码生成等全部能力的同时，新增了 **AI 模型管理 / 本地知识库 / 多类型智能体 / AI 对话 / MCP 工具市场 / Skills 技能体系** 等一站式 AI 能力，致力于帮助开发者以最低成本快速构建企业级 AI 应用。

v1.1 起，平台内置 **Dify 类型智能体**、**原生智能体调用本地 Java Tool**、**调用 Python Sandbox 执行 Python Tool** 三种智能体形态。
v1.2 起，进一步引入 **MCP 协议工具市场** 与 **Skills 技能体系**：智能体既可挂载任意 MCP Server 暴露的工具（`sse` / `stdio` / `streamable-http`），也可绑定 Skills 技能目录，由 LLM 在匹配场景下自动 `activate_skill` 后再继续作答。
v1.4 起，新增两个独立 Python 微服务：**doc-parser**（基于 Docling + VLM 的文档解析服务，支持 PDF / Office / 代码等 60+ 格式转 Markdown）与 **agentic**（基于 LangChain / DeepAgents 的智能体网关，通过 gRPC 与 Java 后端通信，支持多智能体编排 Flow 的流式对话）。

### 🧩 技术栈

| 类别 | 技术 |
| --- | --- |
| 前端 | Vue、Element UI、Axios、Vue Router、Vuex、Monaco 代码编辑器 |
| 后端 | Spring Boot、Spring Security、MyBatis-Plus、Redis、Jwt、WebSocket |
| AI 框架 | [LangChain4j](https://docs.langchain4j.info/) + [langchain4j-mcp](https://docs.langchain4j.info/)（兼容 Ollama / OpenAI 协议） |
| Dify 生态 | [Dify](https://dify.ai/) 应用接入 |
| **代码沙箱** | [python-sandbox](https://github.com/zjwan461/python-sandbox) 自研 Python 代码执行沙箱（本仓库使用镜像 `ghcr.io/zjwan461/python-sandbox:v1.0.0`） |
| **Python 微服务** | **doc-parser**（[FastAPI](https://fastapi.tiangolo.com/) + [Docling](https://github.com/DS4SD/docling) + VLM 文档解析）/ **agentic**（[FastAPI](https://fastapi.tiangolo.com/) + [LangChain](https://www.langchain.com/) + [DeepAgents](https://github.com/langchain-ai/deepagents) 智能体网关，gRPC 通信） |
| **MCP** | **[Model Context Protocol](https://modelcontextprotocol.io/) 工具协议（`sse` / `stdio` / `streamable-http`）** |
| **Skills** | **基于 `agent_skill` 的技能目录管理，支持 `create` / `import` 两种类型，自动追加到 System Prompt** |
| **多智能体编排** | **[AntV X6](https://x6.antv.antgroup.com/) 可视化画布 + Flow 节点驱动，多个智能体串联协作** |
| 向量库 | [PgVector](https://github.com/pgvector/pgvector)（基于 PostgreSQL） |
| 部署 | Docker、Docker Compose、Nginx |

### ⭐ 核心特性

- 🤖 **AI 模型管理**：统一管理大语言模型（LLM）与文本向量模型（Embedding），支持 Ollama 与 OpenAI 兼容协议，支持在线下载/加载本地向量模型。
- 📚 **本地知识库**：支持文档自动分段、人工调整分段、向量化与重向量化。
- 🧠 **多类型智能体（Agent）**：内置三类智能体形态 —— 原生 LangChain4j Agent、Dify 应用代理、以及两者的工具调用扩展。
- 💬 **AI 对话**：支持流式输出、深度思考（Chain-of-Thought）UI 渲染、历史会话管理。
- 🔔 **异步推送**：基于 WebSocket 推送向量下载、文档分段处理等耗时任务结果。
- 🛠️ **RuoYi 全量能力**：用户/角色/菜单/字典/定时任务/代码生成/服务监控等开箱即用。
- 🔐 **JWT 多端鉴权**：动态权限菜单 + 数据权限 + 按钮级别控制。
- 🪄 **Dify 类型智能体**：通过 [Dify](https://dify.ai/) 平台统一纳管 Chatflow / Workflow / Agent 等 Dify 应用，对话与会话映射全部走本地 `dify_session_mapping`，流式输出 + 历史会话一键拉取，纳管入口在【AI 工具箱 → Dify API 管理】。
- ☕️ **原生智能体调用本地 Java Tool**：原生（LangChain4j）智能体支持挂载 `builtin` 类型的 Java 工具（基于 `dev.langchain4j.agent.tool.Tool` 注解），在 `JToolProvider` 中通过反射扫描 `@Tool` 方法注册到 LangChain4j，实现 LLM ↔ 本地业务方法的工具调用。
- 🐍 **原生智能体调用 Python Sandbox 执行 Python Tool**：原生智能体支持挂载 `sandbox` 类型的 Python 工具，`SandboxToolExecutor` 会自动将 LLM 传入的参数按 `parameters` 元数据拼装成 Python 函数调用，远程下发 [python-sandbox](https://github.com/zjwan461/python-sandbox) 沙箱执行并回传 `print(result)` 结果。**v1.2 起新增 Monaco 代码编辑器**（`ruoyi-ui/src/components/Editor/codeEditor.vue`），在线编辑 Python 函数代码更友好。
- 🔌 **MCP 工具市场（v1.2 新增）**：原生智能体支持挂载来自 [Model Context Protocol](https://modelcontextprotocol.io/) Server 的工具。在【AI 工具箱 → MCP】维护 `agent_mcp_service`（`url` + `transport` + `authToken`）与子级 `agent_mcp_tool`（`name` / `description` / `parameters` / `metadata`），支持 `sse` / `stdio` / `streamable-http` 三种传输方式；`JToolProvider` 通过 langchain4j-mcp 加载后注册到智能体，LLM 可直接调用远端 MCP 工具。智能体侧通过 `agent_mcp_tool_mapping` 多对多绑定 MCP 工具，已自带 OkHttp 请求日志拦截器（`McpRequestLoggingInterceptor`）便于排障。
- 📜 **Skills 技能体系（v1.2 新增）**：在【AI 工具箱 → Skill】维护 `agent_skill` 技能（`skill_name` / `skill_description` / `skill_content` / `skill_type`），支持 `create`（系统创建）和 `import`（从仓库 `skills/` 目录导入）两种类型；智能体通过 `agent_skill_mapping` 多对多挂载技能，平台会把技能信息按 `Constants.SKILL_APPEND_SYSTEM_MESSAGE` 模板追加到 System Prompt，提示模型通过 `activate_skill` 工具在匹配场景下先激活技能再继续作答。
- 🕸 **多智能体编排 / Agent Flow（v1.3 新增）**：在【AI 工具箱 → 多智能体】通过 **[AntV X6](https://x6.antv.antgroup.com/)** 可视化画布拖拽多个智能体节点，拼接为多智能体协作链/树状 Flow（`agent_flow`）；后端 `FlowTreeParser` 把 X6 `flowJson` 解析为节点树，按节点切换执行智能体并通过 `AgentFlowChatServiceImpl` 产出流式响应，节点间可独立设置 `promptTemplate` / `flowMemory` / `showReasoning`。每个 Flow 生成独立 `visit_url`，支持单客户端每日访问限流（`day_lmt_per_client`）。
- 📄 **文档解析服务 doc-parser（v1.4 新增）**：独立 Python 微服务（`doc-parser/`），基于 [Docling](https://github.com/DS4SD/docling) + 视觉语言模型（VLM），支持 PDF / DOCX / XLSX / PPTX / HTML / CSV / Markdown 等 60+ 种文件格式统一转为 Markdown，内置 OCR、表格识别、图片描述生成能力，通过 `PUT /process` 接口对外提供服务，支持 API Key 鉴权。
- 🤖 **智能体网关 agentic（v1.4 新增）**：独立 Python 微服务（`agentic/`），基于 [LangChain](https://www.langchain.com/) + [DeepAgents](https://github.com/langchain-ai/deepagents) + [LangGraph](https://github.com/langchain-ai/langgraph)，通过 gRPC 与 Java 后端（`ruoyi-admin:9090`）通信获取模型配置，接收前端 Agent Flow 的 `flow_json` 并解析为 `TreeNode`，构建嵌套 Deep Agent 执行多智能体协作流式对话，支持联网搜索（Tavily）、图片上传、Sandbox 代码执行、**Skill 技能调用**、PostgreSQL checkpoint 持久化。
- 📊 **工具调用与召回可观测**：自动记录 `agent_tool_invoke`（工具调用结果）与 `agent_kb_invoke`（知识库召回分段），与消息 ID 关联，方便排障与审计；前端可通过 `TOOL_CALL` / `TOOL_CALLED` / `RETRIEVED` 三类 SSE 事件实时观察调用过程。

---

## ⚠️ 重要提示

> 1. **基础环境部署**与 RuoYi-Vue 一致，详见官方文档：<https://doc.ruoyi.vip/ruoyi-vue/>
> 2. **RAG 功能依赖 PgVector**，本地推荐使用 Docker 部署（项目已附带 `docker-compose-middleware.yml`）。
> 3. **AI 对话需提前在【AI 工具箱 → 模型管理】中配置可用的大语言模型与向量模型。**
> 4. **Dify 类型智能体**需提前在【AI 工具箱 → Dify API 管理】中配置外部 Dify 服务的 URL 前缀与 API Key；本地推荐使用 Docker Compose 部署 [Dify](https://github.com/langgenius/dify)。
> 5. **Python 工具（Sandbox 类型）**依赖自研 [python-sandbox](https://github.com/zjwan461/python-sandbox) 服务，部署完成后请在 `application.yml` 中配置 `ai.sandbox.*` 相关地址与 API Key。`docker-compose.yml` 中 sandbox 服务使用的是镜像 `ghcr.io/zjwan461/python-sandbox:v1.0.0`。
> 6. **MCP 工具**需要在【AI 工具箱 → MCP】维护外部 MCP Server（`url` + `transport`），`sse` / `streamable-http` 需保证后端能访问到 MCP 服务的 HTTP 地址，`stdio` 需确保 MCP 进程在宿主环境可被拉起。
> 7. **Skills 技能**支持两种录入方式：在【AI 工具箱 → Skill】直接 `create` 新增，或者在仓库 `skills/` 目录下维护 `SKILL.md` 后选择 `import` 同步入库；所有已挂载到智能体的技能都会通过 `SKILL_APPEND_SYSTEM_MESSAGE` 自动注入到系统提示词。
> 8. **数据库迁移（Flyway 自动执行）**：本项目已集成 **[Flyway](https://flywaydb.org/)**，`ruoyi-admin` 启动时会按版本号顺序自动执行 `ruoyi-admin/src/main/resources/db/migration/` 下所有脚本（`V1__init_database.sql` → `V2__create_tool_table.sql` → `V3__create_skill_table.sql` → `V4__create_mcp_table.sql` → `V5__create_agent_flow_table.sql` → `V6__create_attachment_table.sql` → `V7__add_trace_id_to_chat_tables.sql` → `V8__add_web_search_to_ai_agent_tables.sql` → `V9__add_impl_type_to_agent_flow_table.sql` → `V10__update_agent_flow_message_table.sql` → `V11__add_run_python_code_builtin_tool.sql`），涵盖业务库初始化、AI 工具表（`agent_tool` / `agent_tool_mapping` / `agent_tool_invoke` / `agent_kb_invoke`）、Skill / MCP 表与字典、Agent Flow（`agent_flow` / `chat_flow_message`）、附件表（`attachment`）、聊天日志 trace_id 字段与网页搜索相关字段、Agent Flow `impl_type` 字段、`run_python_code` 内置工具，无需手动执行 SQL。
> 9. **多智能体编排（Agent Flow）**：v1.3 起新增 `agent_flow`、`chat_flow_message` 两张表，同时 `ai_agent` 表新增 `description` 列、AI 工具箱子菜单同步重命名为「工具管理 / 多智能体 / Skill管理 / MCP管理」，全部纳入 `V5__create_agent_flow_table.sql` 由 Flyway 自动迁移。
> 10. **doc-parser 文档解析服务**：独立 Python 微服务（`doc-parser/`），Docker 部署时通过 `Dockerfile-doc-parser` 构建，端口 `8143`；需在 `.env` 或 `docker-compose.yml` 中配置 `OPENAI_API_KEY` / `OPENAI_BASE_URL`（VLM 模型地址）与 `SERVER_API_KEY`（接口鉴权密钥）；后端 `application.yml` 中配置 `ai.doc-parser.url` 指向该服务。
> 11. **agentic 智能体网关**：独立 Python 微服务（`agentic/`），Docker 部署时通过 `Dockerfile-agentic` 构建，端口 `8142`；通过 gRPC 连接 `ruoyi-admin:9090` 获取模型配置，需配置 `GRPC_HOST` / `GRPC_PORT`、`TAVILY_API_KEY`（联网搜索）、`DB_URI`（PostgreSQL checkpoint 地址）、`SANDBOX_BASE_URL` / `SANDBOX_API_KEY`（代码沙箱）等环境变量。

---

## 🆕 最新更新

> ⭐ **随手 Star 是一种美德**，你们的 Star 就是我持续维护的动力！

### v1.4（网页搜索 / 聊天日志 / 附件管理 / langchain4j 修复）

1. 🆕 新增 **Tavily 网页搜索集成**：在【AI 工具箱 → 智能体管理】新增 `web_search_enabled`、`tavily_result_count`、`tavily_source_urls` 字段，支持在 RAG 管道中通过内容检索器集成 Tavily 网页搜索，实现同步网页搜索增强直接聊天提示。
2. 🆕 新增 **聊天日志管理（Trace ID）**：新增 `trace_id` 字段用于跨请求追踪聊天消息，新增【聊天日志】菜单页面，支持按 trace_id 查询聊天详情与 Flow 轮次上下文；后端新增 `ChatLogController`、`ChatLogQuery`、`ChatLogDetail`、`FlowRoundContext.traceId` 等模块。
3. 🐛 修复 **langchain4j 知识库查询与多模态输入冲突问题**：修复无法同时开启 context.retrieval（知识库查询）和传入多条 Content（如多图片）的 bug，重构 `JAiServices` 与 `JContentInjector` 组件以支持混合内容注入。
4. 🆕 新增 **聊天附件与文档解析能力**：新增 `attachment` 表与 `IAttachmentService`、`AttachmentServiceImpl`，支持聊天中上传图片 / 文档预览与描述生成；集成 doc-parser 文档解析服务（支持 PDF / XLS 等格式），完善 `DocumentParseClient` 与前端 `DocPreview.vue` / `ImagePreview.vue` 组件。
5. 🔧 优化 **多智能体 Flow 调度策略**：Agent Flow 从原选择器模式切换为有序批量顺序执行，改进 `FlowTool` 节点流转逻辑与 `FlowRoundContext` 线程安全机制。
6. 🔧 优化 **SSE 流式响应**：提取 SSE emitter 上下文到基类统一事件分发，修复 SSE 提前截断问题；前端聊天页面添加文件上传 loading 效果。
7. 🆕 新增 **数据库迁移脚本**：`V6__create_attachment_table.sql`（附件表）、`V7__add_trace_id_to_chat_tables.sql`（trace_id 字段）、`V8__add_web_search_to_ai_agent_tables.sql`（网页搜索字段）。
8. 🐳 **Docker 镜像优化**：添加 ghcr.io 镜像前缀以优化文档解析服务拉取，更新 docker-compose 配置。

### v1.3（多智能体编排 / Agent Flow）

1. 🆕 新增 **多智能体编排（Agent Flow）**：在【AI 工具箱 → 多智能体】通过 [AntV X6](https://x6.antv.antgroup.com/) 可视化画布拖拽多个智能体节点，拼接为多智能体协作链 / 树状 Flow（`agent_flow`）。后端 `FlowTreeParser` 将 X6 `flowJson` 解析为节点树，由 `AgentFlowChatServiceImpl` 按节点调度智能体并产出流式响应，支持节点独立设置 `promptTemplate` / `flowMemory` / `showReasoning`。
2. 🆕 新增 **Flow 对话与独立访问 URL**：每个 Flow 自动生成 `visit_url` 访问链接，前端可通过 `ruoyi-ui/src/views/ai/agent/aiFlowChat.vue` 与整个 Flow 对话；支持 `day_lmt_per_client`（单客户端每日访问限流）。
3. 🆕 新增 **Flow 聊天记录** `chat_flow_message` 表：记录每个 Flow 中每个智能体节点产生的消息，包含 `agent_flow_id` / `agent_id` / `agent_name` / `session_id` / `client_id`，方便排障与审计。
4. 🆕 新增 **AntV X6 可视化画布**：集成 `@antv/x6` + `@antv/x6-plugin-dnd` / `snapline` / `history`，在线拖拽、保存、加载 `flowJson`；菜单路由 `ai/agent_flow_edit/index/:id`。
5. 🆕 新增 **数据库迁移脚本** `sql/V5__create_agent_flow_table.sql`：包含 `agent_flow`、`chat_flow_message` 新表，`ai_agent.description` 列补充，以及 AI 工具箱子菜单统一重命名（Agent Tools → 工具管理、Agent Flow → 多智能体、Skill → Skill管理、MCP → MCP管理）。
6. 🆕 新增 **后端模块**：`ruoyi-ai-core` 新增 `component/flow`（`FlowTreeParser`、`AgentSpecAdaptor` 与 DTO）、`service/IAgentFlowService`、`controller/AgentFlowController`、`domain/AgentFlow` / `ChatFlowMessage`；`ruoyi-ai-chat` 新增 `AgentFlowChatServiceImpl` 与 `controller/AIFlowChatController`。
7. 🆕 新增 **`FlowTool`**：`ruoyi-ai-core/.../tool/FlowTool.java` 为 Flow 调度提供节点流转工具入口。
8. 🔧 **菜单路径统一**：AI 工具箱下子菜单统一使用中文名（多智能体 / 工具管理 / Skill管理 / MCP管理），并同步 Gitee 链接路径。

### v1.2（MCP 工具市场 & Skills 技能体系）

1. 🆕 新增 **MCP 工具市场**：在【AI 工具箱 → MCP】统一维护 `agent_mcp_service`（服务地址 / `transport` / `authToken`）与子级 `agent_mcp_tool`（名称 / 描述 / 参数 / 元数据）。支持 `sse` / `stdio` / `streamable-http` 三种传输方式（字典 `ai_mcp_transport`），底层通过 [langchain4j-mcp](https://docs.langchain4j.info/) 把远端 MCP 工具加载到 `JToolProvider`，原生智能体可直接调用。智能体侧通过 `agent_mcp_tool_mapping` 多对多绑定 MCP 工具。
2. 🆕 新增 **Skills 技能体系**：在【AI 工具箱 → Skill】维护 `agent_skill` 技能，支持 `create`（系统创建）与 `import`（从仓库 `skills/` 目录导入）两种 `ai_skill_type` 字典。智能体通过 `agent_skill_mapping` 多对多挂载技能，平台会把技能列表按 `Constants.SKILL_APPEND_SYSTEM_MESSAGE` 模板拼接到 System Prompt，提示模型通过 `activate_skill` 工具先激活技能再继续作答。
3. 🆕 新增 **GitHub Skills 同步工具**：`GitHubSkillsSyncUtil` 支持一键把仓库 `skills/` 目录（如 `skills/json/SKILL.md`）导入数据库，保持技能定义与代码同源。
4. 🆕 新增 **MCP 请求日志拦截器**：`McpRequestLoggingInterceptor` 基于 OkHttp 拦截器记录 MCP 请求 / 响应，便于排障。
5. 🆕 新增 **数据库迁移脚本** `sql/V3__create_skill_table.sql`、`sql/V4__create_mcp_table.sql`，包含全部表结构、菜单与字典数据，请在 v1.1 数据库基础上继续按顺序执行。
6. 🆕 新增 **Monaco 代码编辑器**：Agent Tools（sandbox 类型）页面集成 Monaco 代码编辑器（`ruoyi-ui/src/components/Editor/codeEditor.vue`），在线编辑 Python 函数代码更友好。
7. 🔧 修复 `langchain4j-mcp` 引入后的 jar 包版本冲突。

### v1.1（智能体工具化 & Dify 接入）

1. 🆕 新增 **Dify 类型智能体**：在【AI 工具箱 → Dify API 管理】统一维护外部 Dify 服务，通过 `agentType=dify` 即可一键纳管 Chatflow / Workflow / Agent 应用，对话与历史会话走本地 `dify_session_mapping` 映射，全链路流式输出。
2. 🆕 新增 **原生智能体调用本地 Java Tool**：智能体支持挂载 `builtin` 类型工具，后台通过反射扫描 `dev.langchain4j.agent.tool.Tool` 注解方法注册到 LangChain4j，大模型可自动调用本地 Java 业务方法（已内置 `time_tool` 时间工具示例）。
3. 🆕 新增 **调用 Python Sandbox 执行 Python Tool**：智能体支持挂载 `sandbox` 类型工具，自动把 LLM 参数按 `parameters` 元数据拼装成 `func_name(args)` 并下发到 [python-sandbox](https://github.com/zjwan461/python-sandbox) 沙箱执行 Python 代码，回传 `print(result)` 文本给 LLM。
4. 🆕 新增 **工具管理**：新增 `agent_tool`、`agent_tool_mapping` 表与对应菜单（AI 工具箱 → Agent Tools），支持「builtin / sandbox」两种工具类型，新增 `ai_tool_type` 字典，并预置 `time_tool` 内置工具。
5. 🆕 新增 **Dify API 管理**：新增 `dify_api`、`dify_session_mapping` 表与对应菜单，统一维护外部 Dify 服务接入。
6. 🆕 新增 **工具调用与召回可观测**：新增 `agent_tool_invoke`、`agent_kb_invoke` 两张记录表，聊天响应中推送 `TOOL_CALL` / `TOOL_CALLED` / `RETRIEVED` 三类 SSE 事件，前端可实时展示工具调用过程。
7. 🆕 新增 **数据库迁移脚本** `sql/V2__create_tool_table.sql`，包含全部表结构与字典数据。
8. 🆕 新增 **sandbox 部署资源**，仓库 `sandbox/` 目录提供 python-sandbox 启动配置模板与依赖说明。

### v1.0

1. 🆕 新增 **AI 模型管理**（LLM / Embedding），支持 Ollama 与 OpenAI 兼容协议，支持本地向量模型运行（[shibing624_text2vec-base-chinese](https://modelscope.cn/models/zjwan461/shibing624_text2vec-base-chinese/files)），支持在线下载本地向量模型。
2. 🆕 新增 **本地知识库** 功能，支持文档分段、手动调整分段内容。
3. 🆕 新增 **智能体管理**，支持多知识库挂载、系统提示词、用户提示词模板、记忆功能、客户端限流。
4. 🆕 新增 **AI 聊天** 功能，支持深度思考 UI 渲染，支持删除聊天历史。
5. 🆕 新增 **推送服务**，支持异步下载本地向量模型结果推送、文档分段向量化处理结果推送。
6. 🆕 新增 **向量模型维度检测** 功能。
7. 🆕 新增 **知识库重向量** 功能。

---

## 🪄 v1.2 新能力详解：MCP 工具市场 & Skills 技能体系

v1.2 在三类智能体形态（`origin` / `dify`）之上，进一步引入 **MCP（Model Context Protocol）工具市场** 与 **Skills 技能体系**，可在【AI 工具箱 → MCP】与【AI 工具箱 → Skill】独立维护，并在【智能体管理】中按需挂载到任意原生智能体。

| 能力 | 入口 | 字典 | 关联表 | 典型用途 |
| --- | --- | --- | --- | --- |
| **MCP 工具市场** | AI 工具箱 → MCP | `ai_mcp_transport`（`sse` / `stdio` / `streamable-http`） | `agent_mcp_service`、`agent_mcp_tool`、`agent_mcp_tool_mapping` | 调用远端 MCP Server 暴露的工具（如 GitHub / 数据库 / 浏览器 / 自研服务） |
| **Skills 技能体系** | AI 工具箱 → Skill | `ai_skill_type`（`create` / `import`） | `agent_skill`、`agent_skill_mapping` | 把可复用的领域提示 / 流程模板抽成技能，由 LLM 自动激活 |

### 1. Skills 技能体系

实现路径：
- 实体与映射：`ruoyi-ai-core/.../domain/AgentSkill.java`、`AgentSkillMapping.java`
- 服务：`ruoyi-ai-core/.../service/impl/AgentSkillServiceImpl.java`
- 同步工具：`ruoyi-ai-core/.../util/GitHubSkillsSyncUtil.java`
- 前端：`ruoyi-ui/src/views/ai/skill/index.vue`、`ruoyi-ui/src/api/ai/skill.js`
- 提示词模板：`ruoyi-ai-core/.../util/Constants.java` 中的 `SKILL_APPEND_SYSTEM_MESSAGE`

- 在【AI 工具箱 → Skill】选择 `create`（系统创建）或 `import`（从仓库 `skills/` 目录的 `SKILL.md` 一键导入）维护技能定义（`skill_name` / `skill_description` / `skill_content` / `skill_type`）。
- 在【智能体管理】编辑智能体时，通过 `agent_skill_mapping` 多对多绑定若干技能；保存后平台会把已绑定的技能列表按下面的模板拼接到 System Prompt。

```text
You have access to the following skills:

<skill_name_1>: <skill_description_1>
<skill_name_2>: <skill_description_2>
...

When the user's request relates to one of these skills, activate it first
using the `activate_skill` tool before proceeding.
```

- 模型在匹配场景下会先调用 `activate_skill` 工具激活技能，再继续作答，从而把领域提示 / SOP 沉淀为可复用资产。

> **目录约定**：仓库 `skills/` 目录下放置 `SKILL.md`（以及自定义目录如 `skills/json/SKILL.md`）作为技能的源代码版本，`import` 类型技能会同步这些文件内容到数据库。

### 2. MCP 工具市场

实现路径：
- 实体与映射：`ruoyi-ai-core/.../domain/AgentMcpService.java`、`AgentMcpTool.java`、`AgentMcpToolMapping.java`
- 服务：`ruoyi-ai-core/.../service/impl/AgentMcpServiceImpl.java`
- 控制层：`ruoyi-ai-core/.../controller/AgentMcpServiceController.java`
- 工具加载：`ruoyi-ai-core/.../tool/JToolProvider.java`
- MCP 组件：`ruoyi-ai-core/.../component/mcp/JHttpMcpTransport.java`、`McpRequestLoggingInterceptor.java`
- 前端：`ruoyi-ui/src/views/ai/mcp/index.vue`、`ruoyi-ui/src/api/ai/mcp.js`

- 在【AI 工具箱 → MCP】新建 MCP 服务：填写 `mcp_service_name`、`mcp_url`、`mcp_transport`（`sse` / `stdio` / `streamable-http`）、可选的 `auth_token`。
- 通过“同步工具”能力从远端 MCP Server 拉取工具列表，落地到 `agent_mcp_tool`（包含 `mcp_tool_name` / `mcp_tool_description` / `mcp_tool_parameters` / `mcp_tool_metadata`）。
- 在【智能体管理】编辑智能体时，通过 `agent_mcp_tool_mapping` 多对多绑定 MCP 工具；保存后 `JToolProvider` 会按 `transport` 选择合适的客户端：
  - `sse` / `streamable-http`：通过自定义 `JHttpMcpTransport` + OkHttp 调用 MCP HTTP 接口，并启用 `McpRequestLoggingInterceptor` 打印请求 / 响应日志。
  - `stdio`：直接 fork 外部 MCP 进程，通过标准 I/O 与 MCP Server 通信（要求宿主环境可拉起进程）。
- 加载完成后的 MCP 工具会自动注册为 LangChain4j 的 `ToolSpecification`，原生智能体（`agentType=origin`）即可在 LLM 推理过程中按需调用。

> **依赖说明**：v1.2 起引入 `langchain4j-mcp` 依赖并升级 Spring Boot 至 `2.7.18`，如遇到 jar 包冲突请确认 `langchain4j-*` 系列版本统一。

### 3. 原生智能体调用本地 Java Tool（沿用 v1.1）

实现路径：`ruoyi-ai-core/.../tool/JToolProvider.java`

- 在【Agent Tools】新建工具，`toolType=builtin`，`clazz` 填写含 `@dev.langchain4j.agent.tool.Tool` 注解的 Spring Bean 全类名。
- 平台在 `JToolProvider.provideTools` 中反射加载类、扫描所有 `@Tool` 注解方法，使用 LangChain4j 的 `ToolSpecifications.toolSpecificationFrom(method)` 生成工具规范并注册。
- LLM 在推理过程中会自动调用匹配的工具，平台记录每次调用到 `agent_tool_invoke` 表（`messageId` 关联）。

```java
// 示例：com.ruoyi.ai.tool.TimeTool（已内置）
public class TimeTool {
    @Tool("获取当前时间，格式 yyyy-MM-dd HH:mm:ss")
    public String currentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
```

### 4. 原生智能体调用 Python Sandbox 执行 Python Tool（沿用 v1.1）

实现路径：`ruoyi-ai-core/.../tool/SandboxToolExecutor.java`

- 在【Agent Tools】新建工具，`toolType=sandbox`，`code` 填写 Python 函数定义，`parameters` 填写参数定义 JSON（如 `[{"name":"x","type":"int"}]`），v1.2 起页面集成 Monaco 代码编辑器。
- `SandboxToolExecutor` 会按 LLM 实际传入的参数，把函数调用拼装成 `func_name(arg1=value1, arg2='value2')` 并加上 `print(result)` 发送到 python-sandbox。
- python-sandbox 执行后回传 `print` 输出（即 `result` 的字符串表示），作为工具调用结果注入到 LLM 上下文中。

```python
# 示例：calc_circle_area.py（code 字段）
import math
def calc_circle_area(radius):
    return math.pi * radius * radius
```

> **部署提示**：先按 [python-sandbox](https://github.com/zjwan461/python-sandbox) 官方文档启动沙箱服务，后端 `application.yml` 中配置 `ai.sandbox.url-prefix` 与 `ai.sandbox.api-key`。

### 5. Dify 类型智能体（沿用 v1.1）

实现路径：`ruoyi-ai-dify` 模块 + `ruoyi-ai-chat/.../controller/DifyChatController.java`

- 在【AI 工具箱 → Dify API 管理】维护外部 Dify 服务的 `urlPrefix` 与 `apiKey`，一个智能体绑定一个 Dify 应用。
- 后端将本地 `sessionId` 通过 `dify_session_mapping` 映射到 Dify 的 `conversation_id`，并通过 [SSE 转发 Dify 流式响应](ruoyi-ai/ruoyi-ai-chat/src/main/java/com/ruoyi/ai/controller/DifyChatController.java:80)。
- 支持会话列表、历史消息的本地化分页查询，前端开箱即用。

---

## 🐍 Python 微服务详解：doc-parser & agentic

v1.4 引入两个独立的 Python 微服务，通过 Docker Compose 统一编排部署，与 Java 后端协同工作。

### 1. doc-parser 文档解析服务

**目录结构**：`doc-parser/`

**核心能力**：
- 基于 [Docling](https://github.com/DS4SD/docling) + 视觉语言模型（VLM）的文档解析引擎
- 支持 **60+ 种文件格式**统一转为 Markdown：
  - **Office 文档**：PDF / DOCX / XLSX / PPTX / DOC / XLS
  - **网页 / 标记语言**：HTML / XHTML / Markdown / CSV
  - **代码文件**：Python / Java / JavaScript / TypeScript / Go / Rust / C++ / SQL 等 50+ 种编程语言
  - **纯文本**：TXT / JSON / LOG / INI / CONF 等
- 内置 **OCR 能力**（基于 RapidOCR）与**表格识别**
- 支持**图片描述生成**（通过 VLM 模型）
- 提供 `PUT /process` REST API，支持 API Key 鉴权

**技术栈**：
- FastAPI + Uvicorn
- Docling（文档解析核心引擎）
- RapidOCR（OCR 识别）
- OpenAI 兼容 API（VLM 模型调用）
- PyMuPDF / Pillow / unstructured / msoffcrypto-tool

**部署配置**：
```bash
# doc-parser/.env.example
OPENAI_API_KEY=sk-xxxxxx              # VLM 模型 API Key
OPENAI_BASE_URL=https://dashscope-intl.aliyuncs.com/compatible-mode/v1
OCR_MODEL=qwen3.7-plus                # VLM 模型名称
OCR_MAX_CONCURRENT=10                 # OCR 最大并发数
SERVER_API_KEY=ocr-xxxxx              # 接口鉴权密钥
DESCRIBE_DOC_IMG=false                # 是否启用图片描述生成
```

**Docker 部署**：
- 使用 `Dockerfile-doc-parser` 构建（端口 `8143`）
- 内置 LibreOffice 用于 Office 文档转换
- 构建时自动下载 PDF 解析模型（支持代理配置）

**后端集成**：
- Java 后端通过 `DocumentParseClient` 调用 doc-parser 服务
- 用于聊天附件的文档预览与解析（`IAttachmentService`）

---

### 2. agentic 智能体网关

**目录结构**：`agentic/`

**核心能力**：
- 基于 [LangChain](https://www.langchain.com/) + [DeepAgents](https://github.com/langchain-ai/deepagents) + [LangGraph](https://github.com/langchain-ai/langgraph) 的智能体执行引擎
- 通过 **gRPC** 与 Java 后端（`ruoyi-admin:9090`）通信，获取模型配置与工具定义
- 接收前端 Agent Flow 的 `flow_json`，解析为 `TreeNode` 结构，构建**嵌套 Deep Agent** 执行多智能体协作
- 支持**流式对话**（SSE）与**联网搜索**（Tavily）
- 支持**图片上传**（多模态输入）与**代码沙箱执行**（Python Sandbox）
- 支持 **Skill 技能调用**：agentic 服务通过 gRPC 从 Java 后端获取智能体绑定的 Skill 列表，自动将本地 `skills/` 目录下的技能文件上传到 Sandbox，Deep Agent 在对话过程中可按需激活并调用技能
- 使用 **PostgreSQL checkpoint** 持久化对话状态（`langgraph-checkpoint-postgres`）

**技术栈**：
- FastAPI + Uvicorn
- LangChain + LangGraph + DeepAgents
- gRPC（与 Java 后端通信）
- Tavily（联网搜索）
- PostgreSQL（checkpoint 持久化）
- [Python Sandbox](https://github.com/zjwan461/python-sandbox)（代码执行）
- Skills 技能目录（自动上传至 Sandbox 供 Agent 调用）

**部署配置**：
```bash
# agentic/.env.example
GRPC_HOST=ruoyi-admin                 # Java 后端 gRPC 地址
GRPC_PORT=9090                        # Java 后端 gRPC 端口
TAVILY_API_KEY=tvly-xxxxx             # Tavily 联网搜索 API Key
TOKEN_WHITELIST=admin_token,dev_token # 接口鉴权 Token 白名单
SANDBOX_BASE_URL=http://sandbox:8194  # Python Sandbox 地址
SANDBOX_API_KEY=python-sandbox        # Python Sandbox API Key
DB_URI=postgresql://root:root@postgres:5432/embedding?sslmode=disable
```

**Docker 部署**：
- 使用 `Dockerfile-agentic` 构建（端口 `8142`）
- 构建时自动执行 `gen_grpc.sh` 生成 gRPC 客户端代码

**后端集成**：
- Java 后端通过 `AgenticServiceImpl` 调用 agentic 服务
- 用于 Agent Flow（多智能体编排）的流式对话执行

---

### 3. 微服务架构图

```
┌─────────────┐
│   前端 UI   │
│  (Vue 2.x)  │
└──────┬──────┘
       │ HTTP/SSE
       ▼
┌─────────────────────────────────────────────┐
│           Java 后端 (ruoyi-admin)           │
│  ┌──────────────────────────────────────┐  │
│  │  Spring Boot + LangChain4j + gRPC    │  │
│  │  - AI 模型管理 / 知识库 / 智能体     │  │
│  │  - Agent Flow 编排 / MCP / Skills    │  │
│  └──────────────┬───────────────────────┘  │
│                 │ gRPC (9090)              │
└─────────────────┼───────────────────────────┘
                  │
       ┌──────────┴──────────┐
       ▼                     ▼
┌──────────────┐      ┌──────────────┐
│  doc-parser  │      │   agentic    │
│  (Python)    │      │  (Python)    │
│  Port: 8143  │      │  Port: 8142  │
│              │      │              │
│ - 文档解析   │      │ - 智能体执行 │
│ - OCR 识别   │      │ - 多 Agent   │
│ - VLM 描述   │      │ - 联网搜索   │
└──────────────┘      └──────────────┘
       │                     │
       ▼                     ▼
┌──────────────┐      ┌──────────────┐
│  VLM 模型    │      │  PostgreSQL  │
│  (OpenAI API)│      │  (checkpoint)│
└──────────────┘      └──────────────┘
```

---

## 🚀 快速开始

### 一、基础环境

| 组件 | 版本要求 | 说明 |
| --- | --- | --- |
| JDK | 1.8+ | 后端运行环境 |
| Maven | 3.6+ | 项目构建 |
| Node.js | 14+ | 前端构建 |
| MySQL | 5.7+ / 8.0 | 业务数据库 |
| Redis | 5.0+ | 缓存 |
| PostgreSQL + PgVector | 14+ | 向量库（RAG 必选） |
| Dify（可选） | 0.6+ | 仅在使用 Dify 类型智能体时需要 |
| [python-sandbox](https://github.com/zjwan461/python-sandbox)（可选） | latest | 自研 Python 代码沙箱，仅在使用 `sandbox` 类型 Python 工具或 agentic 服务时需要 |
| MCP Server（可选） | — | 仅在使用 MCP 类型工具时需要，支持 `sse` / `stdio` / `streamable-http` |
| Python（可选） | 3.11+ | 仅在使用 doc-parser / agentic 微服务时需要（Docker 部署已内置） |

> 基础环境的安装与配置请参考 RuoYi 官方文档：<https://doc.ruoyi.vip/ruoyi-vue/>

### 二、Docker 容器化部署（推荐）

`docker-compose.yml` 中已为 `ruoyi-admin` 与 `ruoyi-ui` 服务分别指定了 `Dockerfile-admin` 与 `Dockerfile-ui`，**只需一条命令即可同时拉起中间件（MySQL / Redis / PgVector 等）并自动构建前后端镜像**：

```bash
docker compose -f docker-compose.yml up -d --build
```

> - 无需手动执行 `build.sh` 或 `bin/` 下的脚本；`--build` 会让 Compose 按 Dockerfile 自动构建前后端镜像。
> - 后端容器启动后，**Flyway 会自动执行** `ruoyi-admin/src/main/resources/db/migration/` 下全部 `V1` ~ `V11` 脚本完成建表与字典初始化。
> - `doc-parser` 与 `agentic` 两个 Python 微服务也会随 `docker compose` 一并构建并拉起（分别使用 `Dockerfile-doc-parser` 与 `Dockerfile-agentic`）。
> - Windows PowerShell / CMD 用户可直接使用 `docker compose -f docker-compose.yml up -d --build`。

### 三、本地开发模式

项目已集成 **[Flyway](https://flywaydb.org/)**，启动后端时 `ruoyi-admin` 会自动扫描并按版本号顺序执行 `ruoyi-admin/src/main/resources/db/migration/` 下所有迁移脚本（`V1__init_database.sql` → `V2__create_tool_table.sql` → `V3__create_skill_table.sql` → `V4__create_mcp_table.sql` → `V5__create_agent_flow_table.sql` → `V6__create_attachment_table.sql` → `V7__add_trace_id_to_chat_tables.sql` → `V8__add_web_search_to_ai_agent_tables.sql` → `V9__add_impl_type_to_agent_flow_table.sql` → `V10__update_agent_flow_message_table.sql` → `V11__add_run_python_code_builtin_tool.sql`），**无需手动执行任何 SQL**。首次启动前只需在 MySQL 中手动创建一个空库（库名与 `application-druid.yml` / `application.yml` 中的 `url` 保持一致），Flyway 会自动建表与初始化业务数据。

> 如需本地开发 doc-parser / agentic 微服务，可参考各目录下 `.env.example` 配置环境变量后通过 `uvicorn main:app --reload` 启动。

```bash
# （可选）首次启动前在 MySQL 中手动创建空库
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS `ry-vue` DEFAULT CHARACTER SET utf8mb4;"

# 启动后端（Flyway 会自动执行 db/migration 下所有 V1~V8 脚本）
mvn spring-boot:run -pl ruoyi-admin
cd ruoyi-ui
npm install
npm run dev
```

---

## 🧱 内置功能

| # | 模块 | 说明 |
| --- | --- | --- |
| 1 | 用户管理 | 系统操作者管理 |
| 2 | 部门管理 | 树形组织机构，支持数据权限 |
| 3 | 岗位管理 | 用户所属职务配置 |
| 4 | 菜单管理 | 菜单、操作权限、按钮权限 |
| 5 | 角色管理 | 角色菜单与数据范围权限 |
| 6 | 字典管理 | 固定数据维护 |
| 7 | 参数管理 | 系统动态参数配置 |
| 8 | 通知公告 | 公告信息发布维护 |
| 9 | 操作日志 | 正常 / 异常操作日志 |
| 10 | 登录日志 | 登录与登录异常日志 |
| 11 | 在线用户 | 当前活跃用户监控 |
| 12 | 定时任务 | 在线任务调度与执行日志 |
| 13 | 代码生成 | 一键生成前后端 CRUD 代码 |
| 14 | 系统接口 | 自动生成 API 文档（Swagger） |
| 15 | 服务监控 | CPU / 内存 / 磁盘 / 堆栈监控 |
| 16 | 在线构建器 | 拖拽表单生成 HTML |
| 17 | 连接池监视 | Druid 连接池状态与 SQL 分析 |
| 18 | 🤖 **AI 工具箱** | **统一管理 AI 模型 / 知识库 / 智能体 / Dify API / Agent Tools** |

AI 工具箱菜单下包含：

- **模型管理**：LLM / Embedding 模型维护（Ollama / OpenAI 协议）。
- **知识库管理**：文档分段、向量化、人工分段调整。
- **智能体管理**：多类型智能体（`origin` / `dify`），支持系统提示词、记忆、知识库挂载、**MCP 工具挂载**、**Skill 技能挂载**、Agent Tools 挂载、客户端限流。
- **多智能体**（v1.3 新增）：基于 [AntV X6](https://x6.antv.antgroup.com/) 可视化画布拼接多个智能体节点为 Flow，生成独立 `visit_url` 并提供专属对话页面（`aiFlowChat.vue`），支持 Flow 内记忆共享 / 提示词模板 / 深度思考 / 单客户端日访问限流。
- **工具管理**（原 Agent Tools）：管理 `builtin`（Java Tool）与 `sandbox`（Python Tool）两种工具类型，`sandbox` 工具页面集成 Monaco 代码编辑器，可直接挂载到任意智能体上。
- **Skill管理**（v1.2 新增）：维护 `agent_skill` 技能，支持 `create` / `import` 两种类型，支持从仓库 `skills/` 目录的 `SKILL.md` 一键导入，技能列表会拼接到 System Prompt 并通过 `activate_skill` 工具激活。
- **MCP管理**（v1.2 新增）：维护 `agent_mcp_service` 与子级 `agent_mcp_tool`，支持 `sse` / `stdio` / `streamable-http` 三种传输方式，自动同步远端 MCP 工具列表，可挂载到任意原生智能体。
- **Dify API 管理**：统一维护外部 Dify 服务接入（URL 前缀 + API Key）。
- **AI 聊天**：流式对话、深度思考 UI 渲染、历史会话管理。
- **Flow 对话**：v1.3 起为每个 Agent Flow 生成的独立对话页面，记录 `chat_flow_message`，可用于交付独立的多智能体应用链接。
- **doc-parser 文档解析**（v1.4 新增）：独立 Python 微服务，基于 Docling + VLM 将 PDF / Office / 代码等 60+ 格式文件转为 Markdown，支持 OCR、表格识别、图片描述生成，后端通过 HTTP 调用该服务实现聊天附件的文档预览与解析。
- **agentic 智能体网关**（v1.4 新增）：独立 Python 微服务，基于 LangChain + DeepAgents + LangGraph，通过 gRPC 与 Java 后端通信，接收 Agent Flow 的 `flow_json` 构建嵌套 Deep Agent 执行多智能体协作流式对话，支持联网搜索（Tavily）、图片上传、Sandbox 代码执行、**Skill 技能调用**、PostgreSQL checkpoint 持久化。

---

## 🌐 在线体验

- 默认账号：`admin / admin123`
- 演示地址：<http://204.141.218.130>
- 文档地址：详见 [重要提示](#-重要提示) 章节

> 演示服务器收到的打赏已用于升级，感谢各位小伙伴的支持 ❤️

---

## 🖼 演示图

<table>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913005255.png" alt="演示图-1" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913005626.png" alt="演示图-2" width="1700"/></td>
    </tr>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913005758.png" alt="演示图-3" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913005843.png" alt="演示图-4" width="1700"/></td>
    </tr>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913005928.png" alt="演示图-5" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010015.png" alt="演示图-6" width="1700"/></td>
    </tr>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010059.png" alt="演示图-7" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010129.png" alt="演示图-8" width="1700"/></td>
    </tr>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010242.png" alt="演示图-9" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010305.png" alt="演示图-10" width="1700"/></td>
    </tr>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20260704001023.png" alt="演示图-11" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20260704001058.png" alt="演示图-12" width="1700"/></td>
    </tr>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20260704001134.png" alt="演示图-13" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20260704000922.png" alt="演示图-14" width="1700"/></td>
    </tr>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20260712234728.png" alt="演示图-15" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20260712234912.png" alt="演示图-16" width="1700"/></td>
    </tr>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20260725001320.png" alt="演示图-17" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20260725001539.png" alt="演示图-18" width="1700"/></td>
    </tr>
</table>

---

## ❤️ 支持项目

RuoYi-Langchain4j 的源码全部开源免费，如果你觉得这个项目帮助到了你，欢迎通过以下方式支持：

- 🌹 向您的朋友推荐或分享
- ⭐ 在项目页面右上角点一个 Star
- ☕️ 通过下方二维码捐赠，请作者喝杯咖啡

<div align="center">
  <img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/%E6%94%AF%E4%BB%98%E5%AE%9D%E6%94%B6%E6%AC%BE%E7%A0%81.jpg" alt="支付宝收款码" width="280" />
  &nbsp;&nbsp;&nbsp;
  <img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/%E5%BE%AE%E4%BF%A1%E6%94%B6%E6%AC%BE%E7%A0%81.jpg" alt="微信收款码" width="280" />
</div>

---

## 📚 参考资料

- 若依框架：<http://www.ruoyi.vip>
- RuoYi-Vue 文档：<https://doc.ruoyi.vip/ruoyi-vue/>
- LangChain4j 中文文档：<https://docs.langchain4j.info/category/教程>
- LangChain4j MCP 模块：<https://docs.langchain4j.info/category/tools/mcp>
- Ollama 官网：<https://ollama.com/>
- Ollama 本地部署教程：<https://developer.aliyun.com/article/1656872>
- Dify 官方仓库：<https://github.com/langgenius/dify>
- python-sandbox 自研代码沙箱：<https://github.com/zjwan461/python-sandbox>
- Model Context Protocol 官网：<https://modelcontextprotocol.io/>
- MCP Java SDK：<https://github.com/modelcontextprotocol/java-sdk>
- MCP Server 示例合集：<https://github.com/modelcontextprotocol/servers>
- Anthropic Skills 介绍：<https://docs.anthropic.com/en/docs/agents-and-tools/agent-skills/overview>
- AntV X6 图编辑引擎：<https://x6.antv.antgroup.com/>
- AntV X6 Dnd 插件：<https://x6.antv.antgroup.com/api/plugins/dnd>
- AntV X6 History 插件：<https://x6.antv.antgroup.com/api/plugins/history>

---

## 📄 开源协议

本项目基于 [MIT License](./LICENSE) 开源。
