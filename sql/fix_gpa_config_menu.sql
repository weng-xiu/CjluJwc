-- 修复 GPA 算法配置页面 404
USE `yu-CjluJwc`;

-- 父级目录
INSERT INTO sys_menu VALUES('2900', 'GPA算法', '2200', '4', 'gpa-group', NULL, '', '', 1, 0, 'M', '0', '0', '', 'example', 'admin', sysdate(), '', null, 'GPA算法目录');

-- 具体页面
INSERT INTO sys_menu VALUES('2901', 'GPA算法配置', '2900', '1', 'gpaConfig', 'aem/gpaConfig/index', '', '', 1, 0, 'C', '0', '0', 'aem:gpaConfig:list', 'edit', 'admin', sysdate(), '', null, 'GPA算法配置菜单');

-- 按钮权限
INSERT INTO sys_menu VALUES('2902', 'GPA配置查询', '2901', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gpaConfig:query',  '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2903', 'GPA配置新增', '2901', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gpaConfig:add',    '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2904', 'GPA配置修改', '2901', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gpaConfig:edit',   '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2905', 'GPA配置删除', '2901', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gpaConfig:remove', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2906', 'GPA配置导出', '2901', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gpaConfig:export', '#', 'admin', sysdate(), '', null, '');

SELECT menu_id, menu_name, parent_id, path, component FROM sys_menu WHERE menu_id BETWEEN 2900 AND 2906;
