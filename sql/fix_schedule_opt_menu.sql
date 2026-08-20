-- 修复测试中发现的菜单缺失问题：
-- 1. scheduleOpt 菜单(2620-2625)未导入数据库，导致前端访问 /tpm/schedule-group/scheduleOpt 404
-- 2. graduationProcedure 菜单需要确认是否存在（截图虽然 200，但实际可能也是默认 404 组件）
USE `yu-CjluJwc`;

-- 排课优化菜单（如果已存在则跳过）
INSERT IGNORE INTO sys_menu VALUES('2620', '排课优化', '2105', '4', 'scheduleOpt', 'tpm/scheduleOpt/index', '', '', 1, 0, 'C', '0', '0', 'tpm:scheduleOpt:list', 'tool', 'admin', sysdate(), '', null, '');
INSERT IGNORE INTO sys_menu VALUES('2621', '优化查询', '2620', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:scheduleOpt:query', '#', 'admin', sysdate(), '', null, '');
INSERT IGNORE INTO sys_menu VALUES('2622', '优化执行', '2620', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:scheduleOpt:execute', '#', 'admin', sysdate(), '', null, '');
INSERT IGNORE INTO sys_menu VALUES('2623', '优化修改', '2620', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:scheduleOpt:edit', '#', 'admin', sysdate(), '', null, '');
INSERT IGNORE INTO sys_menu VALUES('2624', '优化删除', '2620', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:scheduleOpt:remove', '#', 'admin', sysdate(), '', null, '');
INSERT IGNORE INTO sys_menu VALUES('2625', '优化导出', '2620', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:scheduleOpt:export', '#', 'admin', sysdate(), '', null, '');

-- 验证
SELECT menu_id, menu_name, path, component FROM sys_menu WHERE menu_id BETWEEN 2620 AND 2625;
