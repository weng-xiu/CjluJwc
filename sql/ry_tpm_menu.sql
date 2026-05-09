-- ----------------------------
-- 培养过程管理(tpm)菜单SQL
-- ----------------------------
-- 一级菜单 - 培养过程管理目录
insert into sys_menu values('2100', '培养过程管理', '0', '6', 'tpm',              null, '', '', 1, 0, 'M', '0', '0', '',              'guide',     'admin', sysdate(), '', null, '');

-- ====================
-- 培养方案管理 (parent=2100)
-- ====================
insert into sys_menu values('2101', '培养方案管理', '2100', '1', 'plan-group',    null,                     '', '', 1, 0, 'M', '0', '0', '',                   'education', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2102', '培养方案',     '2101', '1', 'plan',          'tpm/plan/index',         '', '', 1, 0, 'C', '0', '0', 'tpm:plan:list',       'documentation', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2103', '课程库',       '2101', '2', 'courseLib',     'tpm/courseLib/index',    '', '', 1, 0, 'C', '0', '0', 'tpm:course:list',     'list',      'admin', sysdate(), '', null, '');
insert into sys_menu values('2104', '学分结构',     '2101', '3', 'creditStruct',  'tpm/creditStruct/index', '', '', 1, 0, 'C', '0', '0', 'tpm:credit:list',     'chart',     'admin', sysdate(), '', null, '');

-- ====================
-- 开课与排课管理 (parent=2100)
-- ====================
insert into sys_menu values('2105', '开课与排课管理', '2100', '2', 'schedule-group', null,                   '', '', 1, 0, 'M', '0', '0', '',                   'date-range','admin', sysdate(), '', null, '');
insert into sys_menu values('2106', '开课计划',     '2105', '1', 'offering',      'tpm/offering/index',     '', '', 1, 0, 'C', '0', '0', 'tpm:offering:list',   'tab',       'admin', sysdate(), '', null, '');
insert into sys_menu values('2107', '排课管理',     '2105', '2', 'schedule',      'tpm/schedule/index',     '', '', 1, 0, 'C', '0', '0', 'tpm:schedule:list',   'time',      'admin', sysdate(), '', null, '');
insert into sys_menu values('2108', '调停课管理',   '2105', '3', 'adjust',        'tpm/adjust/index',       '', '', 1, 0, 'C', '0', '0', 'tpm:adjust:list',     'switch',    'admin', sysdate(), '', null, '');

-- ====================
-- 选课管理 (parent=2100)
-- ====================
insert into sys_menu values('2109', '选课管理',     '2100', '3', 'selection-group', null,                   '', '', 1, 0, 'M', '0', '0', '',                   'edit',      'admin', sysdate(), '', null, '');
insert into sys_menu values('2110', '选课轮次',     '2109', '1', 'round',         'tpm/round/index',        '', '', 1, 0, 'C', '0', '0', 'tpm:round:list',      'cascader',  'admin', sysdate(), '', null, '');
insert into sys_menu values('2111', '选课规则',     '2109', '2', 'rule',          'tpm/rule/index',         '', '', 1, 0, 'C', '0', '0', 'tpm:rule:list',       'validCode', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2112', '选课名单',     '2109', '3', 'enroll',        'tpm/enroll/index',       '', '', 1, 0, 'C', '0', '0', 'tpm:enroll:list',     'peoples',   'admin', sysdate(), '', null, '');

-- ====================
-- 按钮权限 - 培养方案 (parent=2102)
-- ====================
insert into sys_menu values('2113', '方案查询', '2102', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:plan:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2114', '方案新增', '2102', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:plan:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2115', '方案修改', '2102', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:plan:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2116', '方案删除', '2102', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:plan:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2117', '方案导出', '2102', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:plan:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 课程库 (parent=2103)
insert into sys_menu values('2118', '课程查询', '2103', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:course:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2119', '课程新增', '2103', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:course:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2120', '课程修改', '2103', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:course:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2121', '课程删除', '2103', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:course:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2122', '课程导出', '2103', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:course:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 学分结构 (parent=2104)
insert into sys_menu values('2123', '学分查询', '2104', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:credit:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2124', '学分新增', '2104', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:credit:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2125', '学分修改', '2104', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:credit:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2126', '学分删除', '2104', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:credit:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2127', '学分导出', '2104', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:credit:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 开课计划 (parent=2106)
insert into sys_menu values('2128', '开课查询', '2106', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:offering:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2129', '开课新增', '2106', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:offering:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2130', '开课修改', '2106', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:offering:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2131', '开课删除', '2106', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:offering:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2132', '开课导出', '2106', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:offering:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 排课管理 (parent=2107)
insert into sys_menu values('2133', '排课查询', '2107', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:schedule:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2134', '排课新增', '2107', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:schedule:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2135', '排课修改', '2107', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:schedule:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2136', '排课删除', '2107', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:schedule:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2137', '排课导出', '2107', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:schedule:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 调停课管理 (parent=2108)
insert into sys_menu values('2138', '调整查询', '2108', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:adjust:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2139', '调整新增', '2108', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:adjust:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2140', '调整修改', '2108', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:adjust:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2141', '调整删除', '2108', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:adjust:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2142', '调整导出', '2108', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:adjust:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 选课轮次 (parent=2110)
insert into sys_menu values('2143', '轮次查询', '2110', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:round:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2144', '轮次新增', '2110', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:round:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2145', '轮次修改', '2110', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:round:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2146', '轮次删除', '2110', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:round:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2147', '轮次导出', '2110', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:round:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 选课规则 (parent=2111)
insert into sys_menu values('2148', '规则查询', '2111', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:rule:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2149', '规则新增', '2111', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:rule:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2150', '规则修改', '2111', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:rule:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2151', '规则删除', '2111', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:rule:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2152', '规则导出', '2111', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:rule:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 选课名单 (parent=2112)
insert into sys_menu values('2153', '名单查询', '2112', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:enroll:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2154', '名单新增', '2112', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:enroll:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2155', '名单修改', '2112', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:enroll:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2156', '名单删除', '2112', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:enroll:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2157', '名单导出', '2112', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:enroll:export',   '#', 'admin', sysdate(), '', null, '');
