-- ===============================================
-- S4 学生自助毕业预审 数据库升级脚本（幂等）
-- 内容：新增学生门户「毕业预审」菜单（perms=portal:graduation:list）及查询按钮，
--       授权学生角色(role_id=7)与超级管理员(role_id=1)
-- 说明：门户前端 yu-portal-ui 路由为静态注册，本菜单主要提供权限点，
--       供「角色管理→菜单权限」分配及后端 @PreAuthorize 校验
-- 幂等：INSERT ... WHERE NOT EXISTS
-- menu_id 采用 2543 段（2541/2542 已被门户预警占用，2550+ 为学生账号管理）
-- ===============================================

-- 毕业预审菜单（parent=2501 学生服务）
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2543,'毕业预审',2501,8,'graduation','portal/graduation/index','','',1,0,'C','0','0','portal:graduation:list','checkbox','admin',sysdate(),'',NULL,'S4 学生自助毕业预审'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2543);

-- 毕业预审查询按钮
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2544,'预审查询',2543,1,'','','','',1,0,'F','0','0','portal:graduation:query','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2544);

-- 授权：学生角色(7)
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 7,2543 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=7 AND `menu_id`=2543);
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 7,2544 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=7 AND `menu_id`=2544);

-- 授权：超级管理员(1)
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,2543 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=2543);
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,2544 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=2544);
