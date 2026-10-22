-- =====================================================================================
-- 回滚：V20261021.1 —— 删除 OBE 持续改进台账表 sam_obe_improvement
-- 对应变更：db/migration/V20261021_1__add_obe_improvement_ledger.sql
-- 逆向操作：DROP TABLE IF EXISTS（幂等）
-- 影响表：sam_obe_improvement（整表删除；已登记的改进台账随之丢失，导出留档后再回滚）
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261021.1';
-- =====================================================================================
DROP TABLE IF EXISTS sam_obe_improvement;
