-- G4 CI 基线 · 结构快照（自动生成于 2026-10-18 12:43，勿手改；重生成：bin/db-ci-export.ps1）
-- 注：不含 ACT_*/FLW_*（Flowable 自有表），由集成测试 it profile 的 database-schema-update:true 启动自建
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `aem_evaluation_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `aem_evaluation_question` (
  `question_id` bigint NOT NULL AUTO_INCREMENT COMMENT '问题ID',
  `questionnaire_id` bigint NOT NULL COMMENT '问卷ID',
  `question_type` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '问题类型（0单选 1多选 2评分 3文本）',
  `question_content` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '问题内容',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `max_score` decimal(4,1) DEFAULT '10.0' COMMENT '最高评分（评分类型时有效）',
  `options_json` text COLLATE utf8mb4_general_ci COMMENT '选项JSON（[{"label":"非常满意","value":5},...]）',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`question_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9127 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='评教问题表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `aem_evaluation_questionnaire`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `aem_evaluation_questionnaire` (
  `questionnaire_id` bigint NOT NULL AUTO_INCREMENT COMMENT '问卷ID',
  `semester_id` bigint NOT NULL COMMENT '学期ID（关联brm_semester）',
  `title` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '问卷标题',
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '问卷说明',
  `question_count` int DEFAULT '0' COMMENT '题目数量',
  `full_score` decimal(5,2) DEFAULT '100.00' COMMENT '满分',
  `start_time` datetime DEFAULT NULL COMMENT '评教开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '评教结束时间',
  `eval_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '评教状态（0未开始 1进行中 2已结束）',
  `is_anonymous` char(1) COLLATE utf8mb4_general_ci DEFAULT '1' COMMENT '是否匿名（0实名 1匿名）',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`questionnaire_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9004 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='评教问卷配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `aem_evaluation_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `aem_evaluation_result` (
  `result_id` bigint NOT NULL AUTO_INCREMENT COMMENT '结果ID',
  `questionnaire_id` bigint NOT NULL COMMENT '问卷ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID',
  `teacher_id` bigint DEFAULT NULL COMMENT '教师ID（关联brm_teacher）',
  `student_id` bigint DEFAULT NULL COMMENT '学生ID',
  `total_score` decimal(5,2) DEFAULT '0.00' COMMENT '总评分',
  `eval_date` datetime DEFAULT NULL COMMENT '评教时间',
  `comment` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '评语建议',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`result_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='评教结果表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `aem_exam_invigilation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `aem_exam_invigilation` (
  `invigilation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '监考ID',
  `exam_id` bigint NOT NULL COMMENT '考试ID',
  `classroom_id` bigint DEFAULT NULL COMMENT '教室ID（关联brm_classroom）',
  `teacher_id` bigint DEFAULT NULL COMMENT '监考教师ID（关联brm_teacher）',
  `exam_date` date DEFAULT NULL COMMENT '考试日期',
  `start_time` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '开始时间',
  `end_time` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结束时间',
  `duty_type` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '职责（0主监考 1副监考 2巡考）',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`invigilation_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9047 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='监考分配表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `aem_exam_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `aem_exam_plan` (
  `exam_id` bigint NOT NULL AUTO_INCREMENT COMMENT '考试ID',
  `exam_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '考试名称',
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `exam_type` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '考试类型（0期末考试 1补考 2重修考试）',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID（关联tpm_course_library）',
  `exam_date` date DEFAULT NULL COMMENT '考试日期',
  `start_time` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '开始时间（如14:30）',
  `end_time` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结束时间（如16:30）',
  `duration` int DEFAULT '120' COMMENT '考试时长（分钟）',
  `total_students` int DEFAULT '0' COMMENT '考生人数',
  `plan_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '安排状态（0未安排 1已安排 2已发布）',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`exam_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9009 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考试安排表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `aem_exam_seat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `aem_exam_seat` (
  `seat_id` bigint NOT NULL AUTO_INCREMENT COMMENT '座位ID',
  `exam_id` bigint NOT NULL COMMENT '考试ID',
  `classroom_id` bigint DEFAULT NULL COMMENT '教室ID（关联brm_classroom）',
  `student_id` bigint DEFAULT NULL COMMENT '学生ID',
  `seat_number` int DEFAULT NULL COMMENT '座位号',
  `row_number` int DEFAULT NULL COMMENT '行号',
  `col_number` int DEFAULT NULL COMMENT '列号',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1缺考）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`seat_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9608 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考场座位编排表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `aem_grade_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `aem_grade_record` (
  `grade_id` bigint NOT NULL AUTO_INCREMENT COMMENT '成绩ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID（关联tpm_course_library）',
  `semester_id` bigint NOT NULL COMMENT '学期ID（关联brm_semester）',
  `exam_type` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '考试类型（0正考 1补考 2重修）',
  `regular_score` decimal(5,2) DEFAULT '0.00' COMMENT '平时成绩',
  `exam_score` decimal(5,2) DEFAULT '0.00' COMMENT '考试成绩',
  `total_score` decimal(5,2) DEFAULT '0.00' COMMENT '总成绩',
  `grade_point` decimal(3,1) DEFAULT '0.0' COMMENT '绩点',
  `grade_level` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '等级（A优秀 B良好 C中等 D及格 F不及格）',
  `is_pass` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '是否通过（0否 1是）',
  `is_reviewed` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '是否已复核（0未复核 1已复核）',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `submit_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '提交状态（0未提交 1已提交待审 2已锁定 3已驳回）',
  `submit_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '提交人',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `lock_time` datetime DEFAULT NULL COMMENT '锁定时间',
  PRIMARY KEY (`grade_id`),
  KEY `idx_grade_submit_status` (`submit_status`)
) ENGINE=InnoDB AUTO_INCREMENT=9732 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成绩记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `aem_grade_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `aem_grade_review` (
  `review_id` bigint NOT NULL AUTO_INCREMENT COMMENT '复核ID',
  `grade_id` bigint NOT NULL COMMENT '成绩ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID',
  `original_score` decimal(5,2) DEFAULT '0.00' COMMENT '原成绩',
  `new_score` decimal(5,2) DEFAULT '0.00' COMMENT '新成绩',
  `review_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '复核原因',
  `review_type` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '复核类型（0成绩修改 1成绩复核）',
  `approve_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '审批状态（0待审/初审中 4待教务处终审 1通过 2驳回 3已撤销）',
  `approve_by` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批人',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approve_opinion` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批意见',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `proc_inst_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Flowable流程实例ID',
  `dept_approve_by` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '课程负责人初审人',
  `dept_approve_time` datetime DEFAULT NULL COMMENT '课程负责人初审时间',
  `dept_opinion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '课程负责人初审意见',
  `aa_approve_by` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '教务处终审人',
  `aa_approve_time` datetime DEFAULT NULL COMMENT '教务处终审时间',
  `aa_opinion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '教务处终审意见',
  PRIMARY KEY (`review_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9785 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成绩复核审批表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `aem_grade_statistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`stat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成绩统计分析表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `aem_grade_weight`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `aem_grade_weight` (
  `weight_id` bigint NOT NULL AUTO_INCREMENT COMMENT '权重ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID（关联tpm_course_library，NULL表示非课程级）',
  `course_category` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '课程类别（关联字典tpm_course_category，NULL表示全局默认）',
  `regular_ratio` decimal(5,2) NOT NULL DEFAULT '30.00' COMMENT '平时成绩占比（%）',
  `exam_ratio` decimal(5,2) NOT NULL DEFAULT '70.00' COMMENT '考试成绩占比（%）',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`weight_id`),
  KEY `idx_grade_weight_course` (`course_id`),
  KEY `idx_grade_weight_category` (`course_category`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成绩权重配置表（A4）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `aem_supervision_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `aem_supervision_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID',
  `teacher_id` bigint DEFAULT NULL COMMENT '授课教师ID（关联brm_teacher）',
  `supervisor` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '督导姓名',
  `visit_date` date DEFAULT NULL COMMENT '听课日期',
  `class_hours` int DEFAULT '1' COMMENT '听课节数',
  `teaching_content` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '教学内容',
  `evaluation_score` decimal(4,1) DEFAULT '0.0' COMMENT '评价评分',
  `evaluation_level` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '评价等级（0优秀 1良好 2合格 3不合格）',
  `suggestion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '改进建议',
  `record_type` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '记录类型（0常规听课 1专项督导 2反馈复查）',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='督导听课记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `brm_academic_year`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brm_academic_year` (
  `year_id` bigint NOT NULL AUTO_INCREMENT COMMENT '学年ID',
  `year_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '学年名称',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`year_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学年表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `brm_building`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brm_building` (
  `building_id` bigint NOT NULL AUTO_INCREMENT COMMENT '教学楼ID',
  `building_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '教学楼名称',
  `building_code` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '教学楼编码',
  `campus_id` bigint DEFAULT NULL COMMENT '所属校区ID',
  `floor_count` int DEFAULT '1' COMMENT '楼层数',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`building_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教学楼表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `brm_campus`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brm_campus` (
  `campus_id` bigint NOT NULL AUTO_INCREMENT COMMENT '校区ID',
  `campus_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '校区名称',
  `campus_address` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '校区地址',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`campus_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='校区表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `brm_class`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brm_class` (
  `class_id` bigint NOT NULL AUTO_INCREMENT COMMENT '班级ID',
  `class_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '班级编码',
  `class_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '班级名称',
  `major_id` bigint DEFAULT NULL COMMENT '所属专业ID',
  `grade` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '年级',
  `student_count` int DEFAULT '0' COMMENT '学生人数',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`class_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='班级表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `brm_classroom`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brm_classroom` (
  `classroom_id` bigint NOT NULL AUTO_INCREMENT COMMENT '教室ID',
  `classroom_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '教室名称',
  `building_id` bigint DEFAULT NULL COMMENT '所属教学楼ID',
  `type_id` bigint DEFAULT NULL COMMENT '教室类型ID',
  `capacity` int DEFAULT '0' COMMENT '容纳人数',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`classroom_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9005 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教室表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `brm_classroom_borrow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brm_classroom_borrow` (
  `borrow_id` bigint NOT NULL AUTO_INCREMENT COMMENT '借用ID',
  `classroom_id` bigint NOT NULL COMMENT '教室ID',
  `applicant` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '申请人',
  `applicant_dept` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '申请人部门',
  `borrow_date` date NOT NULL COMMENT '借用日期',
  `start_time` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '开始时间',
  `end_time` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结束时间',
  `purpose` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '借用用途',
  `approve_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '审批状态（0待院系审核 1待教务处审核 2已通过 3已驳回 4已撤销）',
  `approve_by` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批人',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `applicant_user_id` bigint DEFAULT NULL COMMENT '申请用户ID（门户申请人绑定）',
  `contact_phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
  `attendee_count` int DEFAULT NULL COMMENT '借用人次',
  `dept_approve_by` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '院系初审人',
  `dept_approve_time` datetime DEFAULT NULL COMMENT '院系初审时间',
  `dept_opinion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '院系初审意见',
  `aa_approve_by` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '教务处终审人',
  `aa_approve_time` datetime DEFAULT NULL COMMENT '教务处终审时间',
  `aa_opinion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '教务处终审意见',
  `proc_inst_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Flowable流程实例ID',
  PRIMARY KEY (`borrow_id`),
  KEY `idx_borrow_classroom_date` (`classroom_id`,`borrow_date`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教室借用表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `brm_classroom_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brm_classroom_type` (
  `type_id` bigint NOT NULL AUTO_INCREMENT COMMENT '类型ID',
  `type_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型名称',
  `type_desc` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '类型描述',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教室类型表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `brm_department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brm_department` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '院系ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父院系ID',
  `ancestors` varchar(500) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '祖级列表',
  `dept_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '院系编码',
  `dept_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '院系名称',
  `leader` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '负责人',
  `phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=216 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='院系表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `brm_equipment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brm_equipment` (
  `equip_id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  `equip_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '设备名称',
  `classroom_id` bigint DEFAULT NULL COMMENT '所属教室ID',
  `equip_type` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备类型',
  `model` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备型号',
  `purchase_date` date DEFAULT NULL COMMENT '购置日期',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`equip_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='多媒体设备表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `brm_equipment_maintenance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brm_equipment_maintenance` (
  `maintenance_id` bigint NOT NULL AUTO_INCREMENT COMMENT '维护ID',
  `equip_id` bigint NOT NULL COMMENT '设备ID',
  `fault_desc` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '故障描述',
  `repair_date` date DEFAULT NULL COMMENT '维修日期',
  `repair_by` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '维修人',
  `repair_cost` decimal(10,2) DEFAULT '0.00' COMMENT '维修费用',
  `result` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '维修结果',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`maintenance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备维护记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `brm_major`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brm_major` (
  `major_id` bigint NOT NULL AUTO_INCREMENT COMMENT '专业ID',
  `major_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '专业编码',
  `major_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '专业名称',
  `dept_id` bigint DEFAULT NULL COMMENT '所属院系ID',
  `education_level` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '学历层次（本科/硕士/博士）',
  `duration` int DEFAULT '4' COMMENT '学制（年）',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`major_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='专业表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `brm_semester`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brm_semester` (
  `semester_id` bigint NOT NULL AUTO_INCREMENT COMMENT '学期ID',
  `semester_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '学期名称',
  `academic_year_id` bigint NOT NULL COMMENT '学年ID',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `semester_order` int DEFAULT '1' COMMENT '学期排序',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`semester_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学期表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `brm_teacher`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brm_teacher` (
  `teacher_id` bigint NOT NULL AUTO_INCREMENT COMMENT '教师ID',
  `teacher_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '教师工号',
  `teacher_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '教师姓名',
  `user_id` bigint DEFAULT NULL COMMENT '关联系统用户ID（关联sys_user）',
  `dept_id` bigint DEFAULT NULL COMMENT '所属院系ID',
  `gender` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '性别（0男 1女 2未知）',
  `phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `title` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '职称',
  `education` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '学历',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`teacher_id`)
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `brm_teacher_position`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brm_teacher_position` (
  `pos_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任职ID',
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `dept_id` bigint DEFAULT NULL COMMENT '任职院系ID',
  `position_title` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '任职岗位',
  `start_date` date DEFAULT NULL COMMENT '任职开始日期',
  `end_date` date DEFAULT NULL COMMENT '任职结束日期',
  `is_current` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '是否现任（0是 1否）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`pos_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师任职信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `brm_teacher_qualification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brm_teacher_qualification` (
  `qual_id` bigint NOT NULL AUTO_INCREMENT COMMENT '资格ID',
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `course_category` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '可授课程类别',
  `certify_authority` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '认证机构',
  `qualify_date` date DEFAULT NULL COMMENT '获证日期',
  `expire_date` date DEFAULT NULL COMMENT '到期日期',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`qual_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师授课资格表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dis_data_exchange_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dis_data_exchange_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `system_id` bigint DEFAULT NULL COMMENT '外部系统ID',
  `interface_id` bigint DEFAULT NULL COMMENT '接口ID',
  `request_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '请求完整URL',
  `request_method` varchar(10) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '请求方式',
  `request_data` longtext COLLATE utf8mb4_general_ci COMMENT '请求数据',
  `response_data` longtext COLLATE utf8mb4_general_ci COMMENT '响应数据',
  `response_code` int DEFAULT NULL COMMENT 'HTTP响应码',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '执行状态（0成功 1失败）',
  `error_msg` varchar(2000) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '错误消息',
  `execute_time` datetime DEFAULT NULL COMMENT '执行时间',
  `cost_time` bigint DEFAULT '0' COMMENT '消耗时间（毫秒）',
  `operator` varchar(50) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '操作人员',
  `sync_batch_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '鍚屾?鎵规?鍙凤紙涓??鎵ц?鍏ㄩ摼璺?暀鐥曪級',
  `retry_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '鏄?惁浜哄伐閲嶆帹锛?鍚?1鏄?級',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_system_id` (`system_id`),
  KEY `idx_status` (`status`),
  KEY `idx_execute_time` (`execute_time`),
  KEY `idx_sync_batch_no` (`sync_batch_no`)
) ENGINE=InnoDB AUTO_INCREMENT=10964 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据交换日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dis_external_system`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dis_external_system` (
  `system_id` bigint NOT NULL AUTO_INCREMENT COMMENT '系统ID',
  `system_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '系统名称',
  `system_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '系统编码',
  `system_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '系统类型（GRADUATE-研究生系统 FINANCE-财务系统 CARD-一卡通系统 AUTH-统一身份认证平台）',
  `base_url` varchar(255) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '基础URL',
  `auth_type` varchar(20) COLLATE utf8mb4_general_ci DEFAULT 'TOKEN' COMMENT '认证方式（TOKEN BASIC OAUTH2 NONE）',
  `auth_config` text COLLATE utf8mb4_general_ci COMMENT '认证配置（JSON格式，如token、appKey等）',
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '系统描述',
  `contact_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '对接负责人',
  `contact_phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '联系电话',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`system_id`),
  UNIQUE KEY `uk_system_code` (`system_code`)
) ENGINE=InnoDB AUTO_INCREMENT=7002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='外部系统配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dis_field_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dis_field_mapping` (
  `mapping_id` bigint NOT NULL AUTO_INCREMENT COMMENT '映射ID',
  `interface_id` bigint NOT NULL COMMENT '接口ID（关联 dis_interface_config）',
  `target_table` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标业务表名',
  `source_field` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '源字段名（响应JSON键，支持 a.b.c 嵌套路径）',
  `target_column` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标列名',
  `key_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '是否主键/唯一键（0否 1是）',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`mapping_id`),
  KEY `idx_interface_id` (`interface_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据同步字段映射配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dis_interface_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dis_interface_config` (
  `interface_id` bigint NOT NULL AUTO_INCREMENT COMMENT '接口ID',
  `system_id` bigint NOT NULL COMMENT '所属系统ID',
  `interface_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '接口名称',
  `interface_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '接口编码',
  `request_method` varchar(10) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'GET' COMMENT '请求方式（GET POST PUT DELETE）',
  `request_path` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '请求路径',
  `request_template` text COLLATE utf8mb4_general_ci COMMENT '请求模板（JSON格式参数定义）',
  `response_template` text COLLATE utf8mb4_general_ci COMMENT '响应模板',
  `timeout_seconds` int DEFAULT '30' COMMENT '超时时间（秒）',
  `retry_count` int DEFAULT '0' COMMENT '重试次数',
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '接口描述',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`interface_id`),
  KEY `idx_system_id` (`system_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='接口配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dis_sync_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dis_sync_task` (
  `task_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `task_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务编码',
  `system_id` bigint NOT NULL COMMENT '外部系统ID',
  `interface_id` bigint DEFAULT NULL COMMENT '接口ID',
  `cron_expression` varchar(100) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT 'Cron表达式',
  `last_execute_time` datetime DEFAULT NULL COMMENT '上次执行时间',
  `next_execute_time` datetime DEFAULT NULL COMMENT '下次执行时间',
  `execute_count` int DEFAULT '0' COMMENT '执行次数',
  `fail_count` int DEFAULT '0' COMMENT '失败次数',
  `sync_mode` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '鍚屾?妯″紡锛?鍏ㄩ噺 1澧為噺锛',
  `last_watermark` datetime DEFAULT NULL COMMENT '澧為噺姘翠綅锛堜笂娆℃垚鍔熷悓姝ョ殑鏁版嵁鏃堕棿鐐癸級',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_task_code` (`task_code`),
  KEY `idx_system_id` (`system_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据同步任务表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `gen_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table` (
  `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_name` varchar(200) DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `tpl_web_type` varchar(30) DEFAULT '' COMMENT '前端模板类型（element-ui模版 element-plus模版）',
  `package_name` varchar(100) DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) DEFAULT NULL COMMENT '生成功能作者',
  `form_col_num` int DEFAULT '1' COMMENT '表单布局（单列 双列 三列）',
  `gen_type` char(1) DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) DEFAULT NULL COMMENT '其它生成选项',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成业务表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `gen_table_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table_column` (
  `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_id` bigint DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) DEFAULT NULL COMMENT '列类型',
  `java_type` varchar(500) DEFAULT NULL COMMENT 'JAVA类型',
  `java_field` varchar(200) DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk` char(1) DEFAULT NULL COMMENT '是否主键（1是）',
  `is_increment` char(1) DEFAULT NULL COMMENT '是否自增（1是）',
  `is_required` char(1) DEFAULT NULL COMMENT '是否必填（1是）',
  `is_insert` char(1) DEFAULT NULL COMMENT '是否为插入字段（1是）',
  `is_edit` char(1) DEFAULT NULL COMMENT '是否编辑字段（1是）',
  `is_list` char(1) DEFAULT NULL COMMENT '是否列表字段（1是）',
  `is_query` char(1) DEFAULT NULL COMMENT '是否查询字段（1是）',
  `query_type` varchar(200) DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  `html_type` varchar(200) DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  `dict_type` varchar(200) DEFAULT '' COMMENT '字典类型',
  `sort` int DEFAULT NULL COMMENT '排序',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`column_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成业务表字段';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `oa_countersign_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_countersign_batch` (
  `batch_id` bigint NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `task_id` varchar(64) NOT NULL COMMENT 'Flowable任务ID',
  `proc_inst_id` varchar(64) DEFAULT NULL COMMENT 'Flowable流程实例ID',
  `process_name` varchar(100) DEFAULT NULL COMMENT '流程名称',
  `node_id` varchar(64) DEFAULT NULL COMMENT '节点ID',
  `node_name` varchar(100) DEFAULT NULL COMMENT '节点名称',
  `sign_mode` char(1) NOT NULL DEFAULT '0' COMMENT '动作类型（0前加签 1后加签 2会签 3委托）',
  `sign_rule` varchar(10) DEFAULT 'ALL' COMMENT '表决规则（ALL全部同意 ANY一人同意即定论）',
  `initiator` varchar(50) NOT NULL COMMENT '发起人登录名',
  `total_count` int DEFAULT '0' COMMENT '应处理人数',
  `done_count` int DEFAULT '0' COMMENT '已处理人数',
  `agree_count` int DEFAULT '0' COMMENT '同意人数',
  `disagree_count` int DEFAULT '0' COMMENT '不同意人数',
  `status` char(1) DEFAULT '0' COMMENT '批次状态（0进行中 1意见已齐 2已取消）',
  `release_time` datetime DEFAULT NULL COMMENT '意见齐备（门禁释放）时间',
  `create_by` varchar(50) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`batch_id`),
  KEY `idx_cs_batch_task` (`task_id`),
  KEY `idx_cs_batch_proc` (`proc_inst_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流加签/会签/委托批次表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `oa_countersign_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_countersign_item` (
  `item_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `batch_id` bigint NOT NULL COMMENT '批次ID',
  `task_id` varchar(64) DEFAULT NULL COMMENT 'Flowable任务ID',
  `proc_inst_id` varchar(64) DEFAULT NULL COMMENT 'Flowable流程实例ID',
  `handler` varchar(50) NOT NULL COMMENT '协同办理人登录名',
  `handler_name` varchar(50) DEFAULT NULL COMMENT '协同办理人昵称',
  `vote` char(1) DEFAULT '0' COMMENT '表决结果（0未表态 1同意 2不同意）',
  `opinion` varchar(500) DEFAULT NULL COMMENT '意见内容',
  `status` char(1) DEFAULT '0' COMMENT '明细状态（0待处理 1已处理 2已取消）',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `create_by` varchar(50) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`item_id`),
  KEY `idx_cs_item_batch` (`batch_id`),
  KEY `idx_cs_item_task` (`task_id`),
  KEY `idx_cs_item_handler` (`handler`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流加签/会签/委托明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `oa_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_document` (
  `document_id` bigint NOT NULL AUTO_INCREMENT COMMENT '公文ID',
  `document_no` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文号',
  `title` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '公文标题',
  `document_type` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '公文类型（0发文 1收文 2签报）',
  `secret_level` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '密级（0公开 1内部 2秘密 3机密）',
  `urgent_level` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '紧急程度（0普通 1加急 2特急）',
  `content` longtext COLLATE utf8mb4_general_ci COMMENT '正文内容',
  `attachments` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '附件URL（逗号分隔）',
  `originator_id` bigint DEFAULT NULL COMMENT '发起人用户ID',
  `originator_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发起人姓名',
  `origin_dept_id` bigint DEFAULT NULL COMMENT '发起部门ID',
  `origin_dept_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发起部门名称',
  `process_instance_id` bigint DEFAULT NULL COMMENT '流程实例ID',
  `document_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '公文状态（0草稿 1审批中 2已发布 3已归档 4已驳回）',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`document_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公文主表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `oa_document_attach`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_document_attach` (
  `attach_id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `document_id` bigint NOT NULL COMMENT '公文ID',
  `file_name` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文件名称',
  `file_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文件路径',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小（字节）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`attach_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公文附件表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `oa_document_copy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_document_copy` (
  `copy_id` bigint NOT NULL AUTO_INCREMENT COMMENT '抄送ID',
  `document_id` bigint NOT NULL COMMENT '公文ID',
  `user_id` bigint NOT NULL COMMENT '抄送人用户ID',
  `user_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '抄送人姓名',
  `read_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '阅读状态（0未读 1已读）',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`copy_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公文抄送记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `oa_meeting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_meeting` (
  `meeting_id` bigint NOT NULL AUTO_INCREMENT COMMENT '会议ID',
  `meeting_theme` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '会议主题',
  `room_id` bigint DEFAULT NULL COMMENT '会议室ID',
  `room_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会议室名称',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `organizer_id` bigint DEFAULT NULL COMMENT '组织者用户ID',
  `organizer_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组织者姓名',
  `meeting_type` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '会议类型（0普通会议 1视频会议 2紧急会议）',
  `meeting_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '会议状态（0未开始 1进行中 2已结束 3已取消）',
  `content` text COLLATE utf8mb4_general_ci COMMENT '会议内容',
  `attachment_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '附件URL',
  `process_instance_id` bigint DEFAULT NULL COMMENT '流程实例ID',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`meeting_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4104 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会议主表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `oa_meeting_minutes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_meeting_minutes` (
  `minutes_id` bigint NOT NULL AUTO_INCREMENT COMMENT '纪要ID',
  `meeting_id` bigint NOT NULL COMMENT '会议ID',
  `content` longtext COLLATE utf8mb4_general_ci COMMENT '纪要内容',
  `recorder_id` bigint DEFAULT NULL COMMENT '记录人用户ID',
  `recorder_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '记录人姓名',
  `issue_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '下发状态（0未下发 1已下发）',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`minutes_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会议纪要表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `oa_meeting_participant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_meeting_participant` (
  `participant_id` bigint NOT NULL AUTO_INCREMENT COMMENT '参会ID',
  `meeting_id` bigint NOT NULL COMMENT '会议ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户姓名',
  `dept_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '部门名称',
  `attend_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '出席状态（0待确认 1参加 2不参加 3待定）',
  `sign_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '签到状态（0未签到 1已签到）',
  `sign_time` datetime DEFAULT NULL COMMENT '签到时间',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`participant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='参会人员表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `oa_meeting_room`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_meeting_room` (
  `room_id` bigint NOT NULL AUTO_INCREMENT COMMENT '会议室ID',
  `room_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '会议室名称',
  `room_location` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会议室位置',
  `capacity` int DEFAULT '0' COMMENT '容纳人数',
  `equipment` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备配置',
  `admin_id` bigint DEFAULT NULL COMMENT '管理员用户ID',
  `admin_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '管理员姓名',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`room_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4003 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会议室基础信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `oa_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_notice` (
  `notice_id` bigint NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `notice_title` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告标题',
  `notice_content` longtext COLLATE utf8mb4_general_ci COMMENT '公告内容',
  `notice_type` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '公告类型（0通知 1公告 2通报）',
  `publish_scope` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '发布范围（0全体 1指定部门 2指定人员）',
  `is_top` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '是否置顶（0否 1是）',
  `publish_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '发布状态（0草稿 1已发布 2已撤回）',
  `publisher_id` bigint DEFAULT NULL COMMENT '发布人用户ID',
  `publisher_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发布人姓名',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `end_time` datetime DEFAULT NULL COMMENT '截止时间',
  `read_count` int DEFAULT '0' COMMENT '阅读次数',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知公告主表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `oa_notice_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_notice_dept` (
  `notice_dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '范围ID',
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  `dept_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '部门名称',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`notice_dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知公告部门范围表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `oa_notice_read`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_notice_read` (
  `read_id` bigint NOT NULL AUTO_INCREMENT COMMENT '已读ID',
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户姓名',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`read_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知公告已读记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `oa_process_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_process_category` (
  `category_id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `category_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称',
  `category_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类编码',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流程分类表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `oa_process_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_process_definition` (
  `definition_id` bigint NOT NULL AUTO_INCREMENT COMMENT '定义ID',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `process_key` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程标识（BPMN process id）',
  `process_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程名称',
  `version` int DEFAULT '1' COMMENT '版本号',
  `deployment_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Flowable 部署ID',
  `proc_def_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Flowable 流程定义ID',
  `bpmn_xml` text COLLATE utf8mb4_general_ci COMMENT 'BPMN XML 内容',
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程说明',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`definition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流程定义快照表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `oa_process_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_process_instance` (
  `instance_id` bigint NOT NULL AUTO_INCREMENT COMMENT '实例ID',
  `business_type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务类型（document/meeting/leave等）',
  `business_id` bigint NOT NULL COMMENT '业务主键',
  `definition_id` bigint DEFAULT NULL COMMENT '流程定义ID',
  `proc_inst_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Flowable 流程实例ID',
  `starter_id` bigint DEFAULT NULL COMMENT '发起人用户ID',
  `starter_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发起人姓名',
  `current_task_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '当前任务名称',
  `current_assignee` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '当前处理人',
  `process_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '流程状态（0运行中 1已完成 2已驳回 3已撤销）',
  `start_time` datetime DEFAULT NULL COMMENT '发起时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`instance_id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='业务流程实例关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `oa_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_schedule` (
  `schedule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日程ID',
  `schedule_title` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '日程标题',
  `schedule_type` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '日程类型（0个人 1会议 2任务 3提醒）',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `all_day` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '是否全天（0否 1是）',
  `remind_type` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '提醒方式（0不提醒 1系统消息 2邮件 3短信）',
  `remind_time` datetime DEFAULT NULL COMMENT '提醒时间',
  `color` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '颜色标记',
  `location` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地点',
  `schedule_content` text COLLATE utf8mb4_general_ci COMMENT '日程内容',
  `owner_id` bigint NOT NULL COMMENT '所属人用户ID',
  `owner_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属人姓名',
  `schedule_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '日程状态（0正常 1已完成 2已取消）',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`schedule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='日程安排主表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `oa_schedule_share`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_schedule_share` (
  `share_id` bigint NOT NULL AUTO_INCREMENT COMMENT '共享ID',
  `schedule_id` bigint NOT NULL COMMENT '日程ID',
  `user_id` bigint NOT NULL COMMENT '共享用户ID',
  `user_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '共享用户姓名',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`share_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='日程共享人员表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `oa_task_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_task_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `instance_id` bigint NOT NULL COMMENT '流程实例ID',
  `task_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Flowable 任务ID',
  `task_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务名称',
  `assignee_id` bigint DEFAULT NULL COMMENT '处理人用户ID',
  `assignee_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '处理人姓名',
  `action_type` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作类型（0通过 1驳回 2转办 3会签 4撤销）',
  `comment` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批意见',
  `start_time` datetime DEFAULT NULL COMMENT '任务开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '任务结束时间',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审批任务记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `portal_article`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_article` (
  `article_id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `column_id` bigint NOT NULL COMMENT '所属栏目ID',
  `title` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '文章标题',
  `summary` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '摘要',
  `content` longtext COLLATE utf8mb4_general_ci COMMENT '文章内容(富文本)',
  `cover_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '封面图URL',
  `source` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源',
  `author` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '作者',
  `publish_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '发布状态(0草稿 1待审核 2已发布 3已撤回)',
  `publish_date` datetime DEFAULT NULL COMMENT '发布时间',
  `is_top` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '是否置顶(0否 1是)',
  `is_featured` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '是否推荐到首页(0否 1是)',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID',
  `review_comment` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核意见',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`article_id`),
  KEY `idx_column_status` (`column_id`,`publish_status`),
  KEY `idx_publish_date` (`publish_date` DESC),
  KEY `idx_featured` (`is_featured`,`publish_date` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=9016 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户文章表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `portal_banner`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_banner` (
  `banner_id` bigint NOT NULL AUTO_INCREMENT COMMENT '轮播ID',
  `title` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题',
  `image_url` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '图片URL',
  `link_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '点击跳转链接',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `is_active` char(1) COLLATE utf8mb4_general_ci DEFAULT '1' COMMENT '是否激活(0否 1是)',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`banner_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9006 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户轮播表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `portal_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_column` (
  `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT '栏目ID',
  `column_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '栏目名称',
  `column_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '栏目编码',
  `parent_id` bigint DEFAULT '0' COMMENT '父栏目ID',
  `column_type` char(1) COLLATE utf8mb4_general_ci DEFAULT '1' COMMENT '栏目类型(1列表 2单页 3链接)',
  `icon` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标',
  `sort_order` int DEFAULT '0' COMMENT '显示排序',
  `is_visible` char(1) COLLATE utf8mb4_general_ci DEFAULT '1' COMMENT '是否显示(0否 1是)',
  `external_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '外部链接(type=3时使用)',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`column_id`),
  UNIQUE KEY `uk_column_code` (`column_code`)
) ENGINE=InnoDB AUTO_INCREMENT=9003 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户栏目表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `portal_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_notice` (
  `notice_id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `notice_title` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '通知标题',
  `notice_type` char(1) COLLATE utf8mb4_general_ci DEFAULT '1' COMMENT '通知类型（1选课通知 2考试通知 3学籍通知 4综合通知）',
  `notice_content` text COLLATE utf8mb4_general_ci COMMENT '通知内容',
  `publish_dept_id` bigint DEFAULT NULL COMMENT '发布部门ID（关联brm_department）',
  `publish_dept_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发布部门名称',
  `publish_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '发布状态（0草稿 1已发布 2已撤回）',
  `publish_date` date DEFAULT NULL COMMENT '发布日期',
  `target_role` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '目标角色（0所有人 1学生 2教师）',
  `is_top` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '是否置顶（0否 1是）',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教务通知表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_blob_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_blob_triggers` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `blob_data` blob COMMENT '存放持久化Trigger对象',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Blob类型的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_calendars`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_calendars` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `calendar_name` varchar(200) NOT NULL COMMENT '日历名称',
  `calendar` blob NOT NULL COMMENT '存放持久化calendar对象',
  PRIMARY KEY (`sched_name`,`calendar_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='日历信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_cron_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_cron_triggers` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `cron_expression` varchar(200) NOT NULL COMMENT 'cron表达式',
  `time_zone_id` varchar(80) DEFAULT NULL COMMENT '时区',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Cron类型的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_fired_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_fired_triggers` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `entry_id` varchar(95) NOT NULL COMMENT '调度器实例id',
  `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `instance_name` varchar(200) NOT NULL COMMENT '调度器实例名',
  `fired_time` bigint NOT NULL COMMENT '触发的时间',
  `sched_time` bigint NOT NULL COMMENT '定时器制定的时间',
  `priority` int NOT NULL COMMENT '优先级',
  `state` varchar(16) NOT NULL COMMENT '状态',
  `job_name` varchar(200) DEFAULT NULL COMMENT '任务名称',
  `job_group` varchar(200) DEFAULT NULL COMMENT '任务组名',
  `is_nonconcurrent` varchar(1) DEFAULT NULL COMMENT '是否并发',
  `requests_recovery` varchar(1) DEFAULT NULL COMMENT '是否接受恢复执行',
  PRIMARY KEY (`sched_name`,`entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='已触发的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_job_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_job_details` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `job_name` varchar(200) NOT NULL COMMENT '任务名称',
  `job_group` varchar(200) NOT NULL COMMENT '任务组名',
  `description` varchar(250) DEFAULT NULL COMMENT '相关介绍',
  `job_class_name` varchar(250) NOT NULL COMMENT '执行任务类名称',
  `is_durable` varchar(1) NOT NULL COMMENT '是否持久化',
  `is_nonconcurrent` varchar(1) NOT NULL COMMENT '是否并发',
  `is_update_data` varchar(1) NOT NULL COMMENT '是否更新数据',
  `requests_recovery` varchar(1) NOT NULL COMMENT '是否接受恢复执行',
  `job_data` blob COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`,`job_name`,`job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务详细信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_locks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_locks` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `lock_name` varchar(40) NOT NULL COMMENT '悲观锁名称',
  PRIMARY KEY (`sched_name`,`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='存储的悲观锁信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_paused_trigger_grps` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  PRIMARY KEY (`sched_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='暂停的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_scheduler_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_scheduler_state` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `instance_name` varchar(200) NOT NULL COMMENT '实例名称',
  `last_checkin_time` bigint NOT NULL COMMENT '上次检查时间',
  `checkin_interval` bigint NOT NULL COMMENT '检查间隔时间',
  PRIMARY KEY (`sched_name`,`instance_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='调度器状态表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_simple_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_simple_triggers` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `repeat_count` bigint NOT NULL COMMENT '重复的次数统计',
  `repeat_interval` bigint NOT NULL COMMENT '重复的间隔时间',
  `times_triggered` bigint NOT NULL COMMENT '已经触发的次数',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简单触发器的信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_simprop_triggers` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `str_prop_1` varchar(512) DEFAULT NULL COMMENT 'String类型的trigger的第一个参数',
  `str_prop_2` varchar(512) DEFAULT NULL COMMENT 'String类型的trigger的第二个参数',
  `str_prop_3` varchar(512) DEFAULT NULL COMMENT 'String类型的trigger的第三个参数',
  `int_prop_1` int DEFAULT NULL COMMENT 'int类型的trigger的第一个参数',
  `int_prop_2` int DEFAULT NULL COMMENT 'int类型的trigger的第二个参数',
  `long_prop_1` bigint DEFAULT NULL COMMENT 'long类型的trigger的第一个参数',
  `long_prop_2` bigint DEFAULT NULL COMMENT 'long类型的trigger的第二个参数',
  `dec_prop_1` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第一个参数',
  `dec_prop_2` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第二个参数',
  `bool_prop_1` varchar(1) DEFAULT NULL COMMENT 'Boolean类型的trigger的第一个参数',
  `bool_prop_2` varchar(1) DEFAULT NULL COMMENT 'Boolean类型的trigger的第二个参数',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='同步机制的行锁表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_triggers` (
  `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) NOT NULL COMMENT '触发器的名字',
  `trigger_group` varchar(200) NOT NULL COMMENT '触发器所属组的名字',
  `job_name` varchar(200) NOT NULL COMMENT 'qrtz_job_details表job_name的外键',
  `job_group` varchar(200) NOT NULL COMMENT 'qrtz_job_details表job_group的外键',
  `description` varchar(250) DEFAULT NULL COMMENT '相关介绍',
  `next_fire_time` bigint DEFAULT NULL COMMENT '上一次触发时间（毫秒）',
  `prev_fire_time` bigint DEFAULT NULL COMMENT '下一次触发时间（默认为-1表示不触发）',
  `priority` int DEFAULT NULL COMMENT '优先级',
  `trigger_state` varchar(16) NOT NULL COMMENT '触发器状态',
  `trigger_type` varchar(8) NOT NULL COMMENT '触发器的类型',
  `start_time` bigint NOT NULL COMMENT '开始时间',
  `end_time` bigint DEFAULT NULL COMMENT '结束时间',
  `calendar_name` varchar(200) DEFAULT NULL COMMENT '日程表名称',
  `misfire_instr` smallint DEFAULT NULL COMMENT '补偿执行的策略',
  `job_data` blob COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  KEY `sched_name` (`sched_name`,`job_name`,`job_group`),
  CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `job_name`, `job_group`) REFERENCES `qrtz_job_details` (`sched_name`, `job_name`, `job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='触发器详细信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sam_certificate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sam_certificate` (
  `cert_id` bigint NOT NULL AUTO_INCREMENT COMMENT '证书ID',
  `student_id` bigint NOT NULL COMMENT '学生ID（关联sam_student）',
  `cert_type` char(1) COLLATE utf8mb4_general_ci NOT NULL COMMENT '证书类型（0毕业证书 1学位证书 2结业证书）',
  `cert_number` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '证书编号',
  `cert_date` date DEFAULT NULL COMMENT '发证日期',
  `major_id` bigint DEFAULT NULL COMMENT '专业ID（关联brm_major）',
  `education_level` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '学历层次',
  `is_issued` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '是否发放（0否 1是）',
  `issue_date` date DEFAULT NULL COMMENT '发放日期',
  `receiver` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '领取人',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`cert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='证书编号管理表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sam_degree_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sam_degree_config` (
  `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_name` varchar(100) NOT NULL COMMENT '配置名称',
  `gpa_threshold` double DEFAULT '2' COMMENT 'GPA门槛',
  `require_degree_course` char(1) DEFAULT '1' COMMENT '是否要求学位课无不及格（0否 1是）',
  `require_foreign_language` char(1) DEFAULT '0' COMMENT '是否要求外语无不及格（0否 1是）',
  `require_thesis` char(1) DEFAULT '0' COMMENT '是否要求论文合格（0否 1是，需对接论文系统）',
  `require_achievement` char(1) DEFAULT '0' COMMENT '是否要求学术成果（0否 1是，需对接成果数据）',
  `is_default` char(1) DEFAULT '0' COMMENT '是否默认生效配置（0否 1是）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学位审核条件配置';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sam_degree_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sam_degree_review` (
  `review_id` bigint NOT NULL AUTO_INCREMENT COMMENT '审核ID',
  `student_id` bigint NOT NULL COMMENT '学生ID（关联sam_student）',
  `graduation_review_id` bigint DEFAULT NULL COMMENT '毕业审核ID（关联sam_graduation_review）',
  `gpa` decimal(4,2) DEFAULT '0.00' COMMENT '平均绩点',
  `is_gpa_qualified` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '绩点是否合格（0否 1是）',
  `is_degree_course_qualified` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '学位课程是否合格（0否 1是）',
  `is_thesis_qualified` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '论文是否合格（0否 1是）',
  `degree_type` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '学位类型（学士/硕士/博士）',
  `degree_field` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '学位学科门类（工学/理学/管理学等）',
  `review_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '审核状态（0待审 1通过 2不通过）',
  `review_date` date DEFAULT NULL COMMENT '审核日期',
  `reviewer` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核人',
  `review_opinion` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核意见',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学位资格审核表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sam_graduation_procedure`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sam_graduation_procedure` (
  `procedure_id` bigint NOT NULL AUTO_INCREMENT COMMENT '手续ID',
  `student_id` bigint NOT NULL COMMENT '学生ID（关联sam_student）',
  `library_cleared` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '图书馆清还（0未清 1已清）',
  `finance_cleared` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '财务结算（0未结 1已结）',
  `dormitory_cleared` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '宿舍退宿（0未退 1已退）',
  `card_returned` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '一卡通退还（0未退 1已退）',
  `procedure_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '手续状态（0未办理 1办理中 2已完成）',
  `complete_date` date DEFAULT NULL COMMENT '完成日期',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`procedure_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='毕业离校手续表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sam_graduation_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sam_graduation_review` (
  `review_id` bigint NOT NULL AUTO_INCREMENT COMMENT '审核ID',
  `student_id` bigint NOT NULL COMMENT '学生ID（关联sam_student）',
  `total_credits_earned` decimal(5,1) DEFAULT '0.0' COMMENT '已获总学分',
  `required_credits` decimal(5,1) DEFAULT '0.0' COMMENT '要求学分',
  `is_credit_qualified` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '学分是否合格（0否 1是）',
  `is_course_qualified` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '课程是否合格（0否 1是）',
  `is_english_qualified` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '英语是否合格（0否 1是）',
  `is_pe_qualified` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '体育是否合格（0否 1是）',
  `review_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '审核状态（0待审 1通过 2不通过）',
  `review_date` date DEFAULT NULL COMMENT '审核日期',
  `reviewer` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核人',
  `review_opinion` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核意见',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='毕业资格审核表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sam_registration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sam_registration` (
  `registration_id` bigint NOT NULL AUTO_INCREMENT COMMENT '注册记录ID',
  `student_id` bigint NOT NULL COMMENT '学生ID（关联 sam_student）',
  `semester_id` bigint NOT NULL COMMENT '学期ID（关联 brm_semester）',
  `register_status` char(1) NOT NULL DEFAULT '0' COMMENT '注册状态（0未注册 1已注册 2延迟注册）',
  `register_time` datetime DEFAULT NULL COMMENT '报到注册时间',
  `register_by` varchar(64) DEFAULT NULL COMMENT '注册经办人（管理员账号或学生本人）',
  `channel` char(1) DEFAULT '0' COMMENT '注册渠道（0管理端代办 1学生自助）',
  `defer_reason` varchar(500) DEFAULT NULL COMMENT '延迟/未注册原因',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`registration_id`),
  UNIQUE KEY `uk_student_semester` (`student_id`,`semester_id`),
  KEY `idx_semester_status` (`semester_id`,`register_status`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学期注册记录（S8）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sam_status_change`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sam_status_change` (
  `change_id` bigint NOT NULL AUTO_INCREMENT COMMENT '异动ID',
  `student_id` bigint NOT NULL COMMENT '学生ID（关联sam_student）',
  `change_type` char(1) COLLATE utf8mb4_general_ci NOT NULL COMMENT '异动类型（0休学 1复学 2转学 3退学 4保留学籍）',
  `change_date` date DEFAULT NULL COMMENT '异动日期',
  `original_status` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原学籍状态',
  `new_status` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '新学籍状态',
  `reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '申请原因',
  `applicant` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '申请人',
  `approve_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '审批状态（0待审 1通过 2驳回）',
  `approve_by` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批人',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approve_opinion` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批意见',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `proc_inst_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '??????ID',
  PRIMARY KEY (`change_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学籍异动表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sam_student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sam_student` (
  `student_id` bigint NOT NULL AUTO_INCREMENT COMMENT '学生ID',
  `student_no` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '学号',
  `student_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '姓名',
  `gender` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '性别（0男 1女）',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `id_card` varchar(18) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '身份证号',
  `major_id` bigint DEFAULT NULL COMMENT '专业ID（关联brm_major）',
  `user_id` bigint DEFAULT NULL COMMENT '关联系统用户sys_user.user_id（门户本人解析）',
  `dept_id` bigint DEFAULT NULL COMMENT '院系ID（关联brm_department）',
  `class_id` bigint DEFAULT NULL COMMENT '班级ID（关联brm_class）',
  `enrollment_year` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '入学年份（如2024级）',
  `education_level` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '学历层次（本科/硕士/博士）',
  `student_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '学籍状态（0在读 1休学 2退学 3毕业 4转出 5保留学籍）',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`student_id`),
  KEY `idx_sam_student_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=114 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学生学籍信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sam_thesis`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sam_thesis` (
  `thesis_id` bigint NOT NULL AUTO_INCREMENT COMMENT '论文ID',
  `plan_year` varchar(10) NOT NULL COMMENT '届别（如2026）',
  `student_id` bigint NOT NULL COMMENT '学生ID（sam_student）',
  `topic_id` bigint DEFAULT NULL COMMENT '选题库题目ID（自拟题目为空）',
  `topic_name` varchar(200) DEFAULT NULL COMMENT '论文题目快照',
  `advisor` varchar(50) DEFAULT NULL COMMENT '指导教师登录名',
  `advisor_name` varchar(50) DEFAULT NULL COMMENT '指导教师姓名',
  `current_stage` char(1) NOT NULL DEFAULT '1' COMMENT '当前环节（1选题 2开题 3中期检查 4查重 5答辩 6成绩归档）',
  `stage_status` char(1) NOT NULL DEFAULT '0' COMMENT '当前环节状态（0待提交 1待审核 2已通过 3已退回）',
  `check_rate` decimal(5,2) DEFAULT NULL COMMENT '查重重复率（%）',
  `check_pass` char(1) DEFAULT NULL COMMENT '查重是否达标（0否 1是）',
  `defense_score` decimal(5,2) DEFAULT NULL COMMENT '答辩成绩',
  `total_score` decimal(5,2) DEFAULT NULL COMMENT '总评成绩',
  `grade_level` char(1) DEFAULT NULL COMMENT '成绩等级（0优秀 1良好 2中等 3及格 4不及格）',
  `is_qualified` char(1) DEFAULT '0' COMMENT '论文是否合格（0否 1是）——学位审核读取该字段',
  `sample_status` char(1) DEFAULT '0' COMMENT '抽检状态（0未抽检 1已送抽检待结果 2抽检合格 3抽检不合格）',
  `archive_time` datetime DEFAULT NULL COMMENT '成绩归档时间',
  `status` char(1) DEFAULT '0' COMMENT '记录状态（0正常 1作废）',
  `create_by` varchar(50) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`thesis_id`),
  UNIQUE KEY `uk_thesis_student_year` (`student_id`,`plan_year`),
  KEY `idx_thesis_advisor` (`advisor`),
  KEY `idx_thesis_stage` (`current_stage`,`stage_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='毕业论文（设计）全过程主表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sam_thesis_process`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sam_thesis_process` (
  `process_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `thesis_id` bigint NOT NULL COMMENT '论文ID',
  `stage` char(1) NOT NULL COMMENT '环节（1选题 2开题 3中期检查 4查重 5答辩 6成绩归档）',
  `action` varchar(20) NOT NULL DEFAULT 'submit' COMMENT '动作（submit提交 audit审核 record登记）',
  `title` varchar(200) DEFAULT NULL COMMENT '材料/环节名称',
  `content` varchar(2000) DEFAULT NULL COMMENT '提交内容或审核结论说明',
  `attachment` varchar(500) DEFAULT NULL COMMENT '附件地址（报告/查重单/答辩记录）',
  `result` char(1) DEFAULT '2' COMMENT '结果（0退回 1通过 2仅记录）',
  `score` decimal(5,2) DEFAULT NULL COMMENT '本环节成绩（如有）',
  `opinion` varchar(500) DEFAULT NULL COMMENT '意见',
  `operator` varchar(50) DEFAULT NULL COMMENT '操作人登录名',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
  `operate_time` datetime DEFAULT NULL COMMENT '操作时间',
  `create_by` varchar(50) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`process_id`),
  KEY `idx_process_thesis` (`thesis_id`,`stage`),
  KEY `idx_process_operator` (`operator`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='毕业论文环节留痕表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sam_thesis_topic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sam_thesis_topic` (
  `topic_id` bigint NOT NULL AUTO_INCREMENT COMMENT '题目ID',
  `plan_year` varchar(10) NOT NULL COMMENT '届别（如2026）',
  `topic_name` varchar(200) NOT NULL COMMENT '论文（设计）题目',
  `topic_source` char(1) DEFAULT '0' COMMENT '题目来源（0教师科研课题 1生产社会实践 2学生自拟 3学科竞赛）',
  `major_id` bigint DEFAULT NULL COMMENT '适用专业ID',
  `dept_id` bigint DEFAULT NULL COMMENT '所属学院ID',
  `advisor` varchar(50) DEFAULT NULL COMMENT '指导教师登录名',
  `advisor_name` varchar(50) DEFAULT NULL COMMENT '指导教师姓名',
  `capacity` int DEFAULT '1' COMMENT '可选题人数',
  `elected_count` int DEFAULT '0' COMMENT '已选人数',
  `difficulty` char(1) DEFAULT '2' COMMENT '难度（1基础 2中等 3较高）',
  `intro` varchar(1000) DEFAULT NULL COMMENT '题目简介与完成要求',
  `status` char(1) DEFAULT '0' COMMENT '状态（0待审核 1可选题 2已选满 3已下架）',
  `audit_opinion` varchar(500) DEFAULT NULL COMMENT '审核意见',
  `create_by` varchar(50) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`topic_id`),
  KEY `idx_topic_year` (`plan_year`),
  KEY `idx_topic_advisor` (`advisor`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='毕业论文选题库';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sam_warning`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sam_warning` (
  `warning_id` bigint NOT NULL AUTO_INCREMENT COMMENT '预警ID',
  `student_id` bigint NOT NULL COMMENT '学生ID（关联sam_student）',
  `semester_id` bigint DEFAULT NULL COMMENT '学期ID（关联brm_semester）',
  `warning_type` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '预警类型（0成绩预警 1学分预警 2出勤预警 3综合预警）',
  `warning_level` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '预警级别（0一般 1严重 2高危）',
  `warning_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '预警原因',
  `warning_date` date DEFAULT NULL COMMENT '预警日期',
  `is_resolved` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '是否解除（0否 1是）',
  `resolve_date` date DEFAULT NULL COMMENT '解除日期',
  `resolve_remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '解除说明',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`warning_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9364 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学籍预警表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sam_warning_assist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sam_warning_assist` (
  `assist_id` bigint NOT NULL AUTO_INCREMENT COMMENT '帮扶任务ID',
  `warning_id` bigint NOT NULL COMMENT '关联预警ID（sam_warning）',
  `student_id` bigint DEFAULT NULL COMMENT '学生ID',
  `semester_id` bigint DEFAULT NULL COMMENT '学期ID',
  `student_name` varchar(50) DEFAULT NULL COMMENT '学生姓名（快照）',
  `student_no` varchar(30) DEFAULT NULL COMMENT '学号（快照）',
  `warning_type` char(1) DEFAULT NULL COMMENT '预警类型（0成绩 1学分 2出勤 3综合）',
  `warning_level` char(1) DEFAULT NULL COMMENT '预警级别（0一般 1严重 2高危）',
  `helper_user_id` bigint DEFAULT NULL COMMENT '帮扶人用户ID',
  `helper_name` varchar(50) DEFAULT NULL COMMENT '帮扶人姓名',
  `assist_status` char(1) NOT NULL DEFAULT '0' COMMENT '帮扶状态（0待认领 1帮扶中 2已完成 3已关闭）',
  `measure` varchar(500) DEFAULT NULL COMMENT '帮扶措施',
  `follow_count` int DEFAULT '0' COMMENT '跟踪次数',
  `last_follow_time` datetime DEFAULT NULL COMMENT '最近跟踪时间',
  `claim_time` datetime DEFAULT NULL COMMENT '认领时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成/关闭时间',
  `finish_remark` varchar(500) DEFAULT NULL COMMENT '完结/关闭说明',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`assist_id`),
  UNIQUE KEY `uk_warning` (`warning_id`),
  KEY `idx_helper_status` (`helper_user_id`,`assist_status`),
  KEY `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学业预警帮扶任务（S6）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sam_warning_assist_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sam_warning_assist_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `assist_id` bigint NOT NULL COMMENT '帮扶任务ID',
  `follow_type` char(1) DEFAULT '0' COMMENT '跟踪方式（0面谈 1电话 2线上 3家访）',
  `follow_content` varchar(1000) DEFAULT NULL COMMENT '跟踪内容',
  `follow_time` datetime DEFAULT NULL COMMENT '跟踪时间',
  `operator_id` bigint DEFAULT NULL COMMENT '登记人用户ID',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '登记人姓名',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_assist` (`assist_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学业预警帮扶跟踪记录（S6）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_ai_chat_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_ai_chat_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint DEFAULT NULL COMMENT '提问用户ID',
  `user_name` varchar(64) DEFAULT NULL COMMENT '提问用户登录名',
  `user_role` varchar(64) DEFAULT NULL COMMENT '提问用户角色标识',
  `scene` varchar(20) DEFAULT 'portal' COMMENT '提问场景（portal门户自助 admin后台自测）',
  `question` varchar(1000) NOT NULL COMMENT '问题原文',
  `answer` text COMMENT '回答内容',
  `answer_source` varchar(10) DEFAULT NULL COMMENT '回答来源（LLM大模型 EXTRACT本地抽取 NONE未命中 ERROR调用失败）',
  `knowledge_ids` varchar(255) DEFAULT NULL COMMENT '命中知识ID（逗号分隔）',
  `knowledge_titles` varchar(1000) DEFAULT NULL COMMENT '命中知识标题（逗号分隔，用于引用展示）',
  `confidence` decimal(5,2) DEFAULT '0.00' COMMENT '检索置信度（最高命中得分）',
  `cost_time` int DEFAULT '0' COMMENT '耗时（毫秒）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0成功 1失败）',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `create_time` datetime DEFAULT NULL COMMENT '提问时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_user_time` (`user_id`,`create_time`),
  KEY `idx_answer_source` (`answer_source`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI问答留痕记录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_ai_knowledge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_ai_knowledge` (
  `knowledge_id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识ID',
  `category` varchar(30) NOT NULL COMMENT '分类（字典 sys_ai_category）',
  `title` varchar(200) NOT NULL COMMENT '标准问题/条目标题',
  `keywords` varchar(500) DEFAULT NULL COMMENT '检索关键词（逗号分隔，命中加权）',
  `content` text COMMENT '解答正文',
  `source` varchar(200) DEFAULT NULL COMMENT '依据来源（文件名/制度条款/系统口径）',
  `ref_url` varchar(255) DEFAULT NULL COMMENT '参考链接（可选）',
  `hit_count` int DEFAULT '0' COMMENT '累计命中次数',
  `order_num` int DEFAULT '0' COMMENT '排序',
  `status` char(1) DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`knowledge_id`),
  KEY `idx_category_status` (`category`,`status`),
  KEY `idx_title` (`title`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教务政策AI知识库';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `config_id` int NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=126 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参数配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门id',
  `ancestors` varchar(50) DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) DEFAULT '' COMMENT '部门名称',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `leader` varchar(20) DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
  `status` char(1) DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=216 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int DEFAULT '0' COMMENT '字典排序',
  `dict_label` varchar(100) DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`)
) ENGINE=InnoDB AUTO_INCREMENT=9225 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
  `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name` varchar(100) DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `dict_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=333 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_job` (
  `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name` varchar(64) NOT NULL DEFAULT '' COMMENT '任务名称',
  `job_group` varchar(64) NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
  `invoke_target` varchar(500) NOT NULL COMMENT '调用目标字符串',
  `cron_expression` varchar(255) DEFAULT '' COMMENT 'cron执行表达式',
  `misfire_policy` varchar(20) DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  `concurrent` char(1) DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1暂停）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注信息',
  PRIMARY KEY (`job_id`,`job_name`,`job_group`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务调度表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_job_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_job_log` (
  `job_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  `job_name` varchar(64) NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) NOT NULL COMMENT '任务组名',
  `invoke_target` varchar(500) NOT NULL COMMENT '调用目标字符串',
  `job_message` varchar(500) DEFAULT NULL COMMENT '日志信息',
  `status` char(1) DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
  `exception_info` varchar(2000) DEFAULT '' COMMENT '异常信息',
  `start_time` datetime DEFAULT NULL COMMENT '执行开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '执行结束时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`job_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务调度日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_logininfor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_logininfor` (
  `info_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name` varchar(50) DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) DEFAULT '' COMMENT '操作系统',
  `status` char(1) DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) DEFAULT '' COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`),
  KEY `idx_sys_logininfor_s` (`status`),
  KEY `idx_sys_logininfor_lt` (`login_time`)
) ENGINE=InnoDB AUTO_INCREMENT=442 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统访问记录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `path` varchar(200) DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `query` varchar(255) DEFAULT NULL COMMENT '路由参数',
  `route_name` varchar(50) DEFAULT '' COMMENT '路由名称',
  `is_frame` int DEFAULT '1' COMMENT '是否为外链（0是 1否）',
  `is_cache` int DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3212 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_message` (
  `message_id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `receiver_id` bigint NOT NULL COMMENT '接收人用户ID',
  `msg_type` char(1) NOT NULL DEFAULT '3' COMMENT '类型(0预警1审批2变更3通知)',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `content` varchar(1000) DEFAULT NULL COMMENT '内容',
  `business_type` varchar(50) DEFAULT NULL COMMENT '关联业务类型',
  `business_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `read_status` char(1) NOT NULL DEFAULT '0' COMMENT '已读(0未读1已读)',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`message_id`),
  KEY `idx_receiver` (`receiver_id`,`read_status`),
  KEY `idx_business` (`business_type`,`business_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10894 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一消息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notice` (
  `notice_id` int NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `notice_title` varchar(50) NOT NULL COMMENT '公告标题',
  `notice_type` char(1) NOT NULL COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob COMMENT '公告内容',
  `status` char(1) DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知公告表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_notice_read`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notice_read` (
  `read_id` bigint NOT NULL AUTO_INCREMENT COMMENT '已读主键',
  `notice_id` int NOT NULL COMMENT '公告id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `read_time` datetime NOT NULL COMMENT '阅读时间',
  PRIMARY KEY (`read_id`),
  UNIQUE KEY `uk_user_notice` (`user_id`,`notice_id`) COMMENT '同一用户同一公告只记录一次'
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公告已读记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_notify_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notify_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '留痕ID',
  `channel` varchar(10) NOT NULL COMMENT '渠道(site站内信 mail邮件 sms短信)',
  `receiver_id` bigint DEFAULT NULL COMMENT '接收人用户ID',
  `target` varchar(120) DEFAULT NULL COMMENT '送达目标(邮箱/手机号/站内标识)',
  `business_type` varchar(50) DEFAULT NULL COMMENT '关联业务类型',
  `business_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `title` varchar(200) DEFAULT NULL COMMENT '消息标题',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '发送状态(0成功 1失败 2跳过)',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败/跳过原因',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_biz` (`business_type`,`business_id`),
  KEY `idx_receiver` (`receiver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息渠道发送留痕（S6）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
  `oper_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(50) DEFAULT '' COMMENT '模块标题',
  `business_type` int DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(200) DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) DEFAULT '' COMMENT '请求方式',
  `operator_type` int DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(2000) DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) DEFAULT '' COMMENT '返回参数',
  `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint DEFAULT '0' COMMENT '消耗时间',
  PRIMARY KEY (`oper_id`),
  KEY `idx_sys_oper_log_bt` (`business_type`),
  KEY `idx_sys_oper_log_s` (`status`),
  KEY `idx_sys_oper_log_ot` (`oper_time`)
) ENGINE=InnoDB AUTO_INCREMENT=266 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志记录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_post` (
  `post_id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code` varchar(64) NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL COMMENT '显示顺序',
  `status` char(1) NOT NULL COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_print_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_print_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '凭证ID',
  `biz_type` varchar(30) NOT NULL COMMENT '凭证业务类型',
  `biz_id` bigint NOT NULL COMMENT '业务主键（成绩单/课表为用户ID，证书certId，准考证seatId，监考单invigilationId）',
  `title` varchar(200) DEFAULT NULL COMMENT '凭证标题',
  `serial_no` varchar(64) NOT NULL COMMENT '凭证编号',
  `verify_code` varchar(32) NOT NULL COMMENT '验证码',
  `snapshot` longtext COMMENT '数据快照JSON（验真比对基准）',
  `data_hash` varchar(64) DEFAULT NULL COMMENT '快照SHA256',
  `template_code` varchar(50) DEFAULT NULL COMMENT '发放时模板编码（重打按此模板）',
  `receive_id` bigint DEFAULT NULL COMMENT '接收人ID（sys_user）',
  `receive_name` varchar(100) DEFAULT NULL COMMENT '接收人名称',
  `issue_channel` char(1) DEFAULT '0' COMMENT '发放渠道（0管理端 1门户）',
  `issue_by` varchar(64) DEFAULT NULL COMMENT '发放人',
  `issue_time` datetime DEFAULT NULL COMMENT '发放时间',
  `status` char(1) DEFAULT '0' COMMENT '状态（0有效 1已作废）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_serial_no` (`serial_no`),
  KEY `idx_biz` (`biz_type`,`biz_id`),
  KEY `idx_receive` (`receive_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='电子凭证发放记录（P4）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_print_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_print_template` (
  `template_id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_code` varchar(50) NOT NULL COMMENT '模板编码',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `biz_type` varchar(30) NOT NULL COMMENT '业务类型（GRADE成绩证明 SCHEDULE课表 CERTIFICATE证书 EXAM_TICKET准考证 INVIGILATION监考通知单）',
  `content` mediumtext COMMENT '模板内容（Velocity HTML）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打印凭证模板（P4）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) DEFAULT '1' COMMENT '部门树选择项是否关联显示',
  `status` char(1) NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_role_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色和部门关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色和菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_status_report_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_status_report_batch` (
  `batch_id` bigint NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `report_type` char(2) NOT NULL COMMENT '上报类型（01学生基本信息 02课程基本信息 03成绩信息 04教师基本信息）',
  `report_year` varchar(8) NOT NULL COMMENT '上报年度（如2026）',
  `report_name` varchar(120) DEFAULT NULL COMMENT '批次名称',
  `scope_dept_id` bigint DEFAULT '0' COMMENT '统计范围院系ID（0表示全校）',
  `scope_dept_name` varchar(100) DEFAULT NULL COMMENT '统计范围院系名称',
  `row_count` int DEFAULT '0' COMMENT '生成数据行数',
  `batch_status` char(1) DEFAULT '0' COMMENT '批次状态（0已生成 1已导出 2已上报 3已作废）',
  `gen_time` datetime DEFAULT NULL COMMENT '生成时间',
  `export_time` datetime DEFAULT NULL COMMENT '最近导出时间',
  `submit_time` datetime DEFAULT NULL COMMENT '上报标记时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`batch_id`),
  KEY `idx_report_type_year` (`report_type`,`report_year`),
  KEY `idx_batch_status` (`batch_status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教育部状态数据上报批次';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_status_report_field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_status_report_field` (
  `field_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字段ID',
  `report_type` char(2) NOT NULL COMMENT '上报类型（01学生 02课程 03成绩 04教师）',
  `std_code` varchar(30) NOT NULL COMMENT '标准字段代码',
  `std_name` varchar(100) NOT NULL COMMENT '标准字段名称',
  `source_expr` varchar(200) DEFAULT NULL COMMENT '数据来源（表.字段）',
  `convert_rule` varchar(300) DEFAULT NULL COMMENT '值域/转换规则',
  `data_type` varchar(20) DEFAULT 'S' COMMENT '数据类型（S字符 N数值 D日期）',
  `required` char(1) DEFAULT '1' COMMENT '是否必填（0否 1是）',
  `order_num` int DEFAULT '0' COMMENT '展示顺序',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`field_id`),
  UNIQUE KEY `uk_type_std_code` (`report_type`,`std_code`),
  KEY `idx_type_order` (`report_type`,`order_num`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='状态数据上报字段映射';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_student` (
  `student_id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `user_id` bigint NOT NULL COMMENT '????ID',
  `student_code` varchar(30) NOT NULL COMMENT '??',
  `class_id` bigint DEFAULT NULL COMMENT '??ID',
  `major_id` bigint DEFAULT NULL COMMENT '??ID',
  `dept_id` bigint DEFAULT NULL COMMENT '??ID',
  `grade` varchar(10) DEFAULT NULL COMMENT '??',
  `enrollment_year` int DEFAULT NULL COMMENT '????',
  `education_level` varchar(20) DEFAULT NULL COMMENT '????(undergraduate/master/doctor)',
  `student_status` varchar(20) DEFAULT 'enrolled' COMMENT '????',
  `create_by` varchar(64) DEFAULT '' COMMENT '???',
  `create_time` datetime DEFAULT NULL COMMENT '????',
  `update_by` varchar(64) DEFAULT '' COMMENT '???',
  `update_time` datetime DEFAULT NULL COMMENT '????',
  `remark` varchar(500) DEFAULT NULL COMMENT '??',
  PRIMARY KEY (`student_id`),
  UNIQUE KEY `idx_user_id` (`user_id`),
  UNIQUE KEY `idx_student_code` (`student_code`),
  KEY `idx_class_id` (`class_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='?????';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_teacher_link`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_teacher_link` (
  `link_id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `user_id` bigint NOT NULL COMMENT '??ID',
  `teacher_id` bigint NOT NULL COMMENT '??ID(brm_teacher)',
  `teacher_code` varchar(50) NOT NULL COMMENT '????',
  `create_by` varchar(64) DEFAULT '' COMMENT '???',
  `create_time` datetime DEFAULT NULL COMMENT '????',
  `update_by` varchar(64) DEFAULT '' COMMENT '???',
  `update_time` datetime DEFAULT NULL COMMENT '????',
  PRIMARY KEY (`link_id`),
  UNIQUE KEY `idx_user_teacher` (`user_id`,`teacher_id`),
  UNIQUE KEY `idx_user_id` (`user_id`),
  KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='???????';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_todo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_todo` (
  `todo_id` bigint NOT NULL AUTO_INCREMENT COMMENT '待办ID',
  `receiver_id` bigint NOT NULL COMMENT '接收人用户ID',
  `todo_type` char(1) NOT NULL DEFAULT '1' COMMENT '类型(0预警1审批2变更3通知)',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `business_type` varchar(50) DEFAULT NULL COMMENT '关联业务类型',
  `business_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态(0待办1已办)',
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`todo_id`),
  KEY `idx_receiver_status` (`receiver_id`,`status`),
  KEY `idx_business` (`business_type`,`business_id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一待办表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) NOT NULL COMMENT '用户昵称',
  `user_type` varchar(2) DEFAULT '00' COMMENT '用户类型（00系统用户）',
  `email` varchar(50) DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) DEFAULT '' COMMENT '手机号码',
  `sex` char(1) DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) DEFAULT '' COMMENT '密码',
  `status` char(1) DEFAULT '0' COMMENT '账号状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(128) DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `pwd_update_date` datetime DEFAULT NULL COMMENT '密码最后更新时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `user_category` varchar(10) DEFAULT 'admin' COMMENT '????(student/teacher/admin/secretary)',
  `identity_id` bigint DEFAULT NULL COMMENT '??????ID',
  `account_status` varchar(20) DEFAULT 'active' COMMENT '??????',
  `enrollment_date` date DEFAULT NULL COMMENT '??/????',
  `graduation_date` date DEFAULT NULL COMMENT '??/????',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user_mfa`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_mfa` (
  `user_id` bigint NOT NULL COMMENT '用户ID(主键，关联 sys_user.user_id)',
  `secret` varchar(64) NOT NULL COMMENT 'Base32 编码的 TOTP 密钥',
  `status` char(1) DEFAULT '0' COMMENT '状态(0待确认 1已启用)',
  `bind_time` datetime DEFAULT NULL COMMENT '启用(确认)时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户多因子鉴别(MFA)绑定表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_post` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户与岗位关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户和角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tpm_course_library`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tpm_course_library` (
  `course_id` bigint NOT NULL AUTO_INCREMENT COMMENT '课程ID',
  `course_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '课程编码',
  `course_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '课程名称',
  `course_name_en` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '英文名称',
  `credit` decimal(3,1) NOT NULL DEFAULT '0.0' COMMENT '学分',
  `theory_hours` int DEFAULT '0' COMMENT '理论学时',
  `practice_hours` int DEFAULT '0' COMMENT '实践学时',
  `total_hours` int DEFAULT '0' COMMENT '总学时',
  `course_type` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '课程类型（必修/选修/公选）',
  `course_category` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '课程类别（通识/学科基础/专业核心/实践环节）',
  `assessment_method` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '考核方式（考试/考查）',
  `semester_order` int DEFAULT '1' COMMENT '建议修读学期',
  `plan_id` bigint NOT NULL COMMENT '所属方案ID',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9325 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程库表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tpm_course_offering`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tpm_course_offering` (
  `offering_id` bigint NOT NULL AUTO_INCREMENT COMMENT '开课ID',
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `teacher_id` bigint DEFAULT NULL COMMENT '教师ID',
  `campus_id` bigint DEFAULT NULL COMMENT '校区ID',
  `class_count` int DEFAULT '1' COMMENT '教学班数',
  `max_students` int DEFAULT '0' COMMENT '容量上限',
  `offering_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '开课状态（0待确认 1已确认 2已取消）',
  `elastic_enabled` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '弹性扩容开关（0关闭 1开启）',
  `elastic_max` int DEFAULT NULL COMMENT '弹性扩容上限人数',
  `elastic_step` int DEFAULT NULL COMMENT '弹性扩容步长人数',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`offering_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9724 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开课计划表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tpm_credit_structure`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tpm_credit_structure` (
  `struct_id` bigint NOT NULL AUTO_INCREMENT COMMENT '结构ID',
  `plan_id` bigint NOT NULL COMMENT '所属方案ID',
  `credit_type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '学分类型编码',
  `credit_type_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '学分类型名称（通识必修/通识选修/学科基础/专业核心/实践环节等）',
  `required_credit` decimal(5,1) DEFAULT '0.0' COMMENT '要求学分',
  `min_credit` decimal(5,1) DEFAULT '0.0' COMMENT '最低学分',
  `description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '学分说明',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`struct_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9505 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学分结构表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tpm_plan_snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tpm_plan_snapshot` (
  `snapshot_id` bigint NOT NULL AUTO_INCREMENT COMMENT '快照ID(主键)',
  `plan_id` bigint NOT NULL COMMENT '培养方案ID(关联 tpm_training_plan.plan_id)',
  `version` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发布时的方案版本号',
  `content_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '实质内容指纹(SHA-256,十六进制小写,复用 TrainingPlanSnapshotService)',
  `total_credit` decimal(6,2) DEFAULT NULL COMMENT '快照时的要求总学分',
  `credit_structure_json` longtext COLLATE utf8mb4_general_ci COMMENT '学分结构定格快照JSON(发布时刻 tpm_credit_structure 分项)',
  `course_list_json` longtext COLLATE utf8mb4_general_ci COMMENT '课程清单定格快照JSON(发布时刻 tpm_course_library 摘要)',
  `publish_date` datetime DEFAULT NULL COMMENT '方案发布日期',
  `snapshot_time` datetime DEFAULT NULL COMMENT '快照生成时间',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`snapshot_id`),
  KEY `idx_plan_snapshot_plan` (`plan_id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='人才培养方案版本快照表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tpm_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tpm_schedule` (
  `schedule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '排课ID',
  `offering_id` bigint NOT NULL COMMENT '开课ID',
  `classroom_id` bigint DEFAULT NULL COMMENT '教室ID',
  `week_day` int DEFAULT NULL COMMENT '星期几（1-7）',
  `start_period` int DEFAULT NULL COMMENT '开始节次',
  `end_period` int DEFAULT NULL COMMENT '结束节次',
  `start_week` int DEFAULT NULL COMMENT '起始周',
  `end_week` int DEFAULT NULL COMMENT '结束周',
  `schedule_type` varchar(20) COLLATE utf8mb4_general_ci DEFAULT 'manual' COMMENT '排课方式（manual手动/auto自动）',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`schedule_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9346 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='排课表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tpm_schedule_adjustment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tpm_schedule_adjustment` (
  `adjust_id` bigint NOT NULL AUTO_INCREMENT COMMENT '调整ID',
  `schedule_id` bigint NOT NULL COMMENT '排课ID',
  `adjust_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '调整类型（调课/停课/补课）',
  `original_date` date DEFAULT NULL COMMENT '原日期',
  `new_date` date DEFAULT NULL COMMENT '新日期',
  `new_classroom_id` bigint DEFAULT NULL COMMENT '新教室ID',
  `new_week_day` int DEFAULT NULL COMMENT '新星期几',
  `new_start_period` int DEFAULT NULL COMMENT '新开始节次',
  `new_end_period` int DEFAULT NULL COMMENT '新结束节次',
  `reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '申请原因',
  `applicant` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '申请人',
  `approve_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '审批状态（0待审 1通过 2驳回）',
  `approve_by` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批人',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approve_comment` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '瀹℃壒鎰忚?',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`adjust_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9752 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='调停课申请表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tpm_selection_enrollment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tpm_selection_enrollment` (
  `enroll_id` bigint NOT NULL AUTO_INCREMENT COMMENT '选课记录ID',
  `round_id` bigint NOT NULL COMMENT '轮次ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_offering_id` bigint NOT NULL COMMENT '开课ID',
  `select_time` datetime DEFAULT NULL COMMENT '选课时间',
  `lottery_result` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '抽签结果（0未抽签 1中签 2未中签）',
  `result_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '1' COMMENT '结果状态（1选中 2落选 3退课）',
  `drop_time` datetime DEFAULT NULL COMMENT '退课时间',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `waitlist_rank` int DEFAULT NULL COMMENT '候补排名（抽签落选后的递补顺序，null表示非候补）',
  `priority` int DEFAULT NULL COMMENT '志愿优先级（1=第一志愿，越小越优先；weighted抽签按此加权）',
  PRIMARY KEY (`enroll_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9350 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='选课名单表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tpm_selection_round`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tpm_selection_round` (
  `round_id` bigint NOT NULL AUTO_INCREMENT COMMENT '轮次ID',
  `semester_id` bigint NOT NULL COMMENT '学期ID',
  `round_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '轮次名称（第一轮选课/第二轮选课/补退选）',
  `round_order` int DEFAULT '1' COMMENT '轮次顺序',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `max_courses_per_student` int DEFAULT '0' COMMENT '每人最多选课门数',
  `round_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '轮次状态（0未开始 1进行中 2已结束）',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `lottery_seed` bigint DEFAULT NULL COMMENT '抽签随机种子（可复现审计）',
  `lottery_time` datetime DEFAULT NULL COMMENT '抽签执行时间',
  `lottery_mode` varchar(20) COLLATE utf8mb4_general_ci DEFAULT 'random' COMMENT '抽签模式（random公平随机/weighted志愿权重）',
  `allow_drop_adjust` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '是否开放退改选窗口（0否 1是）',
  `drop_adjust_start` datetime DEFAULT NULL COMMENT '退改选窗口开始时间',
  `drop_adjust_end` datetime DEFAULT NULL COMMENT '退改选窗口结束时间',
  PRIMARY KEY (`round_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9712 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='选课轮次表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tpm_selection_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tpm_selection_rule` (
  `rule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `round_id` bigint NOT NULL COMMENT '轮次ID',
  `rule_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则名称',
  `rule_type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则类型（专业限制/年级限制/院系限制/人数上限/先修课程）',
  `restrict_target` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '限制目标（如专业编码、年级等）',
  `restrict_value` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '限制值',
  `priority` int DEFAULT '0' COMMENT '优先级',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`rule_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9713 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='选课规则表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tpm_teacher_forbidden`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tpm_teacher_forbidden` (
  `forbidden_id` bigint NOT NULL AUTO_INCREMENT COMMENT '禁排ID',
  `semester_id` bigint DEFAULT NULL COMMENT '学期ID（NULL表示全学期通用禁排）',
  `teacher_id` bigint NOT NULL COMMENT '教师ID（关联brm_teacher）',
  `week_day` int NOT NULL COMMENT '星期几（1-7）',
  `start_period` int NOT NULL COMMENT '开始节次',
  `end_period` int NOT NULL COMMENT '结束节次',
  `reason` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '禁排原因',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`forbidden_id`),
  KEY `idx_tf_teacher_sem` (`teacher_id`,`semester_id`),
  KEY `idx_tf_status` (`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师禁排时间片表（F2-1）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tpm_training_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tpm_training_plan` (
  `plan_id` bigint NOT NULL AUTO_INCREMENT COMMENT '方案ID',
  `plan_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '方案名称',
  `major_id` bigint NOT NULL COMMENT '所属专业ID',
  `dept_id` bigint DEFAULT NULL COMMENT '所属院系ID',
  `education_level` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '学历层次（本科/硕士/博士）',
  `plan_year` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '方案年份（如2024级）',
  `total_credits` decimal(5,1) DEFAULT '0.0' COMMENT '总学分',
  `publish_status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '发布状态（0草稿 1已发布 2已废止）',
  `publish_date` date DEFAULT NULL COMMENT '发布日期',
  `version` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '1.0' COMMENT '版本号',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`plan_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='人才培养方案表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


SET FOREIGN_KEY_CHECKS = 1;
