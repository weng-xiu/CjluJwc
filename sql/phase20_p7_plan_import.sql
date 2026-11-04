-- =============================================================
-- phase20：P7 培养方案导入 —— 按钮权限菜单（tpm:plan:import）
-- 菜单 ID 3000（方案按钮段 2113-2117 后紧跟课程 2118 无空隙，改用高位空闲段，全局 max=2992）
-- 幂等：INSERT ... SELECT WHERE NOT EXISTS
-- 注：G4/V6.5 起不再硬编码 USE `yu-CjluJwc`，库名由调用方指定
--     （bin/db-baseline.ps1 -DbName / CI 的 yu_cjlujwc），
--     否则 Linux 下库名大小写敏感会直接 Unknown database 中断重放。
-- =============================================================

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3000, '方案导入', 2102, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:plan:import', '#', 'admin', sysdate(), 'P7 培养方案Excel导入'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3000);

-- 角色授权：凡已拥有“培养方案”菜单(2102)的角色，自动获得导入按钮
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, 3000 FROM sys_role_menu rm WHERE rm.menu_id = 2102
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = 3000);

SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id = 3000;
