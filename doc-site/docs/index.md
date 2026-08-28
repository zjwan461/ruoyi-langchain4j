# RuoYi-Langchain4j

**基于 RuoYi-Vue 集成 LangChain4j 的企业级 AI 快速开发平台**

[![RuoYi-Vue](https://img.shields.io/badge/Base-RuoYi--Vue-blue)](https://github.com/yangzongzhuan/RuoYi-Vue)
[![LangChain4j](https://img.shields.io/badge/AI-LangChain4j-green)](https://docs.langchain4j.info/)
[![MCP](https://img.shields.io/badge/Protocol-MCP-9cf)](https://modelcontextprotocol.io/)
[![Spring Boot](https://img.shields.io/badge/SpringBoot-2.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-2.x-success)](https://vuejs.org/)
[![License](https://img.shields.io/badge/license-MIT-orange)](https://github.com/zjwan461/ruoyi-langchain4j/blob/main/LICENSE)

> 前端 Vue / Element UI · 后端 Spring Boot / Spring Security / Redis & Jwt / WebSocket / LangChain4j / PgVector / Python Sandbox / **MCP** / **Skills** · Python 微服务（**doc-parser 文档解析** / **agentic 智能体网关**）

---

## 📖 项目简介

RuoYi-Langchain4j 是在经典后台管理框架 **[RuoYi-Vue](https://github.com/yangzongzhuan/RuoYi-Vue)** 之上，深度集成 **[LangChain4j](https://docs.langchain4j.info/)**、向量数据库 **PgVector**、**[Dify](https://dify.ai/)** 生态与 **[MCP（Model Context Protocol）](https://modelcontextprotocol.io/)** 协议 的扩展版本。

项目在保留 RuoYi 原有权限、监控、定时任务、代码生成等全部能力的同时，新增了 **AI 模型管理 / 本地知识库 / 多类型智能体 / AI 对话 / MCP 工具市场 / Skills 技能体系** 等一站式 AI 能力，致力于帮助开发者以最低成本快速构建企业级 AI 应用。

---

## 🧩 技术栈

| 类别 | 技术 |
| --- | --- |
| 前端 | Vue、Element UI、Axios、Vue Router、Vuex、Monaco 代码编辑器 |
| 后端 | Spring Boot、Spring Security、MyBatis-Plus、Redis、Jwt、WebSocket |
| AI 框架 | [LangChain4j](https://docs.langchain4j.info/) + [langchain4j-mcp](https://docs.langchain4j.info/)（兼容 Ollama / OpenAI 协议） |
| Dify 生态 | [Dify](https://dify.ai/) 应用接入 |
| **代码沙箱** | [python-sandbox](https://github.com/zjwan461/python-sandbox) 自研 Python 代码执行沙箱 |
| **Python 微服务** | **doc-parser**（[FastAPI](https://fastapi.tiangolo.com/) + [Docling](https://github.com/DS4SD/docling) + VLM 文档解析）/ **agentic**（[FastAPI](https://fastapi.tiangolo.com/) + [LangChain](https://www.langchain.com/) + [DeepAgents](https://github.com/langchain-ai/deepagents) 智能体网关，gRPC 通信） |
| **MCP** | **[Model Context Protocol](https://modelcontextprotocol.io/) 工具协议（`sse` / `stdio` / `streamable-http`）** |
| **Skills** | **基于 `agent_skill` 的技能目录管理，支持 `create` / `import` 两种类型，自动追加到 System Prompt** |
| **多智能体编排** | **[AntV X6](https://x6.antv.antgroup.com/) 可视化画布 + Flow 节点驱动，多个智能体串联协作** |
| 向量库 | [PgVector](https://github.com/pgvector/pgvector)（基于 PostgreSQL） |
| 部署 | Docker、Docker Compose、Nginx |

---

## ⭐ 核心特性

- 🤖 **AI 模型管理**：统一管理大语言模型（LLM）与文本向量模型（Embedding），支持 Ollama 与 OpenAI 兼容协议，支持在线下载/加载本地向量模型。
- 📚 **本地知识库**：支持文档自动分段、人工调整分段、向量化与重向量化。
- 🧠 **多类型智能体（Agent）**：内置三类智能体形态 —— 原生 LangChain4j Agent、Dify 应用代理、以及两者的工具调用扩展。
- 💬 **AI 对话**：支持流式输出、深度思考（Chain-of-Thought）UI 渲染、历史会话管理。
- 🔔 **异步推送**：基于 WebSocket 推送向量下载、文档分段处理等耗时任务结果。
- 🛠️ **RuoYi 全量能力**：用户/角色/菜单/字典/定时任务/代码生成/服务监控等开箱即用。
- 🔐 **JWT 多端鉴权**：动态权限菜单 + 数据权限 + 按钮级别控制。
- 🪄 **Dify 类型智能体**：通过 [Dify](https://dify.ai/) 平台统一纳管 Chatflow / Workflow / Agent 等 Dify 应用。
- ☕️ **原生智能体调用本地 Java Tool**：原生（LangChain4j）智能体支持挂载 `builtin` 类型的 Java 工具。
- 🐍 **原生智能体调用 Python Sandbox 执行 Python Tool**：原生智能体支持挂载 `sandbox` 类型的 Python 工具。
- 🔌 **MCP 工具市场**：原生智能体支持挂载来自 Model Context Protocol Server 的工具。
- 📜 **Skills 技能体系**：在【AI 工具箱 → Skill】维护技能，支持 `create` 和 `import` 两种类型。
- 🕸 **多智能体编排 / Agent Flow**：通过 AntV X6 可视化画布拖拽多个智能体节点，拼接为多智能体协作链。
- 📄 **文档解析服务 doc-parser**：独立 Python 微服务，基于 Docling + VLM，支持 60+ 种文件格式转 Markdown。
- 🤖 **智能体网关 agentic**：独立 Python 微服务，基于 LangChain + DeepAgents，支持多智能体编排 Flow 的流式对话。
- 📊 **工具调用与召回可观测**：自动记录工具调用结果与知识库召回分段，方便排障与审计。

---

## 🚀 快速导航

- [快速开始](quick-start.md) - 环境准备与部署指南
- [架构设计](architecture.md) - 系统架构与调用链路
- [AI 功能](ai-features.md) - AI 能力详解
- [部署指南](deployment.md) - Docker 部署与配置
- [更新日志](changelog.md) - 版本更新记录

---

## 📄 开源协议

本项目采用 [MIT License](https://github.com/zjwan461/ruoyi-langchain4j/blob/main/LICENSE) 开源协议。
