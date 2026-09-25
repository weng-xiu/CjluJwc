-- =============================================================
-- Phase 25：tpm_* 字典整组缺失修复（存量缺陷）
-- 现场核查发现 sys_dict_type 仅剩 tpm_plan_publish_status，
-- tpm_adjust_type / tpm_approve_status 等 13 组类型与数据缺失，
-- 导致管理端调停课等页面 dict-tag 渲染空白。
-- 按 phase4_tpm_enhancement.sql 原定义恢复，并含 P6 新增
-- tpm_approve_status '3'=已撤销（phase24 口径）。幂等可重复执行。
-- =============================================================

-- 1. 补建缺失的字典类型（已存在则跳过）
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
SELECT * FROM (
  SELECT 300 AS a,'学历层次(TPM)' AS b,'tpm_education_level' AS c,'0' AS d,'admin' AS e,sysdate() AS f,'培养方案学历层次' AS g UNION ALL
  SELECT 302,'课程类型','tpm_course_type','0','admin',sysdate(),'必修/选修/公选' UNION ALL
  SELECT 303,'课程类别','tpm_course_category','0','admin',sysdate(),'通识/学科基础/专业核心等' UNION ALL
  SELECT 304,'考核方式','tpm_assessment','0','admin',sysdate(),'考试/考查' UNION ALL
  SELECT 305,'学分类型','tpm_credit_type','0','admin',sysdate(),'学分结构类型' UNION ALL
  SELECT 306,'开课状态','tpm_offering_status','0','admin',sysdate(),'0待确认 1已确认 2已取消' UNION ALL
  SELECT 307,'排课方式','tpm_schedule_type','0','admin',sysdate(),'manual手工/auto自动' UNION ALL
  SELECT 308,'调课类型','tpm_adjust_type','0','admin',sysdate(),'1调课 2停课 3补课' UNION ALL
  SELECT 309,'审批状态','tpm_approve_status','0','admin',sysdate(),'0待审 1通过 2驳回 3已撤销' UNION ALL
  SELECT 310,'选课轮次状态','tpm_round_status','0','admin',sysdate(),'0未开始 1进行中 2已结束' UNION ALL
  SELECT 311,'抽签结果','tpm_lottery_result','0','admin',sysdate(),'0未抽签 1中签 2未中签' UNION ALL
  SELECT 312,'选课结果','tpm_enroll_result','0','admin',sysdate(),'1选中 2落选 3退课' UNION ALL
  SELECT 313,'选课规则类型','tpm_rule_type','0','admin',sysdate(),'专业/年级/院系/人数/先修'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type s WHERE s.dict_type = t.c);

-- 2. 恢复字典数据（幂等：先删后插，dict_code 沿用 9000 段）
DELETE FROM sys_dict_data WHERE dict_type IN
  ('tpm_education_level','tpm_course_type','tpm_course_category','tpm_assessment',
   'tpm_credit_type','tpm_offering_status','tpm_schedule_type','tpm_adjust_type',
   'tpm_approve_status','tpm_round_status','tpm_lottery_result','tpm_enroll_result','tpm_rule_type');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(9000,1,'本科','本科','tpm_education_level','','primary','Y','0','admin',sysdate()),
(9001,2,'硕士','硕士','tpm_education_level','','success','N','0','admin',sysdate()),
(9002,3,'博士','博士','tpm_education_level','','warning','N','0','admin',sysdate()),
(9003,4,'专科','专科','tpm_education_level','','info','N','0','admin',sysdate()),
(9020,1,'必修','1','tpm_course_type','','danger','N','0','admin',sysdate()),
(9021,2,'选修','2','tpm_course_type','','warning','N','0','admin',sysdate()),
(9022,3,'公选','3','tpm_course_type','','success','N','0','admin',sysdate()),
(9030,1,'通识必修','1','tpm_course_category','','','N','0','admin',sysdate()),
(9031,2,'学科基础','2','tpm_course_category','','','N','0','admin',sysdate()),
(9032,3,'专业核心','3','tpm_course_category','','','N','0','admin',sysdate()),
(9033,4,'实践环节','4','tpm_course_category','','','N','0','admin',sysdate()),
(9034,5,'公共选修','5','tpm_course_category','','','N','0','admin',sysdate()),
(9040,1,'考试','1','tpm_assessment','','danger','N','0','admin',sysdate()),
(9041,2,'考查','2','tpm_assessment','','info','N','0','admin',sysdate()),
(9050,1,'通识必修','1','tpm_credit_type','','','N','0','admin',sysdate()),
(9051,2,'专业必修','2','tpm_credit_type','','','N','0','admin',sysdate()),
(9052,3,'实践环节','3','tpm_credit_type','','','N','0','admin',sysdate()),
(9053,4,'公共选修','4','tpm_credit_type','','','N','0','admin',sysdate()),
(9054,5,'个性发展','5','tpm_credit_type','','','N','0','admin',sysdate()),
(9060,1,'待确认','0','tpm_offering_status','','warning','Y','0','admin',sysdate()),
(9061,2,'已确认','1','tpm_offering_status','','success','N','0','admin',sysdate()),
(9062,3,'已取消','2','tpm_offering_status','','danger','N','0','admin',sysdate()),
(9070,1,'手工排课','manual','tpm_schedule_type','','primary','Y','0','admin',sysdate()),
(9071,2,'自动排课','auto','tpm_schedule_type','','success','N','0','admin',sysdate()),
(9080,1,'调课','1','tpm_adjust_type','','primary','N','0','admin',sysdate()),
(9081,2,'停课','2','tpm_adjust_type','','warning','N','0','admin',sysdate()),
(9082,3,'补课','3','tpm_adjust_type','','success','N','0','admin',sysdate()),
(9090,1,'待审','0','tpm_approve_status','','warning','Y','0','admin',sysdate()),
(9091,2,'通过','1','tpm_approve_status','','success','N','0','admin',sysdate()),
(9092,3,'驳回','2','tpm_approve_status','','danger','N','0','admin',sysdate()),
(9093,4,'已撤销','3','tpm_approve_status','','info','N','0','admin',sysdate());

-- 3. 其余恢复项（round/lottery/enroll/rule）
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(9100,1,'未开始','0','tpm_round_status','','info','Y','0','admin',sysdate()),
(9101,2,'进行中','1','tpm_round_status','','success','N','0','admin',sysdate()),
(9102,3,'已结束','2','tpm_round_status','','danger','N','0','admin',sysdate()),
(9110,1,'未抽签','0','tpm_lottery_result','','info','Y','0','admin',sysdate()),
(9111,2,'中签','1','tpm_lottery_result','','success','N','0','admin',sysdate()),
(9112,3,'未中签','2','tpm_lottery_result','','danger','N','0','admin',sysdate()),
(9120,1,'选中','1','tpm_enroll_result','','success','N','0','admin',sysdate()),
(9121,2,'落选','2','tpm_enroll_result','','danger','N','0','admin',sysdate()),
(9122,3,'退课','3','tpm_enroll_result','','info','N','0','admin',sysdate()),
(9130,1,'专业限制','1','tpm_rule_type','','','N','0','admin',sysdate()),
(9131,2,'年级限制','2','tpm_rule_type','','','N','0','admin',sysdate()),
(9132,3,'院系限制','3','tpm_rule_type','','','N','0','admin',sysdate()),
(9133,4,'人数上限','4','tpm_rule_type','','','N','0','admin',sysdate()),
(9134,5,'先修课程','5','tpm_rule_type','','','N','0','admin',sysdate());
