-- ----------------------------
-- 学籍与学位管理(sam)菜单SQL
-- ----------------------------
-- 一级菜单 - 学籍与学位管理目录
insert into sys_menu values('2300', '学籍与学位管理', '0', '8', 'sam',              null, '', '', 1, 0, 'M', '0', '0', '',              'guide',     'admin', sysdate(), '', null, '');

-- ====================
-- 学籍管理 (parent=2300)
-- ====================
insert into sys_menu values('2301', '学籍管理',     '2300', '1', 'student-group',    null,                        '', '', 1, 0, 'M', '0', '0', '',                   'people',    'admin', sysdate(), '', null, '');
insert into sys_menu values('2302', '学生学籍',     '2301', '1', 'student',          'sam/student/index',         '', '', 1, 0, 'C', '0', '0', 'sam:student:list',   'list',      'admin', sysdate(), '', null, '');
insert into sys_menu values('2303', '学籍异动',     '2301', '2', 'statusChange',     'sam/statusChange/index',    '', '', 1, 0, 'C', '0', '0', 'sam:statusChange:list','switch',    'admin', sysdate(), '', null, '');
insert into sys_menu values('2304', '学籍预警',     '2301', '3', 'warning',          'sam/warning/index',         '', '', 1, 0, 'C', '0', '0', 'sam:warning:list',    'warning',   'admin', sysdate(), '', null, '');

-- ====================
-- 毕业与学位管理 (parent=2300)
-- ====================
insert into sys_menu values('2305', '毕业与学位管理', '2300', '2', 'graduation-group',  null,                       '', '', 1, 0, 'M', '0', '0', '',                   'education', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2306', '毕业审核',     '2305', '1', 'graduationReview', 'sam/graduationReview/index', '', '', 1, 0, 'C', '0', '0', 'sam:graduationReview:list','check',  'admin', sysdate(), '', null, '');
insert into sys_menu values('2307', '学位审核',     '2305', '2', 'degreeReview',     'sam/degreeReview/index',    '', '', 1, 0, 'C', '0', '0', 'sam:degreeReview:list','star',   'admin', sysdate(), '', null, '');
insert into sys_menu values('2308', '证书管理',     '2305', '3', 'certificate',      'sam/certificate/index',     '', '', 1, 0, 'C', '0', '0', 'sam:certificate:list','documentation','admin',sysdate(),'',null,'');
insert into sys_menu values('2309', '离校手续',     '2305', '4', 'graduationProcedure','sam/graduationProcedure/index','','',1,0,'C','0','0','sam:graduationProcedure:list','exit-full','admin',sysdate(),'',null,'');

-- ====================
-- 按钮权限 - 学生学籍 (parent=2302)
-- ====================
insert into sys_menu values('2310', '学籍查询', '2302', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:student:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2311', '学籍新增', '2302', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:student:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2312', '学籍修改', '2302', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:student:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2313', '学籍删除', '2302', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:student:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2314', '学籍导出', '2302', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:student:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 学籍异动 (parent=2303)
insert into sys_menu values('2315', '异动查询', '2303', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:statusChange:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2316', '异动新增', '2303', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:statusChange:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2317', '异动修改', '2303', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:statusChange:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2318', '异动删除', '2303', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:statusChange:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2319', '异动导出', '2303', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:statusChange:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 学籍预警 (parent=2304)
insert into sys_menu values('2320', '预警查询', '2304', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:warning:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2321', '预警新增', '2304', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:warning:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2322', '预警修改', '2304', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:warning:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2323', '预警删除', '2304', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:warning:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2324', '预警导出', '2304', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:warning:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 毕业审核 (parent=2306)
insert into sys_menu values('2325', '毕业查询', '2306', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:graduationReview:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2326', '毕业新增', '2306', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:graduationReview:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2327', '毕业修改', '2306', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:graduationReview:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2328', '毕业删除', '2306', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:graduationReview:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2329', '毕业导出', '2306', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:graduationReview:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 学位审核 (parent=2307)
insert into sys_menu values('2330', '学位查询', '2307', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:degreeReview:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2331', '学位新增', '2307', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:degreeReview:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2332', '学位修改', '2307', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:degreeReview:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2333', '学位删除', '2307', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:degreeReview:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2334', '学位导出', '2307', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:degreeReview:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 证书管理 (parent=2308)
insert into sys_menu values('2335', '证书查询', '2308', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:certificate:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2336', '证书新增', '2308', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:certificate:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2337', '证书修改', '2308', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:certificate:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2338', '证书删除', '2308', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:certificate:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2339', '证书导出', '2308', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:certificate:export',   '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 离校手续 (parent=2309)
insert into sys_menu values('2340', '手续查询', '2309', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:graduationProcedure:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2341', '手续新增', '2309', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:graduationProcedure:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2342', '手续修改', '2309', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:graduationProcedure:edit',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2343', '手续删除', '2309', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:graduationProcedure:remove',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2344', '手续导出', '2309', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'sam:graduationProcedure:export',   '#', 'admin', sysdate(), '', null, '');
