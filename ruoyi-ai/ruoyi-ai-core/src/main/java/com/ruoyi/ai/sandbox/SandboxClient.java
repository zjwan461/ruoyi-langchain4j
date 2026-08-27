package com.ruoyi.ai.sandbox;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.sandbox.dto.SandboxResponse;

@Component
public class SandboxClient {

    private final String baseUrl;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建客户端实例
     *
     * @param baseUrl Sandbox API 基础 URL (如 http://localhost:8080)
     * @param apiKey  API 认证密钥
     */
    public SandboxClient(AiConfig aiConfig) {
        String baseUrl = aiConfig.getSandbox().getBaseUrl();
        String apiKey = aiConfig.getSandbox().getApiKey();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
    }

    // ==================== 会话管理 ====================

    /**
     * 创建新的沙箱会话
     *
     * @return 会话 ID
     */
    public String createSession() throws Exception {
        java.util.Map<String, Object> response = postJson("/api/sandbox/session", "{}", java.util.Map.class);
        return (String) response.get("sessionId");
    }

    /**
     * 创建默认沙箱会话
     *
     * @return 会话 ID
     */
    public String createDefaultSession() throws Exception {
        java.util.Map<String, Object> response = postJson("/api/sandbox/session-default", "{}", java.util.Map.class);
        return (String) response.get("sessionId");
    }

    /**
     * 删除会话并清理容器
     *
     * @param sessionId 会话 ID
     */
    public void deleteSession(String sessionId) throws Exception {
        delete(String.format("/api/sandbox/session/%s", sessionId));
    }

    // ==================== 代码执行 ====================

    /**
     * 在沙箱中执行 Python 代码
     *
     * @param sessionId 会话 ID
     * @param code      Python 源代码
     * @return 执行结果包含退出码、标准输出和错误信息
     */
    public SandboxResponse execPython(String sessionId, String code) throws Exception {
        String json = String.format("{\"sessionId\":\"%s\",\"code\":%s}",
                sessionId, objectMapper.writeValueAsString(code));
        return postJson("/api/sandbox/exec/python", json, SandboxResponse.class);
    }

    /**
     * 在沙箱中执行 Shell 命令
     *
     * @param sessionId 会话 ID
     * @param command   Shell 命令
     * @return 执行结果
     */
    public SandboxResponse execShell(String sessionId, String command) throws Exception {
        String json = String.format("{\"sessionId\":\"%s\",\"command\":%s}",
                sessionId, objectMapper.writeValueAsString(command));
        return postJson("/api/sandbox/exec/shell", json, SandboxResponse.class);
    }

    // ==================== pip 包管理 ====================

    /**
     * 安装 Python 包
     *
     * @param sessionId   会话 ID
     * @param packageName 包名（支持版本约束，如 requests>=2.28）
     * @return 执行结果
     */
    public SandboxResponse pipInstall(String sessionId, String packageName) throws Exception {
        String json = String.format("{\"sessionId\":\"%s\",\"pkg\":%s}",
                sessionId, objectMapper.writeValueAsString(packageName));
        return postJson("/api/sandbox/pip/install", json, SandboxResponse.class);
    }

    /**
     * 卸载 Python 包
     *
     * @param sessionId   会话 ID
     * @param packageName 包名
     * @return 执行结果
     */
    public SandboxResponse pipUninstall(String sessionId, String packageName) throws Exception {
        String json = String.format("{\"sessionId\":\"%s\",\"pkg\":%s}",
                sessionId, objectMapper.writeValueAsString(packageName));
        return postJson("/api/sandbox/pip/uninstall", json, SandboxResponse.class);
    }

    /**
     * 列出已安装的 Python 包
     *
     * @param sessionId 会话 ID
     * @return 安装包列表（文本格式）
     */
    public String pipList(String sessionId) throws Exception {
        java.util.Map<String, Object> response = getJson(String.format("/api/sandbox/pip/list?sessionId=%s", sessionId),
                java.util.Map.class);
        return (String) response.get("packages");
    }

    // ==================== 文件操作 ====================

    /**
     * 向沙箱写入文件内容
     *
     * @param sessionId     会话 ID
     * @param containerPath 容器内目标路径
     * @param content       文件内容
     */
    public void writeFile(String sessionId, String containerPath, String content) throws Exception {
        String json = String.format(
                "{\"sessionId\":\"%s\",\"path\":%s,\"content\":%s}",
                sessionId, objectMapper.writeValueAsString(containerPath),
                objectMapper.writeValueAsString(content));
        postJson("/api/sandbox/file/write", json, Void.class);
    }

    /**
     * 读取沙箱中的文件内容
     *
     * @param sessionId     会话 ID
     * @param containerPath 容器内文件路径
     * @return 文件内容字符串
     */
    public String readFile(String sessionId, String containerPath) throws Exception {
        java.util.Map<String, Object> response = getJson(String.format("/api/sandbox/file/read?sessionId=%s&path=%s",
                sessionId, containerPath), java.util.Map.class);
        return (String) response.get("content");
    }

    /**
     * 上传二进制文件到沙箱
     *
     * @param sessionId     会话 ID
     * @param containerPath 容器内目标路径
     * @param data          文件字节数据
     */
    public void uploadFile(String sessionId, String containerPath, byte[] data) throws Exception {
        if (data == null)
            throw new IllegalArgumentException("File data cannot be null");

        // 构造 multipart/form-data 请求
        String boundary = "----SandboxSDKBoundary" + System.currentTimeMillis();
        URL url = new URL(baseUrl + "/api/sandbox/file/upload");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("X-Api-Key", apiKey);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {

            // sessionId 字段
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"sessionId\"\r\n\r\n");
            writer.append(sessionId).append("\r\n");
            writer.flush();

            // path 字段
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"path\"\r\n\r\n");
            writer.append(containerPath).append("\r\n");
            writer.flush();

            // file 字段
            String fileName = containerPath.substring(containerPath.lastIndexOf('/') + 1);
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName)
                    .append("\"\r\n");
            writer.append("Content-Type: application/octet-stream\r\n\r\n");
            writer.flush();
            os.write(data);
            os.flush();
            writer.append("\r\n");
            writer.flush();

