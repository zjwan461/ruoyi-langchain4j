-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('模型管理', '2021', '1', 'model', 'ai/model/index', 1, 0, 'C', '0', '0', 'ai:model:list', '#', 'admin', sysdate(), '', null, '模型管理菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('模型管理查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'ai:model:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('模型管理新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'ai:model:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('模型管理修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'ai:model:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('模型管理删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'ai:model:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('模型管理导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'ai:model:export',       '#', 'admin', sysdate(), '', null, '');