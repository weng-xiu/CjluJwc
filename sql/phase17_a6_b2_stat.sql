-- ===============================================
-- 第二期 P1 升级脚本：A6 评教统计与反馈 / B2 资源利用分析
-- 幂等：菜单用 INSERT ... WHERE NOT EXISTS；授权用 WHERE NOT EXISTS
-- 依赖：aem_evaluation_result / brm_classroom / brm_equipment(_maintenance) / tpm_schedule / tpm_course_offering 已存在
-- 菜单ID：现有最大为 2964，本脚本使用 2970+ 段避免冲突
-- 父节点：A6 挂 2209(教学质量评价)，B2 挂 2001(基础信息管理)
-- ===============================================

-- ----------------------------
-- 1. A6 评教统计（管理端菜单 + 按钮）
-- ----------------------------
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2970,'评教统计',2209,5,'evaluationStat','aem/evaluationStat/index','','',1,0,'C','0','0','aem:evaluationStat:list','chart','admin',sysdate(),'',NULL,'A6 评教多维统计与反馈分析'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2970);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2971,'统计查询',2970,1,'','','','',1,0,'F','0','0','aem:evaluationStat:query','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2971);

-- ----------------------------
-- 2. B2 资源利用分析（管理端菜单 + 按钮）
-- ----------------------------
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2972,'资源利用分析',2001,9,'resourceStat','brm/resourceStat/index','','',1,0,'C','0','0','brm:resourceStat:list','tree-table','admin',sysdate(),'',NULL,'B2 教室利用率/设备维保/教师工作量'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2972);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2973,'分析查询',2972,1,'','','','',1,0,'F','0','0','brm:resourceStat:query','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2973);

-- ----------------------------
-- 3. 授权：超级管理员(role_id=1) 获得本批全部新增菜单/按钮
-- ----------------------------
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,m.menu_id FROM (
  SELECT 2970 AS menu_id UNION ALL SELECT 2971 UNION ALL SELECT 2972 UNION ALL SELECT 2973
) m WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=1 AND rm.`menu_id`=m.menu_id);
