# 快速开始

本指南将帮助你快速搭建和运行 RuoYi-Langchain4j 项目。

---

## 📋 环境要求

### 基础环境

- **JDK 1.8+**
- **Maven 3.6+**
- **Node.js 14+**
- **MySQL 5.7+**
- **Redis 5.0+**

### AI 功能依赖

- **PostgreSQL 15+**（带 pgvector 扩展）- 用于向量数据库
- **Python 3.10+** - 用于 doc-parser 和 agentic 服务
- **Docker** - 用于运行中间件和沙箱服务

---

## 🚀 快速部署（推荐）

### 1. 克隆项目

```bash
git clone https://github.com/zjwan461/ruoyi-langchain4j.git
cd ruoyi-langchain4j
```

### 2. 启动中间件

使用 Docker Compose 启动 MySQL、Redis、PostgreSQL 和 Sandbox：

```bash
docker compose -f docker-compose-middleware.yml up -d
```

**端口映射**：
- MySQL: 33306 → 3306
- Redis: 36379 → 6379
- PostgreSQL: 35432 → 5432
- Sandbox: 8194

### 3. 配置后端

编辑 `ruoyi-admin/src/main/resources/application-middleware.yml`：

```yaml
spring:
  datasource:
    druid:
      master:
        url: jdbc:mysql://localhost:33306/ry-vue
        username: root
        password: root
  redis:
    host: localhost
    port: 36379

ai:
  pgVector:
    host: localhost
    port: 35432
    database: embedding
    user: root
    password: root
  sandbox:
    base-url: http://localhost:8194
    api-key: sandbox-secret-key
```

### 4. 启动后端服务

```bash
cd ruoyi-admin
mvn clean install -DskipTests
mvn spring-boot:run
```

后端服务启动在 `http://localhost:8080`

### 5. 启动前端

```bash
cd ruoyi-ui
npm install
npm run dev
```

前端服务启动在 `http://localhost:80`

### 6. 启动 Python 服务（可选）

#### 启动 doc-parser（文档解析服务）

```bash
cd doc-parser
pip install -r requirements.txt
python main.py
```

服务启动在 `http://localhost:8143`

#### 启动 agentic（智能体网关）

```bash
cd agentic
pip install -r requirements.txt
python main.py
```

服务启动在 `http://localhost:8142`

---

## 🐳 完整 Docker 部署

### 1. 配置环境变量

创建 `.env` 文件：

```env
# 数据库密码
MYSQL_ROOT_PASSWORD=root

# Sandbox API Key
SANDBOX_API_KEY=sandbox-secret-key

# doc-parser 配置
OPENAI_API_KEY=your-openai-api-key
OPENAI_BASE_URL=https://api.openai.com/v1
SERVER_API_KEY=your-doc-parser-api-key

# agentic 配置
TAVILY_API_KEY=your-tavily-api-key
```

### 2. 启动所有服务

```bash
docker compose up -d
```

**服务端口**：
- 前端: 80
- 后端: 8080
- gRPC: 9090
- doc-parser: 8143
- agentic: 8142
- sandbox: 8194

### 3. 查看服务状态

```bash
docker compose ps
```

### 4. 查看日志

```bash
docker compose logs -f ruoyi-admin
```

---

## ⚙️ 初始化配置

### 1. 访问系统

打开浏览器访问 `http://localhost`（Docker 部署）或 `http://localhost:80`（开发环境）

默认管理员账号：
- 用户名: `admin`
- 密码: `admin123`

### 2. 配置 AI 模型

进入【AI 工具箱 → 模型管理】，添加 LLM 和 Embedding 模型：

**LLM 模型示例**（OpenAI 兼容接口）：
- 模型名称: `gpt-4`
- API Key: `your-api-key`
- Base URL: `https://api.openai.com/v1`

**Embedding 模型示例**：
- 模型名称: `text-embedding-3-small`
- API Key: `your-api-key`
- Base URL: `https://api.openai.com/v1`

### 3. 创建知识库

进入【AI 工具箱 → 知识库管理】：
1. 点击"新建知识库"
2. 上传文档（支持 PDF、Word、TXT 等）
3. 等待文档分段和向量化完成

### 4. 创建智能体

进入【AI 工具箱 → 智能体管理】：
1. 点击"新建智能体"
2. 选择模型
3. 关联知识库（可选）
4. 配置工具（可选）
5. 设置系统提示词

### 5. 开始对话

进入【AI 工具箱 → AI 对话】，选择智能体开始对话。

---

## 🔧 开发环境配置

### 后端开发

```bash
# 导入项目到 IDE（推荐 IntelliJ IDEA）
# 运行 RuoYiApplication.java
```

**热部署**：修改代码后自动重启

### 前端开发

```bash
cd ruoyi-ui
npm install
npm run dev
```

**代理配置**：`vue.config.js` 中已配置 API 代理到 `http://localhost:8080`

### Python 服务开发

```bash
# doc-parser
cd doc-parser
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
python main.py

# agentic
cd agentic
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python main.py
```

---

## ⚠️ 常见问题

### 1. 数据库连接失败

**问题**：后端启动时提示数据库连接失败

**解决**：
- 检查 MySQL 是否启动：`docker ps | grep mysql`
- 检查端口映射：33306 → 3306
- 检查数据库配置：`application-middleware.yml`

### 2. Redis 连接失败

**问题**：后端启动时提示 Redis 连接失败

**解决**：
- 检查 Redis 是否启动：`docker ps | grep redis`
- 检查端口映射：36379 → 6379
- 测试连接：`redis-cli -h localhost -p 36379 ping`

### 3. 向量数据库不可用

**问题**：知识库功能无法使用

**解决**：
- 检查 PostgreSQL 是否启动：`docker ps | grep postgres`
- 检查 pgvector 扩展是否安装
- 检查配置：`ai.pgVector.*`

### 4. Sandbox 服务不可用

**问题**：Python 工具执行失败

**解决**：
- 检查 Sandbox 是否启动：`docker ps | grep sandbox`
- 检查端口：8194
- 检查 API Key 配置

### 5. 前端无法访问后端 API

**问题**：前端页面显示但无法加载数据

**解决**：
- 检查后端是否启动：`http://localhost:8080`
- 检查 `vue.config.js` 代理配置
- 清除浏览器缓存

---

## 📚 下一步

- 阅读 [架构设计](architecture.md) 了解系统架构
- 阅读 [AI 功能](ai-features.md) 了解 AI 能力详解
- 阅读 [部署指南](deployment.md) 了解生产环境部署
