-- ===============================================
-- 考核与评价管理模块 (AEM) 数据库初始化脚本
-- 包含：考试管理、成绩管理、教学质量评价
-- ===============================================

-- ----------------------------
-- 1. 考试安排表
-- ----------------------------
DROP TABLE IF EXISTS `aem_exam_plan`;
CREATE TABLE `aem_exam_plan` (
  `exam_id` bigint NOT NULL AUTO_INCREMENT COMMENT '考试ID',
  `exam_name` varchar(200) NOT NULL COMMENT '考试名称',
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `exam_type` char(1) DEFAULT '0' COMMENT '考试类型（0期末考试 1补考 2重修考试）',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID（关联tpm_course_library）',
  `exam_date` date DEFAULT NULL COMMENT '考试日期',
  `start_time` varchar(10) DEFAULT NULL COMMENT '开始时间（如14:30）',
  `end_time` varchar(10) DEFAULT NULL COMMENT '结束时间（如16:30）',
  `duration` int DEFAULT '120' COMMENT '考试时长（分钟）',
  `total_students` int DEFAULT '0' COMMENT '考生人数',
  `plan_status` char(1) DEFAULT '0' COMMENT '安排状态（0未安排 1已安排 2已发布）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`exam_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考试安排表';

-- ----------------------------
-- 2. 监考分配表
-- ----------------------------
DROP TABLE IF EXISTS `aem_exam_invigilation`;
CREATE TABLE `aem_exam_invigilation` (
  `invigilation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '监考ID',
  `exam_id` bigint NOT NULL COMMENT '考试ID',
  `classroom_id` bigint DEFAULT NULL COMMENT '教室ID（关联brm_classroom）',
  `teacher_id` bigint DEFAULT NULL COMMENT '监考教师ID（关联brm_teacher）',
  `exam_date` date DEFAULT NULL COMMENT '考试日期',
  `start_time` varchar(10) DEFAULT NULL COMMENT '开始时间',
  `end_time` varchar(10) DEFAULT NULL COMMENT '结束时间',
  `duty_type` char(1) DEFAULT '0' COMMENT '职责（0主监考 1副监考 2巡考）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`invigilation_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='监考分配表';

-- ----------------------------
-- 3. 考场座位编排表
-- ----------------------------
DROP TABLE IF EXISTS `aem_exam_seat`;
CREATE TABLE `aem_exam_seat` (
  `seat_id` bigint NOT NULL AUTO_INCREMENT COMMENT '座位ID',
  `exam_id` bigint NOT NULL COMMENT '考试ID',
  `classroom_id` bigint DEFAULT NULL COMMENT '教室ID（关联brm_classroom）',
  `student_id` bigint DEFAULT NULL COMMENT '学生ID',
  `seat_number` int DEFAULT NULL COMMENT '座位号',
  `row_number` int DEFAULT NULL COMMENT '行号',
  `col_number` int DEFAULT NULL COMMENT '列号',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1缺考）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`seat_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考场座位编排表';

-- ----------------------------
-- 4. 成绩记录表
-- ----------------------------
DROP TABLE IF EXISTS `aem_grade_record`;
CREATE TABLE `aem_grade_record` (
  `grade_id` bigint NOT NULL AUTO_INCREMENT COMMENT '成绩ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID（关联tpm_course_library）',
  `semester_id` bigint NOT NULL COMMENT '学期ID（关联brm_semester）',
  `exam_type` char(1) DEFAULT '0' COMMENT '考试类型（0正考 1补考 2重修）',
  `regular_score` decimal(5,2) DEFAULT '0.00' COMMENT '平时成绩',
  `exam_score` decimal(5,2) DEFAULT '0.00' COMMENT '考试成绩',
  `total_score` decimal(5,2) DEFAULT '0.00' COMMENT '总成绩',
  `grade_point` decimal(3,1) DEFAULT '0.0' COMMENT '绩点',
  `grade_level` char(1) DEFAULT NULL COMMENT '等级（A优秀 B良好 C中等 D及格 F不及格）',
  `is_pass` char(1) DEFAULT '0' COMMENT '是否通过（0否 1是）',
  `is_reviewed` char(1) DEFAULT '0' COMMENT '是否已复核（0未复核 1已复核）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`grade_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成绩记录表';

-- ----------------------------
-- 5. 成绩复核审批表
-- ----------------------------
DROP TABLE IF EXISTS `aem_grade_review`;
CREATE TABLE `aem_grade_review` (
  `review_id` bigint NOT NULL AUTO_INCREMENT COMMENT '复核ID',
  `grade_id` bigint NOT NULL COMMENT '成绩ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID',
  `original_score` decimal(5,2) DEFAULT '0.00' COMMENT '原成绩',
  `new_score` decimal(5,2) DEFAULT '0.00' COMMENT '新成绩',
  `review_reason` varchar(500) DEFAULT NULL COMMENT '复核原因',
  `review_type` char(1) DEFAULT '0' COMMENT '复核类型（0成绩修改 1成绩复核）',
  `approve_status` char(1) DEFAULT '0' COMMENT '审批状态（0待审 1通过 2驳回）',
  `approve_by` varchar(50) DEFAULT NULL COMMENT '审批人',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approve_opinion` varchar(255) DEFAULT NULL COMMENT '审批意见',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`review_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成绩复核审批表';

-- ----------------------------
-- 6. 成绩统计分析表
-- ----------------------------
DROP TABLE IF EXISTS `aem_grade_statistics`;
CREATE TABLE `aem_grade_statistics` (
  `stat_id` bigint NOT NULL AUTO_INCREMENT COMMENT '统计ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID',
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `class_id` bigint DEFAULT NULL COMMENT '班级ID（关联brm_class）',
  `total_students` int DEFAULT '0' COMMENT '总人数',
  `max_score` decimal(5,2) DEFAULT '0.00' COMMENT '最高分',
  `min_score` decimal(5,2) DEFAULT '0.00' COMMENT '最低分',
  `avg_score` decimal(5,2) DEFAULT '0.00' COMMENT '平均分',
  `pass_count` int DEFAULT '0' COMMENT '通过人数',
  `fail_count` int DEFAULT '0' COMMENT '不及格人数',
  `pass_rate` decimal(5,2) DEFAULT '0.00' COMMENT '通过率（%）',
  `excellent_count` int DEFAULT '0' COMMENT '优秀人数（90分以上）',
  `excellent_rate` decimal(5,2) DEFAULT '0.00' COMMENT '优秀率（%）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`stat_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成绩统计分析表';

-- ----------------------------
-- 7. 评教问卷配置表
-- ----------------------------
DROP TABLE IF EXISTS `aem_evaluation_questionnaire`;
CREATE TABLE `aem_evaluation_questionnaire` (
  `questionnaire_id` bigint NOT NULL AUTO_INCREMENT COMMENT '问卷ID',
  `semester_id` bigint NOT NULL COMMENT '学期ID（关联brm_semester）',
  `title` varchar(200) NOT NULL COMMENT '问卷标题',
  `description` varchar(500) DEFAULT NULL COMMENT '问卷说明',
  `question_count` int DEFAULT '0' COMMENT '题目数量',
  `full_score` decimal(5,2) DEFAULT '100.00' COMMENT '满分',
  `start_time` datetime DEFAULT NULL COMMENT '评教开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '评教结束时间',
  `eval_status` char(1) DEFAULT '0' COMMENT '评教状态（0未开始 1进行中 2已结束）',
  `is_anonymous` char(1) DEFAULT '1' COMMENT '是否匿名（0实名 1匿名）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`questionnaire_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='评教问卷配置表';

-- ----------------------------
-- 8. 评教问题表
-- ----------------------------
DROP TABLE IF EXISTS `aem_evaluation_question`;
CREATE TABLE `aem_evaluation_question` (
  `question_id` bigint NOT NULL AUTO_INCREMENT COMMENT '问题ID',
  `questionnaire_id` bigint NOT NULL COMMENT '问卷ID',
  `question_type` char(1) DEFAULT '0' COMMENT '问题类型（0单选 1多选 2评分 3文本）',
  `question_content` varchar(500) NOT NULL COMMENT '问题内容',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `max_score` decimal(4,1) DEFAULT '10.0' COMMENT '最高评分（评分类型时有效）',
  `options_json` text COMMENT '选项JSON（[{"label":"非常满意","value":5},...]）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`question_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='评教问题表';

-- ----------------------------
-- 9. 评教结果表
-- ----------------------------
DROP TABLE IF EXISTS `aem_evaluation_result`;
CREATE TABLE `aem_evaluation_result` (
  `result_id` bigint NOT NULL AUTO_INCREMENT COMMENT '结果ID',
  `questionnaire_id` bigint NOT NULL COMMENT '问卷ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID',
  `teacher_id` bigint DEFAULT NULL COMMENT '教师ID（关联brm_teacher）',
  `student_id` bigint DEFAULT NULL COMMENT '学生ID',
  `total_score` decimal(5,2) DEFAULT '0.00' COMMENT '总评分',
  `eval_date` datetime DEFAULT NULL COMMENT '评教时间',
  `comment` varchar(500) DEFAULT NULL COMMENT '评语建议',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`result_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='评教结果表';

-- ----------------------------
-- 10. 督导听课记录表
-- ----------------------------
DROP TABLE IF EXISTS `aem_supervision_record`;
CREATE TABLE `aem_supervision_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID',
  `teacher_id` bigint DEFAULT NULL COMMENT '授课教师ID（关联brm_teacher）',
  `supervisor` varchar(50) NOT NULL COMMENT '督导姓名',
  `visit_date` date DEFAULT NULL COMMENT '听课日期',
  `class_hours` int DEFAULT '1' COMMENT '听课节数',
  `teaching_content` varchar(500) DEFAULT NULL COMMENT '教学内容',
  `evaluation_score` decimal(4,1) DEFAULT '0.0' COMMENT '评价评分',
  `evaluation_level` char(1) DEFAULT '0' COMMENT '评价等级（0优秀 1良好 2合格 3不合格）',
  `suggestion` varchar(500) DEFAULT NULL COMMENT '改进建议',
  `record_type` char(1) DEFAULT '0' COMMENT '记录类型（0常规听课 1专项督导 2反馈复查）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='督导听课记录表';
