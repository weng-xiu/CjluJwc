-- =====================================================================================
-- 回滚：V20261004.2 —— 撤销「数据变更审计」查询菜单与授权
-- 对应变更：db/migration/V20261004_2__add_data_audit_menu.sql
-- 逆向操作：先删 sys_role_menu 中 menu_id=3300 的关联行，再删 sys_menu 中 menu_id=3300 的菜单行
-- 影响表：sys_role_menu（删除本变更新增的关联行）、sys_menu（删除 1 行）
-- 回滚后果：管理端「系统管理 > 日志管理」下不再出现「数据变更审计」入口；data_change_audit 表与
--       后端 /monitor/dataAudit 接口不受影响（本脚本仅移除前端可见性与授权，不动数据/接口）。
-- 幂等性：是（DELETE 命中 0 行不报错）
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261004.2';
--            否则 Flyway 仍认为该版本已应用，下次 migrate 不会重放。
-- =====================================================================================

DELETE FROM sys_role_menu WHERE menu_id = 3300;
DELETE FROM sys_menu      WHERE menu_id = 3300 AND perms = 'monitor:dataAudit:list';

-- 校验：两表均应返回 0 行
SELECT COUNT(*) AS menu_cnt   FROM sys_menu      WHERE menu_id = 3300;
SELECT COUNT(*) AS rolemenu_cnt FROM sys_role_menu WHERE menu_id = 3300;
