-- ===============================================
-- 学籍与学位管理模块 (SAM) 数据库初始化脚本
-- 包含：学籍管理、毕业与学位管理
-- ===============================================

-- ----------------------------
-- 1. 学生学籍信息表
-- ----------------------------
DROP TABLE IF EXISTS `sam_student`;
CREATE TABLE `sam_student` (
  `student_id` bigint NOT NULL AUTO_INCREMENT COMMENT '学生ID',
  `student_no` varchar(50) NOT NULL COMMENT '学号',
  `student_name` varchar(100) NOT NULL COMMENT '姓名',
  `gender` char(1) DEFAULT '0' COMMENT '性别（0男 1女）',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `id_card` varchar(18) DEFAULT NULL COMMENT '身份证号',
  `major_id` bigint DEFAULT NULL COMMENT '专业ID（关联brm_major）',
  `dept_id` bigint DEFAULT NULL COMMENT '院系ID（关联brm_department）',
  `class_id` bigint DEFAULT NULL COMMENT '班级ID（关联brm_class）',
  `enrollment_year` varchar(10) DEFAULT NULL COMMENT '入学年份（如2024级）',
  `education_level` varchar(20) DEFAULT NULL COMMENT '学历层次（本科/硕士/博士）',
  `student_status` char(1) DEFAULT '0' COMMENT '学籍状态（0在读 1休学 2退学 3毕业 4转出 5保留学籍）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学生学籍信息表';

-- ----------------------------
-- 2. 学籍异动表
-- ----------------------------
DROP TABLE IF EXISTS `sam_status_change`;
CREATE TABLE `sam_status_change` (
  `change_id` bigint NOT NULL AUTO_INCREMENT COMMENT '异动ID',
  `student_id` bigint NOT NULL COMMENT '学生ID（关联sam_student）',
  `change_type` char(1) NOT NULL COMMENT '异动类型（0休学 1复学 2转学 3退学 4保留学籍）',
  `change_date` date DEFAULT NULL COMMENT '异动日期',
  `original_status` char(1) DEFAULT NULL COMMENT '原学籍状态',
  `new_status` char(1) DEFAULT NULL COMMENT '新学籍状态',
  `reason` varchar(500) DEFAULT NULL COMMENT '申请原因',
  `applicant` varchar(50) DEFAULT NULL COMMENT '申请人',
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
  PRIMARY KEY (`change_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学籍异动表';

-- ----------------------------
-- 3. 学籍预警表
-- ----------------------------
DROP TABLE IF EXISTS `sam_warning`;
CREATE TABLE `sam_warning` (
  `warning_id` bigint NOT NULL AUTO_INCREMENT COMMENT '预警ID',
  `student_id` bigint NOT NULL COMMENT '学生ID（关联sam_student）',
  `semester_id` bigint DEFAULT NULL COMMENT '学期ID（关联brm_semester）',
  `warning_type` char(1) DEFAULT '0' COMMENT '预警类型（0成绩预警 1学分预警 2出勤预警 3综合预警）',
  `warning_level` char(1) DEFAULT '0' COMMENT '预警级别（0一般 1严重 2高危）',
  `warning_reason` varchar(500) DEFAULT NULL COMMENT '预警原因',
  `warning_date` date DEFAULT NULL COMMENT '预警日期',
  `is_resolved` char(1) DEFAULT '0' COMMENT '是否解除（0否 1是）',
  `resolve_date` date DEFAULT NULL COMMENT '解除日期',
  `resolve_remark` varchar(500) DEFAULT NULL COMMENT '解除说明',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`warning_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学籍预警表';

-- ----------------------------
-- 4. 毕业资格审核表
-- ----------------------------
DROP TABLE IF EXISTS `sam_graduation_review`;
CREATE TABLE `sam_graduation_review` (
  `review_id` bigint NOT NULL AUTO_INCREMENT COMMENT '审核ID',
  `student_id` bigint NOT NULL COMMENT '学生ID（关联sam_student）',
  `total_credits_earned` decimal(5,1) DEFAULT '0.0' COMMENT '已获总学分',
  `required_credits` decimal(5,1) DEFAULT '0.0' COMMENT '要求学分',
  `is_credit_qualified` char(1) DEFAULT '0' COMMENT '学分是否合格（0否 1是）',
  `is_course_qualified` char(1) DEFAULT '0' COMMENT '课程是否合格（0否 1是）',
  `is_english_qualified` char(1) DEFAULT '0' COMMENT '英语是否合格（0否 1是）',
  `is_pe_qualified` char(1) DEFAULT '0' COMMENT '体育是否合格（0否 1是）',
  `review_status` char(1) DEFAULT '0' COMMENT '审核状态（0待审 1通过 2不通过）',
  `review_date` date DEFAULT NULL COMMENT '审核日期',
  `reviewer` varchar(50) DEFAULT NULL COMMENT '审核人',
  `review_opinion` varchar(255) DEFAULT NULL COMMENT '审核意见',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`review_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='毕业资格审核表';

-- ----------------------------
-- 5. 学位资格审核表
-- ----------------------------
DROP TABLE IF EXISTS `sam_degree_review`;
CREATE TABLE `sam_degree_review` (
  `review_id` bigint NOT NULL AUTO_INCREMENT COMMENT '审核ID',
  `student_id` bigint NOT NULL COMMENT '学生ID（关联sam_student）',
  `graduation_review_id` bigint DEFAULT NULL COMMENT '毕业审核ID（关联sam_graduation_review）',
  `gpa` decimal(4,2) DEFAULT '0.00' COMMENT '平均绩点',
  `is_gpa_qualified` char(1) DEFAULT '0' COMMENT '绩点是否合格（0否 1是）',
  `is_degree_course_qualified` char(1) DEFAULT '0' COMMENT '学位课程是否合格（0否 1是）',
  `is_thesis_qualified` char(1) DEFAULT '0' COMMENT '论文是否合格（0否 1是）',
  `degree_type` varchar(20) DEFAULT NULL COMMENT '学位类型（学士/硕士/博士）',
  `degree_field` varchar(100) DEFAULT NULL COMMENT '学位学科门类（工学/理学/管理学等）',
  `review_status` char(1) DEFAULT '0' COMMENT '审核状态（0待审 1通过 2不通过）',
  `review_date` date DEFAULT NULL COMMENT '审核日期',
  `reviewer` varchar(50) DEFAULT NULL COMMENT '审核人',
  `review_opinion` varchar(255) DEFAULT NULL COMMENT '审核意见',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`review_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学位资格审核表';

-- ----------------------------
-- 6. 证书编号管理表
-- ----------------------------
DROP TABLE IF EXISTS `sam_certificate`;
CREATE TABLE `sam_certificate` (
  `cert_id` bigint NOT NULL AUTO_INCREMENT COMMENT '证书ID',
  `student_id` bigint NOT NULL COMMENT '学生ID（关联sam_student）',
  `cert_type` char(1) NOT NULL COMMENT '证书类型（0毕业证书 1学位证书 2结业证书）',
  `cert_number` varchar(100) NOT NULL COMMENT '证书编号',
  `cert_date` date DEFAULT NULL COMMENT '发证日期',
  `major_id` bigint DEFAULT NULL COMMENT '专业ID（关联brm_major）',
  `education_level` varchar(20) DEFAULT NULL COMMENT '学历层次',
  `is_issued` char(1) DEFAULT '0' COMMENT '是否发放（0否 1是）',
  `issue_date` date DEFAULT NULL COMMENT '发放日期',
  `receiver` varchar(50) DEFAULT NULL COMMENT '领取人',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`cert_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='证书编号管理表';

-- ----------------------------
-- 7. 毕业离校手续表
-- ----------------------------
DROP TABLE IF EXISTS `sam_graduation_procedure`;
CREATE TABLE `sam_graduation_procedure` (
  `procedure_id` bigint NOT NULL AUTO_INCREMENT COMMENT '手续ID',
  `student_id` bigint NOT NULL COMMENT '学生ID（关联sam_student）',
  `library_cleared` char(1) DEFAULT '0' COMMENT '图书馆清还（0未清 1已清）',
  `finance_cleared` char(1) DEFAULT '0' COMMENT '财务结算（0未结 1已结）',
  `dormitory_cleared` char(1) DEFAULT '0' COMMENT '宿舍退宿（0未退 1已退）',
  `card_returned` char(1) DEFAULT '0' COMMENT '一卡通退还（0未退 1已退）',
  `procedure_status` char(1) DEFAULT '0' COMMENT '手续状态（0未办理 1办理中 2已完成）',
  `complete_date` date DEFAULT NULL COMMENT '完成日期',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`procedure_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='毕业离校手续表';
