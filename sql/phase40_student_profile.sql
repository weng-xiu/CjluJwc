-- =============================================================
-- Phase 40：N6 学生数字档案全息画像（跨模块只读聚合，无新表）
-- 覆盖：
--   1) 管理端「学生数字档案」菜单（只读画像，聚合 学籍/学业/培养方案/OBE达成度/预警/审核/论文/成绩）
--   2) 后端只读接口 GET /sam/studentProfile/{studentId}（perms sam:studentProfile:query）
-- 说明：本特性纯只读聚合，不新增业务表；依赖 Phase 39 的 OBE 达成度数据。
-- 菜单ID：现有最大为 3251（phase39），本脚本使用 3260—3263 段。
-- 本脚本幂等，可重复执行。
-- =============================================================

-- ----------------------------
-- 1. 管理端菜单：学生数字档案（置于学籍管理目录 2301 下）
-- ----------------------------
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3260,'学生数字档案',2301,6,'studentProfile','sam/studentProfile/index','','',1,0,'C','0','0','sam:studentProfile:list','user','admin',sysdate(),'',NULL,'N6 学生全息数字档案只读画像'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3260);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3261,'档案查询',3260,1,'','','','',1,0,'F','0','0','sam:studentProfile:query','#','admin',sysdate(),'',NULL,'按学生聚合各模块数据'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3261);

-- ----------------------------
-- 2. 角色授权：超级管理员(1)、教务处管理员(3)、学院管理员(4)、教学秘书(5)
-- ----------------------------
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT r.role_id, m.menu_id FROM
  (SELECT 1 AS role_id UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5) r
  CROSS JOIN (SELECT 3260 AS menu_id UNION ALL SELECT 3261) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=r.role_id AND rm.`menu_id`=m.menu_id);

-- ----------------------------
-- 验证
-- ----------------------------
SELECT menu_id, menu_name, menu_type, perms FROM sys_menu WHERE menu_id BETWEEN 3260 AND 3263 ORDER BY menu_id;
