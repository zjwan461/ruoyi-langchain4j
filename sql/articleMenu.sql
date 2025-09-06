-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('论坛帖子', '2000', '1', 'article', 'forum/article/index', 1, 0, 'C', '0', '0', 'forum:article:list', '#', 'admin', sysdate(), '', null, '论坛帖子菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('论坛帖子查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'forum:article:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('论坛帖子新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'forum:article:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('论坛帖子修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'forum:article:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('论坛帖子删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'forum:article:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('论坛帖子导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'forum:article:export',       '#', 'admin', sysdate(), '', null, '');