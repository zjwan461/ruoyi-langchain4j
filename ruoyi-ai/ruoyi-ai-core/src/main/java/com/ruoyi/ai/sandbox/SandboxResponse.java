package com.ruoyi.ai.sandbox;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * dify-sandbox 执行响应体。
 *
 * <p>对应 dify-sandbox <code>POST /v1/sandbox/run</code> 的返回结构：
 * <pre>{@code
 * {
 *   "code": 0,            // 0 表示执行成功，非 0 表示失败
 *   "data": {
 *     "stdout": "...",
 *     "stderr": "..."
 *   },
 *   "error": null
 * }
 * }</pre>
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SandboxResponse {

    /**
     * 业务状态码：{@code 0} 表示执行成功，非 0 表示执行失败。
     *
     * <p>与 HTTP 状态码不同，dify-sandbox 即便在沙箱内 Python 抛异常时，
     * HTTP 仍返回 200，由该字段标识脚本是否正常运行。</p>
     */
    private int code;

    /**
     * 成功时的标准输出 / 标准错误输出。
     */
    private Data data;

    private String message;


    public SandboxResponse() {
    }

    /**
     * 是否执行成功（业务码为 0）。
     */
    public boolean isSuccess() {
        return code == 0;
    }

    /**
     * 获取标准输出（{@code stdout}），若执行失败返回 {@code null}。
     */
    public String getStdout() {
        return data == null ? null : data.getStdout();
    }

    /**
     * 获取标准错误输出（{@code stderr}），若执行失败返回 {@code null}。
     */
    public String getStderr() {
        return data == null ? null : data.getError();
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "SandboxResponse{" +
                "code=" + code +
                ", message=" + message +
                ", data=" + data +
                '}';
    }

    /**
     * dify-sandbox 执行成功时的输出载荷。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {

        /**
         * Python 进程的标准输出。
         */
        private String stdout;

        /**
         * Python 进程的标准错误输出。
         */
        private String error;

        public Data() {
        }

        public String getStdout() {
            return stdout;
        }

        public void setStdout(String stdout) {
            this.stdout = stdout;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        @Override
        public String toString() {
            return "Data{" +
                    "stdout='" + stdout + '\'' +
                    ", error='" + error + '\'' +
                    '}';
        }
    }
}