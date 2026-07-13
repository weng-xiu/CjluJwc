-- ===============================================
-- 教务系统升级 阶段一：数据库升级脚本
-- 内容：新增GPA算法配置、预警规则配置、索引优化、菜单数据
-- 创建时间：2026-07-13
-- ===============================================

-- ===============================================
-- 1. 新增表
-- ===============================================

-- ----------------------------
-- 1.1 GPA算法配置表
-- ----------------------------
DROP TABLE IF EXISTS `aem_gpa_algorithm_config`;
CREATE TABLE `aem_gpa_algorithm_config` (
  `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `algorithm_code` varchar(50) NOT NULL COMMENT '算法代码(CN_STANDARD/GPA_4_0/CUSTOM)',
  `algorithm_name` varchar(100) NOT NULL COMMENT '算法名称',
  `description` varchar(500) DEFAULT NULL COMMENT '算法描述',
  `is_default` char(1) DEFAULT '0' COMMENT '是否默认（0否 1是）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_algorithm_code` (`algorithm_code`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='GPA算法配置表';

-- ----------------------------
-- 1.2 GPA分数段映射表
-- ----------------------------
DROP TABLE IF EXISTS `aem_gpa_score_mapping`;
CREATE TABLE `aem_gpa_score_mapping` (
  `mapping_id` bigint NOT NULL AUTO_INCREMENT COMMENT '映射ID',
  `config_id` bigint NOT NULL COMMENT '算法配置ID（关联aem_gpa_algorithm_config）',
  `min_score` decimal(5,2) NOT NULL COMMENT '最低分（含）',
  `max_score` decimal(5,2) NOT NULL COMMENT '最高分（含）',
  `grade_point` decimal(3,2) NOT NULL COMMENT '绩点值',
  `grade_level` varchar(20) DEFAULT NULL COMMENT '等级（优秀/良好/中等/及格/不及格）',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`mapping_id`),
  KEY `idx_config_id` (`config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='GPA分数段映射表';

-- ----------------------------
-- 1.3 预警规则配置表
-- ----------------------------
DROP TABLE IF EXISTS `sam_warning_rule_config`;
CREATE TABLE `sam_warning_rule_config` (
  `rule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `rule_code` varchar(50) NOT NULL COMMENT '规则代码(GPA_LOW/CREDIT_LOW/ATTENDANCE_LOW)',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `warning_type` char(1) NOT NULL COMMENT '预警类型（0成绩 1学分 2出勤 3综合）',
  `threshold_value` varchar(50) NOT NULL COMMENT '阈值',
  `warning_level` char(1) DEFAULT '0' COMMENT '预警级别（0一般 1严重 2高危）',
  `message_template` varchar(500) DEFAULT NULL COMMENT '消息模板',
  `is_enabled` char(1) DEFAULT '1' COMMENT '是否启用（0否 1是）',
  `semester_id` bigint DEFAULT NULL COMMENT '适用学期ID（空表示所有）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预警规则配置表';

-- ===============================================
-- 2. 新增索引（性能优化）
-- ===============================================

-- 选课名单索引（按轮次查询学生选课）
CREATE INDEX `idx_sel_round_student` ON `tpm_selection_enrollment` (`round_id`, `student_id`);
CREATE INDEX `idx_sel_offering_status` ON `tpm_selection_enrollment` (`course_offering_id`, `result_status`);

-- 排课索引（按教室和时间查询排课冲突）
CREATE INDEX `idx_schedule_classroom_time` ON `tpm_schedule` (`classroom_id`, `week_day`, `start_period`, `end_period`);

-- 成绩记录索引（按学生和学期查询成绩）
CREATE INDEX `idx_grade_student_semester` ON `aem_grade_record` (`student_id`, `semester_id`);

-- 预警索引（按学生和学期查询预警信息）
CREATE INDEX `idx_warning_student_semester` ON `sam_warning` (`student_id`, `semester_id`, `warning_type`);

-- ===============================================
-- 3. 初始数据 - GPA算法配置
-- ===============================================

-- 中国标准GPA算法（默认）
INSERT INTO `aem_gpa_algorithm_config` (`algorithm_code`, `algorithm_name`, `description`, `is_default`, `status`, `create_by`, `create_time`, `remark`) VALUES
('CN_STANDARD', '中国标准GPA算法', '采用中国高校通用绩点计算公式：绩点=(分数/10)-5，60分以下为0绩点', '1', '0', 'admin', sysdate(), '中国标准GPA算法');

-- 四分制GPA算法
INSERT INTO `aem_gpa_algorithm_config` (`algorithm_code`, `algorithm_name`, `description`, `is_default`, `status`, `create_by`, `create_time`, `remark`) VALUES
('GPA_4_0', '四分制GPA算法', '采用4.0分制绩点计算，A级4.0、B级3.0、C级2.0、D级1.0、F级0.0', '0', '0', 'admin', sysdate(), '四分制GPA算法');

-- ===============================================
-- 4. 初始数据 - GPA分数段映射
-- ===============================================

-- 中国标准GPA算法分数段映射（config_id=1）
INSERT INTO `aem_gpa_score_mapping` (`config_id`, `min_score`, `max_score`, `grade_point`, `grade_level`, `sort_order`) VALUES
(1, 90.00, 100.00, 4.00, '优秀', 1),
(1, 80.00, 89.99, 3.00, '良好', 2),
(1, 70.00, 79.99, 2.00, '中等', 3),
(1, 60.00, 69.99, 1.00, '及格', 4),
(1, 0.00, 59.99, 0.00, '不及格', 5);

-- 四分制GPA算法分数段映射（config_id=2）
INSERT INTO `aem_gpa_score_mapping` (`config_id`, `min_score`, `max_score`, `grade_point`, `grade_level`, `sort_order`) VALUES
(2, 90.00, 100.00, 4.00, '优秀', 1),
(2, 80.00, 89.99, 3.00, '良好', 2),
(2, 70.00, 79.99, 2.00, '中等', 3),
(2, 60.00, 69.99, 1.00, '及格', 4),
(2, 0.00, 59.99, 0.00, '不及格', 5);

-- ===============================================
-- 5. 初始数据 - 预警规则配置
-- ===============================================

-- GPA低预警规则
INSERT INTO `sam_warning_rule_config` (`rule_code`, `rule_name`, `warning_type`, `threshold_value`, `warning_level`, `message_template`, `is_enabled`, `status`, `create_by`, `create_time`, `remark`) VALUES
('GPA_LOW', 'GPA低预警', '0', '2.0', '1', '您的GPA为{gpa}，低于预警阈值{threshold}，请注意提升学业成绩', '1', '0', 'admin', sysdate(), 'GPA低于2.0触发成绩预警');

-- 学分不足预警规则
INSERT INTO `sam_warning_rule_config` (`rule_code`, `rule_name`, `warning_type`, `threshold_value`, `warning_level`, `message_template`, `is_enabled`, `status`, `create_by`, `create_time`, `remark`) VALUES
('CREDIT_LOW', '学分不足预警', '1', '70%', '1', '您已修学分{earned}学分，低于要求学分{required}的{threshold}，请注意修读进度', '1', '0', 'admin', sysdate(), '已修学分低于要求学分的70%触发学分预警');

-- 出勤率低预警规则
INSERT INTO `sam_warning_rule_config` (`rule_code`, `rule_name`, `warning_type`, `threshold_value`, `warning_level`, `message_template`, `is_enabled`, `status`, `create_by`, `create_time`, `remark`) VALUES
('ATTENDANCE_LOW', '出勤率低预警', '2', '80%', '0', '您的课程出勤率为{attendance_rate}%，低于预警阈值{threshold}%，请注意按时出勤', '1', '0', 'admin', sysdate(), '出勤率低于80%触发出勤预警');

-- ===============================================
-- 6. 新增菜单数据（menu_id从2600开始，避开2550-2562）
-- ===============================================

-- ====================
-- GPA算法配置管理 (parent=2205 成绩管理)
-- ====================
insert into sys_menu values('2600', 'GPA算法配置',   '2205', '4', 'gpaConfig',     'aem/gpaConfig/index',     '', '', 1, 0, 'C', '0', '0', 'aem:gpaConfig:list',   'slider',  'admin', sysdate(), '', null, '');

-- 按钮权限 - GPA算法配置 (parent=2600)
insert into sys_menu values('2601', '配置查询',     '2600', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gpaConfig:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2602', '配置新增',     '2600', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gpaConfig:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2603', '配置修改',     '2600', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gpaConfig:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2604', '配置删除',     '2600', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gpaConfig:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2605', '配置导出',     '2600', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gpaConfig:export',   '#', 'admin', sysdate(), '', null, '');

-- ====================
-- 预警规则配置管理 (parent=2301 学籍管理)
-- ====================
insert into sys_menu values('2610', '预警规则配置', '2301', '4', 'warningRule',   'sam/warningRule/index',    '', '', 1, 0, 'C', '0', '0', 'sam:warningRule:list', 'rule',    'admin', sysdate(), '', null, '');

-- 按钮权限 - 预警规则配置 (parent=2610)
insert into sys_menu values('2611', '规则查询',     '2610', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:warningRule:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2612', '规则新增',     '2610', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:warningRule:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2613', '规则修改',     '2610', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:warningRule:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2614', '规则删除',     '2610', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:warningRule:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2615', '规则导出',     '2610', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:warningRule:export',   '#', 'admin', sysdate(), '', null, '');

-- ====================
-- 排课优化 (parent=2105 开课与排课管理)
-- ====================
insert into sys_menu values('2620', '排课优化',     '2105', '4', 'scheduleOpt',   'tpm/scheduleOpt/index',    '', '', 1, 0, 'C', '0', '0', 'tpm:scheduleOpt:list', 'tool',    'admin', sysdate(), '', null, '');

-- 按钮权限 - 排课优化 (parent=2620)
insert into sys_menu values('2621', '优化查询',     '2620', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:scheduleOpt:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2622', '优化执行',     '2620', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:scheduleOpt:execute',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2623', '优化修改',     '2620', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:scheduleOpt:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2624', '优化删除',     '2620', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:scheduleOpt:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2625', '优化导出',     '2620', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:scheduleOpt:export',   '#', 'admin', sysdate(), '', null, '');
