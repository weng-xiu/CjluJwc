-- =====================================================================================
-- V20261024.3 —— F3 专业质量档案一页纸：菜单与按钮权限
-- 内容：1) sys_menu 增加 3280 C 型页面「专业质量档案」（挂 3220 OBE达成度管理目录下，order 5）
--          + 3281/3282 查询、导出 F 型按钮（sam:programQuality:*）
--       2) 授权给当前已持有 3250 达成度看板的角色（动态跟随）+ 超级管理员(1) 兜底
-- 说明：专业质量档案为只读跨模块聚合（师资/实践/评教/督导/OBE达成度），无新表
-- 影响表：sys_menu、sys_role_menu
-- 幂等性：是（WHERE NOT EXISTS）
-- 回滚：db/rollback/V20261024_3__add_program_quality_menu.sql
-- =====================================================================================

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3280,'专业质量档案',3220,5,'programQuality','sam/programQuality/index','','',1,0,'C','0','0','sam:programQuality:list','documentation','admin',sysdate(),'',NULL,'F3 专业质量档案一页纸：师资/实践/评教/督导/达成度跨模块聚合'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3280);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3281,'档案查询',3280,1,'','','','',1,0,'F','0','0','sam:programQuality:query','#','admin',sysdate(),'',NULL,'按专业聚合各模块质量指标'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3281);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3282,'档案导出',3280,2,'','','','',1,0,'F','0','0','sam:programQuality:export','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3282);

-- 授权：跟随已持有 3250（达成度看板）的角色
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT rm.role_id, m.menu_id
FROM (SELECT DISTINCT role_id FROM `sys_role_menu` WHERE menu_id = 3250) rm
CROSS JOIN (SELECT 3280 AS menu_id UNION ALL SELECT 3281 UNION ALL SELECT 3282) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id`=rm.role_id AND x.`menu_id`=m.menu_id);

-- 兜底：超级管理员(1) 始终可用
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1, m.menu_id
FROM (SELECT 3280 AS menu_id UNION ALL SELECT 3281 UNION ALL SELECT 3282) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=1 AND rm.`menu_id`=m.menu_id);

-- 验证
SELECT menu_id, menu_name, menu_type, perms FROM sys_menu WHERE menu_id BETWEEN 3280 AND 3282 ORDER BY menu_id;
