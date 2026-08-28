# 部署指南

本文档介绍 RuoYi-Langchain4j 的生产环境部署方案。

---

## 🐳 Docker Compose 部署（推荐）

### 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少 8GB 内存
- 至少 50GB 磁盘空间

### 部署步骤

#### 1. 克隆项目

```bash
git clone https://github.com/zjwan461/ruoyi-langchain4j.git
cd ruoyi-langchain4j
```

#### 2. 配置环境变量

创建 `.env` 文件：

```env
# 数据库配置
MYSQL_ROOT_PASSWORD=your_secure_password

# Redis 配置
REDIS_PASSWORD=your_redis_password

# Sandbox 配置
SANDBOX_API_KEY=your_sandbox_api_key

# doc-parser 配置
OPENAI_API_KEY=your_openai_api_key
OPENAI_BASE_URL=https://api.openai.com/v1
SERVER_API_KEY=your_doc_parser_api_key

# agentic 配置
TAVILY_API_KEY=your_tavily_api_key
```

#### 3. 启动服务

```bash
# 启动所有服务
docker compose up -d

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f
```

#### 4. 验证部署

```bash
# 检查服务健康状态
curl http://localhost:8080/actuator/health
curl http://localhost:8142/health
curl http://localhost:8143/health
```

### 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| ruoyi-ui | 80 | 前端界面 |
| ruoyi-admin | 8080 | 后端 API |
| ruoyi-admin gRPC | 9090 | gRPC 服务 |
| agentic | 8142 | Python 智能体网关 |
| doc-parser | 8143 | 文档解析服务 |
| agentic-sandbox | 8194 | Python 沙箱 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| PostgreSQL | 5432 | 向量数据库 |

### 数据持久化

```yaml
volumes:
  mysql_data:      # MySQL 数据
  redis_data:      # Redis 数据
  postgres_data:   # PostgreSQL 数据
  upload_path:     # 上传文件
  skills:          # Skills 技能文件
```

---

## 🔧 配置说明

### 后端配置

编辑 `ruoyi-admin/src/main/resources/application.yml`：

```yaml
# 服务端口
server:
  port: 8080

# 数据库配置
spring:
  datasource:
    druid:
      master:
        url: jdbc:mysql://mysql:3306/ruoyi_langchain4j?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8
        username: root
        password: ${MYSQL_ROOT_PASSWORD}
  
  # Redis 配置
  redis:
    host: redis
    port: 6379
    password: ${REDIS_PASSWORD}

# AI 配置
ai:
  # 向量数据库配置
  pgVector:
    host: postgres
    port: 5432
    database: ruoyi_langchain4j
    user: postgres
    password: ${POSTGRES_PASSWORD}
  
  # 沙箱配置
  sandbox:
    base-url: http://agentic-sandbox:8194
    api-key: ${SANDBOX_API_KEY}
    timeout: 30
  
  # 文档解析服务配置
  doc-parser:
    url: http://doc-parser:8143
    api-key: ${SERVER_API_KEY}
  
  # agentic 服务配置
  agentic:
    url: http://agentic:8142
```

### 前端配置

编辑 `ruoyi-ui/vue.config.js`：

```javascript
module.exports = {
  devServer: {
    proxy: {
      '/dev-api': {
        target: 'http://ruoyi-admin:8080',
        changeOrigin: true,
        pathRewrite: {
          '^/dev-api': ''
        }
      }
    }
  }
}
```

### Nginx 配置

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://ruoyi-admin:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # WebSocket 代理
    location /websocket/ {
        proxy_pass http://ruoyi-admin:8080/websocket/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host $host;
    }
}
```

---

## 🚀 生产环境优化

### 1. 数据库优化

**MySQL 配置** (`my.cnf`)：

```ini
[mysqld]
# 连接数
max_connections = 1000
max_connect_errors = 1000

# 缓存
innodb_buffer_pool_size = 2G
query_cache_size = 128M

# 日志
slow_query_log = ON
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2

# 字符集
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci
```

**PostgreSQL 配置**：

```ini
# 连接数
max_connections = 200

# 内存
shared_buffers = 2GB
effective_cache_size = 6GB
work_mem = 16MB

# WAL
wal_buffers = 64MB
max_wal_size = 4GB
```

### 2. JVM 优化

编辑 `Dockerfile-admin`：

```dockerfile
ENV JAVA_OPTS="-Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/logs/heapdump.hprof"
```

### 3. Redis 优化

```conf
# 内存限制
maxmemory 2gb
maxmemory-policy allkeys-lru

# 持久化
save 900 1
save 300 10
save 60 10000

# 连接数
maxclients 10000
```

### 4. 连接池优化

**Druid 连接池**：

```yaml
spring:
  datasource:
    druid:
      initial-size: 10
      min-idle: 10
      max-active: 100
      max-wait: 60000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
```

---

## 🔒 安全配置

### 1. HTTPS 配置

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    
    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
}

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}
```

### 2. 防火墙配置

```bash
# 仅开放必要端口
ufw allow 80/tcp    # HTTP
ufw allow 443/tcp   # HTTPS
ufw allow 22/tcp    # SSH

# 启用防火墙
ufw enable
```

### 3. API Key 安全

- 使用强密码生成器生成 API Key
- 定期轮换 API Key
- 使用环境变量存储敏感信息
- 不要将 API Key 提交到代码仓库

