-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI智能体', '2021', '1', 'agent', 'ai/agent/index', 1, 0, 'C', '0', '0', 'ai:agent:list', '#', 'admin', sysdate(), '', null, 'AI智能体菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI智能体查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'ai:agent:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI智能体新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'ai:agent:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI智能体修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'ai:agent:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI智能体删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'ai:agent:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI智能体导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'ai:agent:export',       '#', 'admin', sysdate(), '', null, '');