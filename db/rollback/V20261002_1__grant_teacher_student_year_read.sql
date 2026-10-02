-- =====================================================================================
-- 回滚：V20261002.1 —— 撤销「教师 / 学生」角色的学年/学期只读授权
-- 对应变更：db/migration/V20261002_1__grant_teacher_student_year_read.sql
-- 逆向操作：删除 sys_role_menu 中 role_id IN (6,7) × menu_id IN (2002,2019,2003,2024) 的关联行
-- 影响表：sys_role_menu（至多删除 8 行；这些行由本变更引入，不存在历史数据误删风险）
-- 回滚后果：门户教师/学生端页面的学年/学期筛选恢复 403（回到变更前的缺陷状态）
-- 幂等性：是（重复执行删除 0 行不报错）
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261002.1';
--            否则 Flyway 仍认为该版本已应用，下次 migrate 不会重放。
-- =====================================================================================

DELETE FROM sys_role_menu
 WHERE role_id IN (6, 7)
   AND menu_id IN (2002, 2019, 2003, 2024);

-- 校验：应返回 0 行
SELECT COUNT(*) AS remaining FROM sys_role_menu
 WHERE role_id IN (6, 7) AND menu_id IN (2002, 2019, 2003, 2024);
