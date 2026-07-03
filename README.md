<div align="center">

# RuoYi-Langchain4j

**基于 RuoYi-Vue 集成 LangChain4j 的企业级 AI 快速开发平台**

[![RuoYi-Vue](https://img.shields.io/badge/Base-RuoYi--Vue-blue)](https://github.com/yangzongzhuan/RuoYi-Vue)
[![LangChain4j](https://img.shields.io/badge/AI-LangChain4j-green)](https://docs.langchain4j.info/)
[![Spring Boot](https://img.shields.io/badge/SpringBoot-2.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-2.x-success)](https://vuejs.org/)
[![License](https://img.shields.io/badge/license-MIT-orange)](./LICENSE)

> 前端 Vue / Element UI · 后端 Spring Boot / Spring Security / Redis & Jwt / WebSocket / LangChain4j / PgVector

[快速开始](#-快速开始) · [在线体验](#-在线体验) · [演示图](#-演示图) · [支持项目](#-支持项目)

</div>

---

## 📖 项目简介

RuoYi-Langchain4j 是在经典后台管理框架 **[RuoYi-Vue](https://github.com/yangzongzhuan/RuoYi-Vue)** 之上，深度集成 **[LangChain4j](https://docs.langchain4j.info/)** 与向量数据库 **PgVector** 的扩展版本。
项目在保留 RuoYi 原有权限、监控、定时任务、代码生成等全部能力的同时，新增了 **AI 模型管理 / 本地知识库 / 智能体（Agent）/ AI 对话** 等一站式 AI 能力，致力于帮助开发者以最低成本快速构建企业级 AI 应用。

### 🧩 技术栈

| 类别 | 技术 |
| --- | --- |
| 前端 | Vue、Element UI、Axios、Vue Router、Vuex |
| 后端 | Spring Boot、Spring Security、MyBatis-Plus、Redis、Jwt、WebSocket |
| AI 框架 | [LangChain4j](https://docs.langchain4j.info/)（兼容 Ollama / OpenAI 协议） |
| 向量库 | [PgVector](https://github.com/pgvector/pgvector)（基于 PostgreSQL） |
| 部署 | Docker、Docker Compose、Nginx |

### ⭐ 核心特性

- 🤖 **AI 模型管理**：统一管理大语言模型（LLM）与文本向量模型（Embedding），支持 Ollama 与 OpenAI 兼容协议，支持在线下载/加载本地向量模型。
- 📚 **本地知识库**：支持文档自动分段、人工调整分段、向量化与重向量化。
- 🧠 **智能体（Agent）**：支持多知识库挂载、系统提示词、用户提示词模板、多轮记忆与客户端限流。
- 💬 **AI 对话**：支持流式输出、深度思考（Chain-of-Thought）UI 渲染、历史会话管理。
- 🔔 **异步推送**：基于 WebSocket 推送向量下载、文档分段处理等耗时任务结果。
- 🛠️ **RuoYi 全量能力**：用户/角色/菜单/字典/定时任务/代码生成/服务监控等开箱即用。
- 🔐 **JWT 多端鉴权**：动态权限菜单 + 数据权限 + 按钮级别控制。

---

## ⚠️ 重要提示

> 1. **基础环境部署**与 RuoYi-Vue 一致，详见官方文档：<https://doc.ruoyi.vip/ruoyi-vue/>
> 2. **RAG 功能依赖 PgVector**，本地推荐使用 Docker 部署（项目已附带 `docker-compose-middleware.yml`）。
> 3. **AI 对话需提前在【AI 工具箱 → 模型管理】中配置可用的大语言模型与向量模型。**

---

## 🆕 最新更新

> ⭐ **随手 Star 是一种美德**，你们的 Star 就是我持续维护的动力！

### v1.0

1. 🆕 新增 **AI 模型管理**（LLM / Embedding），支持 Ollama 与 OpenAI 兼容协议，支持本地向量模型运行（[shibing624_text2vec-base-chinese](https://modelscope.cn/models/zjwan461/shibing624_text2vec-base-chinese/files)），支持在线下载本地向量模型。
2. 🆕 新增 **本地知识库** 功能，支持文档分段、手动调整分段内容。
3. 🆕 新增 **智能体管理**，支持多知识库挂载、系统提示词、用户提示词模板、记忆功能、客户端限流。
4. 🆕 新增 **AI 聊天** 功能，支持深度思考 UI 渲染，支持删除聊天历史。
5. 🆕 新增 **推送服务**，支持异步下载本地向量模型结果推送、文档分段向量化处理结果推送。
6. 🆕 新增 **向量模型维度检测** 功能。
7. 🆕 新增 **知识库重向量** 功能。

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
| 18 | 🤖 **AI 工具箱** | **统一管理 AI 模型 / 知识库 / 智能体** |

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

---

## 📄 开源协议

本项目基于 [MIT License](./LICENSE) 开源。
