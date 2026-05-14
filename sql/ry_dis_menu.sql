-- ----------------------------
-- 数据对接与共享(dis)菜单SQL
-- ----------------------------
-- 一级菜单 - 数据对接与共享目录
insert into sys_menu values('2400', '数据对接与共享', '0', '9', 'dis',              null, '', '', 1, 0, 'M', '0', '0', '',              'link',       'admin', sysdate(), '', null, '');

-- 二级菜单
insert into sys_menu values('2401', '外部系统管理', '2400', '1', 'system',          'dis/system/index',        '', '', 1, 0, 'C', '0', '0', 'dis:system:list',        'server',     'admin', sysdate(), '', null, '');
insert into sys_menu values('2402', '接口配置管理', '2400', '2', 'interface',       'dis/interface/index',     '', '', 1, 0, 'C', '0', '0', 'dis:interface:list',     'api',        'admin', sysdate(), '', null, '');
insert into sys_menu values('2403', '数据交换日志', '2400', '3', 'exchangeLog',     'dis/exchangeLog/index',   '', '', 1, 0, 'C', '0', '0', 'dis:exchangeLog:list',   'log',        'admin', sysdate(), '', null, '');
insert into sys_menu values('2404', '数据同步任务', '2400', '4', 'syncTask',        'dis/syncTask/index',      '', '', 1, 0, 'C', '0', '0', 'dis:syncTask:list',      'job',        'admin', sysdate(), '', null, '');

-- ====================
-- 按钮权限 - 外部系统管理 (parent=2401)
-- ====================
insert into sys_menu values('2405', '系统查询', '2401', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:system:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2406', '系统新增', '2401', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:system:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2407', '系统修改', '2401', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:system:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2408', '系统删除', '2401', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:system:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2409', '系统导出', '2401', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:system:export',   '#', 'admin', sysdate(), '', null, '');

-- ====================
-- 按钮权限 - 接口配置管理 (parent=2402)
-- ====================
insert into sys_menu values('2410', '接口查询', '2402', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:interface:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2411', '接口新增', '2402', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:interface:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2412', '接口修改', '2402', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:interface:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2413', '接口删除', '2402', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:interface:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2414', '接口导出', '2402', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:interface:export',   '#', 'admin', sysdate(), '', null, '');

-- ====================
-- 按钮权限 - 数据交换日志 (parent=2403)
-- ====================
insert into sys_menu values('2415', '日志查询', '2403', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:exchangeLog:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2416', '日志删除', '2403', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:exchangeLog:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2417', '日志导出', '2403', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:exchangeLog:export',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2418', '日志清空', '2403', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:exchangeLog:clean',    '#', 'admin', sysdate(), '', null, '');

-- ====================
-- 按钮权限 - 数据同步任务 (parent=2404)
-- ====================
insert into sys_menu values('2419', '任务查询', '2404', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:syncTask:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2420', '任务新增', '2404', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:syncTask:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2421', '任务修改', '2404', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:syncTask:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2422', '任务删除', '2404', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:syncTask:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2423', '任务执行', '2404', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'dis:syncTask:execute',  '#', 'admin', sysdate(), '', null, '');
