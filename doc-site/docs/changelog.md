# 更新日志

本项目的所有重要变更都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
并且本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [Unreleased]

### 计划添加
- 多 Agent 协作优化
- 更丰富的工具生态
- 性能监控与告警
- 分布式部署支持
- 更多模型 Provider 接入

---

## [1.4.0] - 2024-01-15

### 新增
- 🆕 **Tavily 网页搜索集成**：在【AI 工具箱 → 智能体管理】新增 `web_search_enabled`、`tavily_result_count`、`tavily_source_urls` 字段，支持在 RAG 管道中通过内容检索器集成 Tavily 网页搜索，实现同步网页搜索增强直接聊天提示。
- 🆕 **聊天日志管理（Trace ID）**：新增 `trace_id` 字段用于跨请求追踪聊天消息，新增【聊天日志】菜单页面，支持按 trace_id 查询聊天详情与 Flow 轮次上下文；后端新增 `ChatLogController`、`ChatLogQuery`、`ChatLogDetail`、`FlowRoundContext.traceId` 等模块。
- 🆕 **聊天附件与文档解析能力**：新增 `attachment` 表与 `IAttachmentService`、`AttachmentServiceImpl`，支持聊天中上传图片 / 文档预览与描述生成；集成 doc-parser 文档解析服务（支持 PDF / XLS 等格式），完善 `DocumentParseClient` 与前端 `DocPreview.vue` / `ImagePreview.vue` 组件。
- 🆕 **数据库迁移脚本**：`V6__create_attachment_table.sql`（附件表）、`V7__add_trace_id_to_chat_tables.sql`（trace_id 字段）、`V8__add_web_search_to_ai_agent_tables.sql`（网页搜索字段）。

### 优化
- 🔧 **多智能体 Flow 调度策略**：Agent Flow 从原选择器模式切换为有序批量顺序执行，改进 `FlowTool` 节点流转逻辑与 `FlowRoundContext` 线程安全机制。
- 🔧 **SSE 流式响应**：提取 SSE emitter 上下文到基类统一事件分发，修复 SSE 提前截断问题；前端聊天页面添加文件上传 loading 效果。
- 🐳 **Docker 镜像优化**：添加 ghcr.io 镜像前缀以优化文档解析服务拉取，更新 docker-compose 配置。

### 修复
- 🐛 **langchain4j 知识库查询与多模态输入冲突问题**：修复无法同时开启 context.retrieval（知识库查询）和传入多条 Content（如多图片）的 bug，重构 `JAiServices` 与 `JContentInjector` 组件以支持混合内容注入。

---

## [1.3.0] - 2023-12-20

