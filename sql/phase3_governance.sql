-- ===============================================
-- 教务系统升级 阶段三：数据治理
-- 内容：menu_id冲突修复、索引补全、del_flag统一、定时任务启用
-- 创建时间：2026-08-05
-- ===============================================

-- ===============================================
-- 1. 修复 menu_id 2600 冲突（门户CMS与phase1重叠）
--    将门户CMS菜单ID从2600-2634迁移至2800-2834
-- ===============================================

-- 1.1 更新角色-菜单关联（先删除旧的，再插入新的）
DELETE FROM sys_role_menu WHERE menu_id IN (2600,2601,2602,2603,2611,2612,2613,2614,2621,2622,2623,2624,2625,2626,2631,2632,2633,2634);
INSERT INTO sys_role_menu VALUES ('1','2800');
INSERT INTO sys_role_menu VALUES ('1','2801');
INSERT INTO sys_role_menu VALUES ('1','2811');
INSERT INTO sys_role_menu VALUES ('1','2812');
INSERT INTO sys_role_menu VALUES ('1','2813');
INSERT INTO sys_role_menu VALUES ('1','2814');
INSERT INTO sys_role_menu VALUES ('1','2802');
INSERT INTO sys_role_menu VALUES ('1','2821');
INSERT INTO sys_role_menu VALUES ('1','2822');
INSERT INTO sys_role_menu VALUES ('1','2823');
INSERT INTO sys_role_menu VALUES ('1','2824');
INSERT INTO sys_role_menu VALUES ('1','2825');
INSERT INTO sys_role_menu VALUES ('1','2826');
INSERT INTO sys_role_menu VALUES ('1','2803');
INSERT INTO sys_role_menu VALUES ('1','2831');
INSERT INTO sys_role_menu VALUES ('1','2832');
INSERT INTO sys_role_menu VALUES ('1','2833');
INSERT INTO sys_role_menu VALUES ('1','2834');

-- 1.2 更新按钮权限的parent_id（先更新子菜单，再更新父菜单）
UPDATE sys_menu SET menu_id=2811, parent_id=2801 WHERE menu_id=2611;
UPDATE sys_menu SET menu_id=2812, parent_id=2801 WHERE menu_id=2612;
UPDATE sys_menu SET menu_id=2813, parent_id=2801 WHERE menu_id=2613;
UPDATE sys_menu SET menu_id=2814, parent_id=2801 WHERE menu_id=2614;
UPDATE sys_menu SET menu_id=2821, parent_id=2802 WHERE menu_id=2621;
UPDATE sys_menu SET menu_id=2822, parent_id=2802 WHERE menu_id=2622;
UPDATE sys_menu SET menu_id=2823, parent_id=2802 WHERE menu_id=2623;
UPDATE sys_menu SET menu_id=2824, parent_id=2802 WHERE menu_id=2624;
UPDATE sys_menu SET menu_id=2825, parent_id=2802 WHERE menu_id=2625;
UPDATE sys_menu SET menu_id=2826, parent_id=2802 WHERE menu_id=2626;
UPDATE sys_menu SET menu_id=2831, parent_id=2803 WHERE menu_id=2631;
UPDATE sys_menu SET menu_id=2832, parent_id=2803 WHERE menu_id=2632;
UPDATE sys_menu SET menu_id=2833, parent_id=2803 WHERE menu_id=2633;
UPDATE sys_menu SET menu_id=2834, parent_id=2803 WHERE menu_id=2634;

-- 1.3 更新子菜单的parent_id和menu_id
UPDATE sys_menu SET menu_id=2801, parent_id=2800 WHERE menu_id=2601;
UPDATE sys_menu SET menu_id=2802, parent_id=2800 WHERE menu_id=2602;
UPDATE sys_menu SET menu_id=2803, parent_id=2800 WHERE menu_id=2603;

-- 1.4 更新父菜单
UPDATE sys_menu SET menu_id=2800 WHERE menu_id=2600;

