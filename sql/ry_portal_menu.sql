-- ----------------------------
-- 师生互动服务门户(portal)菜单SQL
-- 菜单ID从2500开始（2400-2499已被yu-dis占用）
-- ----------------------------
-- 一级菜单 - 师生互动门户
insert into sys_menu values('2500', '师生互动门户', '0', '7', 'portal',           null, '', '', 1, 0, 'M', '0', '0', '',               'dashboard',  'admin', sysdate(), '', null, '');

-- ====================
-- 学生端 (parent=2500)
-- ====================
insert into sys_menu values('2501', '学生服务',     '2500', '1', 'student-group',  null, '', '', 1, 0, 'M', '0', '0', '',               'user',       'admin', sysdate(), '', null, '');
insert into sys_menu values('2502', '课表查询',     '2501', '1', 'schedule',       'portal/schedule/index',              '', '', 1, 0, 'C', '0', '0', 'portal:schedule:list',     'time',            'admin', sysdate(), '', null, '');
insert into sys_menu values('2503', '成绩查询',     '2501', '2', 'grade',          'portal/grade/index',                 '', '', 1, 0, 'C', '0', '0', 'portal:grade:list',        'documentation',   'admin', sysdate(), '', null, '');
insert into sys_menu values('2504', '选课中心',     '2501', '3', 'selection',      'portal/selection/index',             '', '', 1, 0, 'C', '0', '0', 'portal:selection:list',    'edit',            'admin', sysdate(), '', null, '');
insert into sys_menu values('2505', '考试安排',     '2501', '4', 'exam',           'portal/exam/index',                  '', '', 1, 0, 'C', '0', '0', 'portal:exam:list',         'date-range',      'admin', sysdate(), '', null, '');
insert into sys_menu values('2506', '评教入口',     '2501', '5', 'evaluation',     'portal/evaluation/index',            '', '', 1, 0, 'C', '0', '0', 'portal:evaluation:list',   'star',            'admin', sysdate(), '', null, '');
insert into sys_menu values('2507', '学籍服务',     '2501', '6', 'status',         'portal/studentStatus/index',         '', '', 1, 0, 'C', '0', '0', 'portal:status:list',       'form',            'admin', sysdate(), '', null, '');
insert into sys_menu values('2508', '教务通知',     '2501', '7', 'notice',         'portal/notice/index',                '', '', 1, 0, 'C', '0', '0', 'portal:notice:list',       'message',         'admin', sysdate(), '', null, '');

-- ====================
-- 教师端 (parent=2500)
-- ====================
insert into sys_menu values('2509', '教师服务',     '2500', '2', 'teacher-group',  null, '', '', 1, 0, 'M', '0', '0', '',               'peoples',      'admin', sysdate(), '', null, '');
insert into sys_menu values('2510', '个人课表',     '2509', '1', 'teacherSchedule', 'portal/teacherSchedule/index',       '', '', 1, 0, 'C', '0', '0', 'portal:teacherSchedule:list', 'time',        'admin', sysdate(), '', null, '');
insert into sys_menu values('2511', '成绩录入',     '2509', '2', 'gradeEntry',     'portal/gradeEntry/index',            '', '', 1, 0, 'C', '0', '0', 'portal:gradeEntry:list',   'edit',         'admin', sysdate(), '', null, '');
insert into sys_menu values('2512', '教学任务查询', '2509', '3', 'teachingTask',   'portal/teachingTask/index',          '', '', 1, 0, 'C', '0', '0', 'portal:teachingTask:list', 'list',         'admin', sysdate(), '', null, '');
insert into sys_menu values('2513', '监考安排',     '2509', '4', 'invigilation',   'portal/invigilation/index',          '', '', 1, 0, 'C', '0', '0', 'portal:invigilation:list', 'cascader',     'admin', sysdate(), '', null, '');
insert into sys_menu values('2514', '评教结果查询', '2509', '5', 'evalResult',     'portal/evalResult/index',            '', '', 1, 0, 'C', '0', '0', 'portal:evalResult:list',   'chart',        'admin', sysdate(), '', null, '');
insert into sys_menu values('2515', '调停课申请',   '2509', '6', 'adjustment',     'portal/adjustment/index',            '', '', 1, 0, 'C', '0', '0', 'portal:adjustment:list',   'switch',       'admin', sysdate(), '', null, '');