---

## 📊 监控与日志

### 1. 应用监控

**Spring Boot Actuator**：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  metrics:
    export:
      prometheus:
        enabled: true
```

**Prometheus + Grafana**：

```yaml
# docker-compose.monitoring.yml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    ports:
      - "9090:9090"
  
  grafana:
    image: grafana/grafana:latest
    volumes:
      - grafana_data:/var/lib/grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin

volumes:
  prometheus_data:
  grafana_data:
```

### 2. 日志管理

**日志配置** (`logback-spring.xml`)：

```xml
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/logs/application.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="FILE" />
    </root>
</configuration>
```

**日志收集**（ELK Stack）：

```yaml
# docker-compose.logging.yml
version: '3.8'
services:
  elasticsearch:
    image: elasticsearch:7.17.0
    environment:
      - discovery.type=single-node
    volumes:
      - es_data:/usr/share/elasticsearch/data
    ports:
      - "9200:9200"
  
  logstash:
    image: logstash:7.17.0
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
    ports:
      - "5000:5000"
  
  kibana:
    image: kibana:7.17.0
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    ports:
      - "5601:5601"

volumes:
  es_data:
```

---

## 🔄 备份与恢复

### 1. 数据库备份

**MySQL 备份**：

```bash
#!/bin/bash
BACKUP_DIR="/backup/mysql"
DATE=$(date +%Y%m%d_%H%M%S)

# 备份所有数据库
docker exec mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} --all-databases > ${BACKUP_DIR}/backup_${DATE}.sql

# 压缩
gzip ${BACKUP_DIR}/backup_${DATE}.sql

# 删除 7 天前的备份
find ${BACKUP_DIR} -name "backup_*.sql.gz" -mtime +7 -delete
```

**PostgreSQL 备份**：

```bash
#!/bin/bash
BACKUP_DIR="/backup/postgres"
DATE=$(date +%Y%m%d_%H%M%S)

# 备份数据库
docker exec postgres pg_dump -U postgres ruoyi_langchain4j > ${BACKUP_DIR}/backup_${DATE}.sql

# 压缩
gzip ${BACKUP_DIR}/backup_${DATE}.sql
```

### 2. 文件备份

```bash
#!/bin/bash
BACKUP_DIR="/backup/files"
DATE=$(date +%Y%m%d_%H%M%S)

# 备份上传文件
tar -czf ${BACKUP_DIR}/upload_${DATE}.tar.gz /data/upload

# 备份 Skills 文件
tar -czf ${BACKUP_DIR}/skills_${DATE}.tar.gz /data/skills
```

### 3. 恢复数据

```bash
# 恢复 MySQL
docker exec -i mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} < backup_20240101_120000.sql

# 恢复 PostgreSQL
docker exec -i postgres psql -U postgres ruoyi_langchain4j < backup_20240101_120000.sql

# 恢复文件
tar -xzf upload_20240101_120000.tar.gz -C /
```

---

## 📈 性能调优

### 1. 负载均衡

**Nginx 负载均衡**：

```nginx
upstream backend {
    server ruoyi-admin-1:8080 weight=3;
    server ruoyi-admin-2:8080 weight=2;
    server ruoyi-admin-3:8080 weight=1;
}

server {
    location /api/ {
        proxy_pass http://backend/;
    }
}
```

### 2. 缓存策略

**多级缓存**：

```
请求 → Nginx 缓存 → Redis 缓存 → 数据库
```

**缓存配置**：

```yaml
# 本地缓存（Caffeine）
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=10m

# Redis 缓存
redis:
  timeout: 10000
  lettuce:
    pool:
      max-active: 100
      max-idle: 50
      min-idle: 10
```

### 3. 异步处理

```java
@Async
@EventListener
public void handleAsyncEvent(AsyncEvent event) {
    // 异步处理耗时任务
    processDocument(event.getDocument());
}
```

---

## 🛠️ 故障排查

### 1. 服务无法启动

```bash
# 查看日志
docker compose logs ruoyi-admin

# 检查端口占用
netstat -tlnp | grep 8080

# 检查数据库连接
docker exec mysql mysql -u root -p -e "SHOW DATABASES;"
```

### 2. 内存溢出

```bash
# 查看 JVM 内存
docker exec ruoyi-admin jmap -heap 1

# 生成堆转储
docker exec ruoyi-admin jmap -dump:format=b,file=/tmp/heap.hprof 1

# 分析堆转储
docker cp ruoyi-admin:/tmp/heap.hprof ./
jhat -J-Xmx2g heap.hprof
```

### 3. 数据库连接池耗尽

```bash
# 查看连接数
docker exec mysql mysql -u root -p -e "SHOW PROCESSLIST;"

# 查看 Druid 监控
curl http://localhost:8080/druid/datasource.html
```

---

## 📝 维护计划

### 日常维护

- [ ] 每日检查服务状态
- [ ] 每日检查日志错误
- [ ] 每日检查磁盘空间
- [ ] 每周清理过期日志
- [ ] 每周备份数据库

### 定期维护

- [ ] 每月更新系统补丁
- [ ] 每月更新依赖版本
- [ ] 每月优化数据库索引
- [ ] 每季度性能测试
- [ ] 每季度灾难恢复演练
