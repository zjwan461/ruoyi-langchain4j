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

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
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
        List<FuncInfo> allFuncList = new ArrayList<>();

        // 循环解析所有def函数
        while (defMatcher.find()) {
            String funcName = defMatcher.group(1);
            String paramContent = defMatcher.group(2).trim();

            FuncInfo funcInfo = new FuncInfo();
            funcInfo.setFuncName(funcName);

            if (!paramContent.isEmpty()) {
                String[] paramItems = paramContent.split(",");
                for (String item : paramItems) {
                    String paramStr = item.trim();
                    if (paramStr.isBlank()) {
                        continue;
                    }
                    Matcher paramMatcher = SINGLE_PARAM_PATTERN.matcher(paramStr);
                    if (!paramMatcher.find()) {
                        throw new ServiceException("Python代码结构不合法：参数[" + paramStr + "]不符合 name:type 规范");
                    }
                    String paramName = paramMatcher.group(1);
                    String paramType = paramMatcher.group(2);
                    funcInfo.paramList.add(new Param(paramName, paramType));
                }
            }
            allFuncList.add(funcInfo);
        }

        // 校验1：未找到任何函数
        if (allFuncList.isEmpty()) {
            throw new ServiceException("Python代码结构不合法：未找到符合格式的 def 函数定义");
        }

        // 只有一个函数，直接返回
        if (allFuncList.size() == 1) {
            return allFuncList.get(0);
        }

        // 多个函数：必须存在main函数，只返回main
        FuncInfo mainFunc = allFuncList.stream()
                .filter(f -> "main".equals(f.getFuncName()))
                .findFirst()
                .orElseThrow(() -> new ServiceException("Python代码包含多个函数，缺少必需的 main 函数，请补充def main()定义"));

        return mainFunc;
    }

    public static void main(String[] args) {
        // 1. 单个普通函数（直接返回）
        String pySingleOther = """
                def hello(arg1: str):
                    return "test"
                """;
        try {
            FuncInfo info = parsePythonCode(pySingleOther);
            System.out.println("【用例1-单个非main函数】");
            System.out.println(info);
        } catch (ServiceException e) {
            System.err.println("异常：" + e.getMessage());
        }
        System.out.println("----------------------------------------");

        // 2. 多个函数含main，返回main
        String pyMultiHasMain = """
                def util_func(a: int):
                    return a
                def main(data: dict):
                    return util_func(1)
                """;
        try {
            FuncInfo info = parsePythonCode(pyMultiHasMain);
            System.out.println("【用例2-多函数含main，仅返回main】");
            System.out.println(info);
        } catch (ServiceException e) {
            System.err.println("异常：" + e.getMessage());
        }
        System.out.println("----------------------------------------");

        // 3. 多个函数无main，抛异常
        String pyMultiNoMain = """
                def funcA():
                    pass
                def funcB(num: int):
                    pass
                """;
        try {
            parsePythonCode(pyMultiNoMain);
        } catch (ServiceException e) {
            System.err.println("【用例3异常】" + e.getMessage());
        }
        System.out.println("----------------------------------------");

        // 4. 无任何函数
        String pyNoFunc = "import json\nprint(123)";
        try {
            parsePythonCode(pyNoFunc);
        } catch (ServiceException e) {
            System.err.println("【用例4异常】" + e.getMessage());
        }
        System.out.println("----------------------------------------");

        // 5. 单个main函数
        String pySingleMain = """
                def main(arg1: str, arg2:int):
                    return {}
                """;
        try {
            FuncInfo info = parsePythonCode(pySingleMain);
            System.out.println("【用例5-单个main函数】");
            System.out.println(info);
        } catch (ServiceException e) {
            System.err.println("异常：" + e.getMessage());
        }
        System.out.println("----------------------------------------");

        // 6. 参数格式非法
        String pyErrParam = "def test(a, b: str): pass";
        try {
            parsePythonCode(pyErrParam);
        } catch (ServiceException e) {
            System.err.println("【用例6异常】" + e.getMessage());
        }
    }
}