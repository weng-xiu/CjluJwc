-- =====================================================================================
-- V20261023.2 —— F4 学生数字档案与干预闭环：按钮权限（挂在 3260 学生数字档案下）
-- 内容：1) sys_menu 增加 3262~3268 七个 F 型按钮
--         3262 档案导出      sam:studentProfile:export
--         3263 干预列表      sam:intervention:list
--         3264 干预查询      sam:intervention:query
--         3265 干预新增      sam:intervention:add
--         3266 干预修改      sam:intervention:edit
--         3267 干预删除      sam:intervention:remove
--         3268 干预导出      sam:intervention:export
--       2) 授权给当前已持有 3260 学生数字档案的全部角色（动态跟随，不写死角色号）
-- 影响表：sys_menu、sys_role_menu
-- 幂等性：是（WHERE NOT EXISTS）
-- 回滚：db/rollback/V20261023_2__add_student_intervention_menu.sql
-- =====================================================================================

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3262,'档案导出',3260,2,'','','','',1,0,'F','0','0','sam:studentProfile:export','#','admin',sysdate(),'',NULL,'F4 全息档案 Excel 导出'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3262);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3263,'干预列表',3260,3,'','','','',1,0,'F','0','0','sam:intervention:list','#','admin',sysdate(),'',NULL,'F4 干预闭环台账'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3263);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3264,'干预查询',3260,4,'','','','',1,0,'F','0','0','sam:intervention:query','#','admin',sysdate(),'',NULL,'含效果预览'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3264);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3265,'干预新增',3260,5,'','','','',1,0,'F','0','0','sam:intervention:add','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3265);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3266,'干预修改',3260,6,'','','','',1,0,'F','0','0','sam:intervention:edit','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3266);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3267,'干预删除',3260,7,'','','','',1,0,'F','0','0','sam:intervention:remove','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3267);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3268,'干预导出',3260,8,'','','','',1,0,'F','0','0','sam:intervention:export','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3268);

-- 授权：跟随已持有 3260（学生数字档案）的角色
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT rm.role_id, m.menu_id
FROM (SELECT DISTINCT role_id FROM `sys_role_menu` WHERE menu_id = 3260) rm
CROSS JOIN (SELECT 3262 AS menu_id UNION ALL SELECT 3263 UNION ALL SELECT 3264
            UNION ALL SELECT 3265 UNION ALL SELECT 3266 UNION ALL SELECT 3267
            UNION ALL SELECT 3268) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id`=rm.role_id AND x.`menu_id`=m.menu_id);

-- 兜底：超级管理员(1) 始终可用
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1, m.menu_id
FROM (SELECT 3262 AS menu_id UNION ALL SELECT 3263 UNION ALL SELECT 3264
      UNION ALL SELECT 3265 UNION ALL SELECT 3266 UNION ALL SELECT 3267
      UNION ALL SELECT 3268) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=1 AND rm.`menu_id`=m.menu_id);

-- 验证
SELECT menu_id, menu_name, menu_type, perms FROM sys_menu WHERE menu_id BETWEEN 3262 AND 3268 ORDER BY menu_id;
