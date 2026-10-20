-- =============================================================
-- phase41：V4 成绩录入矩阵（管理端化）
-- 优化计划 V5.x 中期项 V4：Excel 式批量成绩录入矩阵
-- 前端页面：aem/gradeEntryMatrix/index（Vue2 yu-ui + Vue3 yu-ui-vue3 双栈一致）
-- 组合现有端点（list/update/submit/entryWindow），不改后端，复用 aem:gradeRecord:* 权限
-- 幂等：WHERE NOT EXISTS，可重复执行
-- =============================================================

-- 1. 菜单：成绩录入矩阵（挂在 2205 成绩管理 下，order_num=9 避开 2206/2920/3210）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3270, '成绩录入矩阵', 2205, 9, 'gradeEntryMatrix', 'aem/gradeEntryMatrix/index', '', '', 1, 0, 'C', '0', '0', 'aem:gradeRecord:list', 'edit', 'admin', sysdate(), '', null, 'V4 Excel 式批量成绩录入矩阵（行=学生，编辑平时/考试，批量保存/提交）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3270);

-- 2. 角色授权：凡是拥有 2206（成绩记录）菜单的角色，同步授予 3270 成绩录入矩阵
--    （页面内部使用 list/edit/submit 权限，均由 2206 现有按钮权限覆盖，无需新增按钮）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, 3270
FROM sys_role_menu rm
WHERE rm.menu_id = 2206
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = 3270);

-- 3. 验证
SELECT 'V4成绩录入矩阵菜单' AS item, COUNT(1) AS cnt FROM sys_menu WHERE menu_id = 3270
UNION ALL SELECT '已授权角色数', COUNT(1) FROM sys_role_menu WHERE menu_id = 3270;
