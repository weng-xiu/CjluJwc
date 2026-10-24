-- =====================================================================================
-- 回滚：V20261024.2 —— 删除 F5 质量异常工单菜单与按钮权限
-- 对应变更：db/migration/V20261024_2__add_quality_ticket_menu.sql
-- 逆向操作：先删 sys_role_menu 授权，再删 sys_menu 2447~2453
-- 影响表：sys_role_menu、sys_menu
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261024.2';
-- =====================================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 2447 AND 2453;
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 2447 AND 2453;
