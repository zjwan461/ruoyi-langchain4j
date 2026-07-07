package com.ruoyi.ai.util;

import com.ruoyi.common.exception.ServiceException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimplePythonFuncParser {

    public static class FuncInfo {
        private String funcName;
        private final List<Param> paramList = new ArrayList<>();

        public String getFuncName() {
            return funcName;
        }

        public void setFuncName(String funcName) {
            this.funcName = funcName;
        }

        public List<Param> getParamList() {
            return paramList;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("函数名：").append(funcName).append("\n参数：\n");
            if (paramList.isEmpty()) {
                sb.append("  无参数");
            } else {
                for (Param p : paramList) {
                    sb.append("  参数名：").append(p.name)
                            .append("，类型：").append(p.type).append("\n");
                }
            }
            return sb.toString();
        }
    }

    public static class Param {
        String name;
        String type;

        public Param(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    private static final Pattern DEF_FUNC_PATTERN = Pattern.compile(
            "def\\s+(\\w+)\\s*\\(([^)]*)\\)",
            Pattern.DOTALL
    );

    private static final Pattern SINGLE_PARAM_PATTERN = Pattern.compile(
            "(\\w+)\\s*:\\s*([a-zA-Z0-9_]+)"
    );

    public static FuncInfo parsePythonCode(String pythonCode) {
        Matcher defMatcher = DEF_FUNC_PATTERN.matcher(pythonCode);

        // 校验1：不存在def函数定义，结构非法
        if (!defMatcher.find()) {
            throw new ServiceException("Python代码结构不合法：未找到符合格式的 def 函数定义");
        }

        String funcName = defMatcher.group(1);
        String paramContent = defMatcher.group(2).trim();

        FuncInfo result = new FuncInfo();
        result.setFuncName(funcName);

        // 无参直接返回，不抛异常
        if (paramContent.isEmpty()) {
            return result;
        }

        String[] paramItems = paramContent.split(",");
        for (String item : paramItems) {
            String paramStr = item.trim();
            if (paramStr.isBlank()) {
                continue;
            }
            Matcher paramMatcher = SINGLE_PARAM_PATTERN.matcher(paramStr);
            // 有参数时必须符合 name:type 格式
            if (!paramMatcher.find()) {
                throw new ServiceException("Python代码结构不合法：参数[" + paramStr + "]不符合 name:type 规范");
            }
            String paramName = paramMatcher.group(1);
            String paramType = paramMatcher.group(2);
            result.paramList.add(new Param(paramName, paramType));
        }

        return result;
    }

    public static void main(String[] args) {
        // 1. 正常带参
        String pyCodeOk1 = """
                import json
                import time

                def main(arg1: str, arg2:int, arg3:dict):
                    return {"a": 1, "b": 2}
                """;
        try {
            FuncInfo info = parsePythonCode(pyCodeOk1);
            System.out.println("【合法-带参数】");
            System.out.println(info);
        } catch (ServiceException e) {
            System.err.println("异常：" + e.getMessage());
        }

        System.out.println("----------------------------------------");

        // 2. 合法无参函数
        String pyCodeOk2 = """
                import datetime

                def hello():
                    return "ok"
                """;
        try {
            FuncInfo info = parsePythonCode(pyCodeOk2);
            System.out.println("【合法-无参数】");
            System.out.println(info);
        } catch (ServiceException e) {
            System.err.println("异常：" + e.getMessage());
        }

        System.out.println("----------------------------------------");

        // 3. 非法：无def函数
        String pyCodeErr1 = "import json\nprint(123)";
        try {
            parsePythonCode(pyCodeErr1);
        } catch (ServiceException e) {
            System.err.println("非法用例1异常：" + e.getMessage());
        }

        // 4. 非法：有参数但缺少类型注解
        String pyCodeErr2 = "def test(a, b: str): pass";
        try {
            parsePythonCode(pyCodeErr2);
        } catch (ServiceException e) {
            System.err.println("非法用例2异常：" + e.getMessage());
        }
    }
}