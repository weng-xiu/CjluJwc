-- ===============================================
-- 第三期 P1 升级脚本：B1 教室借用线上申请 + 智能审核 + 全流程可追溯
-- 内容：brm_classroom_borrow 流程化列升级 / 审批状态字典 / 管理端流程按钮 / 门户借用菜单 / 角色授权
-- 幂等：列/索引用 information_schema + PREPARE；字典先删后插；菜单/授权用 INSERT ... WHERE NOT EXISTS
-- 依赖：brm_classroom_borrow、sys_menu(2016 教室借用、2509 教师服务) 已存在
-- 菜单ID：现有最大为 2973（phase17），本脚本使用 2980+ 段避免冲突
-- 状态机：approve_status 重新定义为 0待院系审核 1待教务处审核 2已通过 3已驳回 4已撤销
-- ===============================================

-- ----------------------------
-- 1. brm_classroom_borrow 流程化列升级
-- ----------------------------
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'brm_classroom_borrow' AND column_name = 'applicant_user_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `brm_classroom_borrow` ADD COLUMN `applicant_user_id` bigint DEFAULT NULL COMMENT ''申请用户ID（门户申请人绑定）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'brm_classroom_borrow' AND column_name = 'contact_phone');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `brm_classroom_borrow` ADD COLUMN `contact_phone` varchar(20) DEFAULT NULL COMMENT ''联系电话''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'brm_classroom_borrow' AND column_name = 'attendee_count');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `brm_classroom_borrow` ADD COLUMN `attendee_count` int DEFAULT NULL COMMENT ''借用人次''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'brm_classroom_borrow' AND column_name = 'dept_approve_by');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `brm_classroom_borrow` ADD COLUMN `dept_approve_by` varchar(50) DEFAULT NULL COMMENT ''院系初审人''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'brm_classroom_borrow' AND column_name = 'dept_approve_time');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `brm_classroom_borrow` ADD COLUMN `dept_approve_time` datetime DEFAULT NULL COMMENT ''院系初审时间''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'brm_classroom_borrow' AND column_name = 'dept_opinion');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `brm_classroom_borrow` ADD COLUMN `dept_opinion` varchar(500) DEFAULT NULL COMMENT ''院系初审意见''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'brm_classroom_borrow' AND column_name = 'aa_approve_by');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `brm_classroom_borrow` ADD COLUMN `aa_approve_by` varchar(50) DEFAULT NULL COMMENT ''教务处终审人''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'brm_classroom_borrow' AND column_name = 'aa_approve_time');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `brm_classroom_borrow` ADD COLUMN `aa_approve_time` datetime DEFAULT NULL COMMENT ''教务处终审时间''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'brm_classroom_borrow' AND column_name = 'aa_opinion');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `brm_classroom_borrow` ADD COLUMN `aa_opinion` varchar(500) DEFAULT NULL COMMENT ''教务处终审意见''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'brm_classroom_borrow' AND column_name = 'proc_inst_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `brm_classroom_borrow` ADD COLUMN `proc_inst_id` varchar(64) DEFAULT NULL COMMENT ''Flowable流程实例ID''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 冲突校验/占用日历检索索引（同教室同日）
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'brm_classroom_borrow' AND index_name = 'idx_borrow_classroom_date');
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `brm_classroom_borrow` ADD INDEX `idx_borrow_classroom_date` (`classroom_id`, `borrow_date`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 审批状态注释升级（新语义）
ALTER TABLE `brm_classroom_borrow`
    MODIFY COLUMN `approve_status` char(1) DEFAULT '0'
    COMMENT '审批状态（0待院系审核 1待教务处审核 2已通过 3已驳回 4已撤销）';

-- 存量数据状态映射（旧语义 0待审/1通过/2驳回 → 新语义），表为空时无影响。
-- 单语句 CASE 一次性映射，重复执行时新值 2/3/4 不再命中源值，幂等安全。
UPDATE `brm_classroom_borrow`
SET `approve_status` = CASE `approve_status` WHEN '1' THEN '2' WHEN '2' THEN '3' ELSE `approve_status` END
WHERE `proc_inst_id` IS NULL AND `approve_status` IN ('1','2');

-- ----------------------------
-- 2. 字典：借用审批状态 brm_borrow_approve_status（先删后插，可重复执行）
-- ----------------------------
DELETE FROM sys_dict_data WHERE dict_type = 'brm_borrow_approve_status';
DELETE FROM sys_dict_type WHERE dict_type = 'brm_borrow_approve_status';

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
VALUES ('借用审批状态', 'brm_borrow_approve_status', '0', 'admin', sysdate(), 'B1 教室借用两级审批状态机');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES
(330, 1, '待院系审核', '0', 'brm_borrow_approve_status', '', 'warning', 'Y', '0', 'admin', sysdate(), '院系初审环节'),
(331, 2, '待教务处审核', '1', 'brm_borrow_approve_status', '', 'warning', 'N', '0', 'admin', sysdate(), '教务处终审环节'),
(332, 3, '已通过',       '2', 'brm_borrow_approve_status', '', 'success', 'N', '0', 'admin', sysdate(), '流程通过，占用生效'),
(333, 4, '已驳回',       '3', 'brm_borrow_approve_status', '', 'danger',  'N', '0', 'admin', sysdate(), '任一环节驳回'),
(334, 5, '已撤销',       '4', 'brm_borrow_approve_status', '', 'info',    'N', '0', 'admin', sysdate(), '申请人主动撤销');

-- ----------------------------
-- 3. 管理端按钮权限：提交审批 / 审批（parent=2016 教室借用）
-- ----------------------------
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2980,'提交审批',2016,6,'','','','',1,0,'F','0','0','brm:borrow:submit','#','admin',sysdate(),'',NULL,'B1 提交进入两级审批流程'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2980);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2981,'借用审批',2016,7,'','','','',1,0,'F','0','0','brm:borrow:approve','#','admin',sysdate(),'',NULL,'B1 院系/教务处两级审批'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2981);

-- ----------------------------
-- 4. 门户菜单：教室借用申请（parent=2509 教师服务，参照 2515 调停课 2菜单+2按钮结构）
-- ----------------------------
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2982,'教室借用申请',2509,7,'borrow','portal/borrow/index','','',1,0,'C','0','0','portal:borrow:list','office-building','admin',sysdate(),'',NULL,'B1 门户教师教室借用申请入口'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2982);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2983,'提交借用申请',2982,1,'','','','',1,0,'F','0','0','portal:borrow:add','#','admin',sysdate(),'',NULL,'B1 门户提交借用申请并启动流程'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2983);

-- ----------------------------
-- 5. 角色授权
-- ----------------------------
-- 超级管理员(role_id=1)：本批全部菜单/按钮
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,m.menu_id FROM (
  SELECT 2980 AS menu_id UNION ALL SELECT 2981 UNION ALL SELECT 2982 UNION ALL SELECT 2983
) m WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=1 AND rm.`menu_id`=m.menu_id);

-- 教师(role_id=6)：门户借用菜单 + 提交按钮
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 6,m.menu_id FROM (
  SELECT 2982 AS menu_id UNION ALL SELECT 2983
) m WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=6 AND rm.`menu_id`=m.menu_id);
