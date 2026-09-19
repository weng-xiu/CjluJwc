-- ===============================================
-- phase13：P7 基础数据导入 —— 课程库导入按钮权限
-- 本脚本幂等，可重复执行
-- 关联需求：长江大学教务管理系统需求优化计划 P7
-- ===============================================

-- ----------------------------
-- 1. 按钮权限：课程导入（挂在 2103 课程库 下，order_num=6，ID=2123 未占用）
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2123, '课程导入', 2103, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:course:import', '#', 'admin', sysdate(), '', null, '课程库导入按钮（P7）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2123);

-- ----------------------------
-- 2. 角色授权：凡是拥有 2122（课程导出）按钮的角色，同步授予 2123（课程导入）
-- ----------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, 2123
FROM sys_role_menu rm
WHERE rm.menu_id = 2122
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = 2123);

-- ----------------------------
-- 3. 验证
-- ----------------------------
SELECT menu_id, menu_name, parent_id, perms FROM sys_menu WHERE menu_id = 2123;
