-- =====================================================================================
-- 版本：V20261004.2
-- 标题：登记「字段级数据变更流水」查询菜单与权限（K1 合规③前端闭环）
-- 目的：V20261004.1 已建 data_change_audit 表、后端 /monitor/dataAudit 只读接口与 Vue3 页
--       views/monitor/dataAudit/index 已就绪；本脚本登记对应 sys_menu 节点与权限点
--       monitor:dataAudit:list，使审计流水可被具备日志查看权限的角色核查，形成闭环。
--       该页为纯只读（流水由业务事务自动写入），故仅登记 1 个 C 型菜单 + list 权限，
--       不设 add/edit/remove/export 按钮权限（后端亦无相应端点，避免登记死权限）。
-- 做法：菜单挂在「系统管理 > 日志管理」(parent_id=108) 下，与操作日志(500)/登录日志(501) 同级；
--       menu_id 取 db/README.md 规定的 3000+ 新增变更保留段（3300，3000~3211 已被既有脚本占用）。
--       授权对象：所有已拥有「操作日志」(menu_id=500) 的角色（即既有审计/日志查看角色）+ admin(1)。
-- 影响表：sys_menu（新增至多 1 行）、sys_role_menu（新增若干关联行）
-- 关联事项：V4.1 §6.2 K1、§7.3
-- 幂等性：是（INSERT ... SELECT WHERE NOT EXISTS 按 menu_id 与 perms 双判重；role_menu 亦 NOT EXISTS 兜底）
-- 回滚脚本：db/rollback/V20261004_2__add_data_audit_menu.sql
-- 执行说明：本机存量库按既有约定「人工应用 + 带回滚脚本」，FLYWAY_ENABLED 默认 false；
--       新环境或发布流程注入 FLYWAY_ENABLED=true 时由 Flyway 自动重放。
-- =====================================================================================

-- 1. C 型页面菜单：数据变更审计（parent=108 日志管理）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3300, '数据变更审计', 108, 4, 'dataAudit', 'monitor/dataAudit/index', '', '', 1, 0, 'C', '0', '0', 'monitor:dataAudit:list', 'documentation', 'admin', NOW(), '', NULL, 'K1 字段级数据变更流水只读查询'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3300 OR perms = 'monitor:dataAudit:list');

-- 2. 角色菜单关联：授予所有已拥有「操作日志」(500) 的角色（复用既有审计查看授权面）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, 3300 FROM sys_role_menu rm
WHERE rm.menu_id = 500
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = 3300);

-- 3. 兜底：确保 admin(role_id=1) 一定拥有（admin 虽有超级短路，仍显式登记保持一致性）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 3300 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 3300);

-- 校验：应返回菜单 1 行 + 授权关联若干行
SELECT menu_id, menu_name, parent_id, path, component, perms FROM sys_menu WHERE menu_id = 3300;
SELECT rm.role_id, ro.role_key, rm.menu_id FROM sys_role_menu rm JOIN sys_role ro ON ro.role_id = rm.role_id WHERE rm.menu_id = 3300 ORDER BY rm.role_id;
