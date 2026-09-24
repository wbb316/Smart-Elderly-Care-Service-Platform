-- 业务模块权限点 + 工作流角色 补建脚本（幂等）
-- 日期: 2026-08-16  目标库: ry-vue
-- 背景: 后端 68 个 code:* 权限点与退住工作流 6 个候选组角色在库中均不存在

-- ==================== 1. 工作流角色 ====================
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT 101, '护理员', 'nurse', 10, '1', 1, 1, '0', '0', 'admin', NOW(), '退住审批工作流候选组'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'nurse');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT 102, '护理组长', 'nurse_leader', 11, '1', 1, 1, '0', '0', 'admin', NOW(), '退住审批工作流候选组'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'nurse_leader');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT 103, '法务专员', 'legal_staff', 12, '1', 1, 1, '0', '0', 'admin', NOW(), '退住审批工作流候选组'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'legal_staff');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT 104, '结算员', 'settleman_staff', 13, '1', 1, 1, '0', '0', 'admin', NOW(), '退住审批工作流候选组'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'settleman_staff');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT 105, '结算组长', 'settleman_leader', 14, '1', 1, 1, '0', '0', 'admin', NOW(), '退住审批工作流候选组'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'settleman_leader');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT 106, '副院长', 'vice_dean', 15, '1', 1, 1, '0', '0', 'admin', NOW(), '退住审批工作流候选组'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'vice_dean');

-- ==================== 2. 权限点容器（隐藏目录，不生成侧边栏）====================
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2100, '业务管理', 0, 10, 'code', NULL, NULL, '', 1, 0, 'M', '1', '0', NULL, 'documentation', 'admin', NOW(), '业务模块权限点容器（隐藏）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2100);

-- ==================== 3. 68 个按钮型权限点（F 型不参与路由生成）====================
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2101, '来访管理-新增', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:arrival:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:arrival:add');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2102, '来访管理-修改', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:arrival:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:arrival:edit');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2103, '来访管理-导出', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:arrival:export', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:arrival:export');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2104, '来访管理-列表', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:arrival:list', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:arrival:list');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2105, '来访管理-查询', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:arrival:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:arrival:query');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2106, '来访管理-删除', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:arrival:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:arrival:remove');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2107, '余额管理-新增', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:balance:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:balance:add');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2108, '余额管理-修改', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:balance:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:balance:edit');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2109, '余额管理-导出', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:balance:export', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:balance:export');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2110, '余额管理-列表', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:balance:list', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:balance:list');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2111, '余额管理-查询', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:balance:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:balance:query');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2112, '余额管理-删除', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:balance:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:balance:remove');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2113, '账单管理-新增', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:bill:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:bill:add');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2114, '账单管理-修改', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:bill:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:bill:edit');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2115, '账单管理-导出', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:bill:export', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:bill:export');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2116, '账单管理-列表', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:bill:list', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:bill:list');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2117, '账单管理-查询', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:bill:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:bill:query');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2118, '账单管理-删除', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:bill:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:bill:remove');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2119, '退住管理-新增', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:checkout:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:checkout:add');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2120, '退住管理-修改', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:checkout:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:checkout:edit');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2121, '退住管理-导出', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:checkout:export', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:checkout:export');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2122, '退住管理-列表', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:checkout:list', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:checkout:list');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2123, '退住管理-查询', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:checkout:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:checkout:query');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2124, '退住管理-删除', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:checkout:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:checkout:remove');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2125, '经营看板-统计', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:index:stat', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:index:stat');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2126, '服务项目-新增', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:item:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:item:add');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2127, '服务项目-修改', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:item:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:item:edit');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2128, '服务项目-导出', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:item:export', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:item:export');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2129, '服务项目-列表', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:item:list', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:item:list');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2130, '服务项目-查询', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:item:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:item:query');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2131, '服务项目-删除', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:item:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:item:remove');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2132, '请假管理-新增', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:leave:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:leave:add');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2133, '请假管理-审批', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:leave:approve', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:leave:approve');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2134, '请假管理-修改', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:leave:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:leave:edit');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2135, '请假管理-导出', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:leave:export', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:leave:export');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2136, '请假管理-列表', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:leave:list', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:leave:list');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2137, '请假管理-查询', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:leave:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:leave:query');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2138, '请假管理-删除', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:leave:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:leave:remove');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2139, '请假管理-重新提交', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:leave:resubmit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:leave:resubmit');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2140, '请假管理-提交', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:leave:submit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:leave:submit');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2141, '订单管理-新增', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:orders:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:orders:add');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2142, '订单管理-修改', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:orders:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:orders:edit');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2143, '订单管理-导出', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:orders:export', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:orders:export');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2144, '订单管理-列表', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:orders:list', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:orders:list');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2145, '订单管理-查询', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:orders:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:orders:query');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2146, '订单管理-删除', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:orders:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:orders:remove');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2147, '支付管理-新增', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:payment:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:payment:add');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2148, '支付管理-修改', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:payment:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:payment:edit');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2149, '支付管理-导出', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:payment:export', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:payment:export');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2150, '支付管理-列表', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:payment:list', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:payment:list');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2151, '支付管理-查询', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:payment:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:payment:query');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2152, '支付管理-删除', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:payment:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:payment:remove');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2153, '充值管理-新增', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:recharge:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:recharge:add');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2154, '充值管理-修改', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:recharge:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:recharge:edit');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2155, '充值管理-导出', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:recharge:export', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:recharge:export');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2156, '充值管理-列表', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:recharge:list', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:recharge:list');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2157, '充值管理-查询', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:recharge:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:recharge:query');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2158, '充值管理-删除', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:recharge:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:recharge:remove');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2159, '退款管理-修改', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:refund:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:refund:edit');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2160, '退款管理-导出', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:refund:export', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:refund:export');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2161, '退款管理-列表', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:refund:list', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:refund:list');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2162, '退款管理-查询', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:refund:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:refund:query');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2163, '预约管理-新增', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:reservation:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:reservation:add');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2164, '预约管理-修改', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:reservation:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:reservation:edit');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2165, '预约管理-导出', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:reservation:export', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:reservation:export');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2166, '预约管理-列表', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:reservation:list', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:reservation:list');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2167, '预约管理-查询', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:reservation:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:reservation:query');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2168, '预约管理-删除', 2100, 0, '#', NULL, NULL, '', 1, 0, 'F', '0', '0', 'code:reservation:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'code:reservation:remove');

