-- =====================================================================================
-- V20261101.1 —— S-5 门户调停课审批角色化：新增按钮权限 portal:adjustment:approve
-- 内容：1) sys_menu 增加 3500 一个 F 型按钮（挂在 2515 调停课申请菜单下）
--       2) 授权给当前已持有 2515 的全部角色（动态跟随，不写死角色号）
--       3) 兜底超级管理员(1)
-- 影响表：sys_menu、sys_role_menu
-- 幂等性：是（WHERE NOT EXISTS）
-- 回滚：db/rollback/V20261101_1__add_portal_adjustment_approve_menu.sql
-- 说明：审批资格判定见 PortalAdjustmentApproverPolicy（admin / 本权限字符 / tpm.adjust.approver(s) 名单）
-- =====================================================================================

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3500,'调停课审批',2515,3,'','','','',1,0,'F','0','0','portal:adjustment:approve','#','admin',sysdate(),'',NULL,'S-5 门户审批角色化'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3500);

-- 授权：跟随已持有 2515（调停课申请）的角色，避免新增权限后原有审批人 403
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT rm.role_id, 3500
FROM (SELECT DISTINCT role_id FROM `sys_role_menu` WHERE menu_id = 2515) rm
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id`=rm.role_id AND x.`menu_id`=3500);

-- 兜底：超级管理员(1) 始终可用
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1, 3500 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=1 AND `menu_id`=3500);

-- 验证
SELECT menu_id, menu_name, menu_type, perms FROM sys_menu WHERE menu_id = 3500;
