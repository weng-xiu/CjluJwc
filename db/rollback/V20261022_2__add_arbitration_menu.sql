-- =====================================================================================
-- 回滚：V20261022.2 —— 删除主数据裁决矩阵菜单与按钮权限
-- 对应变更：db/migration/V20261022_2__add_arbitration_menu.sql
-- 逆向操作：DELETE sys_role_menu / sys_menu（幂等）
-- 影响表：sys_menu(2441~2446)、sys_role_menu
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261022.2';
-- =====================================================================================
DELETE FROM sys_role_menu WHERE menu_id BETWEEN 2441 AND 2446;
DELETE FROM sys_menu WHERE menu_id BETWEEN 2441 AND 2446;
