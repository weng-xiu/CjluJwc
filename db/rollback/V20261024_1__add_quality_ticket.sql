-- =====================================================================================
-- 回滚：V20261024.1 —— 删除质量异常工单表 dis_quality_ticket
-- 对应变更：db/migration/V20261024_1__add_quality_ticket.sql
-- 逆向操作：DROP TABLE IF EXISTS（幂等）
-- 影响表：dis_quality_ticket（整表删除；工单处置记录不可恢复，导出留档后再回滚）
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261024.1';
-- =====================================================================================
DROP TABLE IF EXISTS dis_quality_ticket;
