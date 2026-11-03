-- =====================================================================================
-- 回滚：V20261101.1 —— 删除门户调停课审批按钮权限
-- 对应变更：db/migration/V20261101_1__add_portal_adjustment_approve_menu.sql
-- 逆向操作：DELETE sys_role_menu / sys_menu（幂等）
-- 影响表：sys_menu(3500)、sys_role_menu
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261101.1';
-- =====================================================================================
DELETE FROM sys_role_menu WHERE menu_id = 3500;
DELETE FROM sys_menu WHERE menu_id = 3500;
