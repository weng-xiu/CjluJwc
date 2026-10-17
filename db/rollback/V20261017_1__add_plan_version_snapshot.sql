-- =====================================================================================
-- 回滚：V20261017.1 —— 删除人才培养方案版本快照表 tpm_plan_snapshot
-- 对应变更：db/migration/V20261017_1__add_plan_version_snapshot.sql
-- 逆向操作：DROP TABLE IF EXISTS（幂等）
-- 影响表：tpm_plan_snapshot（整表删除；已生成的历史版本快照随之丢失，按版审核回退为仅比对实时方案）
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261017.1';
-- =====================================================================================
DROP TABLE IF EXISTS tpm_plan_snapshot;
