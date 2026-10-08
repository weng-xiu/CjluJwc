-- =====================================================================================
-- 版本：V20261007.3
-- 标题：登记「数据分类分级台账」管理端菜单与权限（K1 等保数据新规 · 前端闭环）
-- 目的：V20261007.2 已建 sys_data_class 表与三字典、后端 /system/dataClass 全 CRUD+导出、
--       Vue3 页 views/system/dataClass/index 已就绪；本脚本登记对应 sys_menu 节点与权限点，
--       使台账可在管理端可视化维护并导出，形成"从查系统转向查数据"的合规闭环。
-- 做法：C 型菜单挂「系统管理」(parent_id=1)，紧随 AI问答分析(order=14) 之后取 order_num=15；
--       另登记 5 个 F 型按钮权限（查询/新增/修改/删除/导出），与后端 @PreAuthorize 端点一一对应，
--       不留死权限、不缺按钮。menu_id 取 db/README 规定的 3000+ 新增保留段中未占用的 3400~3405
--       （3000~3211 既有、3300 数据变更审计、3160/3170 AI 已占用，3400 段经核对空闲）。
-- 授权：默认仅显式授予 admin(role_id=1)；其他角色由超级管理员在「角色管理-菜单权限」按需分配，
--       避免将数据登记类写权限过度下发给只读审计角色。
-- 影响表：sys_menu（新增至多 6 行）、sys_role_menu（新增至多 6 行）
-- 关联事项：V4.x §6.2 K1；前端 yu-ui-vue3 views/system/dataClass/index.vue
-- 幂等性：是（INSERT ... SELECT WHERE NOT EXISTS 按 menu_id 与 perms 双判重；role_menu 亦 NOT EXISTS 兜底）
-- 回滚脚本：db/rollback/V20261007_3__add_data_class_menu.sql
-- 执行说明：本机存量库按既有约定「人工应用 + 带回滚脚本」，FLYWAY_ENABLED 默认 false。
-- =====================================================================================

-- 1. C 型页面菜单：数据分类分级台账（parent=1 系统管理）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3400, '数据分类分级', 1, 15, 'dataClass', 'system/dataClass/index', '', '', 1, 0, 'C', '0', '0', 'system:dataClass:list', 'list', 'admin', NOW(), '', NULL, 'K1 等保数据新规·数据资产分类分级台账'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3400 OR perms = 'system:dataClass:list');

-- 2. F 型按钮权限（与 /system/dataClass 各端点一一对应）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3401, '台账查询', 3400, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dataClass:query', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3401 OR perms = 'system:dataClass:query');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3402, '台账新增', 3400, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dataClass:add', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3402 OR perms = 'system:dataClass:add');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3403, '台账修改', 3400, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dataClass:edit', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3403 OR perms = 'system:dataClass:edit');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3404, '台账删除', 3400, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dataClass:remove', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3404 OR perms = 'system:dataClass:remove');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3405, '台账导出', 3400, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dataClass:export', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3405 OR perms = 'system:dataClass:export');

-- 3. 角色菜单关联：默认仅授予 admin(role_id=1) 全套（C + 5 按钮）；其他角色按需经角色管理分配
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM (
  SELECT 3400 AS menu_id UNION ALL SELECT 3401 UNION ALL SELECT 3402
  UNION ALL SELECT 3403 UNION ALL SELECT 3404 UNION ALL SELECT 3405
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = 1 AND x.menu_id = m.menu_id);

-- 校验：菜单 6 行 + admin 关联 6 行
SELECT menu_id, menu_name, parent_id, menu_type, path, component, perms FROM sys_menu WHERE menu_id BETWEEN 3400 AND 3405 ORDER BY menu_id;
SELECT rm.role_id, rm.menu_id FROM sys_role_menu rm WHERE rm.menu_id BETWEEN 3400 AND 3405 ORDER BY rm.menu_id;
