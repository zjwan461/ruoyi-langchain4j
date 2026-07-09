drop table if exists agent_tool;
create table agent_tool
(
    id          bigint unsigned not null auto_increment comment '主键ID',
    tool_name   varchar(255)    not null comment '工具名称',
    tool_type   varchar(255)    not null comment '工具类型，可选项：builtin,sandbox',
    code        longtext        null comment '工具代码，仅sandbox类型有值',
    clazz       varchar(255)    null comment '内置工具全类名，仅builtin类型有值',
    description varchar(1000)   null comment '工具描述',
    parameters  varchar(2000)   null comment '工具参数定义',
    create_time DATETIME        NULL DEFAULT NULL COMMENT '创建时间',
    update_time datetime        null default null comment '更新时间',
    create_by   varchar(64)          default '' comment '创建者',
    update_by   varchar(64)          default '' comment '更新者',
    primary key (id)
) engine = innodb
    comment ='agent_tool表';

-- 内置时间工具
INSERT INTO agent_tool (id, tool_name, tool_type, code, clazz, description, parameters, create_time, update_time,
                        create_by, update_by)
VALUES (1, 'time_tool', 'builtin', null, 'com.ruoyi.ai.tool.TimeTool', null, null, sysdate(), null, 'admin', '');

drop table if exists agent_tool_mapping;
create table agent_tool_mapping
(
    agent_id bigint unsigned not null,
    tool_id  bigint unsigned not null,
    primary key (agent_id, tool_id)
) engine = innodb
    comment ='agent_tool_mapping表(多对多关系映射)';


INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time,
                           remark)
VALUES (105, 'AI工具类型', 'ai_tool_type', '0', 'admin', '2026-07-07 16:07:02', '', NULL, 'AI调用的工具类型');

INSERT INTO sys_dict_data
(dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by,
 create_time, update_by, update_time, remark)
VALUES (111, 0, 'builtin', 'builtin', 'ai_tool_type', NULL, 'info', 'N', '0', 'admin', '2026-07-07 16:07:39', 'admin',
        '2026-07-07 16:08:04', NULL);
INSERT INTO sys_dict_data
(dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by,
 create_time, update_by, update_time, remark)
VALUES (112, 0, 'sandbox', 'sandbox', 'ai_tool_type', NULL, 'primary', 'N', '0', 'admin', '2026-07-07 16:07:56', '',
        NULL, NULL);


-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
values ('Agent Tools', '2000', '1', 'agent_tool', 'ai/agent_tool/index', 1, 0, 'C', '0', '0', 'ai:agent_tool:list',
        'tool', 'admin', sysdate(), '', null, 'agent_tool菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
values ('Agent Tools查询', @parentId, '1', '#', '', 1, 0, 'F', '0', '0', 'ai:agent_tool:query', '#', 'admin', sysdate(),
        '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
values ('Agent Tools新增', @parentId, '2', '#', '', 1, 0, 'F', '0', '0', 'ai:agent_tool:add', '#', 'admin', sysdate(),
        '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
values ('Agent Tools修改', @parentId, '3', '#', '', 1, 0, 'F', '0', '0', 'ai:agent_tool:edit', '#', 'admin', sysdate(),
        '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
values ('Agent Tools删除', @parentId, '4', '#', '', 1, 0, 'F', '0', '0', 'ai:agent_tool:remove', '#', 'admin',
        sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
values ('Agent Tools导出', @parentId, '5', '#', '', 1, 0, 'F', '0', '0', 'ai:agent_tool:export', '#', 'admin',
        sysdate(), '', null, '');