-- ===============================================
-- 基础与资源管理模块 (BRM) 数据库初始化脚本
-- 包含：基础信息管理、教师信息管理、教室资源管理
-- ===============================================

-- ----------------------------
-- 1. 学年表
-- ----------------------------
DROP TABLE IF EXISTS `brm_academic_year`;
CREATE TABLE `brm_academic_year` (
  `year_id` bigint NOT NULL AUTO_INCREMENT COMMENT '学年ID',
  `year_name` varchar(50) NOT NULL COMMENT '学年名称',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`year_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学年表';

-- ----------------------------
-- 2. 学期表
-- ----------------------------
DROP TABLE IF EXISTS `brm_semester`;
CREATE TABLE `brm_semester` (
  `semester_id` bigint NOT NULL AUTO_INCREMENT COMMENT '学期ID',
  `semester_name` varchar(50) NOT NULL COMMENT '学期名称',
  `academic_year_id` bigint NOT NULL COMMENT '学年ID',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `semester_order` int DEFAULT '1' COMMENT '学期排序',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`semester_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学期表';

-- ----------------------------
-- 3. 院系表（树形结构）
-- ----------------------------
DROP TABLE IF EXISTS `brm_department`;
CREATE TABLE `brm_department` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '院系ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父院系ID',
  `ancestors` varchar(500) DEFAULT '' COMMENT '祖级列表',
  `dept_code` varchar(50) NOT NULL COMMENT '院系编码',
  `dept_name` varchar(100) NOT NULL COMMENT '院系名称',
  `leader` varchar(50) DEFAULT NULL COMMENT '负责人',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='院系表';

-- ----------------------------
-- 4. 专业表
-- ----------------------------
DROP TABLE IF EXISTS `brm_major`;
CREATE TABLE `brm_major` (
  `major_id` bigint NOT NULL AUTO_INCREMENT COMMENT '专业ID',
  `major_code` varchar(50) NOT NULL COMMENT '专业编码',
  `major_name` varchar(100) NOT NULL COMMENT '专业名称',
  `dept_id` bigint DEFAULT NULL COMMENT '所属院系ID',
  `education_level` varchar(20) DEFAULT NULL COMMENT '学历层次（本科/硕士/博士）',
  `duration` int DEFAULT '4' COMMENT '学制（年）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`major_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='专业表';

-- ----------------------------
-- 5. 班级表
-- ----------------------------
DROP TABLE IF EXISTS `brm_class`;
CREATE TABLE `brm_class` (
  `class_id` bigint NOT NULL AUTO_INCREMENT COMMENT '班级ID',
  `class_code` varchar(50) NOT NULL COMMENT '班级编码',
  `class_name` varchar(100) NOT NULL COMMENT '班级名称',
  `major_id` bigint DEFAULT NULL COMMENT '所属专业ID',
  `grade` varchar(10) DEFAULT NULL COMMENT '年级',
  `student_count` int DEFAULT '0' COMMENT '学生人数',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`class_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='班级表';

-- ----------------------------
-- 6. 校区表
-- ----------------------------
DROP TABLE IF EXISTS `brm_campus`;
CREATE TABLE `brm_campus` (
  `campus_id` bigint NOT NULL AUTO_INCREMENT COMMENT '校区ID',
  `campus_name` varchar(100) NOT NULL COMMENT '校区名称',
  `campus_address` varchar(255) DEFAULT NULL COMMENT '校区地址',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`campus_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='校区表';

-- ----------------------------
-- 7. 教学楼表
-- ----------------------------
DROP TABLE IF EXISTS `brm_building`;
CREATE TABLE `brm_building` (
  `building_id` bigint NOT NULL AUTO_INCREMENT COMMENT '教学楼ID',
  `building_name` varchar(100) NOT NULL COMMENT '教学楼名称',
  `building_code` varchar(50) DEFAULT NULL COMMENT '教学楼编码',
  `campus_id` bigint DEFAULT NULL COMMENT '所属校区ID',
  `floor_count` int DEFAULT '1' COMMENT '楼层数',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`building_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教学楼表';

-- ----------------------------
-- 8. 教室表
-- ----------------------------
DROP TABLE IF EXISTS `brm_classroom`;
CREATE TABLE `brm_classroom` (
  `classroom_id` bigint NOT NULL AUTO_INCREMENT COMMENT '教室ID',
  `classroom_name` varchar(100) NOT NULL COMMENT '教室名称',
  `building_id` bigint DEFAULT NULL COMMENT '所属教学楼ID',
  `type_id` bigint DEFAULT NULL COMMENT '教室类型ID',
  `capacity` int DEFAULT '0' COMMENT '容纳人数',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`classroom_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教室表';

-- ----------------------------
-- 9. 教师表
-- ----------------------------
DROP TABLE IF EXISTS `brm_teacher`;
CREATE TABLE `brm_teacher` (
  `teacher_id` bigint NOT NULL AUTO_INCREMENT COMMENT '教师ID',
  `teacher_code` varchar(50) NOT NULL COMMENT '教师工号',
  `teacher_name` varchar(50) NOT NULL COMMENT '教师姓名',
  `user_id` bigint DEFAULT NULL COMMENT '关联系统用户ID（关联sys_user）',
  `dept_id` bigint DEFAULT NULL COMMENT '所属院系ID',
  `gender` char(1) DEFAULT '0' COMMENT '性别（0男 1女 2未知）',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `title` varchar(50) DEFAULT NULL COMMENT '职称',
  `education` varchar(20) DEFAULT NULL COMMENT '学历',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`teacher_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师表';

-- ----------------------------
-- 10. 教师任职信息表
-- ----------------------------
DROP TABLE IF EXISTS `brm_teacher_position`;
CREATE TABLE `brm_teacher_position` (
  `pos_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任职ID',
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `dept_id` bigint DEFAULT NULL COMMENT '任职院系ID',
  `position_title` varchar(100) NOT NULL COMMENT '任职岗位',
  `start_date` date DEFAULT NULL COMMENT '任职开始日期',
  `end_date` date DEFAULT NULL COMMENT '任职结束日期',
  `is_current` char(1) DEFAULT '0' COMMENT '是否现任（0是 1否）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`pos_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师任职信息表';

-- ----------------------------
-- 11. 教师授课资格表
-- ----------------------------
DROP TABLE IF EXISTS `brm_teacher_qualification`;
CREATE TABLE `brm_teacher_qualification` (
  `qual_id` bigint NOT NULL AUTO_INCREMENT COMMENT '资格ID',
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `course_category` varchar(100) DEFAULT NULL COMMENT '可授课程类别',
  `certify_authority` varchar(100) DEFAULT NULL COMMENT '认证机构',
  `qualify_date` date DEFAULT NULL COMMENT '获证日期',
  `expire_date` date DEFAULT NULL COMMENT '到期日期',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`qual_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师授课资格表';

-- ----------------------------
-- 12. 教室类型表
-- ----------------------------
DROP TABLE IF EXISTS `brm_classroom_type`;
CREATE TABLE `brm_classroom_type` (
  `type_id` bigint NOT NULL AUTO_INCREMENT COMMENT '类型ID',
  `type_name` varchar(50) NOT NULL COMMENT '类型名称',
  `type_desc` varchar(255) DEFAULT NULL COMMENT '类型描述',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`type_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教室类型表';

-- ----------------------------
-- 13. 教室借用表
-- ----------------------------
DROP TABLE IF EXISTS `brm_classroom_borrow`;
CREATE TABLE `brm_classroom_borrow` (
  `borrow_id` bigint NOT NULL AUTO_INCREMENT COMMENT '借用ID',
  `classroom_id` bigint NOT NULL COMMENT '教室ID',
  `applicant` varchar(50) NOT NULL COMMENT '申请人',
  `applicant_dept` varchar(100) DEFAULT NULL COMMENT '申请人部门',
  `borrow_date` date NOT NULL COMMENT '借用日期',
  `start_time` varchar(10) DEFAULT NULL COMMENT '开始时间',
  `end_time` varchar(10) DEFAULT NULL COMMENT '结束时间',
  `purpose` varchar(255) DEFAULT NULL COMMENT '借用用途',
  `approve_status` char(1) DEFAULT '0' COMMENT '审批状态（0待审 1通过 2驳回）',
  `approve_by` varchar(50) DEFAULT NULL COMMENT '审批人',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`borrow_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教室借用表';

-- ----------------------------
-- 14. 多媒体设备表
-- ----------------------------
DROP TABLE IF EXISTS `brm_equipment`;
CREATE TABLE `brm_equipment` (
  `equip_id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  `equip_name` varchar(100) NOT NULL COMMENT '设备名称',
  `classroom_id` bigint DEFAULT NULL COMMENT '所属教室ID',
  `equip_type` varchar(50) DEFAULT NULL COMMENT '设备类型',
  `model` varchar(100) DEFAULT NULL COMMENT '设备型号',
  `purchase_date` date DEFAULT NULL COMMENT '购置日期',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`equip_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='多媒体设备表';

-- ----------------------------
-- 15. 设备维护记录表
-- ----------------------------
DROP TABLE IF EXISTS `brm_equipment_maintenance`;
CREATE TABLE `brm_equipment_maintenance` (
  `maintenance_id` bigint NOT NULL AUTO_INCREMENT COMMENT '维护ID',
  `equip_id` bigint NOT NULL COMMENT '设备ID',
  `fault_desc` varchar(500) NOT NULL COMMENT '故障描述',
  `repair_date` date DEFAULT NULL COMMENT '维修日期',
  `repair_by` varchar(50) DEFAULT NULL COMMENT '维修人',
  `repair_cost` decimal(10,2) DEFAULT '0.00' COMMENT '维修费用',
  `result` varchar(255) DEFAULT NULL COMMENT '维修结果',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`maintenance_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备维护记录表';
