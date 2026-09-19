-- ============================================================
-- Phase 10: S1 毕业/学位自动审核接线 - 按钮权限登记
-- 幂等脚本，可重复执行
-- 背景：后端 SamGraduationReviewController / SamDegreeReviewController
--       的 autoReview/batchReview 端点使用 sam:graduationReview:audit
--       与 sam:degreeReview:audit 权限，但菜单库中未登记，
--       导致前端按钮无法授权。本脚本补齐按钮权限并授予 admin(role_id=1)。
-- ============================================================

-- 1. 按钮权限 - 毕业自动审核 (parent=2306 毕业审核)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2766, '毕业自动审核', 2306, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'sam:graduationReview:audit', '#', 'admin', NOW(), '', NULL, 'S1 单人/批量自动审核'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2766);

-- 2. 按钮权限 - 学位自动审核 (parent=2307 学位审核)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2767, '学位自动审核', 2307, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'sam:degreeReview:audit', '#', 'admin', NOW(), '', NULL, 'S1 单人/批量自动审核'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2767);

-- 兜底：若上述 menu_id 已被占用但 perms 未登记，则按 perms 补插（避免主键冲突时静默丢失）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '毕业自动审核', 2306, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'sam:graduationReview:audit', '#', 'admin', NOW(), '', NULL, 'S1 单人/批量自动审核'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'sam:graduationReview:audit')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2766);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '学位自动审核', 2307, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'sam:degreeReview:audit', '#', 'admin', NOW(), '', NULL, 'S1 单人/批量自动审核'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'sam:degreeReview:audit')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2767);

-- 3. 角色菜单关联：授予 admin(role_id=1) 及 所有已拥有毕业/学位审核列表权限的角色
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, 2766 FROM sys_role_menu rm
WHERE rm.menu_id = 2306
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = 2766);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, 2767 FROM sys_role_menu rm
WHERE rm.menu_id = 2307
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = 2767);

-- 确保 admin(role_id=1) 一定拥有（若其未拥有列表菜单也补上）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2766 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2766);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2767 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2767);

-- 完成
SELECT 'Phase 10 (S1 audit perms) migration completed successfully' AS result;
