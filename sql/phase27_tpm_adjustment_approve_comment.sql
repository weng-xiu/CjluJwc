-- =============================================================
-- Phase 27：tpm_schedule_adjustment 补齐 approve_comment 列（代码与库表漂移修复）
-- TpmScheduleAdjustmentMapper 的 resultMap/selectVo/update 与
-- 门户调停课移动审批端点（PortalAdjustmentController.approve/reject 写入审批意见）
-- 均引用 tpm_schedule_adjustment.approve_comment，但现库建表脚本从未包含该列，
-- 导致调停课列表/详情/审批 SQL 报 Unknown column 'a.approve_comment'。
-- 与 phase26_sam_student_user_id.sql 同属"存量库结构漂移"修复。
-- 幂等：列已存在则跳过。
-- =============================================================

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tpm_schedule_adjustment' AND COLUMN_NAME = 'approve_comment'
);
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE tpm_schedule_adjustment ADD COLUMN approve_comment varchar(500) NULL DEFAULT NULL COMMENT ''审批意见'' AFTER approve_time',
  'SELECT ''tpm_schedule_adjustment.approve_comment already exists'' AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
