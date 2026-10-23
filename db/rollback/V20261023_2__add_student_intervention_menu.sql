-- =====================================================================================
-- 回滚：V20261023.2 —— 删除 F4 学生数字档案与干预闭环按钮权限
-- 对应变更：db/migration/V20261023_2__add_student_intervention_menu.sql
-- 逆向操作：先删 sys_role_menu 授权，再删 sys_menu 3262~3268
-- 影响表：sys_role_menu、sys_menu
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261023.2';
-- =====================================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 3262 AND 3268;
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 3262 AND 3268;
