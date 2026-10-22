-- =====================================================================================
-- V20261021.2 —— F3 OBE 持续改进台账：按钮权限（挂在 3250 达成度看板下）
-- 内容：1) sys_menu 增加 3252~3257 六个 F 型按钮（sam:obeImprovement:*）
--       2) 授权给当前已持有 3250 达成度看板的全部角色（动态跟随，不写死角色号）
-- 影响表：sys_menu、sys_role_menu
-- 幂等性：是（WHERE NOT EXISTS）
-- 回滚：db/rollback/V20261021_2__add_obe_improvement_menu.sql
-- =====================================================================================

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3252,'台账列表',3250,2,'','','','',1,0,'F','0','0','sam:obeImprovement:list','#','admin',sysdate(),'',NULL,'F3 持续改进台账'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3252);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3253,'台账查询',3250,3,'','','','',1,0,'F','0','0','sam:obeImprovement:query','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3253);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3254,'台账新增',3250,4,'','','','',1,0,'F','0','0','sam:obeImprovement:add','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3254);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3255,'台账修改',3250,5,'','','','',1,0,'F','0','0','sam:obeImprovement:edit','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3255);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3256,'台账删除',3250,6,'','','','',1,0,'F','0','0','sam:obeImprovement:remove','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3256);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3257,'台账导出',3250,7,'','','','',1,0,'F','0','0','sam:obeImprovement:export','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3257);

-- 授权：跟随已持有 3250（达成度看板）的角色
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT rm.role_id, m.menu_id
FROM (SELECT DISTINCT role_id FROM `sys_role_menu` WHERE menu_id = 3250) rm
CROSS JOIN (SELECT 3252 AS menu_id UNION ALL SELECT 3253 UNION ALL SELECT 3254
            UNION ALL SELECT 3255 UNION ALL SELECT 3256 UNION ALL SELECT 3257) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id`=rm.role_id AND x.`menu_id`=m.menu_id);

-- 兜底：超级管理员(1) 始终可用
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1, m.menu_id
FROM (SELECT 3252 AS menu_id UNION ALL SELECT 3253 UNION ALL SELECT 3254
      UNION ALL SELECT 3255 UNION ALL SELECT 3256 UNION ALL SELECT 3257) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=1 AND rm.`menu_id`=m.menu_id);

-- 验证
SELECT menu_id, menu_name, menu_type, perms FROM sys_menu WHERE menu_id BETWEEN 3252 AND 3257 ORDER BY menu_id;
