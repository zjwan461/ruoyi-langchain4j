package com.ruoyi.ai.util;

import java.util.regex.Pattern;

public class JsonCheckUtil {

    // 正则：匹配以 { 或 [ 包裹的JSON字符串，首尾允许空白
    private static final Pattern JSON_PATTERN = Pattern.compile("^\\s*([\\[\\{].*[\\]\\}])\\s*$", Pattern.DOTALL);

    /**
     * 简单判断字符串是否可能是JSON（对象/数组）
     *
     * @param str 待检测字符串
     * @return true：疑似JSON（{} / []）
     */
    public static boolean isMaybeJson(String str) {
        if (str == null || str.isBlank()) {
            return false;
        }
        return JSON_PATTERN.matcher(str).matches();
    }
}

