-- =====================================================================================
-- 回滚：V20261004.1 —— 删除「字段级数据变更流水」表 data_change_audit
-- 对应变更：db/migration/V20261004_1__add_data_change_audit.sql
-- 逆向操作：DROP TABLE IF EXISTS data_change_audit
-- 影响：审计流水将全部丢失；成绩等实体的字段级修改历史不可再查询（回到变更前状态）。
--       业务主表（aem_grade_record 等）不受影响——本表为纯新增旁路表，无外键约束指向它。
-- 幂等性：是（DROP TABLE IF EXISTS；重复执行不报错）
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261004.1';
--            否则 Flyway 仍认为该版本已应用，下次 migrate 不会重放。
-- =====================================================================================

DROP TABLE IF EXISTS data_change_audit;

-- 校验：应返回 0
SELECT COUNT(*) AS tbl_exists FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name = 'data_change_audit';
