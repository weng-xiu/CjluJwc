-- ===============================================
-- phase29：S6 学业预警闭环（多渠道通知 + 帮扶任务跟踪）数据库升级脚本（幂等）
-- 内容：
--   1) 新建 sys_notify_log（消息渠道发送留痕：站内信/邮件/短信）
--   2) 新建 sam_warning_assist（预警帮扶任务）+ sam_warning_assist_record（帮扶跟踪记录）
--   3) 新增字典 sam_assist_status、sam_follow_type
--   4) 新增管理端「预警帮扶」菜单（parent=2301 学籍管理）及按钮，授权超级管理员
--   5) 新增系统参数：多渠道开关/SMTP + 帮扶自动派发配置
-- 幂等：CREATE TABLE IF NOT EXISTS + INSERT ... WHERE NOT EXISTS
-- menu_id 采用 3080 段（3070 已被 phase28 占用）
-- ===============================================

-- 1) 消息渠道发送留痕表
CREATE TABLE IF NOT EXISTS `sys_notify_log` (
  `log_id`        bigint       NOT NULL AUTO_INCREMENT COMMENT '留痕ID',
  `channel`       varchar(10)  NOT NULL COMMENT '渠道(site站内信 mail邮件 sms短信)',
  `receiver_id`   bigint       DEFAULT NULL COMMENT '接收人用户ID',
  `target`        varchar(120) DEFAULT NULL COMMENT '送达目标(邮箱/手机号/站内标识)',
  `business_type` varchar(50)  DEFAULT NULL COMMENT '关联业务类型',
  `business_id`   bigint       DEFAULT NULL COMMENT '关联业务ID',
  `title`         varchar(200) DEFAULT NULL COMMENT '消息标题',
  `status`        char(1)      NOT NULL DEFAULT '0' COMMENT '发送状态(0成功 1失败 2跳过)',
  `error_msg`     varchar(500) DEFAULT NULL COMMENT '失败/跳过原因',
  `send_time`     datetime     DEFAULT NULL COMMENT '发送时间',
  `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_biz` (`business_type`, `business_id`),
  KEY `idx_receiver` (`receiver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息渠道发送留痕（S6）';

-- 2) 帮扶任务表 + 跟踪记录表
CREATE TABLE IF NOT EXISTS `sam_warning_assist` (
  `assist_id`        bigint       NOT NULL AUTO_INCREMENT COMMENT '帮扶任务ID',
  `warning_id`       bigint       NOT NULL COMMENT '关联预警ID（sam_warning）',
  `student_id`       bigint       DEFAULT NULL COMMENT '学生ID',
  `semester_id`      bigint       DEFAULT NULL COMMENT '学期ID',
  `student_name`     varchar(50)  DEFAULT NULL COMMENT '学生姓名（快照）',
  `student_no`       varchar(30)  DEFAULT NULL COMMENT '学号（快照）',
  `warning_type`     char(1)      DEFAULT NULL COMMENT '预警类型（0成绩 1学分 2出勤 3综合）',
  `warning_level`    char(1)      DEFAULT NULL COMMENT '预警级别（0一般 1严重 2高危）',
  `helper_user_id`   bigint       DEFAULT NULL COMMENT '帮扶人用户ID',
  `helper_name`      varchar(50)  DEFAULT NULL COMMENT '帮扶人姓名',
  `assist_status`    char(1)      NOT NULL DEFAULT '0' COMMENT '帮扶状态（0待认领 1帮扶中 2已完成 3已关闭）',
  `measure`          varchar(500) DEFAULT NULL COMMENT '帮扶措施',
  `follow_count`     int          DEFAULT 0 COMMENT '跟踪次数',
  `last_follow_time` datetime     DEFAULT NULL COMMENT '最近跟踪时间',
  `claim_time`       datetime     DEFAULT NULL COMMENT '认领时间',
  `finish_time`      datetime     DEFAULT NULL COMMENT '完成/关闭时间',
  `finish_remark`    varchar(500) DEFAULT NULL COMMENT '完结/关闭说明',
  `remark`           varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by`        varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`      datetime     DEFAULT NULL COMMENT '创建时间',
  `update_by`        varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`      datetime     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`assist_id`),
  UNIQUE KEY `uk_warning` (`warning_id`),
  KEY `idx_helper_status` (`helper_user_id`, `assist_status`),
  KEY `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学业预警帮扶任务（S6）';

CREATE TABLE IF NOT EXISTS `sam_warning_assist_record` (
  `record_id`      bigint       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `assist_id`      bigint       NOT NULL COMMENT '帮扶任务ID',
  `follow_type`    char(1)      DEFAULT '0' COMMENT '跟踪方式（0面谈 1电话 2线上 3家访）',
  `follow_content` varchar(1000) DEFAULT NULL COMMENT '跟踪内容',
  `follow_time`    datetime     DEFAULT NULL COMMENT '跟踪时间',
  `operator_id`    bigint       DEFAULT NULL COMMENT '登记人用户ID',
  `operator_name`  varchar(50)  DEFAULT NULL COMMENT '登记人姓名',
  `create_by`      varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`    datetime     DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_assist` (`assist_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学业预警帮扶跟踪记录（S6）';

-- 3) 字典：帮扶状态
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '预警帮扶状态', 'sam_assist_status', '0', 'admin', sysdate(), 'S6 帮扶任务状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'sam_assist_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '待认领', '0', 'sam_assist_status', '', 'info', 'Y', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_assist_status' AND dict_value = '0');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '帮扶中', '1', 'sam_assist_status', '', 'warning', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_assist_status' AND dict_value = '1');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已完成', '2', 'sam_assist_status', '', 'success', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_assist_status' AND dict_value = '2');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '已关闭', '3', 'sam_assist_status', '', 'danger', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_assist_status' AND dict_value = '3');

