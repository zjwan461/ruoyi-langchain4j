<div align="center">

# RuoYi-Langchain4j

**基于 RuoYi-Vue 集成 LangChain4j 的企业级 AI 快速开发平台**

[![RuoYi-Vue](https://img.shields.io/badge/Base-RuoYi--Vue-blue)](https://github.com/yangzongzhuan/RuoYi-Vue)
[![LangChain4j](https://img.shields.io/badge/AI-LangChain4j-green)](https://docs.langchain4j.info/)
[![Spring Boot](https://img.shields.io/badge/SpringBoot-2.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-2.x-success)](https://vuejs.org/)
[![License](https://img.shields.io/badge/license-MIT-orange)](./LICENSE)

> 前端 Vue / Element UI · 后端 Spring Boot / Spring Security / Redis & Jwt / WebSocket / LangChain4j / PgVector / Dify Sandbox

[快速开始](#-快速开始) · [在线体验](#-在线体验) · [演示图](#-演示图) · [支持项目](#-支持项目)

</div>

---

## 📖 项目简介

RuoYi-Langchain4j 是在经典后台管理框架 **[RuoYi-Vue](https://github.com/yangzongzhuan/RuoYi-Vue)** 之上，深度集成 **[LangChain4j](https://docs.langchain4j.info/)**、向量数据库 **PgVector** 与 **[Dify](https://dify.ai/)** 生态的扩展版本。
项目在保留 RuoYi 原有权限、监控、定时任务、代码生成等全部能力的同时，新增了 **AI 模型管理 / 本地知识库 / 多类型智能体 / AI 对话** 等一站式 AI 能力，致力于帮助开发者以最低成本快速构建企业级 AI 应用。

v1.1 起，平台内置 **Dify 类型智能体**、**原生智能体调用本地 Java Tool**、**调用 Dify Sandbox 执行 Python Tool** 三种智能体形态，覆盖 Chatflow / Workflow / 本地 Agent / 沙箱代码执行等多种落地场景。

### 🧩 技术栈

| 类别 | 技术 |
| --- | --- |
| 前端 | Vue、Element UI、Axios、Vue Router、Vuex |
| 后端 | Spring Boot、Spring Security、MyBatis-Plus、Redis、Jwt、WebSocket |
| AI 框架 | [LangChain4j](https://docs.langchain4j.info/)（兼容 Ollama / OpenAI 协议） |
| Dify 生态 | [Dify](https://dify.ai/) 应用接入 + [dify-sandbox](https://github.com/langgenius/dify-sandbox) 代码执行沙箱 |
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
- 🐍 **原生智能体调用 Dify Sandbox 执行 Python Tool**：原生智能体支持挂载 `sandbox` 类型的 Python 工具，`SandboxToolExecutor` 会自动将 LLM 传入的参数按 `parameters` 元数据拼装成 Python 函数调用，远程下发 [dify-sandbox](https://github.com/langgenius/dify-sandbox) 沙箱执行并回传 `print(result)` 结果。
- 📊 **工具调用与召回可观测**：自动记录 `agent_tool_invoke`（工具调用结果）与 `agent_kb_invoke`（知识库召回分段），与消息 ID 关联，方便排障与审计；前端可通过 `TOOL_CALL` / `TOOL_CALLED` / `RETRIEVED` 三类 SSE 事件实时观察调用过程。

---

## ⚠️ 重要提示

> 1. **基础环境部署**与 RuoYi-Vue 一致，详见官方文档：<https://doc.ruoyi.vip/ruoyi-vue/>
> 2. **RAG 功能依赖 PgVector**，本地推荐使用 Docker 部署（项目已附带 `docker-compose-middleware.yml`）。
> 3. **AI 对话需提前在【AI 工具箱 → 模型管理】中配置可用的大语言模型与向量模型。**
> 4. **Dify 类型智能体**需提前在【AI 工具箱 → Dify API 管理】中配置外部 Dify 服务的 URL 前缀与 API Key；本地推荐使用 Docker Compose 部署 [Dify](https://github.com/langgenius/dify)。
> 5. **Python 工具（Sandbox 类型）**依赖外部 [dify-sandbox](https://github.com/langgenius/dify-sandbox) 服务，部署完成后请在 `application.yml` 中配置 `ai.sandbox.*` 相关地址与 API Key；本仓库 `sandbox/conf/config.yaml` 已附 dify-sandbox 启动配置模板。
> 6. **数据库变更**：v1.1 起新增 `agent_tool`、`agent_tool_mapping`、`agent_tool_invoke`、`agent_kb_invoke` 等表，并补充 `ai_agent_type`、`ai_tool_type` 字典数据，请在已有环境上按顺序执行 `sql/V2__create_tool_table.sql`。

---

## 🆕 最新更新

> ⭐ **随手 Star 是一种美德**，你们的 Star 就是我持续维护的动力！

### v1.1（智能体工具化 & Dify 接入）

1. 🆕 新增 **Dify 类型智能体**：在【AI 工具箱 → Dify API 管理】统一维护外部 Dify 服务，通过 `agentType=dify` 即可一键纳管 Chatflow / Workflow / Agent 应用，对话与历史会话走本地 `dify_session_mapping` 映射，全链路流式输出。
2. 🆕 新增 **原生智能体调用本地 Java Tool**：智能体支持挂载 `builtin` 类型工具，后台通过反射扫描 `dev.langchain4j.agent.tool.Tool` 注解方法注册到 LangChain4j，大模型可自动调用本地 Java 业务方法（已内置 `time_tool` 时间工具示例）。
3. 🆕 新增 **调用 Dify Sandbox 执行 Python Tool**：智能体支持挂载 `sandbox` 类型工具，自动把 LLM 参数按 `parameters` 元数据拼装成 `func_name(args)` 并下发到 [dify-sandbox](https://github.com/langgenius/dify-sandbox) 沙箱执行 Python 代码，回传 `print(result)` 文本给 LLM。
4. 🆕 新增 **工具管理**：新增 `agent_tool`、`agent_tool_mapping` 表与对应菜单（AI 工具箱 → Agent Tools），支持「builtin / sandbox」两种工具类型，新增 `ai_tool_type` 字典，并预置 `time_tool` 内置工具。
5. 🆕 新增 **Dify API 管理**：新增 `dify_api`、`dify_session_mapping` 表与对应菜单，统一维护外部 Dify 服务接入。
6. 🆕 新增 **工具调用与召回可观测**：新增 `agent_tool_invoke`、`agent_kb_invoke` 两张记录表，聊天响应中推送 `TOOL_CALL` / `TOOL_CALLED` / `RETRIEVED` 三类 SSE 事件，前端可实时展示工具调用过程。
7. 🆕 新增 **数据库迁移脚本** `sql/V2__create_tool_table.sql`，包含全部表结构与字典数据。
8. 🆕 新增 **sandbox 部署资源**，仓库 `sandbox/` 目录提供 dify-sandbox 启动配置模板与依赖说明。

### v1.0

1. 🆕 新增 **AI 模型管理**（LLM / Embedding），支持 Ollama 与 OpenAI 兼容协议，支持本地向量模型运行（[shibing624_text2vec-base-chinese](https://modelscope.cn/models/zjwan461/shibing624_text2vec-base-chinese/files)），支持在线下载本地向量模型。
2. 🆕 新增 **本地知识库** 功能，支持文档分段、手动调整分段内容。
3. 🆕 新增 **智能体管理**，支持多知识库挂载、系统提示词、用户提示词模板、记忆功能、客户端限流。
4. 🆕 新增 **AI 聊天** 功能，支持深度思考 UI 渲染，支持删除聊天历史。
5. 🆕 新增 **推送服务**，支持异步下载本地向量模型结果推送、文档分段向量化处理结果推送。
6. 🆕 新增 **向量模型维度检测** 功能。
7. 🆕 新增 **知识库重向量** 功能。

---

## 🪄 v1.1 新能力详解：三类智能体形态

v1.1 起平台支持三种智能体形态，可在【AI 工具箱 → 智能体管理】中按 `agentType` 字段切换：

| 形态 | agentType | 底层实现 | 典型用途 |
| --- | --- | --- | --- |
| **原生智能体**（LangChain4j） | `origin` | `dev.langchain4j.service.AiServices` + 本地 LLM | 内置 RAG、Java Tool、Python Tool 调用 |
| **Dify 智能体** | `dify` | 转发到外部 [Dify](https://dify.ai/) `/v1/chat-messages` 流式接口 | 纳管现有 Dify Chatflow / Workflow / Agent |
| **（扩展位）** | 预留 | — | 后续接入更多智能体平台 |

> 字典定义：`ai_agent_type` 下包含 `origin`、`dify` 两项枚举。

### 1. 原生智能体调用本地 Java Tool

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

### 2. 原生智能体调用 Dify Sandbox 执行 Python Tool

实现路径：`ruoyi-ai-core/.../tool/SandboxToolExecutor.java`

- 在【Agent Tools】新建工具，`toolType=sandbox`，`code` 填写 Python 函数定义，`parameters` 填写参数定义 JSON（如 `[{"name":"x","type":"int"}]`）。
- `SandboxToolExecutor` 会按 LLM 实际传入的参数，把函数调用拼装成 `func_name(arg1=value1, arg2='value2')` 并加上 `print(result)` 发送到 dify-sandbox。
- dify-sandbox 执行后回传 `print` 输出（即 `result` 的字符串表示），作为工具调用结果注入到 LLM 上下文中。

```python
# 示例：calc_circle_area.py（code 字段）
import math
def calc_circle_area(radius):
    return math.pi * radius * radius
```

> **部署提示**：先按 [dify-sandbox](https://github.com/langgenius/dify-sandbox) 官方文档启动沙箱服务（仓库 `sandbox/conf/config.yaml` 已附模板），后端 `application.yml` 中配置 `ai.sandbox.url-prefix` 与 `ai.sandbox.api-key`。

### 3. Dify 类型智能体

实现路径：`ruoyi-ai-dify` 模块 + `ruoyi-ai-chat/.../controller/DifyChatController.java`

- 在【AI 工具箱 → Dify API 管理】维护外部 Dify 服务的 `urlPrefix` 与 `apiKey`，一个智能体绑定一个 Dify 应用。
- 后端将本地 `sessionId` 通过 `dify_session_mapping` 映射到 Dify 的 `conversation_id`，并通过 [SSE 转发 Dify 流式响应](ruoyi-ai/ruoyi-ai-chat/src/main/java/com/ruoyi/ai/controller/DifyChatController.java:80)。
- 支持会话列表、历史消息的本地化分页查询，前端开箱即用。

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
| dify-sandbox（可选） | latest | 仅在使用 `sandbox` 类型 Python 工具时需要 |

> 基础环境的安装与配置请参考 RuoYi 官方文档：<https://doc.ruoyi.vip/ruoyi-vue/>

### 二、Docker 容器化部署（推荐）

项目根目录内置了容器化构建脚本：

```bash
# 1. 构建 ruoyi-admin 与 ruoyi-ui 镜像
./build.sh

# 2. 启动项目（含 MySQL / Redis / PgVector 等中间件）
docker compose -f docker-compose.yml up -d
```

> Windows 用户可使用 PowerShell 执行 `build.sh`，或参考 `bin/` 目录下的脚本。

### 三、本地开发模式

```bash
# 初始化数据库（按顺序执行）
sql/ry_*.sql
sql/quartz.sql
# v1.1 起还需执行工具相关迁移
sql/V2__create_tool_table.sql

# 启动后端
mvn spring-boot:run -pl ruoyi-admin

# 启动前端
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
- **智能体管理**：多类型智能体（`origin` / `dify`），支持系统提示词、记忆、知识库挂载、工具挂载、客户端限流。
- **Agent Tools**：管理 `builtin`（Java Tool）与 `sandbox`（Python Tool）两种工具类型，可直接挂载到任意智能体上。
- **Dify API 管理**：统一维护外部 Dify 服务接入（URL 前缀 + API Key）。
- **AI 聊天**：流式对话、深度思考 UI 渲染、历史会话管理。

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
- Ollama 官网：<https://ollama.com/>
- Ollama 本地部署教程：<https://developer.aliyun.com/article/1656872>
- Dify 官方仓库：<https://github.com/langgenius/dify>
- dify-sandbox 官方仓库：<https://github.com/langgenius/dify-sandbox>

---

## 📄 开源协议

本项目基于 [MIT License](./LICENSE) 开源。