-- ===============================================
-- 2. 补全高频查询索引
-- ===============================================

-- 排课表：按开课ID查询（选课冲突检测高频使用）
CREATE INDEX `idx_schedule_offering` ON `tpm_schedule` (`offering_id`);

-- 开课表：按学期和课程查询
CREATE INDEX `idx_offering_semester_course` ON `tpm_course_offering` (`semester_id`, `course_id`);

-- 成绩记录表：按课程和学期查询（统计聚合高频使用）
CREATE INDEX `idx_grade_course_semester` ON `aem_grade_record` (`course_id`, `semester_id`);

-- 考试座位表：按考试ID查询（座位编排高频使用）
CREATE INDEX `idx_exam_seat_exam` ON `aem_exam_seat` (`exam_id`);

-- 监考分配表：按考试ID查询（监考派发高频使用）
CREATE INDEX `idx_exam_invigilation_exam` ON `aem_exam_invigilation` (`exam_id`);

-- 成绩复核表：按成绩ID查询（复核回写高频使用）
CREATE INDEX `idx_grade_review_grade` ON `aem_grade_review` (`grade_id`);

-- 评教结果表：按教师和课程查询（评教聚合高频使用）
CREATE INDEX `idx_eval_result_teacher_course` ON `aem_evaluation_result` (`teacher_id`, `course_id`);

-- 选课名单表：按学生ID查询（选课查询高频使用）
CREATE INDEX `idx_sel_enrollment_student` ON `tpm_selection_enrollment` (`student_id`);

-- 考试计划表：按学期查询
CREATE INDEX `idx_exam_plan_semester` ON `aem_exam_plan` (`semester_id`);

-- ===============================================
-- 3. 统一 del_flag 软删除字段
--    为缺少del_flag的关键业务表添加该字段
-- ===============================================

ALTER TABLE `aem_grade_record` ADD COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）';
ALTER TABLE `aem_exam_plan` ADD COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）';
ALTER TABLE `aem_exam_seat` ADD COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）';
ALTER TABLE `aem_exam_invigilation` ADD COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）';
ALTER TABLE `aem_grade_review` ADD COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）';
ALTER TABLE `aem_grade_statistics` ADD COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）';
ALTER TABLE `aem_evaluation_result` ADD COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）';
ALTER TABLE `tpm_schedule` ADD COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）';
ALTER TABLE `tpm_course_offering` ADD COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）';
ALTER TABLE `tpm_selection_enrollment` ADD COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）';
ALTER TABLE `tpm_selection_round` ADD COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）';
ALTER TABLE `sam_warning` ADD COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）';
ALTER TABLE `sam_graduation_review` ADD COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）';
ALTER TABLE `sam_degree_review` ADD COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）';
ALTER TABLE `oa_document` ADD COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）';
ALTER TABLE `dis_sync_task` ADD COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）';
ALTER TABLE `dis_interface_config` ADD COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）';

-- ===============================================
-- 4. 启用学业预警定时任务
--    将status从1(暂停)改为0(启用)，misfire_policy改为3(立即执行)
-- ===============================================

UPDATE sys_job SET status='0', misfire_policy='3' WHERE job_id=200 AND job_name='学业预警生成';

-- ===============================================
-- 5. 统一字符集为 utf8mb4 / utf8mb4_bin
--    修复 Flowable 表的字符集不一致问题
-- ===============================================

-- 将 Flowable 相关表的字符集统一为 utf8mb4
ALTER TABLE `act_re_procdef` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
ALTER TABLE `act_re_deployment` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
ALTER TABLE `act_ru_execution` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
ALTER TABLE `act_ru_task` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
ALTER TABLE `act_ru_variable` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
ALTER TABLE `act_hi_procinst` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
ALTER TABLE `act_hi_taskinst` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
ALTER TABLE `act_hi_varinst` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
