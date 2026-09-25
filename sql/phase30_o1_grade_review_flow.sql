-- =============================================================
-- Phase 30：O1 成绩变更（复核）接入 Flowable 多级审批流程
-- 覆盖：
--   1) aem_grade_review 增加流程实例与两级审批留痕列（列漂移补齐）
--   2) 补齐 aem_review_type / aem_approve_status 字典（此前建库脚本从未创建，
--      前端 gradeReview 页 dict-tag 引用导致审批状态/类型列空白）
--   3) 新增"提交流程 / 审批"按钮权限（parent=2207 成绩复核），授权超级管理员
-- 菜单ID：现有最大为 3087，本脚本使用 3090+ 段避免冲突
-- 本脚本幂等，可重复执行
-- =============================================================

-- ----------------------------
-- 1. aem_grade_review 流程化列（幂等：列已存在则跳过）
-- ----------------------------
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aem_grade_review' AND COLUMN_NAME='proc_inst_id');
SET @sql = IF(@c=0,'ALTER TABLE `aem_grade_review` ADD COLUMN `proc_inst_id` varchar(64) NULL DEFAULT NULL COMMENT ''Flowable流程实例ID''','SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aem_grade_review' AND COLUMN_NAME='dept_approve_by');
SET @sql = IF(@c=0,'ALTER TABLE `aem_grade_review` ADD COLUMN `dept_approve_by` varchar(50) NULL DEFAULT NULL COMMENT ''课程负责人初审人''','SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aem_grade_review' AND COLUMN_NAME='dept_approve_time');
SET @sql = IF(@c=0,'ALTER TABLE `aem_grade_review` ADD COLUMN `dept_approve_time` datetime NULL DEFAULT NULL COMMENT ''课程负责人初审时间''','SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aem_grade_review' AND COLUMN_NAME='dept_opinion');
SET @sql = IF(@c=0,'ALTER TABLE `aem_grade_review` ADD COLUMN `dept_opinion` varchar(500) NULL DEFAULT NULL COMMENT ''课程负责人初审意见''','SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aem_grade_review' AND COLUMN_NAME='aa_approve_by');
SET @sql = IF(@c=0,'ALTER TABLE `aem_grade_review` ADD COLUMN `aa_approve_by` varchar(50) NULL DEFAULT NULL COMMENT ''教务处终审人''','SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aem_grade_review' AND COLUMN_NAME='aa_approve_time');
SET @sql = IF(@c=0,'ALTER TABLE `aem_grade_review` ADD COLUMN `aa_approve_time` datetime NULL DEFAULT NULL COMMENT ''教务处终审时间''','SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aem_grade_review' AND COLUMN_NAME='aa_opinion');
SET @sql = IF(@c=0,'ALTER TABLE `aem_grade_review` ADD COLUMN `aa_opinion` varchar(500) NULL DEFAULT NULL COMMENT ''教务处终审意见''','SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- approve_status 注释扩展为五态（0待审/初审中 4待教务处终审 1通过 2驳回 3已撤销），仅更新列注释
ALTER TABLE `aem_grade_review`
  MODIFY COLUMN `approve_status` char(1) NULL DEFAULT '0'
  COMMENT '审批状态（0待审/初审中 4待教务处终审 1通过 2驳回 3已撤销）';

-- ----------------------------
-- 2. 字典补齐：aem_review_type（复核类型）
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '成绩复核类型', 'aem_review_type', '0', 'admin', sysdate(), '成绩复核/修改类型'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='aem_review_type');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '成绩修改', '0', 'aem_review_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '审批通过后回写成绩'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='aem_review_type' AND dict_value='0');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '成绩复核', '1', 'aem_review_type', '', 'success', 'N', '0', 'admin', sysdate(), '仅复核不改分'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='aem_review_type' AND dict_value='1');

-- ----------------------------
--   字典补齐：aem_approve_status（审批状态，含流程五态）
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '成绩复核审批状态', 'aem_approve_status', '0', 'admin', sysdate(), '成绩变更流程审批状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='aem_approve_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '待审/初审中', '0', 'aem_approve_status', '', 'warning', 'Y', '0', 'admin', sysdate(), '提交前待初审或初审后未流转'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='aem_approve_status' AND dict_value='0');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '已通过', '1', 'aem_approve_status', '', 'success', 'N', '0', 'admin', sysdate(), '终审通过并回写成绩'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='aem_approve_status' AND dict_value='1');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已驳回', '2', 'aem_approve_status', '', 'danger', 'N', '0', 'admin', sysdate(), '任一审批环节驳回'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='aem_approve_status' AND dict_value='2');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '已撤销', '3', 'aem_approve_status', '', 'info', 'N', '0', 'admin', sysdate(), '申请人撤销'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='aem_approve_status' AND dict_value='3');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '待教务处终审', '4', 'aem_approve_status', '', 'warning', 'N', '0', 'admin', sysdate(), '初审通过待终审'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='aem_approve_status' AND dict_value='4');

-- ----------------------------
-- 3. 菜单按钮：成绩复核提交流程 / 审批（parent=2207），幂等
-- ----------------------------
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3090,'复核提交',2207,6,'','','','',1,0,'F','0','0','aem:gradeReview:submit','#','admin',sysdate(),'',NULL,'O1 提交进入Flowable审批流程/撤销'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3090);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3091,'复核审批',2207,7,'','','','',1,0,'F','0','0','aem:gradeReview:audit','#','admin',sysdate(),'',NULL,'O1 课程负责人初审/教务处终审'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3091);

-- ----------------------------
-- 4. 角色授权：超级管理员(role_id=1)、教务处管理员(role_id=3)
--    （admin 本拥有全权限，此处一并显式授予 3 号教务处角色以便流程演示）
-- ----------------------------
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT r.role_id, m.menu_id FROM
  (SELECT 1 AS role_id UNION ALL SELECT 3) r
  CROSS JOIN (SELECT 3090 AS menu_id UNION ALL SELECT 3091) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=r.role_id AND rm.`menu_id`=m.menu_id);

-- 确保教务处角色已拥有成绩复核页面及查询权限（父菜单链），否则按钮无入口
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 3, x.menu_id FROM (
  SELECT 2207 AS menu_id UNION ALL SELECT 2234 UNION ALL SELECT 2235 UNION ALL SELECT 2236 UNION ALL SELECT 2238
) x WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=3 AND rm.`menu_id`=x.menu_id);

-- ----------------------------
-- 验证
-- ----------------------------
SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='aem_grade_review' AND COLUMN_NAME IN ('proc_inst_id','dept_approve_by','aa_approve_by') ORDER BY COLUMN_NAME;
SELECT dict_type, dict_value, dict_label FROM sys_dict_data WHERE dict_type IN ('aem_review_type','aem_approve_status') ORDER BY dict_type, dict_sort;
SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id IN (3090,3091);
