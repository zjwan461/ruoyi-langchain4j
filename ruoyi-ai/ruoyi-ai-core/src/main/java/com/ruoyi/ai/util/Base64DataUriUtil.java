package com.ruoyi.ai.util;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base64 DataURI解析工具
 * 解析形如 data:image/png;base64,xxxxxx 的字符串
 * 提取 mime类型 和 纯净base64编码
 */
public class Base64DataUriUtil {

    /**
     * data URI 正则匹配
     * 分组1: mime type  分组2: base64内容
     */
    private static final Pattern DATA_URI_PATTERN = Pattern.compile("^data:(.+?);base64,(.+)$");

    /**
     * 解析结果实体
     */
    @Getter
    @Setter
    @ToString
    public static class Base64ParseResult {
        /**
         * mime类型，纯base64无前缀时为null，例：image/png
         */
        private String mimeType;
        /**
         * 剥离前缀后的纯净base64字符串
         */
        private String pureBase64;
        /**
         * 是否是标准dataURI格式
         */
        private boolean dataUri;


    }

    /**
     * 解析base64字符串（支持dataURI前缀）
     *
     * @param source 原始字符串，可能为 data:xxx;base64,xxxx 或者纯base64
     * @return 解析结果
     */
    public static Base64ParseResult parse(String source) {
        Base64ParseResult result = new Base64ParseResult();
        if (source == null || source.isBlank()) {
            result.setPureBase64("");
            result.setDataUri(false);
            return result;
        }

        Matcher matcher = DATA_URI_PATTERN.matcher(source.trim());
        if (matcher.find()) {
            // 匹配到 data:xxx;base64, 前缀
            result.setDataUri(true);
            result.setMimeType(matcher.group(1));
            result.setPureBase64(matcher.group(2));
        } else {
            // 纯base64，无mime信息
            result.setDataUri(false);
            result.setMimeType(null);
            result.setPureBase64(source.trim());
        }
        return result;
    }

    /**
     * 简易校验是否疑似base64（快速判断，非严格校验）
     * 注意：仅格式字符校验，不能100%保证合法
     */
    public static boolean isLikelyBase64(String str) {
        if (str == null || str.isBlank()) {
            return false;
        }
        String pureBase64 = parse(str).getPureBase64();
        // base64合法字符 A-Za-z0-9+/，末尾允许0~2个=
        return pureBase64.matches("[A-Za-z0-9+/]+={0,2}");
    }


    // ===== 测试示例 =====
    public static void main(String[] args) {
        // 测试1：带dataURI图片
        String imgBase64 = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD...";
        Base64ParseResult r1 = parse(imgBase64);
        System.out.println(r1);

        // 测试2：纯base64字符串
        String plainBase64 = "SGVsbG8gV29ybGQ=";
        Base64ParseResult r2 = parse(plainBase64);
        System.out.println(r2);

        // 测试3：pdf base64
        String pdfBase64 = "data:application/pdf;base64,JVBERi0xLjQKMSAwIG9iago8PAovVHlwZSAvQ2F0YWxvZw==";
        Base64ParseResult r3 = parse(pdfBase64);
        System.out.println(r3);
    }
}