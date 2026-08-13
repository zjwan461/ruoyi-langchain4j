ALTER TABLE agent_flow
    add COLUMN impl_type varchar(10) NOT null default 'origin' COMMENT '单客户端1天内最大访问次数';


INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, update_by, update_time,
                           remark)
VALUES ('多智能体类型', 'agent_flow_type', '0', 'admin', sysdate(), '', NULL, '多智能体实现方案');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
                           status, create_by, create_time, update_by, update_time, remark)
VALUES (0, 'origin', 'origin', 'agent_flow_type', NULL, 'primary', 'Y', '0', 'admin', sysdate(), '', NULL,
        'langchain4j原生方案'),
       (0, 'agentic', 'agentic', 'agent_flow_type', NULL, 'success', 'N', '0', 'admin', sysdate(), '', NULL,
        'langchain deepagents方案');