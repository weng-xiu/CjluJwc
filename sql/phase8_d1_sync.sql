-- ===============================================
-- D1 数据对接链路贯通 数据库升级脚本（幂等）
-- 内容：新增字段映射配置表 dis_field_mapping、字段映射管理菜单/权限、
--       为既有数据同步任务补齐"任务执行"权限校验点
-- 可重复执行：CREATE TABLE IF NOT EXISTS / INSERT ... WHERE NOT EXISTS
-- ===============================================

-- ----------------------------
-- 1. 字段映射配置表（解析—落库依据）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `dis_field_mapping` (
  `mapping_id`    bigint       NOT NULL AUTO_INCREMENT COMMENT '映射ID',
  `interface_id`  bigint       NOT NULL COMMENT '接口ID（关联 dis_interface_config）',
  `target_table`  varchar(64)  NOT NULL COMMENT '目标业务表名',
  `source_field`  varchar(128) NOT NULL COMMENT '源字段名（响应JSON键，支持 a.b.c 嵌套路径）',
  `target_column` varchar(64)  NOT NULL COMMENT '目标列名',
  `key_flag`      char(1)      DEFAULT '0' COMMENT '是否主键/唯一键（0否 1是）',
  `sort_order`    int          DEFAULT '0' COMMENT '排序',
  `status`        char(1)      DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `remark`        varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by`     varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
  `update_by`     varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`mapping_id`),
  KEY `idx_interface_id` (`interface_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据同步字段映射配置表';

-- ----------------------------
-- 2. 字段映射管理菜单 + 按钮权限（挂在数据对接目录 2400 下，parent 复用同级 2404 之后）
--    menu_id 采用 2430 段，避开 2404~2423
-- ----------------------------
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2430,'字段映射',2400,6,'fieldMapping','dis/fieldMapping/index','','',1,0,'C','0','0','dis:fieldMapping:list','tree','admin',sysdate(),'',NULL,'D1 字段映射配置'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2430);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2431,'映射查询',2430,1,'','','','',1,0,'F','0','0','dis:fieldMapping:query','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2431);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2432,'映射新增',2430,2,'','','','',1,0,'F','0','0','dis:fieldMapping:add','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2432);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2433,'映射修改',2430,3,'','','','',1,0,'F','0','0','dis:fieldMapping:edit','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2433);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2434,'映射删除',2430,4,'','','','',1,0,'F','0','0','dis:fieldMapping:remove','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2434);

-- 将新增菜单授权给超级管理员角色(role_id=1)与已有 dis 权限的角色可按需调整
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,2430 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=2430);
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,2431 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=2431);
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,2432 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=2432);
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,2433 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=2433);
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,2434 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=2434);
