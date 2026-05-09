-- ===============================================
-- 培养过程管理模块 (TPM) 数据库初始化脚本
-- 包含：培养方案管理、开课与排课管理、选课管理
-- ===============================================

-- ----------------------------
-- 1. 人才培养方案表
-- ----------------------------
DROP TABLE IF EXISTS `tpm_training_plan`;
CREATE TABLE `tpm_training_plan` (
  `plan_id` bigint NOT NULL AUTO_INCREMENT COMMENT '方案ID',
  `plan_name` varchar(200) NOT NULL COMMENT '方案名称',
  `major_id` bigint NOT NULL COMMENT '所属专业ID',
  `dept_id` bigint DEFAULT NULL COMMENT '所属院系ID',
  `education_level` varchar(20) DEFAULT NULL COMMENT '学历层次（本科/硕士/博士）',
  `plan_year` varchar(10) DEFAULT NULL COMMENT '方案年份（如2024级）',
  `total_credits` decimal(5,1) DEFAULT '0.0' COMMENT '总学分',
  `publish_status` char(1) DEFAULT '0' COMMENT '发布状态（0草稿 1已发布 2已废止）',
  `publish_date` date DEFAULT NULL COMMENT '发布日期',
  `version` varchar(20) DEFAULT '1.0' COMMENT '版本号',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`plan_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='人才培养方案表';

-- ----------------------------
-- 2. 课程库表
-- ----------------------------
DROP TABLE IF EXISTS `tpm_course_library`;
CREATE TABLE `tpm_course_library` (
  `course_id` bigint NOT NULL AUTO_INCREMENT COMMENT '课程ID',
  `course_code` varchar(50) NOT NULL COMMENT '课程编码',
  `course_name` varchar(200) NOT NULL COMMENT '课程名称',
  `course_name_en` varchar(200) DEFAULT NULL COMMENT '英文名称',
  `credit` decimal(3,1) NOT NULL DEFAULT '0.0' COMMENT '学分',
  `theory_hours` int DEFAULT '0' COMMENT '理论学时',
  `practice_hours` int DEFAULT '0' COMMENT '实践学时',
  `total_hours` int DEFAULT '0' COMMENT '总学时',
  `course_type` varchar(20) DEFAULT NULL COMMENT '课程类型（必修/选修/公选）',
  `course_category` varchar(50) DEFAULT NULL COMMENT '课程类别（通识/学科基础/专业核心/实践环节）',
  `assessment_method` varchar(20) DEFAULT NULL COMMENT '考核方式（考试/考查）',
  `semester_order` int DEFAULT '1' COMMENT '建议修读学期',
  `plan_id` bigint NOT NULL COMMENT '所属方案ID',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程库表';

-- ----------------------------
-- 3. 学分结构表
-- ----------------------------
DROP TABLE IF EXISTS `tpm_credit_structure`;
CREATE TABLE `tpm_credit_structure` (
  `struct_id` bigint NOT NULL AUTO_INCREMENT COMMENT '结构ID',
  `plan_id` bigint NOT NULL COMMENT '所属方案ID',
  `credit_type` varchar(50) NOT NULL COMMENT '学分类型编码',
  `credit_type_name` varchar(100) NOT NULL COMMENT '学分类型名称（通识必修/通识选修/学科基础/专业核心/实践环节等）',
  `required_credit` decimal(5,1) DEFAULT '0.0' COMMENT '要求学分',
  `min_credit` decimal(5,1) DEFAULT '0.0' COMMENT '最低学分',
  `description` varchar(255) DEFAULT NULL COMMENT '学分说明',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`struct_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学分结构表';

-- ----------------------------
-- 4. 开课计划表
-- ----------------------------
DROP TABLE IF EXISTS `tpm_course_offering`;
CREATE TABLE `tpm_course_offering` (
  `offering_id` bigint NOT NULL AUTO_INCREMENT COMMENT '开课ID',
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `teacher_id` bigint DEFAULT NULL COMMENT '教师ID',
  `campus_id` bigint DEFAULT NULL COMMENT '校区ID',
  `class_count` int DEFAULT '1' COMMENT '教学班数',
  `max_students` int DEFAULT '0' COMMENT '容量上限',
  `offering_status` char(1) DEFAULT '0' COMMENT '开课状态（0待确认 1已确认 2已取消）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`offering_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开课计划表';

-- ----------------------------
-- 5. 排课表
-- ----------------------------
DROP TABLE IF EXISTS `tpm_schedule`;
CREATE TABLE `tpm_schedule` (
  `schedule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '排课ID',
  `offering_id` bigint NOT NULL COMMENT '开课ID',
  `classroom_id` bigint DEFAULT NULL COMMENT '教室ID',
  `week_day` int DEFAULT NULL COMMENT '星期几（1-7）',
  `start_period` int DEFAULT NULL COMMENT '开始节次',
  `end_period` int DEFAULT NULL COMMENT '结束节次',
  `start_week` int DEFAULT NULL COMMENT '起始周',
  `end_week` int DEFAULT NULL COMMENT '结束周',
  `schedule_type` varchar(20) DEFAULT 'manual' COMMENT '排课方式（manual手动/auto自动）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`schedule_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='排课表';

-- ----------------------------
-- 6. 调停课申请表
-- ----------------------------
DROP TABLE IF EXISTS `tpm_schedule_adjustment`;
CREATE TABLE `tpm_schedule_adjustment` (
  `adjust_id` bigint NOT NULL AUTO_INCREMENT COMMENT '调整ID',
  `schedule_id` bigint NOT NULL COMMENT '排课ID',
  `adjust_type` varchar(20) NOT NULL COMMENT '调整类型（调课/停课/补课）',
  `original_date` date DEFAULT NULL COMMENT '原日期',
  `new_date` date DEFAULT NULL COMMENT '新日期',
  `new_classroom_id` bigint DEFAULT NULL COMMENT '新教室ID',
  `new_week_day` int DEFAULT NULL COMMENT '新星期几',
  `new_start_period` int DEFAULT NULL COMMENT '新开始节次',
  `new_end_period` int DEFAULT NULL COMMENT '新结束节次',
  `reason` varchar(500) DEFAULT NULL COMMENT '申请原因',
  `applicant` varchar(50) DEFAULT NULL COMMENT '申请人',
  `approve_status` char(1) DEFAULT '0' COMMENT '审批状态（0待审 1通过 2驳回）',
  `approve_by` varchar(50) DEFAULT NULL COMMENT '审批人',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`adjust_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='调停课申请表';

-- ----------------------------
-- 7. 选课轮次表
-- ----------------------------
DROP TABLE IF EXISTS `tpm_selection_round`;
CREATE TABLE `tpm_selection_round` (
  `round_id` bigint NOT NULL AUTO_INCREMENT COMMENT '轮次ID',
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `round_name` varchar(100) NOT NULL COMMENT '轮次名称（第一轮选课/第二轮选课/补退选）',
  `round_order` int DEFAULT '1' COMMENT '轮次顺序',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `max_courses_per_student` int DEFAULT '0' COMMENT '每人最多选课门数',
  `round_status` char(1) DEFAULT '0' COMMENT '轮次状态（0未开始 1进行中 2已结束）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`round_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='选课轮次表';

-- ----------------------------
-- 8. 选课规则表
-- ----------------------------
DROP TABLE IF EXISTS `tpm_selection_rule`;
CREATE TABLE `tpm_selection_rule` (
  `rule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `round_id` bigint NOT NULL COMMENT '轮次ID',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_type` varchar(50) NOT NULL COMMENT '规则类型（专业限制/年级限制/院系限制/人数上限/先修课程）',
  `restrict_target` varchar(100) DEFAULT NULL COMMENT '限制目标（如专业编码、年级等）',
  `restrict_value` varchar(255) DEFAULT NULL COMMENT '限制值',
  `priority` int DEFAULT '0' COMMENT '优先级',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`rule_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='选课规则表';

-- ----------------------------
-- 9. 选课名单表
-- ----------------------------
DROP TABLE IF EXISTS `tpm_selection_enrollment`;
CREATE TABLE `tpm_selection_enrollment` (
  `enroll_id` bigint NOT NULL AUTO_INCREMENT COMMENT '选课记录ID',
  `round_id` bigint NOT NULL COMMENT '轮次ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_offering_id` bigint NOT NULL COMMENT '开课ID',
  `select_time` datetime DEFAULT NULL COMMENT '选课时间',
  `lottery_result` char(1) DEFAULT '0' COMMENT '抽签结果（0未抽签 1中签 2未中签）',
  `result_status` char(1) DEFAULT '1' COMMENT '结果状态（1选中 2落选 3退课）',
  `drop_time` datetime DEFAULT NULL COMMENT '退课时间',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`enroll_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='选课名单表';
