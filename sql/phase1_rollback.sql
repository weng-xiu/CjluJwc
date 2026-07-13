-- ===============================================
-- 教务系统升级 阶段一：回滚脚本
-- 内容：删除新增表、索引、菜单数据
-- 执行前请确认已备份相关数据
-- 创建时间：2026-07-13
-- ===============================================

-- ===============================================
-- 1. 删除新增菜单数据
-- ===============================================

-- 排课优化菜单及按钮权限
DELETE FROM `sys_menu` WHERE `menu_id` IN ('2620', '2621', '2622', '2623', '2624', '2625');

-- 预警规则配置管理菜单及按钮权限
DELETE FROM `sys_menu` WHERE `menu_id` IN ('2610', '2611', '2612', '2613', '2614', '2615');

-- GPA算法配置管理菜单及按钮权限
DELETE FROM `sys_menu` WHERE `menu_id` IN ('2600', '2601', '2602', '2603', '2604', '2605');

-- ===============================================
-- 2. 删除新增索引
-- ===============================================

-- 预警索引
DROP INDEX `idx_warning_student_semester` ON `sam_warning`;

-- 成绩记录索引
DROP INDEX `idx_grade_student_semester` ON `aem_grade_record`;

-- 排课索引
DROP INDEX `idx_schedule_classroom_time` ON `tpm_schedule`;

-- 选课名单索引
DROP INDEX `idx_sel_offering_status` ON `tpm_selection_enrollment`;
DROP INDEX `idx_sel_round_student` ON `tpm_selection_enrollment`;

-- ===============================================
-- 3. 删除新增表
-- ===============================================

-- 预警规则配置表
DROP TABLE IF EXISTS `sam_warning_rule_config`;

-- GPA分数段映射表
DROP TABLE IF EXISTS `aem_gpa_score_mapping`;

-- GPA算法配置表
DROP TABLE IF EXISTS `aem_gpa_algorithm_config`;
