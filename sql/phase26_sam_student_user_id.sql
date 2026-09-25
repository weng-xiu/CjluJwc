-- =============================================================
-- Phase 26：sam_student 补齐 user_id 列（代码与库表结构漂移修复）
-- SamStudentMapper 的 resultMap/list/insert/update 与
-- selectSamStudentByUserId（P6 门户本人解析主路径）均引用
-- sam_student.user_id，但现库建表脚本从未包含该列，
-- 导致门户 /portal/studentStatus/info 及学籍列表 SQL 报错。
-- 幂等：列已存在则跳过。
-- =============================================================

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sam_student' AND COLUMN_NAME = 'user_id'
);
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE sam_student ADD COLUMN user_id bigint NULL DEFAULT NULL COMMENT ''关联系统用户sys_user.user_id（门户本人解析）'' AFTER major_id, ADD INDEX idx_sam_student_user_id (user_id)',
  'SELECT ''sam_student.user_id already exists'' AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
