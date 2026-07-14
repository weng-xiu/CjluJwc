-- ----------------------------
-- 办公自动化管理(oa)菜单SQL
-- ----------------------------

-- 一级菜单 - OA管理目录
insert into sys_menu values('2300', 'OA管理', '0', '8', 'oa', null, '', '', 1, 0, 'M', '0', '0', '', 'tree', 'admin', sysdate(), '', null, '');

-- ====================
-- 工作流程 (parent=2300)
-- ====================
insert into sys_menu values('2301', '工作流程', '2300', '1', 'workflow-group', null, '', '', 1, 0, 'M', '0', '0', '', 'cascader', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2302', '流程定义', '2301', '1', 'definition', 'oa/definition/index', '', '', 1, 0, 'C', '0', '0', 'oa:definition:list', 'tree-table', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2303', '流程实例', '2301', '2', 'instance', 'oa/instance/index', '', '', 1, 0, 'C', '0', '0', 'oa:instance:list', 'list', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2304', '待办任务', '2301', '3', 'task', 'oa/task/index', '', '', 1, 0, 'C', '0', '0', 'oa:task:list', 'edit', 'admin', sysdate(), '', null, '');

-- ====================
-- 公文管理 (parent=2300)
-- ====================
insert into sys_menu values('2305', '公文管理', '2300', '2', 'document-group', null, '', '', 1, 0, 'M', '0', '0', '', 'documentation', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2306', '公文管理', '2305', '1', 'document', 'oa/document/index', '', '', 1, 0, 'C', '0', '0', 'oa:document:list', 'form', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2307', '待办公文', '2305', '2', 'documentTodo', 'oa/document/todo', '', '', 1, 0, 'C', '0', '0', 'oa:document:todo', 'tab', 'admin', sysdate(), '', null, '');

-- ====================
-- 会议管理 (parent=2300)
-- ====================
insert into sys_menu values('2308', '会议管理', '2300', '3', 'meeting-group', null, '', '', 1, 0, 'M', '0', '0', '', 'date', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2309', '会议室管理', '2308', '1', 'meetingRoom', 'oa/meetingRoom/index', '', '', 1, 0, 'C', '0', '0', 'oa:meetingRoom:list', 'tree', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2310', '会议管理', '2308', '2', 'meeting', 'oa/meeting/index', '', '', 1, 0, 'C', '0', '0', 'oa:meeting:list', 'time', 'admin', sysdate(), '', null, '');

-- ====================
-- 通知公告 (parent=2300)
-- ====================
insert into sys_menu values('2311', '通知公告', '2300', '4', 'notice-group', null, '', '', 1, 0, 'M', '0', '0', '', 'message', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2312', '公告管理', '2311', '1', 'notice', 'oa/notice/index', '', '', 1, 0, 'C', '0', '0', 'oa:notice:list', 'edit', 'admin', sysdate(), '', null, '');

-- ====================
-- 日程安排 (parent=2300)
-- ====================
insert into sys_menu values('2313', '日程安排', '2300', '5', 'schedule-group', null, '', '', 1, 0, 'M', '0', '0', '', 'calendar', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2314', '日程管理', '2313', '1', 'schedule', 'oa/schedule/index', '', '', 1, 0, 'C', '0', '0', 'oa:schedule:list', 'date', 'admin', sysdate(), '', null, '');

-- ====================
-- 按钮权限 - 流程定义 (parent=2302)
-- ====================
insert into sys_menu values('2315', '流程定义查询', '2302', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:definition:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2316', '流程定义新增', '2302', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:definition:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2317', '流程定义修改', '2302', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:definition:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2318', '流程定义删除', '2302', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:definition:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2319', '流程定义部署', '2302', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:definition:deploy', '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 流程实例 (parent=2303)
insert into sys_menu values('2320', '流程实例查询', '2303', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:instance:query', '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 待办任务 (parent=2304)
insert into sys_menu values('2321', '待办任务查询', '2304', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:task:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2322', '待办任务审批', '2304', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:task:approve', '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 公文管理 (parent=2306)
insert into sys_menu values('2323', '公文查询', '2306', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:document:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2324', '公文新增', '2306', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:document:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2325', '公文修改', '2306', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:document:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2326', '公文删除', '2306', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:document:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2327', '公文导出', '2306', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:document:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2328', '公文提交', '2306', '6', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:document:submit', '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 待办公文 (parent=2307)
insert into sys_menu values('2329', '待办公文查询', '2307', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:document:todo', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2330', '待办公文审批', '2307', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:document:approve', '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 会议室管理 (parent=2309)
insert into sys_menu values('2331', '会议室查询', '2309', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meetingRoom:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2332', '会议室新增', '2309', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meetingRoom:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2333', '会议室修改', '2309', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meetingRoom:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2334', '会议室删除', '2309', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meetingRoom:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2335', '会议室导出', '2309', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meetingRoom:export', '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 会议管理 (parent=2310)
insert into sys_menu values('2336', '会议查询', '2310', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meeting:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2337', '会议新增', '2310', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meeting:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2338', '会议修改', '2310', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meeting:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2339', '会议删除', '2310', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meeting:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2340', '会议导出', '2310', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meeting:export', '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 公告管理 (parent=2312)
insert into sys_menu values('2341', '公告查询', '2312', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:notice:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2342', '公告新增', '2312', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:notice:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2343', '公告修改', '2312', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:notice:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2344', '公告删除', '2312', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:notice:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2345', '公告导出', '2312', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:notice:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2346', '公告发布', '2312', '6', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:notice:publish', '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 日程管理 (parent=2314)
insert into sys_menu values('2347', '日程查询', '2314', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:schedule:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2348', '日程新增', '2314', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:schedule:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2349', '日程修改', '2314', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:schedule:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2350', '日程删除', '2314', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:schedule:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2351', '日程导出', '2314', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:schedule:export', '#', 'admin', sysdate(), '', null, '');
