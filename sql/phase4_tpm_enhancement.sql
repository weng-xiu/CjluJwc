-- =====================================================================
-- TPM 培养过程管理 - 功能完善升级脚本（幂等，可重复执行）
-- 内容：
--   1. 统一 9 张 tpm_ 表的 del_flag 软删除列（phase3 仅覆盖 4 张）
--   2. tpm_schedule_adjustment 新增审批意见列 approve_comment
--   3. 新增 TPM 业务专用字典（发布状态/课程属性/开课/轮次/抽签/审批/规则等）
--   4. 补充新业务动作的按钮权限（发布、确认/取消开课、审批、轮次开关、抽签/退课）
-- 说明：列新增使用 INFORMATION_SCHEMA 条件判断，避免重复执行报错。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. del_flag 软删除列（0存在 2删除）
-- ---------------------------------------------------------------------
SET @db = DATABASE();

-- tpm_training_plan
SET @s = (SELECT IF(COUNT(*)=0,
  'ALTER TABLE tpm_training_plan ADD COLUMN del_flag char(1) DEFAULT ''0'' COMMENT ''删除标志（0存在 2删除）''',
  'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='tpm_training_plan' AND COLUMN_NAME='del_flag');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tpm_course_library
SET @s = (SELECT IF(COUNT(*)=0,
  'ALTER TABLE tpm_course_library ADD COLUMN del_flag char(1) DEFAULT ''0'' COMMENT ''删除标志（0存在 2删除）''',
  'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='tpm_course_library' AND COLUMN_NAME='del_flag');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tpm_credit_structure
SET @s = (SELECT IF(COUNT(*)=0,
  'ALTER TABLE tpm_credit_structure ADD COLUMN del_flag char(1) DEFAULT ''0'' COMMENT ''删除标志（0存在 2删除）''',
  'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='tpm_credit_structure' AND COLUMN_NAME='del_flag');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tpm_schedule_adjustment
SET @s = (SELECT IF(COUNT(*)=0,
  'ALTER TABLE tpm_schedule_adjustment ADD COLUMN del_flag char(1) DEFAULT ''0'' COMMENT ''删除标志（0存在 2删除）''',
  'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='tpm_schedule_adjustment' AND COLUMN_NAME='del_flag');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tpm_selection_rule（phase3 未覆盖）
SET @s = (SELECT IF(COUNT(*)=0,
  'ALTER TABLE tpm_selection_rule ADD COLUMN del_flag char(1) DEFAULT ''0'' COMMENT ''删除标志（0存在 2删除）''',
  'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='tpm_selection_rule' AND COLUMN_NAME='del_flag');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 以下 4 张表 phase3_governance.sql 已新增 del_flag，此处仅在缺失时补建
-- tpm_schedule
SET @s = (SELECT IF(COUNT(*)=0,
  'ALTER TABLE tpm_schedule ADD COLUMN del_flag char(1) DEFAULT ''0'' COMMENT ''删除标志（0存在 2删除）''',
  'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='tpm_schedule' AND COLUMN_NAME='del_flag');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- tpm_course_offering
SET @s = (SELECT IF(COUNT(*)=0,
  'ALTER TABLE tpm_course_offering ADD COLUMN del_flag char(1) DEFAULT ''0'' COMMENT ''删除标志（0存在 2删除）''',
  'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='tpm_course_offering' AND COLUMN_NAME='del_flag');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- tpm_selection_enrollment
SET @s = (SELECT IF(COUNT(*)=0,
  'ALTER TABLE tpm_selection_enrollment ADD COLUMN del_flag char(1) DEFAULT ''0'' COMMENT ''删除标志（0存在 2删除）''',
  'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='tpm_selection_enrollment' AND COLUMN_NAME='del_flag');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- tpm_selection_round
SET @s = (SELECT IF(COUNT(*)=0,
  'ALTER TABLE tpm_selection_round ADD COLUMN del_flag char(1) DEFAULT ''0'' COMMENT ''删除标志（0存在 2删除）''',
  'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='tpm_selection_round' AND COLUMN_NAME='del_flag');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 2. 调停课审批意见列
-- ---------------------------------------------------------------------
SET @s = (SELECT IF(COUNT(*)=0,
  'ALTER TABLE tpm_schedule_adjustment ADD COLUMN approve_comment varchar(500) DEFAULT NULL COMMENT ''审批意见'' AFTER approve_time',
  'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='tpm_schedule_adjustment' AND COLUMN_NAME='approve_comment');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 3. TPM 业务专用字典（幂等：先按 dict_type 清理再插入）
-- =====================================================================
DELETE FROM sys_dict_data WHERE dict_type IN
  ('tpm_education_level','tpm_plan_publish_status','tpm_course_type','tpm_course_category',
   'tpm_assessment','tpm_credit_type','tpm_offering_status','tpm_schedule_type',
   'tpm_adjust_type','tpm_approve_status','tpm_round_status','tpm_lottery_result',
   'tpm_enroll_result','tpm_rule_type');
DELETE FROM sys_dict_type WHERE dict_type IN
  ('tpm_education_level','tpm_plan_publish_status','tpm_course_type','tpm_course_category',
   'tpm_assessment','tpm_credit_type','tpm_offering_status','tpm_schedule_type',
   'tpm_adjust_type','tpm_approve_status','tpm_round_status','tpm_lottery_result',
   'tpm_enroll_result','tpm_rule_type');

-- 字典类型（dict_id 从 300 段开始，避免与 brm 120/310 段冲突）
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark) VALUES
(300, '学历层次(TPM)',   'tpm_education_level',    '0', 'admin', sysdate(), '培养方案学历层次'),
(301, '培养方案发布状态', 'tpm_plan_publish_status','0', 'admin', sysdate(), '0草稿 1已发布 2已废止'),
(302, '课程类型',         'tpm_course_type',        '0', 'admin', sysdate(), '必修/选修/公选'),
(303, '课程类别',         'tpm_course_category',    '0', 'admin', sysdate(), '通识/学科基础/专业核心等'),
(304, '考核方式',         'tpm_assessment',         '0', 'admin', sysdate(), '考试/考查'),
(305, '学分类型',         'tpm_credit_type',        '0', 'admin', sysdate(), '学分结构类型'),
(306, '开课状态',         'tpm_offering_status',    '0', 'admin', sysdate(), '0待确认 1已确认 2已取消'),
(307, '排课方式',         'tpm_schedule_type',      '0', 'admin', sysdate(), 'manual手工/auto自动'),
(308, '调课类型',         'tpm_adjust_type',        '0', 'admin', sysdate(), '1调课 2停课 3补课'),
(309, '审批状态',         'tpm_approve_status',     '0', 'admin', sysdate(), '0待审 1通过 2驳回'),
(310, '选课轮次状态',     'tpm_round_status',       '0', 'admin', sysdate(), '0未开始 1进行中 2已结束'),
(311, '抽签结果',         'tpm_lottery_result',     '0', 'admin', sysdate(), '0未抽签 1中签 2未中签'),
(312, '选课结果',         'tpm_enroll_result',      '0', 'admin', sysdate(), '1选中 2落选 3退课'),
(313, '选课规则类型',     'tpm_rule_type',          '0', 'admin', sysdate(), '专业/年级/院系/人数/先修');

-- 字典数据（dict_code 从 9000 段开始）
-- tpm_education_level
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(9000,1,'本科','本科','tpm_education_level','','primary','Y','0','admin',sysdate()),
(9001,2,'硕士','硕士','tpm_education_level','','success','N','0','admin',sysdate()),
(9002,3,'博士','博士','tpm_education_level','','warning','N','0','admin',sysdate()),
(9003,4,'专科','专科','tpm_education_level','','info','N','0','admin',sysdate());

-- tpm_plan_publish_status
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(9010,1,'草稿','0','tpm_plan_publish_status','','info','Y','0','admin',sysdate()),
(9011,2,'已发布','1','tpm_plan_publish_status','','success','N','0','admin',sysdate()),
(9012,3,'已废止','2','tpm_plan_publish_status','','danger','N','0','admin',sysdate());

-- tpm_course_type
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(9020,1,'必修','1','tpm_course_type','','danger','N','0','admin',sysdate()),
(9021,2,'选修','2','tpm_course_type','','warning','N','0','admin',sysdate()),
(9022,3,'公选','3','tpm_course_type','','success','N','0','admin',sysdate());

-- tpm_course_category
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(9030,1,'通识必修','1','tpm_course_category','','','N','0','admin',sysdate()),
(9031,2,'学科基础','2','tpm_course_category','','','N','0','admin',sysdate()),
(9032,3,'专业核心','3','tpm_course_category','','','N','0','admin',sysdate()),
(9033,4,'实践环节','4','tpm_course_category','','','N','0','admin',sysdate()),
(9034,5,'公共选修','5','tpm_course_category','','','N','0','admin',sysdate());

-- tpm_assessment
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(9040,1,'考试','1','tpm_assessment','','danger','N','0','admin',sysdate()),
(9041,2,'考查','2','tpm_assessment','','info','N','0','admin',sysdate());

-- tpm_credit_type
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(9050,1,'通识必修','1','tpm_credit_type','','','N','0','admin',sysdate()),
(9051,2,'专业必修','2','tpm_credit_type','','','N','0','admin',sysdate()),
(9052,3,'实践环节','3','tpm_credit_type','','','N','0','admin',sysdate()),
(9053,4,'公共选修','4','tpm_credit_type','','','N','0','admin',sysdate()),
(9054,5,'个性发展','5','tpm_credit_type','','','N','0','admin',sysdate());

-- tpm_offering_status
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(9060,1,'待确认','0','tpm_offering_status','','warning','Y','0','admin',sysdate()),
(9061,2,'已确认','1','tpm_offering_status','','success','N','0','admin',sysdate()),
(9062,3,'已取消','2','tpm_offering_status','','danger','N','0','admin',sysdate());

-- tpm_schedule_type
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(9070,1,'手工排课','manual','tpm_schedule_type','','primary','Y','0','admin',sysdate()),
(9071,2,'自动排课','auto','tpm_schedule_type','','success','N','0','admin',sysdate());

-- tpm_adjust_type
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(9080,1,'调课','1','tpm_adjust_type','','primary','N','0','admin',sysdate()),
(9081,2,'停课','2','tpm_adjust_type','','warning','N','0','admin',sysdate()),
(9082,3,'补课','3','tpm_adjust_type','','success','N','0','admin',sysdate());

-- tpm_approve_status
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(9090,1,'待审','0','tpm_approve_status','','warning','Y','0','admin',sysdate()),
(9091,2,'通过','1','tpm_approve_status','','success','N','0','admin',sysdate()),
(9092,3,'驳回','2','tpm_approve_status','','danger','N','0','admin',sysdate());

-- tpm_round_status
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(9100,1,'未开始','0','tpm_round_status','','info','Y','0','admin',sysdate()),
(9101,2,'进行中','1','tpm_round_status','','success','N','0','admin',sysdate()),
(9102,3,'已结束','2','tpm_round_status','','danger','N','0','admin',sysdate());

-- tpm_lottery_result
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(9110,1,'未抽签','0','tpm_lottery_result','','info','Y','0','admin',sysdate()),
(9111,2,'中签','1','tpm_lottery_result','','success','N','0','admin',sysdate()),
(9112,3,'未中签','2','tpm_lottery_result','','danger','N','0','admin',sysdate());

-- tpm_enroll_result
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(9120,1,'选中','1','tpm_enroll_result','','success','N','0','admin',sysdate()),
(9121,2,'落选','2','tpm_enroll_result','','danger','N','0','admin',sysdate()),
(9122,3,'退课','3','tpm_enroll_result','','info','N','0','admin',sysdate());

-- tpm_rule_type
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(9130,1,'专业限制','1','tpm_rule_type','','','N','0','admin',sysdate()),
(9131,2,'年级限制','2','tpm_rule_type','','','N','0','admin',sysdate()),
(9132,3,'院系限制','3','tpm_rule_type','','','N','0','admin',sysdate()),
(9133,4,'人数上限','4','tpm_rule_type','','','N','0','admin',sysdate()),
(9134,5,'先修课程','5','tpm_rule_type','','','N','0','admin',sysdate());

-- =====================================================================
-- 4. 新增业务动作按钮权限（F 类型，幂等：按 perms 清理重复）
--    菜单ID使用 2700 段，避开已用 2100-2157 与排课优化 2620-2625
-- =====================================================================
DELETE FROM sys_menu WHERE perms IN
  ('tpm:plan:publish','tpm:offering:confirm','tpm:adjust:approve',
   'tpm:round:operate','tpm:enroll:lottery','tpm:enroll:drop');

-- 培养方案发布/废止（挂在培养方案菜单 2102 下）
INSERT INTO sys_menu VALUES (2700,'方案发布','2102','6','','','','',1,0,'F','0','0','tpm:plan:publish','#','admin',sysdate(),'',NULL,'');
-- 开课确认/取消（挂在开课计划菜单 2106 下）
INSERT INTO sys_menu VALUES (2701,'开课确认','2106','6','','','','',1,0,'F','0','0','tpm:offering:confirm','#','admin',sysdate(),'',NULL,'');
-- 调停课审批（挂在调停课管理菜单 2108 下）
INSERT INTO sys_menu VALUES (2702,'调课审批','2108','6','','','','',1,0,'F','0','0','tpm:adjust:approve','#','admin',sysdate(),'',NULL,'');
-- 选课轮次开启/关闭（挂在选课轮次菜单 2110 下）
INSERT INTO sys_menu VALUES (2703,'轮次操作','2110','6','','','','',1,0,'F','0','0','tpm:round:operate','#','admin',sysdate(),'',NULL,'');
-- 选课抽签（挂在选课名单菜单 2112 下）
INSERT INTO sys_menu VALUES (2704,'选课抽签','2112','6','','','','',1,0,'F','0','0','tpm:enroll:lottery','#','admin',sysdate(),'',NULL,'');
-- 选课退课（挂在选课名单菜单 2112 下）
INSERT INTO sys_menu VALUES (2705,'学生退课','2112','7','','','','',1,0,'F','0','0','tpm:enroll:drop','#','admin',sysdate(),'',NULL,'');

-- 注意：新按钮权限默认只授权给超级管理员（admin 角色拥有所有权限）。
-- 若需为其他角色开通，请在「系统管理-角色管理」中勾选上述按钮。
