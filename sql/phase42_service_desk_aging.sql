-- =============================================================
-- phase42：F6 服务大厅时效看板（前端化收口）
-- 优化计划 V5.x：N3 一站式服务大厅后端（ServiceDeskController catalog/dashboard/urge）早已交付，
-- 但双端前端零消费（grep serviceDesk 0 命中），"时效运营"能力不可见。本脚本登记管理端菜单与权限点。
-- 前端页面：system/serviceDesk/index（Vue2 yu-ui + Vue3 yu-ui-vue3 双栈一致）
-- 菜单挂载：2760 消息中心（统一待办目录）下，与"我的待办"(2762) 同级——看板即对 sys_todo 的聚合运营
-- menu_id：3310/3311（3000+ 保留段；3300 数据变更审计、3100 待办协同已占用，经全仓 grep 核对 33xx 空闲）
-- 授权：凡拥有"我的待办"(2762) 的角色同步授予；admin(1) 兜底
-- 幂等：WHERE NOT EXISTS，可重复执行
-- =============================================================

-- 1. C 型页面菜单：服务大厅时效看板（查看权限 system:serviceDesk:view 与后端 @PreAuthorize 一致）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3310, '服务大厅时效看板', 2760, 3, 'serviceDesk', 'system/serviceDesk/index', '', '', 1, 0, 'C', '0', '0', 'system:serviceDesk:view', 'chart', 'admin', sysdate(), '', null, 'F6 服务事项办理时效分布/超时率看板与一键催办'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3310 OR perms = 'system:serviceDesk:view');

-- 2. F 型按钮权限：超时催办（POST /system/serviceDesk/urge）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3311, '超时催办', 3310, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:serviceDesk:urge', '#', 'admin', sysdate(), '', null, '向全部超期待办承办人下发催办提醒（复用 A2 消息事件通道）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3311);

-- 3. 角色授权：凡拥有"我的待办"(2762) 的角色，同步授予看板与催办（复用既有待办授权面）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, m.menu_id
FROM sys_role_menu rm
CROSS JOIN (SELECT 3310 AS menu_id UNION ALL SELECT 3311) m
WHERE rm.menu_id = 2762
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = m.menu_id);

-- 4. 兜底：admin(1) 显式登记
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM (SELECT 3310 AS menu_id UNION ALL SELECT 3311) m
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = 1 AND x.menu_id = m.menu_id);

-- 5. 验证
SELECT 'F6服务大厅看板菜单' AS item, COUNT(1) AS cnt FROM sys_menu WHERE menu_id IN (3310, 3311)
UNION ALL SELECT '已授权角色数', COUNT(DISTINCT role_id) FROM sys_role_menu WHERE menu_id IN (3310, 3311);
