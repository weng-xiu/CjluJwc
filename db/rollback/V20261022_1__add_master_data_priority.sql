-- =====================================================================================
-- 回滚：V20261022.1 —— 删除主数据裁决优先级配置表 dis_source_priority
-- 对应变更：db/migration/V20261022_1__add_master_data_priority.sql
-- 逆向操作：DROP TABLE IF EXISTS（幂等）
-- 影响表：dis_source_priority（整表删除；配置丢失后裁决回落内存基线，导出留档后再回滚）
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261022.1';
-- =====================================================================================
DROP TABLE IF EXISTS dis_source_priority;
