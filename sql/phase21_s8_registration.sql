-- ===============================================
-- phase21：S8 学期注册与报到管理 数据库升级脚本（幂等）
-- 内容：
--   1) 新建 sam_registration（学期注册记录）表
--   2) 新增字典 sam_register_status（0未注册 1已注册 2延迟注册）
--   3) 新增管理端「学期注册」菜单（parent=2301 学籍管理）及按钮，授权超级管理员
-- 说明：P7 已交付学籍 Excel 导入（sam:student:import），本脚本补齐 S8 的"学期注册/报到 + 注册率统计"缺口
-- 幂等：CREATE TABLE IF NOT EXISTS + INSERT ... WHERE NOT EXISTS
-- menu_id 采用 3010 段（3000 已被 phase20 占用）
-- ===============================================

-- 1) 学期注册记录表
CREATE TABLE IF NOT EXISTS `sam_registration` (
  `registration_id`  bigint       NOT NULL AUTO_INCREMENT COMMENT '注册记录ID',
  `student_id`       bigint       NOT NULL COMMENT '学生ID（关联 sam_student）',
  `semester_id`      bigint       NOT NULL COMMENT '学期ID（关联 brm_semester）',
  `register_status`  char(1)      NOT NULL DEFAULT '0' COMMENT '注册状态（0未注册 1已注册 2延迟注册）',
  `register_time`    datetime     DEFAULT NULL COMMENT '报到注册时间',
  `register_by`      varchar(64)  DEFAULT NULL COMMENT '注册经办人（管理员账号或学生本人）',
  `channel`          char(1)      DEFAULT '0' COMMENT '注册渠道（0管理端代办 1学生自助）',
  `defer_reason`     varchar(500) DEFAULT NULL COMMENT '延迟/未注册原因',
  `remark`           varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by`        varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`      datetime     DEFAULT NULL COMMENT '创建时间',
  `update_by`        varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`      datetime     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`registration_id`),
  UNIQUE KEY `uk_student_semester` (`student_id`, `semester_id`),
  KEY `idx_semester_status` (`semester_id`, `register_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学期注册记录（S8）';

-- 2) 字典：sam_register_status
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '学期注册状态', 'sam_register_status', '0', 'admin', sysdate(), 'S8 学期注册/报到状态列表'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'sam_register_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '未注册', '0', 'sam_register_status', '', 'danger', 'Y', '0', 'admin', sysdate(), '本学期尚未注册'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_register_status' AND dict_value = '0');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '已注册', '1', 'sam_register_status', '', 'success', 'N', '0', 'admin', sysdate(), '已完成学期报到注册'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_register_status' AND dict_value = '1');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '延迟注册', '2', 'sam_register_status', '', 'warning', 'N', '0', 'admin', sysdate(), '因故延迟注册（保留学籍）'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_register_status' AND dict_value = '2');

-- 3) 菜单：学期注册（parent=2301 学籍管理）
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3010,'学期注册',2301,4,'registration','sam/registration/index','','',1,0,'C','0','0','sam:registration:list','form','admin',sysdate(),'',NULL,'S8 学期注册与报到管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3010);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3011,'注册查询',3010,1,'','','','',1,0,'F','0','0','sam:registration:query','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3011);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3012,'报到初始化',3010,2,'','','','',1,0,'F','0','0','sam:registration:init','#','admin',sysdate(),'',NULL,'按学期为在读学生批量生成注册记录'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3012);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3013,'注册办理',3010,3,'','','','',1,0,'F','0','0','sam:registration:register','#','admin',sysdate(),'',NULL,'单条/批量报到注册或标记延迟'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3013);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3014,'注册导出',3010,4,'','','','',1,0,'F','0','0','sam:registration:export','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3014);

-- 授权：超级管理员(1)
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,3010 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=3010);
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,3011 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=3011);
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,3012 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=3012);
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,3013 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=3013);
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,3014 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=3014);

-- 验证
SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 3010 AND 3014;
