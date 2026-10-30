-- =====================================================================================
-- 回滚：V20261030.1 —— 删除开课表「已占名额」列 enrolled_count
-- 对应变更：db/migration/V20261030_1__add_offering_enrolled_count.sql
-- 反向操作：DROP COLUMN（幂等，由 information_schema 判定列是否存在）
-- 影响表：tpm_course_offering（仅删除本批新增列；报名数据不受影响）
-- 前置条件：**必须先回滚应用版本**——新代码的 occupySeat/releaseSeat 会引用该列，
--           先删列会让所有选课/退课语句直接报 Unknown column。
-- 说明：删列后容量真相回到 Redis DECR/INCR + 现算回填（旧实现），不丢数据但会恢复旧的漂移面。
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261030.1';
-- =====================================================================================

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tpm_course_offering' AND column_name = 'enrolled_count');
SET @sql = IF(@col_exists = 1,
    'ALTER TABLE `tpm_course_offering` DROP COLUMN `enrolled_count`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
