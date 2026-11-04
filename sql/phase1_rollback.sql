-- ===============================================
-- 教务系统升级 阶段一：回滚脚本
-- 内容：删除新增表、索引、菜单数据
-- 执行前请确认已备份相关数据
-- 创建时间：2026-07-13
-- ===============================================

-- ===============================================
-- 1. 删除新增菜单数据
-- ===============================================

-- 排课优化菜单（数据源头是 fix_schedule_opt_menu.sql，phase1 已不再重复建）
-- 注意：不要再按 2620-2625 整段删！2621-2625 已被 ry_portal_cms_menu.sql 的
-- 「文章管理」按钮占用，整段删会误删门户 CMS 的授权行。按 perms 精确删最安全。
DELETE FROM `sys_menu` WHERE `perms` = 'tpm:scheduleOpt:list';

-- 预警规则配置管理菜单及按钮权限（G4/V6.5：已从 2610-2615 改派到 2352-2357）
DELETE FROM `sys_menu` WHERE `menu_id` IN ('2352', '2353', '2354', '2355', '2356', '2357');

-- GPA 算法配置菜单：G4/V6.5 起由 fix_gpa_config_menu.sql 的 2900-2906 提供，
-- 本回滚脚本不再处理；如需回滚该页，另写成对迁移（db/migration + db/rollback）。

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
