drop table if exists agent_mcp_service;
create table agent_mcp_service
(
    id               bigint unsigned not null auto_increment comment '主键ID',
    mcp_service_name varchar(255)    not null comment 'mcp服务名称',
    mcp_url          varchar(500)    not null comment 'mcp链接',
    mcp_transport    varchar(255)    not null comment 'mcp transport',
    auth_token       varchar(255)    null comment 'mcp认证token',
    create_time      DATETIME        NULL DEFAULT NULL COMMENT '创建时间',
    update_time      datetime        null default null comment '更新时间',
    create_by        varchar(64)          default '' comment '创建者',
    update_by        varchar(64)          default '' comment '更新者',
    primary key (id)
) engine = innodb
    comment ='agent_mcp_service表';

drop table if exists agent_mcp_tool;
create table agent_mcp_tool
(
    id                   bigint unsigned not null auto_increment comment '主键ID',
    mcp_tool_name        varchar(255)    not null comment 'mcp工具名称',
    mcp_tool_description varchar(500)    not null comment 'mcp工具描述',
    mcp_tool_parameters  longtext        not null comment 'mcp工具参数',
    mcp_tool_metadata    longtext        null null comment 'mcp工具元数据',
    pid                  bigint unsigned not null comment '所属mcp service id',
    create_time          DATETIME        NULL DEFAULT NULL COMMENT '创建时间',
    update_time          datetime        null default null comment '更新时间',
    create_by            varchar(64)          default '' comment '创建者',
    update_by            varchar(64)          default '' comment '更新者',
    primary key (id)
) engine = innodb
    comment ='agent_mcp_tool表';

-- 给 agent_mcp_tool.pid 创建外键，关联 agent_mcp_service.id
ALTER TABLE agent_mcp_tool
    ADD CONSTRAINT fk_agent_mcp_tool_pid
        FOREIGN KEY (pid) REFERENCES agent_mcp_service (id)
            ON DELETE CASCADE
            ON UPDATE CASCADE;

INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time,
                           remark)
VALUES (107, 'MCP transport', 'ai_mcp_transport', '0', 'admin', '2026-07-15 09:58:28', '', null, null);
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
                           status, create_by, create_time, update_by, update_time, remark)
VALUES (115, 0, 'sse', 'sse', 'ai_mcp_transport', null, 'primary', 'N', '0', 'admin', sysdate(), '', null, null);
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
                           status, create_by, create_time, update_by, update_time, remark)
VALUES (116, 0, 'stdio', 'stdio', 'ai_mcp_transport', null, 'info', 'N', '0', 'admin', sysdate(), '', null, null);
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
                           status, create_by, create_time, update_by, update_time, remark)
VALUES (117, 0, 'streamable-http', 'streamable-http', 'ai_mcp_transport', null, 'success', 'N', '0', 'admin', sysdate(),
        '', null, null);


-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
values ('MCP', '2000', '1', 'mcp', 'ai/mcp/index', 1, 0, 'C', '0', '0', 'ai:mcp:list', 'skill', 'admin', sysdate(), '',
        null, 'mcp菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
values ('mcp查询', @parentId, '1', '#', '', 1, 0, 'F', '0', '0', 'ai:mcp:query', '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
values ('mcp新增', @parentId, '2', '#', '', 1, 0, 'F', '0', '0', 'ai:mcp:add', '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
values ('mcp修改', @parentId, '3', '#', '', 1, 0, 'F', '0', '0', 'ai:mcp:edit', '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
values ('mcp删除', @parentId, '4', '#', '', 1, 0, 'F', '0', '0', 'ai:mcp:remove', '#', 'admin', sysdate(), '', null,
        '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
values ('mcp导出', @parentId, '5', '#', '', 1, 0, 'F', '0', '0', 'ai:mcp:export', '#', 'admin', sysdate(), '', null,
        '');


drop table if exists agent_mcp_tool_mapping;
create table agent_mcp_tool_mapping
(
    agent_id    bigint unsigned not null,
    mcp_tool_id bigint unsigned not null,
    primary key (agent_id, mcp_tool_id)
) engine = innodb
    comment ='agent_mcp_tool_mapping表';