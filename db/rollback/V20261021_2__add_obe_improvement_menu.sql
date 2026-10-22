-- =====================================================================================
-- 回滚：V20261021.2 —— 删除 OBE 持续改进台账按钮权限
-- 对应变更：db/migration/V20261021_2__add_obe_improvement_menu.sql
-- 逆向操作：DELETE sys_role_menu / sys_menu（幂等）
-- 影响表：sys_menu(3252~3257)、sys_role_menu
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261021.2';
-- =====================================================================================
DELETE FROM sys_role_menu WHERE menu_id BETWEEN 3252 AND 3257;
DELETE FROM sys_menu WHERE menu_id BETWEEN 3252 AND 3257;
