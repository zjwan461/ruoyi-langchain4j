package com.ruoyi.ai.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.config.AiConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * 文档解析服务 Java SDK（Spring Bean版本，Jackson解析）
 * 对接 FastAPI PUT /process
 * 接口返回：数组 List<DocParseItem>，xls多sheet会返回多条
 */
@Component
public class DocumentParseClient {

    /**
     * 服务完整地址: http://ip:port/process
     */
    private final String serverUrl;
    /**
     * 服务端 SERVER_API_KEY
     */
    private final String apiKey;
    /**
     * 连接超时 ms
     */
    private final int connectTimeout;
    /**
     * 读取超时 ms
     */
    private final int readTimeout;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 构造注入 AiConfig
     */
    public DocumentParseClient(AiConfig aiConfig) {
        AiConfig.DocParser docParser = aiConfig.getDocParser();
        Assert.notBlank(docParser.getServerUrl(), "document.parse.server-url 不能为空");
        this.serverUrl = docParser.getServerUrl().trim();
        this.apiKey = docParser.getApiKey() == null ? "" : docParser.getApiKey().trim();
        this.connectTimeout = docParser.getConnectTimeout() <= 0 ? 30000 : docParser.getConnectTimeout();
        this.readTimeout = docParser.getReadTimeout() <= 0 ? 120000 : docParser.getReadTimeout();
    }

    /**
     * 本地文件解析
     *
     * @return 解析结果列表（xls多个工作表会返回多条）
     */
    public DocParseItem parseFile(File file) {
        Assert.isTrue(file.exists() && file.isFile(), "文件不存在");
        byte[] bytes = FileUtil.readBytes(file);
        String fileName = file.getName();
        return parseBytes(bytes, fileName);
    }

    /**
     * Path路径解析
     */
    public DocParseItem parseFile(Path path) {
        return parseFile(path.toFile());
    }

    /**
     * 文件字节数组解析
     *
     * @param fileBytes 文件二进制
     * @param fileName  原始文件名（带后缀，服务端靠后缀识别格式）
     * @return 解析结果列表
     */
    public DocParseItem parseBytes(byte[] fileBytes, String fileName) {
        Assert.notNull(fileBytes, "文件字节不能为空");
        Assert.notBlank(fileName, "fileName不能为空，服务端依靠文件名后缀识别文件类型");

        HttpRequest request = HttpUtil.createRequest(Method.PUT, serverUrl)
                .body(fileBytes)
                .header("X-Filename", fileName)
                .setConnectionTimeout(connectTimeout)
                .setReadTimeout(readTimeout);

        // 鉴权Header
        if (!apiKey.isBlank()) {
            request.header("Authorization", "Bearer " + apiKey);
        }

        try (HttpResponse response = request.execute()) {
            int status = response.getStatus();
            String body = response.body();

            if (status == 200) {
                // 接口永远返回数组
                return objectMapper.readValue(body, new TypeReference<>() {
                });
            }
            throw new DocumentParseException(status, body);
        } catch (DocumentParseException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentParseException(-1, "网络或序列化异常", e);
        }
    }

    /**
     * InputStream解析（一次性加载至内存，大文件慎用）
     */
    public DocParseItem parseStream(InputStream inputStream, String fileName) {
        byte[] bytes = IoUtil.readBytes(inputStream);
        return parseBytes(bytes, fileName);
    }


    // ===================== DTO模型（和Python返回结构完全对齐） =====================
    public static class DocParseItem {
        /**
         * markdown正文
         */
        private String page_content;
        /**
         * 元数据
         */
        private DocMetadata metadata;

        public String getPage_content() {
            return page_content;
        }

        public void setPage_content(String page_content) {
            this.page_content = page_content;
        }

        public DocMetadata getMetadata() {
            return metadata;
        }

        public void setMetadata(DocMetadata metadata) {
            this.metadata = metadata;
        }
    }

    public static class DocMetadata {
        private String trace_id;
        private String filename;
        private String ext;
        private String engine;
        private String status;

        public String getTrace_id() {
            return trace_id;
        }

        public void setTrace_id(String trace_id) {
            this.trace_id = trace_id;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getExt() {
            return ext;
        }

        public void setExt(String ext) {
            this.ext = ext;
        }

        public String getEngine() {
            return engine;
        }

        public void setEngine(String engine) {
            this.engine = engine;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    // ===================== 自定义异常 =====================
    public static class DocumentParseException extends RuntimeException {
        private final int httpCode;
        private final String responseBody;

        public DocumentParseException(int httpCode, String responseBody) {
            super("文档解析服务调用失败, httpCode=" + httpCode + ", body=" + responseBody);
            this.httpCode = httpCode;
            this.responseBody = responseBody;
        }

        public DocumentParseException(int httpCode, String msg, Throwable cause) {
            super(msg, cause);
            this.httpCode = httpCode;
            this.responseBody = null;
        }

        public int getHttpCode() {
            return httpCode;
        }

        public String getResponseBody() {
            return responseBody;
        }
    }
}
