-- ===============================================
-- D2 同步可靠性增强 数据库升级脚本（幂等）
-- 内容：dis_sync_task 增列 sync_mode/last_watermark（增量水位）；
--       dis_data_exchange_log 增列 sync_batch_no/retry_flag（批次留痕+人工重推标记）；
--       新增"任务重推"按钮权限（menu_id=2440）并授权超级管理员
-- 可重复执行：列/索引用 information_schema + PREPARE；菜单用 INSERT ... WHERE NOT EXISTS
-- ===============================================

-- ----------------------------
-- 1. dis_sync_task 增列：sync_mode（0全量 1增量）、last_watermark（增量水位）
-- ----------------------------
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dis_sync_task' AND column_name = 'sync_mode');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `dis_sync_task` ADD COLUMN `sync_mode` char(1) DEFAULT ''0'' COMMENT ''同步模式（0全量 1增量）'' AFTER `fail_count`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dis_sync_task' AND column_name = 'last_watermark');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `dis_sync_task` ADD COLUMN `last_watermark` datetime DEFAULT NULL COMMENT ''增量水位（上次成功同步的数据时间点）'' AFTER `sync_mode`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 2. dis_data_exchange_log 增列：sync_batch_no（批次号）、retry_flag（人工重推标记）
-- ----------------------------
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dis_data_exchange_log' AND column_name = 'sync_batch_no');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `dis_data_exchange_log` ADD COLUMN `sync_batch_no` varchar(64) DEFAULT NULL COMMENT ''同步批次号（一次执行全链路留痕）'' AFTER `operator`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dis_data_exchange_log' AND column_name = 'retry_flag');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `dis_data_exchange_log` ADD COLUMN `retry_flag` char(1) DEFAULT ''0'' COMMENT ''是否人工重推（0否 1是）'' AFTER `sync_batch_no`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 批次号检索索引
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'dis_data_exchange_log' AND index_name = 'idx_sync_batch_no');
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `dis_data_exchange_log` ADD INDEX `idx_sync_batch_no` (`sync_batch_no`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 3. "任务重推"按钮权限（parent=2404 数据同步任务，menu_id=2440）
-- ----------------------------
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2440,'任务重推',2404,6,'','','','',1,0,'F','0','0','dis:syncTask:repush','#','admin',sysdate(),'',NULL,'D2 人工重推补偿同步'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2440);

INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,2440 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=2440);