            // 结束标记
            writer.append("--").append(boundary).append("--\r\n");
            writer.flush();
        }

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    sb.append(line);
                throw new IOException("HTTP " + status + ": " + sb.toString());
            }
        }
    }

    /**
     * 下载沙箱中的文件为字节数组
     *
     * @param sessionId     会话 ID
     * @param containerPath 容器内文件路径
     * @return 文件字节数据
     */
    public byte[] downloadFile(String sessionId, String containerPath) throws Exception {
        HttpURLConnection conn = sendRequest("GET",
                String.format("%s/api/sandbox/file/download?sessionId=%s&path=%s",
                        baseUrl, sessionId, containerPath),
                null);

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    sb.append(line);
                throw new IOException("HTTP " + status + ": " + sb.toString());
            }
        }

        try (InputStream is = conn.getInputStream()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            is.transferTo(baos);
            return baos.toByteArray();
        }
    }

    // ==================== 健康检查 ====================

    /**
     * 检查沙箱服务是否可用
     *
     * @return true 如果服务正常运行
     */
    public boolean isHealth() {
        try {
            HttpURLConnection conn = sendRequest("GET", baseUrl + "/health", null);
            return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 内部方法 ====================

    private <T> T postJson(String path, String json, Class<T> clazz) throws Exception {
        HttpURLConnection conn = sendRequest("POST", baseUrl + path, json);

        int status = conn.getResponseCode();
        if (status >= 200 && status < 300) {
            if (clazz == Void.class)
                return null;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    sb.append(line);
                return objectMapper.readValue(sb.toString(), clazz);
            }
        } else {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    sb.append(line);
                throw new IOException("HTTP " + status + ": " + sb.toString());
            }
        }
    }

    private <T> T getJson(String path, Class<T> clazz) throws Exception {
        HttpURLConnection conn = sendRequest("GET", baseUrl + path, null);
        int status = conn.getResponseCode();
        if (status >= 200 && status < 300) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    sb.append(line);
                return objectMapper.readValue(sb.toString(), clazz);
            }
        } else {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    sb.append(line);
                throw new IOException("HTTP " + status + ": " + sb.toString());
            }
        }
    }

    private void delete(String path) throws Exception {
        HttpURLConnection conn = sendRequest("DELETE", baseUrl + path, null);
        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    sb.append(line);
                throw new IOException("HTTP " + status + ": " + sb.toString());
            }
        }
    }

    private HttpURLConnection sendRequest(String method, String urlString, String body) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("X-Api-Key", apiKey);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("Content-Type", "application/json");

        if (body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        return conn;
    }
}
