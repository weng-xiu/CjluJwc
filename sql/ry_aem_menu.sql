-- ----------------------------
-- 考核与评价管理(aem)菜单SQL
-- ----------------------------
-- 一级菜单 - 考核与评价管理目录
insert into sys_menu values('2200', '考核与评价管理', '0', '7', 'aem',              null, '', '', 1, 0, 'M', '0', '0', '',              'education', 'admin', sysdate(), '', null, '');

-- ====================
-- 考试管理 (parent=2200)
-- ====================
insert into sys_menu values('2201', '考试管理',     '2200', '1', 'exam-group',     null,                     '', '', 1, 0, 'M', '0', '0', '',                   'date',      'admin', sysdate(), '', null, '');
insert into sys_menu values('2202', '考试安排',     '2201', '1', 'examPlan',       'aem/examPlan/index',     '', '', 1, 0, 'C', '0', '0', 'aem:examPlan:list',   'tab',       'admin', sysdate(), '', null, '');
insert into sys_menu values('2203', '监考分配',     '2201', '2', 'invigilation',   'aem/invigilation/index', '', '', 1, 0, 'C', '0', '0', 'aem:invigilation:list','user',      'admin', sysdate(), '', null, '');
insert into sys_menu values('2204', '座位编排',     '2201', '3', 'examSeat',       'aem/examSeat/index',     '', '', 1, 0, 'C', '0', '0', 'aem:examSeat:list',   'table',     'admin', sysdate(), '', null, '');

-- ====================
-- 成绩管理 (parent=2200)
-- ====================
insert into sys_menu values('2205', '成绩管理',     '2200', '2', 'grade-group',    null,                     '', '', 1, 0, 'M', '0', '0', '',                   'edit',      'admin', sysdate(), '', null, '');
insert into sys_menu values('2206', '成绩记录',     '2205', '1', 'gradeRecord',    'aem/gradeRecord/index',  '', '', 1, 0, 'C', '0', '0', 'aem:gradeRecord:list','list',      'admin', sysdate(), '', null, '');
insert into sys_menu values('2207', '成绩复核',     '2205', '2', 'gradeReview',    'aem/gradeReview/index',  '', '', 1, 0, 'C', '0', '0', 'aem:gradeReview:list','check',     'admin', sysdate(), '', null, '');
insert into sys_menu values('2208', '成绩统计',     '2205', '3', 'gradeStatistics','aem/gradeStatistics/index','','', 1, 0, 'C', '0', '0', 'aem:gradeStatistics:list','chart', 'admin', sysdate(), '', null, '');

-- ====================
-- 教学质量评价 (parent=2200)
-- ====================
insert into sys_menu values('2209', '教学质量评价', '2200', '3', 'evaluation-group', null,                   '', '', 1, 0, 'M', '0', '0', '',                   'star',      'admin', sysdate(), '', null, '');
insert into sys_menu values('2210', '评教问卷',     '2209', '1', 'questionnaire',  'aem/questionnaire/index','', '', 1, 0, 'C', '0', '0', 'aem:questionnaire:list','form',    'admin', sysdate(), '', null, '');
insert into sys_menu values('2211', '评教问题',     '2209', '2', 'question',       'aem/question/index',     '', '', 1, 0, 'C', '0', '0', 'aem:question:list',   'nested',    'admin', sysdate(), '', null, '');
insert into sys_menu values('2212', '评教结果',     '2209', '3', 'evaluationResult','aem/evaluationResult/index','','',1,0,'C','0','0','aem:evaluationResult:list','redis','admin', sysdate(), '', null, '');
insert into sys_menu values('2213', '督导听课',     '2209', '4', 'supervision',    'aem/supervision/index',  '', '', 1, 0, 'C', '0', '0', 'aem:supervision:list','eye-open','admin', sysdate(), '', null, '');

-- ====================
-- 按钮权限 - 考试安排 (parent=2202)
-- ====================
insert into sys_menu values('2214', '安排查询', '2202', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:examPlan:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2215', '安排新增', '2202', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:examPlan:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2216', '安排修改', '2202', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:examPlan:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2217', '安排删除', '2202', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:examPlan:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2218', '安排导出', '2202', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:examPlan:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 监考分配 (parent=2203)
insert into sys_menu values('2219', '监考查询', '2203', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:invigilation:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2220', '监考新增', '2203', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:invigilation:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2221', '监考修改', '2203', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:invigilation:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2222', '监考删除', '2203', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:invigilation:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2223', '监考导出', '2203', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:invigilation:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 座位编排 (parent=2204)
insert into sys_menu values('2224', '座位查询', '2204', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:examSeat:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2225', '座位新增', '2204', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:examSeat:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2226', '座位修改', '2204', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:examSeat:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2227', '座位删除', '2204', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:examSeat:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2228', '座位导出', '2204', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:examSeat:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 成绩记录 (parent=2206)
insert into sys_menu values('2229', '成绩查询', '2206', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gradeRecord:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2230', '成绩新增', '2206', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gradeRecord:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2231', '成绩修改', '2206', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gradeRecord:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2232', '成绩删除', '2206', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gradeRecord:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2233', '成绩导出', '2206', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gradeRecord:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 成绩复核 (parent=2207)
insert into sys_menu values('2234', '复核查询', '2207', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gradeReview:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2235', '复核新增', '2207', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gradeReview:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2236', '复核修改', '2207', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gradeReview:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2237', '复核删除', '2207', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gradeReview:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2238', '复核导出', '2207', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gradeReview:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 成绩统计 (parent=2208)
insert into sys_menu values('2239', '统计查询', '2208', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gradeStatistics:query','#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 评教问卷 (parent=2210)
insert into sys_menu values('2240', '问卷查询', '2210', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:questionnaire:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2241', '问卷新增', '2210', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:questionnaire:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2242', '问卷修改', '2210', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:questionnaire:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2243', '问卷删除', '2210', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:questionnaire:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2244', '问卷导出', '2210', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:questionnaire:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 评教问题 (parent=2211)
insert into sys_menu values('2245', '问题查询', '2211', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:question:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2246', '问题新增', '2211', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:question:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2247', '问题修改', '2211', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:question:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2248', '问题删除', '2211', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:question:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2249', '问题导出', '2211', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:question:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 评教结果 (parent=2212)
insert into sys_menu values('2250', '结果查询', '2212', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:evaluationResult:query','#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2251', '结果导出', '2212', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:evaluationResult:export','#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 督导听课 (parent=2213)
insert into sys_menu values('2252', '听课查询', '2213', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:supervision:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2253', '听课新增', '2213', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:supervision:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2254', '听课修改', '2213', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:supervision:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2255', '听课删除', '2213', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:supervision:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2256', '听课导出', '2213', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'aem:supervision:export',   '#', 'admin', sysdate(), '', null, '');