### 新增
- 🕸 **多智能体编排 / Agent Flow**：在【AI 工具箱 → 多智能体】通过 **[AntV X6](https://x6.antv.antgroup.com/)** 可视化画布拖拽多个智能体节点，拼接为多智能体协作链/树状 Flow（`agent_flow`）；后端 `FlowTreeParser` 把 X6 `flowJson` 解析为节点树，按节点切换执行智能体并通过 `AgentFlowChatServiceImpl` 产出流式响应，节点间可独立设置 `promptTemplate` / `flowMemory` / `showReasoning`。每个 Flow 生成独立 `visit_url`，支持单客户端每日访问限流（`day_lmt_per_client`）。
- 🆕 **数据库迁移脚本**：`V5__create_agent_flow_table.sql`（Agent Flow 表）。

### 优化
- 🔧 **AI 工具箱菜单重命名**：原「工具管理」菜单重命名为「工具管理 / 多智能体 / Skill管理 / MCP管理」四个子菜单，更加清晰。
- 🔧 **Flow 节点配置**：每个 Flow 节点支持独立配置 `promptTemplate`、`flowMemory`、`showReasoning`。

---

## [1.2.0] - 2023-11-15

### 新增
- 🔌 **MCP 工具市场**：原生智能体支持挂载来自 [Model Context Protocol](https://modelcontextprotocol.io/) Server 的工具。在【AI 工具箱 → MCP】维护 `agent_mcp_service`（`url` + `transport` + `authToken`）与子级 `agent_mcp_tool`（`name` / `description` / `parameters` / `metadata`），支持 `sse` / `stdio` / `streamable-http` 三种传输方式；`JToolProvider` 通过 langchain4j-mcp 加载后注册到智能体，LLM 可直接调用远端 MCP 工具。智能体侧通过 `agent_mcp_tool_mapping` 多对多绑定 MCP 工具，已自带 OkHttp 请求日志拦截器（`McpRequestLoggingInterceptor`）便于排障。
- 📜 **Skills 技能体系**：在【AI 工具箱 → Skill】维护 `agent_skill` 技能（`skill_name` / `skill_description` / `skill_content` / `skill_type`），支持 `create`（系统创建）和 `import`（从仓库 `skills/` 目录导入）两种类型；智能体通过 `agent_skill_mapping` 多对多挂载技能，平台会把技能信息按 `Constants.SKILL_APPEND_SYSTEM_MESSAGE` 模板追加到 System Prompt，提示模型通过 `activate_skill` 工具在匹配场景下先激活技能再继续作答。
- 🆕 **Monaco 代码编辑器**：`ruoyi-ui/src/components/Editor/codeEditor.vue`，在线编辑 Python 函数代码更友好。
- 🆕 **数据库迁移脚本**：`V3__create_skill_table.sql`（Skill 表）、`V4__create_mcp_table.sql`（MCP 表）。

### 优化
- 🔧 **工具调用可观测**：自动记录 `agent_tool_invoke`（工具调用结果）与 `agent_kb_invoke`（知识库召回分段），与消息 ID 关联，方便排障与审计；前端可通过 `TOOL_CALL` / `TOOL_CALLED` / `RETRIEVED` 三类 SSE 事件实时观察调用过程。

---

## [1.1.0] - 2023-10-20

### 新增
- 🪄 **Dify 类型智能体**：通过 [Dify](https://dify.ai/) 平台统一纳管 Chatflow / Workflow / Agent 等 Dify 应用，对话与会话映射全部走本地 `dify_session_mapping`，流式输出 + 历史会话一键拉取，纳管入口在【AI 工具箱 → Dify API 管理】。
- ☕️ **原生智能体调用本地 Java Tool**：原生（LangChain4j）智能体支持挂载 `builtin` 类型的 Java 工具（基于 `dev.langchain4j.agent.tool.Tool` 注解），在 `JToolProvider` 中通过反射扫描 `@Tool` 方法注册到 LangChain4j，实现 LLM ↔ 本地业务方法的工具调用。
- 🐍 **原生智能体调用 Python Sandbox 执行 Python Tool**：原生智能体支持挂载 `sandbox` 类型的 Python 工具，`SandboxToolExecutor` 会自动将 LLM 传入的参数按 `parameters` 元数据拼装成 Python 函数调用，远程下发 [python-sandbox](https://github.com/zjwan461/python-sandbox) 沙箱执行并回传 `print(result)` 结果。
- 🆕 **数据库迁移脚本**：`V2__create_tool_table.sql`（工具表）。

### 优化
- 🔧 **智能体类型扩展**：从单一类型扩展为三种类型（原生、Dify、Flow），统一在 `ai_agent` 表中通过 `agent_type` 字段区分。

---

## [1.0.0] - 2023-09-15

### 新增
- 🎉 **项目初始化**：基于 RuoYi-Vue 框架，集成 LangChain4j AI 框架。
- 🤖 **AI 模型管理**：统一管理大语言模型（LLM）与文本向量模型（Embedding），支持 Ollama 与 OpenAI 兼容协议，支持在线下载/加载本地向量模型。
- 📚 **本地知识库**：支持文档自动分段、人工调整分段、向量化与重向量化。
- 🧠 **多类型智能体（Agent）**：内置原生 LangChain4j Agent 智能体。
- 💬 **AI 对话**：支持流式输出、深度思考（Chain-of-Thought）UI 渲染、历史会话管理。
- 🔔 **异步推送**：基于 WebSocket 推送向量下载、文档分段处理等耗时任务结果。
- 🛠️ **RuoYi 全量能力**：用户/角色/菜单/字典/定时任务/代码生成/服务监控等开箱即用。
- 🔐 **JWT 多端鉴权**：动态权限菜单 + 数据权限 + 按钮级别控制。
- 🆕 **数据库迁移脚本**：`V1__init_database.sql`（初始化数据库）。

---

## 版本说明

### 版本号规则

本项目遵循语义化版本规范：`MAJOR.MINOR.PATCH`

- **MAJOR**：不兼容的 API 变更
- **MINOR**：向下兼容的功能新增
- **PATCH**：向下兼容的问题修正

### 更新类型标识

- 🎉 **新增**：新功能或新模块
- 🆕 **添加**：新增的配置、脚本等
- 🔧 **优化**：性能优化、代码改进
- 🐛 **修复**：问题修复
- 🐳 **Docker**：Docker 相关变更
- 📚 **文档**：文档更新

---

## 迁移指南

### 从 1.3.x 升级到 1.4.x

1. **数据库迁移**：
   - 自动执行 `V6__create_attachment_table.sql`
   - 自动执行 `V7__add_trace_id_to_chat_tables.sql`
   - 自动执行 `V8__add_web_search_to_ai_agent_tables.sql`

2. **配置更新**：
   - 添加 `ai.doc-parser.*` 配置
   - 添加 Tavily API Key（可选）

3. **服务部署**：
   - 部署 doc-parser 服务
   - 更新 agentic 服务

### 从 1.2.x 升级到 1.3.x

1. **数据库迁移**：
   - 自动执行 `V5__create_agent_flow_table.sql`

2. **菜单更新**：
   - AI 工具箱菜单重命名

### 从 1.1.x 升级到 1.2.x

1. **数据库迁移**：
   - 自动执行 `V3__create_skill_table.sql`
   - 自动执行 `V4__create_mcp_table.sql`

2. **配置更新**：
   - 添加 MCP Server 配置
   - 添加 Skills 配置

---

## 贡献指南

欢迎提交 Issue 和 Pull Request！

### 提交规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

**type 类型**：
- `feat`：新功能
- `fix`：修复 bug
- `docs`：文档变更
- `style`：代码格式（不影响代码运行的变动）
- `refactor`：重构（既不是新增功能，也不是修改 bug 的代码变动）
- `perf`：性能优化
- `test`：增加测试
- `chore`：构建过程或辅助工具的变动

---

[Unreleased]: https://github.com/zjwan461/ruoyi-langchain4j/compare/v1.4.0...HEAD
[1.4.0]: https://github.com/zjwan461/ruoyi-langchain4j/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/zjwan461/ruoyi-langchain4j/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/zjwan461/ruoyi-langchain4j/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/zjwan461/ruoyi-langchain4j/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/zjwan461/ruoyi-langchain4j/releases/tag/v1.0.0
