drop table if exists agent_skill;
create table agent_skill
(
    id                bigint unsigned not null auto_increment comment '主键ID',
    skill_name        varchar(255)    not null comment 'skill名称',
    skill_description varchar(500)    not null comment 'skill描述',
    skill_content     longtext        not null comment 'skill内容',
    create_time       DATETIME        NULL DEFAULT NULL COMMENT '创建时间',
    update_time       datetime        null default null comment '更新时间',
    create_by         varchar(64)          default '' comment '创建者',
    update_by         varchar(64)          default '' comment '更新者',
    primary key (id)
) engine = innodb
    comment ='agent_skill表';

-- 创建唯一索引
CREATE UNIQUE INDEX uk_agent_skill_skill_name ON agent_skill(skill_name);

-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('Skill', '2000', '1', 'skill', 'ai/skill/index', 1, 0, 'C', '0', '0', 'ai:skill:list', 'rate', 'admin', sysdate(), '', null, 'agent_skill菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('agent_skill查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'ai:skill:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('agent_skill新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'ai:skill:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('agent_skill修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'ai:skill:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('agent_skill删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'ai:skill:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('agent_skill导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'ai:skill:export',       '#', 'admin', sysdate(), '', null, '');