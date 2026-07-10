package com.ruoyi.ai.util;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Component
public class PythonIndentChecker {
    // PEP8 标准缩进空格数
    private static final int INDENT_STEP = 4;
    // 存储缩进层级栈
    private final Deque<Integer> indentStack = new ArrayDeque<>();
    // 校验错误信息集合
    private final List<IndentError> errorList = new ArrayList<>();
    // 当前行号
    private int lineNo = 0;
    // 是否检测到Tab缩进
    private boolean hasTab = false;
    // 是否检测到空格缩进
    private boolean hasSpace = false;

    /**
     * 对外入口：校验完整Python代码字符串
     */
    public List<IndentError> check(String pythonCode) {
        reset();
        // 初始化栈：顶层0缩进（文件根层级）
        indentStack.push(0);
        String[] lines = pythonCode.split("\n");

        for (String rawLine : lines) {
            lineNo++;
            String line = rawLine;
            // 1. 分割前置缩进 和 代码正文
            int indentLen = countLeadingIndent(line);
            String content = line.substring(indentLen).trim();

            // 空行 / 纯注释行：跳过缩进校验，不影响层级
            if (content.isEmpty() || content.startsWith("#")) {
                continue;
            }

            // 规则1：混合Tab+空格缩进报错
            if (hasTab && hasSpace) {
                addError(lineNo, "混合使用Tab和空格缩进，违反PEP8规范");
            }

            int currentIndent = indentLen;
            int lastIndent = indentStack.peek();

            // 规则2：缩进必须是4的倍数
            if (currentIndent % INDENT_STEP != 0) {
                addError(lineNo, String.format("缩进长度 %d 不是 %d 的倍数", currentIndent, INDENT_STEP));
            }

            // 规则3：缩进层级逻辑校验
            if (currentIndent > lastIndent) {
                // 缩进变大：必须恰好多4个空格（上一行以:结尾）
                if (currentIndent - lastIndent != INDENT_STEP) {
                    addError(lineNo, String.format("缩进层级跳变，上一层缩进%d，当前%d，仅允许单次增加%d空格",
                            lastIndent, currentIndent, INDENT_STEP));
                }
                indentStack.push(currentIndent);
            } else if (currentIndent < lastIndent) {
                // 缩进回退：弹出栈直到匹配当前层级
                while (!indentStack.isEmpty() && indentStack.peek() > currentIndent) {
                    indentStack.pop();
                }
                // 栈顶必须等于当前缩进，否则缩进不匹配
                if (!indentStack.isEmpty() && indentStack.peek() != currentIndent) {
                    addError(lineNo, String.format("缩进闭合错误，合法层级为%d，当前缩进%d",
                            indentStack.peek(), currentIndent));
                }
            }
            // currentIndent == lastIndent：同级代码，无需操作栈

            // 标记本行是否以 : 结尾（下一行需要增加缩进）
            boolean needNextIndent = content.endsWith(":");
            if (needNextIndent) {
                // 临时标记，下一行校验时使用（简化实现，不跨行存储）
                // 进阶版可增加全局变量记录上一行是否带冒号
            }
        }
        return errorList;
    }

    /**
     * 统计一行开头的缩进字符，区分Tab(\t)和空格
     */
    private int countLeadingIndent(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                count++;
                hasSpace = true;
            } else if (c == '\t') {
                count++;
                hasTab = true;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * 新增错误记录
     */
    private void addError(int line, String msg) {
        errorList.add(new IndentError(line, msg));
    }

    /**
     * 重置所有状态，多次校验复用实例
     */
    private void reset() {
        indentStack.clear();
        errorList.clear();
        lineNo = 0;
        hasTab = false;
        hasSpace = false;
    }

    // 错误实体：行号+错误信息
    public static class IndentError {
        private final int lineNumber;
        private final String message;

        public IndentError(int lineNumber, String message) {
            this.lineNumber = lineNumber;
            this.message = message;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format("第%d行：%s", lineNumber, message);
        }
    }

    // 测试主方法
    public static void main(String[] args) {
        PythonIndentChecker checker = new PythonIndentChecker();

        // 测试用例1：存在缩进错误的Python代码
        String badPythonCode = """
                def test():
                  print("hello")  # 仅2空格，非4倍数
                    if True:
                        print(1)
                \tprint("tab缩进") # 混合tab
                """;

        List<IndentError> errors = checker.check(badPythonCode);
        if (errors.isEmpty()) {
            System.out.println("✅ Python缩进格式校验通过，无缩进问题");
        } else {
            System.out.println("❌ 检测到缩进错误：");
            errors.forEach(System.out::println);
        }

        System.out.println("\n-------------------------\n");

        // 测试用例2：标准规范代码
        String goodPythonCode = """
                def calc(a, b):
                    if a > b:
                        return a - b
                    else:
                        return b - a
                """;
        List<IndentError> goodErrors = checker.check(goodPythonCode);
        if (goodErrors.isEmpty()) {
            System.out.println("✅ 标准代码校验通过");
        }
    }
}