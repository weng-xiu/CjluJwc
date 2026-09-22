-- ===============================================
-- 第三期批次升级脚本：P7 基础数据导入（教师/教室/学籍）+ A7 成绩统计维度扩展
-- 幂等：菜单用 INSERT ... WHERE NOT EXISTS；授权用 WHERE NOT EXISTS
-- 依赖：brm_teacher / brm_classroom / sam_student / aem_grade_record 已存在（无表结构变更）
-- 菜单ID：phase16 用 2950-2964、phase17 用 2970-2973、phase18 用 2980-2983，本脚本使用 2990+ 段
-- 父节点：教师按钮挂 2011(教师管理)、教室按钮挂 2009(教室管理)、学籍按钮挂 2302(学生学籍)
-- A7 说明：新增 byClass/byTeacher/byMajor/trend 端点复用既有 aem:gradeStatistics:query 权限，无需新菜单
-- ===============================================

-- ----------------------------
-- 1. P7 导入按钮（brm 教师 / brm 教室 / sam 学籍）
-- ----------------------------
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2990,'教师导入',2011,6,'','','','',1,0,'F','0','0','brm:teacher:import','#','admin',sysdate(),'',NULL,'P7 教师 Excel 导入'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2990);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2991,'教室导入',2009,6,'','','','',1,0,'F','0','0','brm:classroom:import','#','admin',sysdate(),'',NULL,'P7 教室 Excel 导入'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2991);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2992,'学籍导入',2302,6,'','','','',1,0,'F','0','0','sam:student:import','#','admin',sysdate(),'',NULL,'P7 学籍 Excel 导入'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2992);

-- ----------------------------
-- 2. 授权：超级管理员(role_id=1) 获得本批全部新增按钮
-- ----------------------------
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,m.menu_id FROM (
  SELECT 2990 AS menu_id UNION ALL SELECT 2991 UNION ALL SELECT 2992
) m WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=1 AND rm.`menu_id`=m.menu_id);
