drop table if exists agent_flow;
create table agent_flow
(
    id              bigint unsigned not null auto_increment comment '主键ID',
    agent_flow_name varchar(255)    not null comment 'agent flow名称',
    flow_json       longtext        null comment 'agent flow的json',
    visit_url       varchar(255)    not null comment '访问url',
    status          varchar(255)    not null comment '状态',
    create_time     DATETIME        NULL DEFAULT NULL COMMENT '创建时间',
    update_time     datetime        null default null comment '更新时间',
    create_by       varchar(64)          default '' comment '创建者',
    update_by       varchar(64)          default '' comment '更新者',
    primary key (id)
) engine = innodb
    comment ='agent_flow';

-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
values ('Agent Flow', '2000', '1', 'agent_flow', 'ai/agent_flow/index', 1, 0, 'C', '0', '0', 'ai:agent_flow:list',
        'tree-table', 'admin', sysdate(), '', null, 'agent_flow菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
values ('agent_flow查询', @parentId, '1', '#', '', 1, 0, 'F', '0', '0', 'ai:agent_flow:query', '#', 'admin', sysdate(),
        '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
values ('agent_flow新增', @parentId, '2', '#', '', 1, 0, 'F', '0', '0', 'ai:agent_flow:add', '#', 'admin', sysdate(),
        '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
values ('agent_flow修改', @parentId, '3', '#', '', 1, 0, 'F', '0', '0', 'ai:agent_flow:edit', '#', 'admin', sysdate(),
        '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
values ('agent_flow删除', @parentId, '4', '#', '', 1, 0, 'F', '0', '0', 'ai:agent_flow:remove', '#', 'admin', sysdate(),
        '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status,
                      perms, icon, create_by, create_time, update_by, update_time, remark)
values ('agent_flow导出', @parentId, '5', '#', '', 1, 0, 'F', '0', '0', 'ai:agent_flow:export', '#', 'admin', sysdate(),
        '', null, '');

ALTER TABLE ai_agent
    ADD COLUMN description longtext NULL COMMENT 'Agent描述';

update sys_menu set menu_name='Gitee地址', path = 'https://gitee.com/zjwan461/ruoyi-langchain4j' where menu_id = 4;