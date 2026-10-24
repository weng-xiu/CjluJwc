-- =====================================================================================
-- 回滚：V20261024.3 —— 删除 F3 专业质量档案菜单与按钮权限
-- 对应变更：db/migration/V20261024_3__add_program_quality_menu.sql
-- 逆向操作：先删 sys_role_menu 授权，再删 sys_menu 3280~3282
-- 影响表：sys_role_menu、sys_menu
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261024.3';
-- =====================================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 3280 AND 3282;
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 3280 AND 3282;