-- ==================== 4. 授权（INSERT IGNORE 幂等）====================

-- admin (role_id=1) : 68 个
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 2101),
(1, 2102),
(1, 2103),
(1, 2104),
(1, 2105),
(1, 2106),
(1, 2107),
(1, 2108),
(1, 2109),
(1, 2110),
(1, 2111),
(1, 2112),
(1, 2113),
(1, 2114),
(1, 2115),
(1, 2116),
(1, 2117),
(1, 2118),
(1, 2119),
(1, 2120),
(1, 2121),
(1, 2122),
(1, 2123),
(1, 2124),
(1, 2125),
(1, 2126),
(1, 2127),
(1, 2128),
(1, 2129),
(1, 2130),
(1, 2131),
(1, 2132),
(1, 2133),
(1, 2134),
(1, 2135),
(1, 2136),
(1, 2137),
(1, 2138),
(1, 2139),
(1, 2140),
(1, 2141),
(1, 2142),
(1, 2143),
(1, 2144),
(1, 2145),
(1, 2146),
(1, 2147),
(1, 2148),
(1, 2149),
(1, 2150),
(1, 2151),
(1, 2152),
(1, 2153),
(1, 2154),
(1, 2155),
(1, 2156),
(1, 2157),
(1, 2158),
(1, 2159),
(1, 2160),
(1, 2161),
(1, 2162),
(1, 2163),
(1, 2164),
(1, 2165),
(1, 2166),
(1, 2167),
(1, 2168);

-- nurse (role_id=101) : 13 个
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(101, 2122),
(101, 2123),
(101, 2119),
(101, 2120),
(101, 2144),
(101, 2145),
(101, 2129),
(101, 2130),
(101, 2104),
(101, 2105),
(101, 2136),
(101, 2137),
(101, 2140);

-- nurse_leader (role_id=102) : 10 个
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(102, 2122),
(102, 2123),
(102, 2120),
(102, 2121),
(102, 2136),
(102, 2137),
(102, 2133),
(102, 2135),
(102, 2129),
(102, 2130);

-- legal_staff (role_id=103) : 3 个
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(103, 2122),
(103, 2123),
(103, 2120);

-- settleman_staff (role_id=104) : 8 个
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(104, 2122),
(104, 2123),
(104, 2120),
(104, 2116),
(104, 2117),
(104, 2114),
(104, 2150),
(104, 2151);

-- settleman_leader (role_id=105) : 14 个
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(105, 2122),
(105, 2123),
(105, 2120),
(105, 2121),
(105, 2116),
(105, 2117),
(105, 2114),
(105, 2150),
(105, 2151),
(105, 2110),
(105, 2111),
(105, 2108),
(105, 2156),
(105, 2157);

-- vice_dean (role_id=106) : 4 个
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(106, 2122),
(106, 2123),
(106, 2121),
(106, 2125);

-- ==================== 5. 回退语句 ====================
-- DELETE FROM sys_role_menu WHERE menu_id BETWEEN 2100 AND 2168;
-- DELETE FROM sys_menu WHERE menu_id BETWEEN 2100 AND 2168;
-- DELETE FROM sys_role WHERE role_key IN ('nurse', 'nurse_leader', 'legal_staff', 'settleman_staff', 'settleman_leader', 'vice_dean');
