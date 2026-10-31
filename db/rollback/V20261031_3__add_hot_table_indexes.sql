-- =====================================================================================
-- 回滚：V20261031.3 —— 删除四张热点表的 8 条组合索引
-- 对应变更：db/migration/V20261031_3__add_hot_table_indexes.sql
-- 反向操作：DROP INDEX ×8（均按 information_schema 判存在）
-- 影响表：tpm_selection_enrollment / tpm_schedule / aem_grade_record / sys_user
-- 前置条件：无。本变更只加索引、不改数据，随时可回滚；回滚只影响查询性能，不影响正确性。
-- 说明：删除顺序与创建顺序相反，便于按脚本日志对照。
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261031.3';
-- =====================================================================================

-- ---------------------------------------------------------------------------
-- 1) sys_user
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND index_name = 'idx_user_dept_status');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE `sys_user` DROP INDEX `idx_user_dept_status`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND index_name = 'idx_user_name');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE `sys_user` DROP INDEX `idx_user_name`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2) aem_grade_record
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'aem_grade_record' AND index_name = 'idx_grade_semester_course');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE `aem_grade_record` DROP INDEX `idx_grade_semester_course`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'aem_grade_record' AND index_name = 'idx_grade_student_semester');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE `aem_grade_record` DROP INDEX `idx_grade_student_semester`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3) tpm_schedule
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'tpm_schedule' AND index_name = 'idx_schedule_classroom');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE `tpm_schedule` DROP INDEX `idx_schedule_classroom`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'tpm_schedule' AND index_name = 'idx_schedule_offering');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE `tpm_schedule` DROP INDEX `idx_schedule_offering`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 4) tpm_selection_enrollment（uk_enroll_active 属 V20261031.1，本脚本不动它）
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'tpm_selection_enrollment' AND index_name = 'idx_enroll_student');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE `tpm_selection_enrollment` DROP INDEX `idx_enroll_student`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'tpm_selection_enrollment' AND index_name = 'idx_enroll_offering');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE `tpm_selection_enrollment` DROP INDEX `idx_enroll_offering`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 5) 校验：8 条索引都不在了（期望 remaining = 0）
-- ---------------------------------------------------------------------------
SELECT COUNT(DISTINCT index_name) AS remaining
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND index_name IN ('idx_enroll_offering', 'idx_enroll_student',
                     'idx_schedule_offering', 'idx_schedule_classroom',
                     'idx_grade_student_semester', 'idx_grade_semester_course',
                     'idx_user_name', 'idx_user_dept_status');
