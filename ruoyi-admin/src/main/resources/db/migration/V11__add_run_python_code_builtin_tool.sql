-- 内置python代码执行工具
INSERT INTO agent_tool (tool_name, tool_type, code, clazz, description, parameters, create_time, update_time,
                        create_by, update_by)
VALUES ('run_python_code', 'builtin', null, 'com.ruoyi.ai.tool.RunPythonCodeTool', '内置执行python代码工具', null, sysdate(), null, 'admin', '');