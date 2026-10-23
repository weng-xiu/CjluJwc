-- =====================================================================================
-- V20261022.2 —— F5 主数据裁决前端化：菜单与按钮权限
-- 内容：1) sys_menu 增加 2441 C 型页面「主数据裁决矩阵」（挂 2400 数据对接目录下）
--          + 2442~2446 五个 F 型按钮（dis:priority:*）
--       2) 授权给当前已持有 2401 外部系统管理的角色（动态跟随）+ 超级管理员(1) 兜底
-- 影响表：sys_menu、sys_role_menu
-- 幂等性：是（WHERE NOT EXISTS）
-- 回滚：db/rollback/V20261022_2__add_arbitration_menu.sql
-- =====================================================================================

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2441,'主数据裁决矩阵',2400,7,'arbitration','dis/arbitration/index','','',1,0,'C','0','0','dis:priority:list','tree','admin',sysdate(),'',NULL,'F5 主数据裁决优先级配置与试裁决'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2441);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2442,'裁决查询',2441,1,'','','','',1,0,'F','0','0','dis:priority:query','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2442);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2443,'裁决新增',2441,2,'','','','',1,0,'F','0','0','dis:priority:add','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2443);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2444,'裁决修改',2441,3,'','','','',1,0,'F','0','0','dis:priority:edit','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2444);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2445,'裁决删除',2441,4,'','','','',1,0,'F','0','0','dis:priority:remove','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2445);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2446,'裁决导出',2441,5,'','','','',1,0,'F','0','0','dis:priority:export','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2446);

-- 授权：跟随已持有 2401（外部系统管理）的角色
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT rm.role_id, m.menu_id
FROM (SELECT DISTINCT role_id FROM `sys_role_menu` WHERE menu_id = 2401) rm
CROSS JOIN (SELECT 2441 AS menu_id UNION ALL SELECT 2442 UNION ALL SELECT 2443
            UNION ALL SELECT 2444 UNION ALL SELECT 2445 UNION ALL SELECT 2446) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id`=rm.role_id AND x.`menu_id`=m.menu_id);

-- 兜底：超级管理员(1) 始终可用
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1, m.menu_id
FROM (SELECT 2441 AS menu_id UNION ALL SELECT 2442 UNION ALL SELECT 2443
      UNION ALL SELECT 2444 UNION ALL SELECT 2445 UNION ALL SELECT 2446) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=1 AND rm.`menu_id`=m.menu_id);

-- 验证
SELECT menu_id, menu_name, menu_type, perms FROM sys_menu WHERE menu_id BETWEEN 2441 AND 2446 ORDER BY menu_id;
