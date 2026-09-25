-- ===============================================
-- 第三期 P2 升级脚本：P3 主题分析与上报
-- 幂等：菜单/授权均用 INSERT ... SELECT ... WHERE NOT EXISTS
-- 依赖：sam_student / aem_grade_record / brm_teacher / tpm_course_offering / brm_department 已存在
-- 菜单ID：现有最大为 3060，本脚本使用 3070+ 段避免冲突
-- 父节点：挂 1(系统管理)，后端 SysSubjectStatController 路径 /system/subjectStat
-- ===============================================

-- ----------------------------
-- 1. P3 主题分析（管理端菜单 + 按钮）
-- ----------------------------
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3070,'主题分析',1,11,'subjectStat','system/subjectStat/index','','',1,0,'C','0','0','system:subjectStat:list','chart','admin',sysdate(),'',NULL,'P3 学生结构/成绩分析/师资分析主题看板'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3070);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3071,'分析查询',3070,1,'','','','',1,0,'F','0','0','system:subjectStat:query','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3071);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3072,'上报导出',3070,2,'','','','',1,0,'F','0','0','system:subjectStat:export','#','admin',sysdate(),'',NULL,'P3 师生数据上报Excel导出'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3072);

-- ----------------------------
-- 2. 授权：超级管理员(role_id=1) 获得本批全部新增菜单/按钮
-- ----------------------------
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,m.menu_id FROM (
  SELECT 3070 AS menu_id UNION ALL SELECT 3071 UNION ALL SELECT 3072
) m WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=1 AND rm.`menu_id`=m.menu_id);
