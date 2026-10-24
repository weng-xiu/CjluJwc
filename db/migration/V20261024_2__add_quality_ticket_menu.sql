-- =====================================================================================
-- V20261024.2 —— F5 质量看板异常工单化：菜单与按钮权限
-- 内容：1) sys_menu 增加 2447 C 型页面「数据质量工单」（挂 2400 数据对接目录下，order 8）
--          + 2448~2453 六个 F 型按钮（dis:qualityTicket:*，含异常检测 generate）
--       2) 授权给当前已持有 2404 数据同步任务的角色（动态跟随）+ 超级管理员(1) 兜底
-- 影响表：sys_menu、sys_role_menu
-- 幂等性：是（WHERE NOT EXISTS）
-- 回滚：db/rollback/V20261024_2__add_quality_ticket_menu.sql
-- =====================================================================================

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2447,'数据质量工单',2400,8,'qualityTicket','dis/qualityTicket/index','','',1,0,'C','0','0','dis:qualityTicket:list','bug','admin',sysdate(),'',NULL,'F5 质量看板异常工单化：异常检测/处置/闭环'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2447);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2448,'工单查询',2447,1,'','','','',1,0,'F','0','0','dis:qualityTicket:query','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2448);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2449,'工单登记',2447,2,'','','','',1,0,'F','0','0','dis:qualityTicket:add','#','admin',sysdate(),'',NULL,'人工登记异常工单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2449);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2450,'异常检测',2447,3,'','','','',1,0,'F','0','0','dis:qualityTicket:generate','#','admin',sysdate(),'',NULL,'扫描同步任务失败率超标自动生成工单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2450);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2451,'工单处置',2447,4,'','','','',1,0,'F','0','0','dis:qualityTicket:edit','#','admin',sysdate(),'',NULL,'指派责任人/推进状态/回填处置结论'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2451);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2452,'工单删除',2447,5,'','','','',1,0,'F','0','0','dis:qualityTicket:remove','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2452);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2453,'工单导出',2447,6,'','','','',1,0,'F','0','0','dis:qualityTicket:export','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2453);

-- 授权：跟随已持有 2404（数据同步任务）的角色
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT rm.role_id, m.menu_id
FROM (SELECT DISTINCT role_id FROM `sys_role_menu` WHERE menu_id = 2404) rm
CROSS JOIN (SELECT 2447 AS menu_id UNION ALL SELECT 2448 UNION ALL SELECT 2449
            UNION ALL SELECT 2450 UNION ALL SELECT 2451 UNION ALL SELECT 2452 UNION ALL SELECT 2453) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id`=rm.role_id AND x.`menu_id`=m.menu_id);

-- 兜底：超级管理员(1) 始终可用
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1, m.menu_id
FROM (SELECT 2447 AS menu_id UNION ALL SELECT 2448 UNION ALL SELECT 2449
      UNION ALL SELECT 2450 UNION ALL SELECT 2451 UNION ALL SELECT 2452 UNION ALL SELECT 2453) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=1 AND rm.`menu_id`=m.menu_id);

-- 验证
SELECT menu_id, menu_name, menu_type, perms FROM sys_menu WHERE menu_id BETWEEN 2447 AND 2453 ORDER BY menu_id;
