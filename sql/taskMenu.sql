-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('需求任务', '2013', '1', 'task', 'requirement/task/index', 1, 0, 'C', '0', '0', 'requirement:task:list', '#', 'admin', sysdate(), '', null, '需求任务菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('需求任务查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'requirement:task:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('需求任务新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'requirement:task:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('需求任务修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'requirement:task:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('需求任务删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'requirement:task:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('需求任务导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'requirement:task:export',       '#', 'admin', sysdate(), '', null, '');