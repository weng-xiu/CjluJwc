-- ===============================================
-- 办公自动化模块 (OA) 数据库初始化脚本
-- 包含：工作流、公文管理、会议管理、通知公告、日程安排
-- ===============================================

-- ----------------------------
-- 1. 流程分类表
-- ----------------------------
DROP TABLE IF EXISTS `oa_process_category`;
CREATE TABLE `oa_process_category` (
  `category_id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `category_name` varchar(100) NOT NULL COMMENT '分类名称',
  `category_code` varchar(50) NOT NULL COMMENT '分类编码',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流程分类表';

-- ----------------------------
-- 2. 流程定义快照表
-- ----------------------------
DROP TABLE IF EXISTS `oa_process_definition`;
CREATE TABLE `oa_process_definition` (
  `definition_id` bigint NOT NULL AUTO_INCREMENT COMMENT '定义ID',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `process_key` varchar(100) NOT NULL COMMENT '流程标识（BPMN process id）',
  `process_name` varchar(200) NOT NULL COMMENT '流程名称',
  `version` int DEFAULT '1' COMMENT '版本号',
  `deployment_id` varchar(64) DEFAULT NULL COMMENT 'Flowable 部署ID',
  `proc_def_id` varchar(64) DEFAULT NULL COMMENT 'Flowable 流程定义ID',
  `bpmn_xml` text COMMENT 'BPMN XML 内容',
  `description` varchar(500) DEFAULT NULL COMMENT '流程说明',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`definition_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流程定义快照表';

-- ----------------------------
-- 3. 业务流程实例关联表
-- ----------------------------
DROP TABLE IF EXISTS `oa_process_instance`;
CREATE TABLE `oa_process_instance` (
  `instance_id` bigint NOT NULL AUTO_INCREMENT COMMENT '实例ID',
  `business_type` varchar(50) NOT NULL COMMENT '业务类型（document/meeting/leave等）',
  `business_id` bigint NOT NULL COMMENT '业务主键',
  `definition_id` bigint DEFAULT NULL COMMENT '流程定义ID',
  `proc_inst_id` varchar(64) DEFAULT NULL COMMENT 'Flowable 流程实例ID',
  `starter_id` bigint DEFAULT NULL COMMENT '发起人用户ID',
  `starter_name` varchar(50) DEFAULT NULL COMMENT '发起人姓名',
  `current_task_name` varchar(100) DEFAULT NULL COMMENT '当前任务名称',
  `current_assignee` varchar(50) DEFAULT NULL COMMENT '当前处理人',
  `process_status` char(1) DEFAULT '0' COMMENT '流程状态（0运行中 1已完成 2已驳回 3已撤销）',
  `start_time` datetime DEFAULT NULL COMMENT '发起时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`instance_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='业务流程实例关联表';

-- ----------------------------
-- 4. 审批任务记录表
-- ----------------------------
DROP TABLE IF EXISTS `oa_task_record`;
CREATE TABLE `oa_task_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `instance_id` bigint NOT NULL COMMENT '流程实例ID',
  `task_id` varchar(64) DEFAULT NULL COMMENT 'Flowable 任务ID',
  `task_name` varchar(100) DEFAULT NULL COMMENT '任务名称',
  `assignee_id` bigint DEFAULT NULL COMMENT '处理人用户ID',
  `assignee_name` varchar(50) DEFAULT NULL COMMENT '处理人姓名',
  `action_type` char(1) DEFAULT NULL COMMENT '操作类型（0通过 1驳回 2转办 3会签 4撤销）',
  `comment` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `start_time` datetime DEFAULT NULL COMMENT '任务开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '任务结束时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审批任务记录表';

-- ----------------------------
-- 5. 公文主表
-- ----------------------------
DROP TABLE IF EXISTS `oa_document`;
CREATE TABLE `oa_document` (
  `document_id` bigint NOT NULL AUTO_INCREMENT COMMENT '公文ID',
  `document_no` varchar(100) DEFAULT NULL COMMENT '文号',
  `title` varchar(200) NOT NULL COMMENT '公文标题',
  `document_type` char(1) DEFAULT '0' COMMENT '公文类型（0发文 1收文 2签报）',
  `secret_level` char(1) DEFAULT '0' COMMENT '密级（0公开 1内部 2秘密 3机密）',
  `urgent_level` char(1) DEFAULT '0' COMMENT '紧急程度（0普通 1加急 2特急）',
  `content` longtext COMMENT '正文内容',
  `attachments` varchar(500) DEFAULT NULL COMMENT '附件URL（逗号分隔）',
  `originator_id` bigint DEFAULT NULL COMMENT '发起人用户ID',
  `originator_name` varchar(50) DEFAULT NULL COMMENT '发起人姓名',
  `origin_dept_id` bigint DEFAULT NULL COMMENT '发起部门ID',
  `origin_dept_name` varchar(100) DEFAULT NULL COMMENT '发起部门名称',
  `process_instance_id` bigint DEFAULT NULL COMMENT '流程实例ID',
  `document_status` char(1) DEFAULT '0' COMMENT '公文状态（0草稿 1审批中 2已发布 3已归档 4已驳回）',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`document_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公文主表';

-- ----------------------------
-- 6. 公文附件表
-- ----------------------------
DROP TABLE IF EXISTS `oa_document_attach`;
CREATE TABLE `oa_document_attach` (
  `attach_id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `document_id` bigint NOT NULL COMMENT '公文ID',
  `file_name` varchar(200) DEFAULT NULL COMMENT '文件名称',
  `file_url` varchar(500) DEFAULT NULL COMMENT '文件路径',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小（字节）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`attach_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公文附件表';

-- ----------------------------
-- 7. 公文抄送记录表
-- ----------------------------
DROP TABLE IF EXISTS `oa_document_copy`;
CREATE TABLE `oa_document_copy` (
  `copy_id` bigint NOT NULL AUTO_INCREMENT COMMENT '抄送ID',
  `document_id` bigint NOT NULL COMMENT '公文ID',
  `user_id` bigint NOT NULL COMMENT '抄送人用户ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '抄送人姓名',
  `read_status` char(1) DEFAULT '0' COMMENT '阅读状态（0未读 1已读）',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`copy_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公文抄送记录表';

-- ----------------------------
-- 8. 会议室基础信息表
-- ----------------------------
DROP TABLE IF EXISTS `oa_meeting_room`;
CREATE TABLE `oa_meeting_room` (
  `room_id` bigint NOT NULL AUTO_INCREMENT COMMENT '会议室ID',
  `room_name` varchar(100) NOT NULL COMMENT '会议室名称',
  `room_location` varchar(200) DEFAULT NULL COMMENT '会议室位置',
  `capacity` int DEFAULT '0' COMMENT '容纳人数',
  `equipment` varchar(500) DEFAULT NULL COMMENT '设备配置',
  `admin_id` bigint DEFAULT NULL COMMENT '管理员用户ID',
  `admin_name` varchar(50) DEFAULT NULL COMMENT '管理员姓名',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`room_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会议室基础信息表';

-- ----------------------------
-- 9. 会议主表
-- ----------------------------
DROP TABLE IF EXISTS `oa_meeting`;
CREATE TABLE `oa_meeting` (
  `meeting_id` bigint NOT NULL AUTO_INCREMENT COMMENT '会议ID',
  `meeting_theme` varchar(200) NOT NULL COMMENT '会议主题',
  `room_id` bigint DEFAULT NULL COMMENT '会议室ID',
  `room_name` varchar(100) DEFAULT NULL COMMENT '会议室名称',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `organizer_id` bigint DEFAULT NULL COMMENT '组织者用户ID',
  `organizer_name` varchar(50) DEFAULT NULL COMMENT '组织者姓名',
  `meeting_type` char(1) DEFAULT '0' COMMENT '会议类型（0普通会议 1视频会议 2紧急会议）',
  `meeting_status` char(1) DEFAULT '0' COMMENT '会议状态（0未开始 1进行中 2已结束 3已取消）',
  `content` text COMMENT '会议内容',
  `attachment_url` varchar(500) DEFAULT NULL COMMENT '附件URL',
  `process_instance_id` bigint DEFAULT NULL COMMENT '流程实例ID',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`meeting_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会议主表';

-- ----------------------------
-- 10. 参会人员表
-- ----------------------------
DROP TABLE IF EXISTS `oa_meeting_participant`;
CREATE TABLE `oa_meeting_participant` (
  `participant_id` bigint NOT NULL AUTO_INCREMENT COMMENT '参会ID',
  `meeting_id` bigint NOT NULL COMMENT '会议ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '用户姓名',
  `dept_name` varchar(100) DEFAULT NULL COMMENT '部门名称',
  `attend_status` char(1) DEFAULT '0' COMMENT '出席状态（0待确认 1参加 2不参加 3待定）',
  `sign_status` char(1) DEFAULT '0' COMMENT '签到状态（0未签到 1已签到）',
  `sign_time` datetime DEFAULT NULL COMMENT '签到时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`participant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='参会人员表';

-- ----------------------------
-- 11. 会议纪要表
-- ----------------------------
DROP TABLE IF EXISTS `oa_meeting_minutes`;
CREATE TABLE `oa_meeting_minutes` (
  `minutes_id` bigint NOT NULL AUTO_INCREMENT COMMENT '纪要ID',
  `meeting_id` bigint NOT NULL COMMENT '会议ID',
  `content` longtext COMMENT '纪要内容',
  `recorder_id` bigint DEFAULT NULL COMMENT '记录人用户ID',
  `recorder_name` varchar(50) DEFAULT NULL COMMENT '记录人姓名',
  `issue_status` char(1) DEFAULT '0' COMMENT '下发状态（0未下发 1已下发）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`minutes_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会议纪要表';

-- ----------------------------
-- 12. 通知公告主表
-- ----------------------------
DROP TABLE IF EXISTS `oa_notice`;
CREATE TABLE `oa_notice` (
  `notice_id` bigint NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `notice_title` varchar(200) NOT NULL COMMENT '公告标题',
  `notice_content` longtext COMMENT '公告内容',
  `notice_type` char(1) DEFAULT '0' COMMENT '公告类型（0通知 1公告 2通报）',
  `publish_scope` char(1) DEFAULT '0' COMMENT '发布范围（0全体 1指定部门 2指定人员）',
  `is_top` char(1) DEFAULT '0' COMMENT '是否置顶（0否 1是）',
  `publish_status` char(1) DEFAULT '0' COMMENT '发布状态（0草稿 1已发布 2已撤回）',
  `publisher_id` bigint DEFAULT NULL COMMENT '发布人用户ID',
  `publisher_name` varchar(50) DEFAULT NULL COMMENT '发布人姓名',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `end_time` datetime DEFAULT NULL COMMENT '截止时间',
  `read_count` int DEFAULT '0' COMMENT '阅读次数',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知公告主表';

-- ----------------------------
-- 13. 通知公告部门范围表
-- ----------------------------
DROP TABLE IF EXISTS `oa_notice_dept`;
CREATE TABLE `oa_notice_dept` (
  `notice_dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '范围ID',
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  `dept_name` varchar(100) DEFAULT NULL COMMENT '部门名称',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`notice_dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知公告部门范围表';

-- ----------------------------
-- 14. 通知公告已读记录表
-- ----------------------------
DROP TABLE IF EXISTS `oa_notice_read`;
CREATE TABLE `oa_notice_read` (
  `read_id` bigint NOT NULL AUTO_INCREMENT COMMENT '已读ID',
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '用户姓名',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`read_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知公告已读记录表';

-- ----------------------------
-- 15. 日程安排主表
-- ----------------------------
DROP TABLE IF EXISTS `oa_schedule`;
CREATE TABLE `oa_schedule` (
  `schedule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日程ID',
  `schedule_title` varchar(200) NOT NULL COMMENT '日程标题',
  `schedule_type` char(1) DEFAULT '0' COMMENT '日程类型（0个人 1会议 2任务 3提醒）',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `all_day` char(1) DEFAULT '0' COMMENT '是否全天（0否 1是）',
  `remind_type` char(1) DEFAULT '0' COMMENT '提醒方式（0不提醒 1系统消息 2邮件 3短信）',
  `remind_time` datetime DEFAULT NULL COMMENT '提醒时间',
  `color` varchar(20) DEFAULT NULL COMMENT '颜色标记',
  `location` varchar(200) DEFAULT NULL COMMENT '地点',
  `schedule_content` text COMMENT '日程内容',
  `owner_id` bigint NOT NULL COMMENT '所属人用户ID',
  `owner_name` varchar(50) DEFAULT NULL COMMENT '所属人姓名',
  `schedule_status` char(1) DEFAULT '0' COMMENT '日程状态（0正常 1已完成 2已取消）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`schedule_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='日程安排主表';

-- ----------------------------
-- 16. 日程共享人员表
-- ----------------------------
DROP TABLE IF EXISTS `oa_schedule_share`;
CREATE TABLE `oa_schedule_share` (
  `share_id` bigint NOT NULL AUTO_INCREMENT COMMENT '共享ID',
  `schedule_id` bigint NOT NULL COMMENT '日程ID',
  `user_id` bigint NOT NULL COMMENT '共享用户ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '共享用户姓名',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`share_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='日程共享人员表';
