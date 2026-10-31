-- =====================================================================================
-- 回滚：V20261031.4 —— 删除 P-1 余面四张表的 8 条索引
-- 对应变更：db/migration/V20261031_4__add_remaining_hot_indexes.sql
-- 反向操作：DROP INDEX ×8（均按 information_schema 判存在）
-- 影响表：tpm_course_offering / brm_teacher / tpm_schedule_adjustment / sys_user_role
-- 前置条件：无。本变更只加索引、不改数据，随时可回滚；回滚只影响查询性能，不影响正确性。
-- 说明：删除顺序与创建顺序相反，便于按脚本日志对照。
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261031.4';
-- =====================================================================================

-- ---------------------------------------------------------------------------
-- 1) sys_user_role
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'sys_user_role' AND index_name = 'idx_user_role_role');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE `sys_user_role` DROP INDEX `idx_user_role_role`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2) tpm_schedule_adjustment
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'tpm_schedule_adjustment' AND index_name = 'idx_adjust_creator');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE `tpm_schedule_adjustment` DROP INDEX `idx_adjust_creator`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'tpm_schedule_adjustment' AND index_name = 'idx_adjust_appr_status');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE `tpm_schedule_adjustment` DROP INDEX `idx_adjust_appr_status`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3) brm_teacher
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'brm_teacher' AND index_name = 'idx_teacher_dept_status');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE `brm_teacher` DROP INDEX `idx_teacher_dept_status`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'brm_teacher' AND index_name = 'idx_teacher_user');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE `brm_teacher` DROP INDEX `idx_teacher_user`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'brm_teacher' AND index_name = 'idx_teacher_code');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE `brm_teacher` DROP INDEX `idx_teacher_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 4) tpm_course_offering
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'tpm_course_offering' AND index_name = 'idx_offering_teacher');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE `tpm_course_offering` DROP INDEX `idx_offering_teacher`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'tpm_course_offering' AND index_name = 'idx_offering_semester_status');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE `tpm_course_offering` DROP INDEX `idx_offering_semester_status`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 5) 校验：8 条索引都不在了（期望 remaining = 0）
-- ---------------------------------------------------------------------------
SELECT COUNT(DISTINCT index_name) AS remaining
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND index_name IN ('idx_offering_semester_status', 'idx_offering_teacher',
                     'idx_teacher_code', 'idx_teacher_user', 'idx_teacher_dept_status',
                     'idx_adjust_appr_status', 'idx_adjust_creator',
                     'idx_user_role_role');
