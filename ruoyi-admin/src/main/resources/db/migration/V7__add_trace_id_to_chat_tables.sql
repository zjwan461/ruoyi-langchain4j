-- 为 chat_message 表添加 trace_id 字段
ALTER TABLE chat_message ADD COLUMN trace_id varchar(100) COMMENT '追踪ID';

-- 为 chat_flow_message 表添加 trace_id 字段
ALTER TABLE chat_flow_message ADD COLUMN trace_id varchar(100) COMMENT '追踪ID';

-- 为已有数据补齐 trace_id：基于 (session_id, agent_id) 分组按 id 顺序编号生成，兼容 MySQL 8.0+
-- 单 Agent 表：每对 (session_id, agent_id) 内的消息分配同一 trace_id（PAGED BY id 顺序）
UPDATE chat_message m
JOIN (
    SELECT id,
           CONCAT('legacy-', session_id, '-', agent_id, '-',
                  CAST(
                      ROW_NUMBER() OVER (
                          PARTITION BY session_id, agent_id
                          ORDER BY id
                      ) AS UNSIGNED
                  )
           ) AS new_trace_id
    FROM chat_message
    WHERE trace_id IS NULL OR trace_id = ''
) t ON m.id = t.id
SET m.trace_id = t.new_trace_id;

-- Agent Flow 表：每对 (session_id, agent_flow_id, agent_id) 内的消息使用同一 trace_id
UPDATE chat_flow_message m
JOIN (
    SELECT id,
           CONCAT('legacy-', session_id, '-', agent_flow_id, '-',
                  CAST(
                      ROW_NUMBER() OVER (
                          PARTITION BY session_id, agent_flow_id, agent_id
                          ORDER BY id
                      ) AS UNSIGNED
                  )
           ) AS new_trace_id
    FROM chat_flow_message
    WHERE trace_id IS NULL OR trace_id = ''
) t ON m.id = t.id
SET m.trace_id = t.new_trace_id;

-- =========================
-- 对话日志菜单初始化
-- 父菜单：AI工具箱 (menu_id = 2000)
-- =========================

-- 一级菜单
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_by, create_time, update_by, update_time, remark)
values ('对话日志', '2000', '6', 'chatLog', 'ai/chatLog/index', 1, 0, 'C', '0', '0', 'ai:chatLog:list',
        'log', 'admin', sysdate(), '', null, '对话日志菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_by, create_time, update_by, update_time, remark)
values ('对话日志查询', @parentId, '1', '#', '', 1, 0, 'F', '0', '0', 'ai:chatLog:query', '#', 'admin', sysdate(),
        '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_by, create_time, update_by, update_time, remark)
values ('对话日志导出', @parentId, '2', '#', '', 1, 0, 'F', '0', '0', 'ai:chatLog:export', '#', 'admin', sysdate(),
        '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_by, create_time, update_by, update_time, remark)
values ('对话日志删除', @parentId, '3', '#', '', 1, 0, 'F', '0', '0', 'ai:chatLog:remove', '#', 'admin', sysdate(),
        '', null, '');