-- ====================
-- 后台管理 - 通知管理 (parent=2500)
-- ====================
insert into sys_menu values('2516', '通知管理',     '2500', '3', 'notice-mgmt',    'portal/noticeManage/index',          '', '', 1, 0, 'C', '0', '0', 'portal:noticeManage:list', 'tree-table',   'admin', sysdate(), '', null, '');

-- ====================
-- 按钮权限 - 学生端
-- ====================
-- 课表查询 (parent=2502)
insert into sys_menu values('2517', '课表查询',     '2502', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:schedule:query',    '#', 'admin', sysdate(), '', null, '');
-- 成绩查询 (parent=2503)
insert into sys_menu values('2518', '成绩查询',     '2503', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:grade:query',       '#', 'admin', sysdate(), '', null, '');
-- 选课中心 (parent=2504)
insert into sys_menu values('2519', '选课查询',     '2504', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:selection:query',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2520', '选课操作',     '2504', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:selection:enroll',   '#', 'admin', sysdate(), '', null, '');
-- 考试安排 (parent=2505)
insert into sys_menu values('2521', '考试查询',     '2505', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:exam:query',        '#', 'admin', sysdate(), '', null, '');
-- 评教入口 (parent=2506)
insert into sys_menu values('2522', '评教查询',     '2506', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:evaluation:query',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2523', '提交评教',     '2506', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:evaluation:submit',  '#', 'admin', sysdate(), '', null, '');
-- 学籍服务 (parent=2507)
insert into sys_menu values('2524', '学籍查询',     '2507', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:status:query',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2525', '异动申请',     '2507', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:status:change',     '#', 'admin', sysdate(), '', null, '');
-- 教务通知 (parent=2508)
insert into sys_menu values('2526', '通知查看',     '2508', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:notice:query',      '#', 'admin', sysdate(), '', null, '');

-- ====================
-- 按钮权限 - 教师端
-- ====================
-- 个人课表 (parent=2510)
insert into sys_menu values('2527', '课表查询',     '2510', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:teacherSchedule:query', '#', 'admin', sysdate(), '', null, '');
-- 成绩录入 (parent=2511)
insert into sys_menu values('2528', '成绩查询',     '2511', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:gradeEntry:query',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2529', '成绩录入',     '2511', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:gradeEntry:add',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2530', '成绩修改',     '2511', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:gradeEntry:edit',    '#', 'admin', sysdate(), '', null, '');
-- 教学任务查询 (parent=2512)
insert into sys_menu values('2531', '任务查询',     '2512', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:teachingTask:query', '#', 'admin', sysdate(), '', null, '');
-- 监考安排 (parent=2513)
insert into sys_menu values('2532', '监考查询',     '2513', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:invigilation:query', '#', 'admin', sysdate(), '', null, '');
-- 评教结果查询 (parent=2514)
insert into sys_menu values('2533', '结果查询',     '2514', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:evalResult:query',   '#', 'admin', sysdate(), '', null, '');
-- 调停课申请 (parent=2515)
insert into sys_menu values('2534', '申请查询',     '2515', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:adjustment:query',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2535', '提交申请',     '2515', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:adjustment:add',     '#', 'admin', sysdate(), '', null, '');

-- ====================
-- 按钮权限 - 通知管理(后台)
-- ====================
insert into sys_menu values('2536', '通知查询',     '2516', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:noticeManage:query',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2537', '通知新增',     '2516', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:noticeManage:add',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2538', '通知修改',     '2516', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:noticeManage:edit',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2539', '通知删除',     '2516', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:noticeManage:remove',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2540', '通知导出',     '2516', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'portal:noticeManage:export',  '#', 'admin', sysdate(), '', null, '');
