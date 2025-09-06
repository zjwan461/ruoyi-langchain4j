-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('知识库', '2021', '1', 'knowledgeBase', 'ai/knowledgeBase/index', 1, 0, 'C', '0', '0', 'ai:knowledgeBase:list', '#', 'admin', sysdate(), '', null, '知识库菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('知识库查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'ai:knowledgeBase:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('知识库新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'ai:knowledgeBase:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('知识库修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'ai:knowledgeBase:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('知识库删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'ai:knowledgeBase:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('知识库导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'ai:knowledgeBase:export',       '#', 'admin', sysdate(), '', null, '');