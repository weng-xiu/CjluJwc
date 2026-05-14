-- ===============================================
-- 数据对接与共享模块 (DIS) 数据库初始化脚本
-- 包含：外部系统配置、接口配置、数据交换日志、数据同步任务
-- ===============================================

-- ----------------------------
-- 1. 外部系统配置表
-- ----------------------------
DROP TABLE IF EXISTS `dis_external_system`;
CREATE TABLE `dis_external_system` (
  `system_id` bigint NOT NULL AUTO_INCREMENT COMMENT '系统ID',
  `system_name` varchar(100) NOT NULL COMMENT '系统名称',
  `system_code` varchar(50) NOT NULL COMMENT '系统编码',
  `system_type` varchar(20) NOT NULL COMMENT '系统类型（GRADUATE-研究生系统 FINANCE-财务系统 CARD-一卡通系统 AUTH-统一身份认证平台）',
  `base_url` varchar(255) DEFAULT '' COMMENT '基础URL',
  `auth_type` varchar(20) DEFAULT 'TOKEN' COMMENT '认证方式（TOKEN BASIC OAUTH2 NONE）',
  `auth_config` text COMMENT '认证配置（JSON格式，如token、appKey等）',
  `description` varchar(500) DEFAULT NULL COMMENT '系统描述',
  `contact_name` varchar(50) DEFAULT '' COMMENT '对接负责人',
  `contact_phone` varchar(20) DEFAULT '' COMMENT '联系电话',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`system_id`),
  UNIQUE KEY `uk_system_code` (`system_code`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='外部系统配置表';

-- 初始化外部系统数据
INSERT INTO `dis_external_system` VALUES (1, '研究生管理系统', 'GRADUATE', 'GRADUATE', 'http://graduate.example.com/api', 'TOKEN', '{"headerName":"Authorization","tokenPrefix":"Bearer"}', '研究生招生、培养、学位管理', NULL, NULL, '0', 'admin', sysdate(), '', NULL, NULL);
INSERT INTO `dis_external_system` VALUES (2, '财务管理系统', 'FINANCE', 'FINANCE', 'http://finance.example.com/api', 'BASIC', '{"username":"admin","password":"******"}', '学费、工资、财务核算管理', NULL, NULL, '0', 'admin', sysdate(), '', NULL, NULL);
INSERT INTO `dis_external_system` VALUES (3, '一卡通系统', 'CARD', 'CARD', 'http://card.example.com/api', 'TOKEN', '{"headerName":"X-Auth-Token","tokenPrefix":""}', '校园一卡通消费与管理', NULL, NULL, '0', 'admin', sysdate(), '', NULL, NULL);
INSERT INTO `dis_external_system` VALUES (4, '统一身份认证平台', 'AUTH', 'AUTH', 'http://auth.example.com', 'OAUTH2', '{"clientId":"yu_admin","clientSecret":"******","authorizeUrl":"/oauth/authorize","tokenUrl":"/oauth/token"}', '统一身份认证与单点登录', NULL, NULL, '0', 'admin', sysdate(), '', NULL, NULL);

-- ----------------------------
-- 2. 接口配置表
-- ----------------------------
DROP TABLE IF EXISTS `dis_interface_config`;
CREATE TABLE `dis_interface_config` (
  `interface_id` bigint NOT NULL AUTO_INCREMENT COMMENT '接口ID',
  `system_id` bigint NOT NULL COMMENT '所属系统ID',
  `interface_name` varchar(100) NOT NULL COMMENT '接口名称',
  `interface_code` varchar(50) NOT NULL COMMENT '接口编码',
  `request_method` varchar(10) NOT NULL DEFAULT 'GET' COMMENT '请求方式（GET POST PUT DELETE）',
  `request_path` varchar(255) NOT NULL COMMENT '请求路径',
  `request_template` text COMMENT '请求模板（JSON格式参数定义）',
  `response_template` text COMMENT '响应模板',
  `timeout_seconds` int DEFAULT '30' COMMENT '超时时间（秒）',
  `retry_count` int DEFAULT '0' COMMENT '重试次数',
  `description` varchar(500) DEFAULT NULL COMMENT '接口描述',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`interface_id`),
  KEY `idx_system_id` (`system_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='接口配置表';

-- ----------------------------
-- 3. 数据交换日志表
-- ----------------------------
DROP TABLE IF EXISTS `dis_data_exchange_log`;
CREATE TABLE `dis_data_exchange_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `system_id` bigint DEFAULT NULL COMMENT '外部系统ID',
  `interface_id` bigint DEFAULT NULL COMMENT '接口ID',
  `request_url` varchar(500) DEFAULT '' COMMENT '请求完整URL',
  `request_method` varchar(10) DEFAULT '' COMMENT '请求方式',
  `request_data` longtext COMMENT '请求数据',
  `response_data` longtext COMMENT '响应数据',
  `response_code` int DEFAULT NULL COMMENT 'HTTP响应码',
  `status` char(1) DEFAULT '0' COMMENT '执行状态（0成功 1失败）',
  `error_msg` varchar(2000) DEFAULT '' COMMENT '错误消息',
  `execute_time` datetime DEFAULT NULL COMMENT '执行时间',
  `cost_time` bigint DEFAULT '0' COMMENT '消耗时间（毫秒）',
  `operator` varchar(50) DEFAULT '' COMMENT '操作人员',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_system_id` (`system_id`),
  KEY `idx_status` (`status`),
  KEY `idx_execute_time` (`execute_time`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据交换日志表';

-- ----------------------------
-- 4. 数据同步任务表
-- ----------------------------
DROP TABLE IF EXISTS `dis_sync_task`;
CREATE TABLE `dis_sync_task` (
  `task_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_name` varchar(100) NOT NULL COMMENT '任务名称',
  `task_code` varchar(50) NOT NULL COMMENT '任务编码',
  `system_id` bigint NOT NULL COMMENT '外部系统ID',
  `interface_id` bigint DEFAULT NULL COMMENT '接口ID',
  `cron_expression` varchar(100) DEFAULT '' COMMENT 'Cron表达式',
  `last_execute_time` datetime DEFAULT NULL COMMENT '上次执行时间',
  `next_execute_time` datetime DEFAULT NULL COMMENT '下次执行时间',
  `execute_count` int DEFAULT '0' COMMENT '执行次数',
  `fail_count` int DEFAULT '0' COMMENT '失败次数',
  `status` char(1) DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_task_code` (`task_code`),
  KEY `idx_system_id` (`system_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据同步任务表';
