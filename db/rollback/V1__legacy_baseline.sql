-- =====================================================================================
-- 回滚：V1 —— 基线占位脚本
-- 对应变更：db/migration/V1__legacy_baseline.sql
-- 逆向操作：无结构/数据变更需要回滚（V1 本身不产生副作用），仅清理版本记录。
-- 幂等性：是
-- =====================================================================================

DELETE FROM flyway_schema_history WHERE version = '1';

-- 校验：应返回 0 行
SELECT COUNT(*) AS remaining FROM flyway_schema_history WHERE version = '1';
