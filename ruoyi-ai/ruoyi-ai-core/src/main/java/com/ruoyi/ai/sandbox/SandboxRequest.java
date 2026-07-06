package com.ruoyi.ai.sandbox;

/**
 * dify-sandbox 执行请求体。
 *
 * <p>对应 dify-sandbox 的 <code>POST /v1/sandbox/run</code> 接口，
 * 详见官方文档：<a href="https://github.com/langgenius/dify-sandbox">dify-sandbox</a>。</p>
 *
 * <p>典型用法：</p>
 * <pre>{@code
 * SandboxRequest request = new SandboxRequest();
 * request.setCode("print('hello world')");
 * SandboxResponse response = sandboxSdk.run(request);
 * }</pre>
 */
public class SandboxRequest {

    /**
     * 要在沙箱中执行的源代码。
     */
    private String code;

    /**
     * 语言类型。当前 dify-sandbox 仅支持 {@code python}，此处保留扩展能力。
     */
    private String language = "python";

    public SandboxRequest() {
    }

    public SandboxRequest(String code) {
        this.code = code;
    }

    public SandboxRequest(String code, String language) {
        this.code = code;
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}