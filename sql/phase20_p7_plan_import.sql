-- =============================================================
-- phase20：P7 培养方案导入 —— 按钮权限菜单（tpm:plan:import）
-- 菜单 ID 3000（方案按钮段 2113-2117 后紧跟课程 2118 无空隙，改用高位空闲段，全局 max=2992）
-- 幂等：INSERT ... SELECT WHERE NOT EXISTS
-- =============================================================
USE `yu-CjluJwc`;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3000, '方案导入', 2102, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:plan:import', '#', 'admin', sysdate(), 'P7 培养方案Excel导入'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3000);

-- 角色授权：凡已拥有“培养方案”菜单(2102)的角色，自动获得导入按钮
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, 3000 FROM sys_role_menu rm WHERE rm.menu_id = 2102
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = 3000);

SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id = 3000;
