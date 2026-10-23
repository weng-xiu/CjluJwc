-- =====================================================================================
-- 回滚：V20261023.1 —— 删除学生干预留痕表 sam_student_intervention
-- 对应变更：db/migration/V20261023_1__add_student_intervention.sql
-- 逆向操作：DROP TABLE IF EXISTS（幂等）
-- 影响表：sam_student_intervention（整表删除；干预留痕数据不可恢复，导出留档后再回滚）
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261023.1';
-- =====================================================================================
DROP TABLE IF EXISTS sam_student_intervention;
