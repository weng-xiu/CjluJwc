-- =====================================================================================
-- 回滚：V20261007.3 —— 移除「数据分类分级台账」管理端菜单与其按钮权限及角色关联
-- 影响：sys_menu（删除 3400~3405）、sys_role_menu（删除对应关联）；
--       回滚后管理端「系统管理」下不再出现「数据分类分级」入口，
--       但 sys_data_class 表、三字典与种子数据、后端接口均不受影响（属 V20261007.2 范畴）。
-- 幂等性：是（DELETE 按主键，重复执行无副作用）
-- =====================================================================================

DELETE FROM sys_role_menu WHERE menu_id BETWEEN 3400 AND 3405;
DELETE FROM sys_menu       WHERE menu_id BETWEEN 3400 AND 3405;

-- 校验：应各返回 0 行
SELECT menu_id, menu_name FROM sys_menu WHERE menu_id BETWEEN 3400 AND 3405;
SELECT role_id, menu_id FROM sys_role_menu WHERE menu_id BETWEEN 3400 AND 3405;