-- 字典：跟踪方式
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '帮扶跟踪方式', 'sam_follow_type', '0', 'admin', sysdate(), 'S6 帮扶跟踪方式'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'sam_follow_type');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '面谈', '0', 'sam_follow_type', '', '', 'Y', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_follow_type' AND dict_value = '0');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '电话', '1', 'sam_follow_type', '', '', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_follow_type' AND dict_value = '1');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '线上', '2', 'sam_follow_type', '', '', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_follow_type' AND dict_value = '2');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '家访', '3', 'sam_follow_type', '', '', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_follow_type' AND dict_value = '3');

-- 4) 菜单：预警帮扶（parent=2301 学籍管理）
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3080,'预警帮扶',2301,5,'warningAssist','sam/warningAssist/index','','',1,0,'C','0','0','sam:warningAssist:list','peoples','admin',sysdate(),'',NULL,'S6 学业预警帮扶闭环'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3080);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3081,'帮扶查询',3080,1,'','','','',1,0,'F','0','0','sam:warningAssist:query','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3081);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3082,'帮扶派发',3080,2,'','','','',1,0,'F','0','0','sam:warningAssist:dispatch','#','admin',sysdate(),'',NULL,'手动派发帮扶任务'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3082);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3083,'帮扶认领',3080,3,'','','','',1,0,'F','0','0','sam:warningAssist:claim','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3083);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3084,'帮扶跟踪',3080,4,'','','','',1,0,'F','0','0','sam:warningAssist:follow','#','admin',sysdate(),'',NULL,'登记帮扶跟踪记录'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3084);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3085,'帮扶完结',3080,5,'','','','',1,0,'F','0','0','sam:warningAssist:finish','#','admin',sysdate(),'',NULL,'完结/关闭帮扶任务'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3085);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3086,'帮扶删除',3080,6,'','','','',1,0,'F','0','0','sam:warningAssist:remove','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3086);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3087,'帮扶导出',3080,7,'','','','',1,0,'F','0','0','sam:warningAssist:export','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3087);

-- 授权：超级管理员(1)
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,3080 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=3080);
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,3081 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=3081);
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,3082 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=3082);
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,3083 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=3083);
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,3084 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=3084);
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,3085 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=3085);
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,3086 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=3086);
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,3087 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=3087);

-- 5) 系统参数
-- 5.1 多渠道通知开关与 SMTP（默认关闭，配置后启用；站内信恒发不受开关影响）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '通知-邮件渠道开关', 'sys.notify.mail.enabled', 'false', 'Y', 'admin', sysdate(), 'true开启邮件通知（需配置下方SMTP参数）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.notify.mail.enabled');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '通知-邮件SMTP主机', 'sys.notify.mail.host', '', 'Y', 'admin', sysdate(), '如 smtp.qq.com，为空则邮件渠道跳过'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.notify.mail.host');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '通知-邮件SMTP端口', 'sys.notify.mail.port', '465', 'Y', 'admin', sysdate(), 'SSL一般465，非SSL一般25/587'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.notify.mail.port');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '通知-邮件账号', 'sys.notify.mail.username', '', 'Y', 'admin', sysdate(), 'SMTP登录名（通常即发件邮箱）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.notify.mail.username');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '通知-邮件密码/授权码', 'sys.notify.mail.password', '', 'Y', 'admin', sysdate(), 'SMTP密码或邮箱授权码'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.notify.mail.password');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '通知-邮件发件人', 'sys.notify.mail.from', '', 'Y', 'admin', sysdate(), '为空时取邮件账号'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.notify.mail.from');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '通知-邮件SSL开关', 'sys.notify.mail.ssl', 'true', 'Y', 'admin', sysdate(), 'true启用SSL（465端口）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.notify.mail.ssl');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '通知-短信渠道开关', 'sys.notify.sms.enabled', 'false', 'Y', 'admin', sysdate(), 'true开启短信渠道（默认实现仅留痕，学校短信网关待对接）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.notify.sms.enabled');

-- 5.2 帮扶自动派发配置
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '预警-帮扶自动派发开关', 'sam.warning.assist.auto', 'true', 'Y', 'admin', sysdate(), 'true时预警生成后按级别自动派发帮扶任务'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sam.warning.assist.auto');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '预警-帮扶自动派发最低级别', 'sam.warning.assist.minLevel', '1', 'Y', 'admin', sysdate(), '达到该级别(1严重 2高危 0一般)才自动派发'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sam.warning.assist.minLevel');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '预警-帮扶默认帮扶人ID', 'sam.warning.assist.helperUserId', '1', 'Y', 'admin', sysdate(), '自动派发时的默认帮扶人用户ID'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sam.warning.assist.helperUserId');
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '预警-帮扶默认帮扶人姓名', 'sam.warning.assist.helperName', '管理员', 'Y', 'admin', sysdate(), '自动派发时的默认帮扶人姓名'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sam.warning.assist.helperName');

-- 验证
SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 3080 AND 3087;
SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()
  AND table_name IN ('sys_notify_log','sam_warning_assist','sam_warning_assist_record');
