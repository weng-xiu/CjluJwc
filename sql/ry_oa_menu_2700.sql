-- ----------------------------
-- 办公自动化管理(oa)菜单SQL
-- ----------------------------

-- 一级菜单 - OA管理目录
insert into sys_menu values('2700', 'OA管理', '0', '8', 'oa', null, '', '', 1, 0, 'M', '0', '0', '', 'tree', 'admin', sysdate(), '', null, '');

-- ====================
-- 工作流程 (parent=2700)
-- ====================
insert into sys_menu values('2701', '工作流程', '2700', '1', 'workflow-group', null, '', '', 1, 0, 'M', '0', '0', '', 'cascader', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2702', '流程定义', '2701', '1', 'definition', 'oa/definition/index', '', '', 1, 0, 'C', '0', '0', 'oa:definition:list', 'tree-table', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2703', '流程实例', '2701', '2', 'instance', 'oa/instance/index', '', '', 1, 0, 'C', '0', '0', 'oa:instance:list', 'list', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2704', '待办任务', '2701', '3', 'task', 'oa/task/index', '', '', 1, 0, 'C', '0', '0', 'oa:task:list', 'edit', 'admin', sysdate(), '', null, '');

-- ====================
-- 公文管理 (parent=2700)
-- ====================
insert into sys_menu values('2705', '公文管理', '2700', '2', 'document-group', null, '', '', 1, 0, 'M', '0', '0', '', 'documentation', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2706', '公文管理', '2705', '1', 'document', 'oa/document/index', '', '', 1, 0, 'C', '0', '0', 'oa:document:list', 'form', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2707', '待办公文', '2705', '2', 'documentTodo', 'oa/document/todo', '', '', 1, 0, 'C', '0', '0', 'oa:document:todo', 'tab', 'admin', sysdate(), '', null, '');

-- ====================
-- 会议管理 (parent=2700)
-- ====================
insert into sys_menu values('2708', '会议管理', '2700', '3', 'meeting-group', null, '', '', 1, 0, 'M', '0', '0', '', 'date', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2709', '会议室管理', '2708', '1', 'meetingRoom', 'oa/meetingRoom/index', '', '', 1, 0, 'C', '0', '0', 'oa:meetingRoom:list', 'tree', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2710', '会议管理', '2708', '2', 'meeting', 'oa/meeting/index', '', '', 1, 0, 'C', '0', '0', 'oa:meeting:list', 'time', 'admin', sysdate(), '', null, '');

-- ====================
-- 通知公告 (parent=2700)
-- ====================
insert into sys_menu values('2711', '通知公告', '2700', '4', 'notice-group', null, '', '', 1, 0, 'M', '0', '0', '', 'message', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2712', '公告管理', '2711', '1', 'oaNotice', 'oa/notice/index', '', '', 1, 0, 'C', '0', '0', 'oa:notice:list', 'edit', 'admin', sysdate(), '', null, '');

-- ====================
-- 日程安排 (parent=2700)
-- ====================
insert into sys_menu values('2713', '日程安排', '2700', '5', 'oaSchedule-group', null, '', '', 1, 0, 'M', '0', '0', '', 'calendar', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2714', '日程管理', '2713', '1', 'oaSchedule', 'oa/schedule/index', '', '', 1, 0, 'C', '0', '0', 'oa:schedule:list', 'date', 'admin', sysdate(), '', null, '');

-- ====================
-- 按钮权限 - 流程定义 (parent=2702)
-- ====================
insert into sys_menu values('2715', '流程定义查询', '2702', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:definition:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2716', '流程定义新增', '2702', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:definition:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2717', '流程定义修改', '2702', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:definition:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2718', '流程定义删除', '2702', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:definition:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2719', '流程定义部署', '2702', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:definition:deploy', '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 流程实例 (parent=2703)
insert into sys_menu values('2720', '流程实例查询', '2703', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:instance:query', '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 待办任务 (parent=2704)
insert into sys_menu values('2721', '待办任务查询', '2704', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:task:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2722', '待办任务审批', '2704', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:task:approve', '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 公文管理 (parent=2706)
insert into sys_menu values('2723', '公文查询', '2706', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:document:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2724', '公文新增', '2706', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:document:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2725', '公文修改', '2706', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:document:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2726', '公文删除', '2706', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:document:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2727', '公文导出', '2706', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:document:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2728', '公文提交', '2706', '6', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:document:submit', '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 待办公文 (parent=2707)
insert into sys_menu values('2729', '待办公文查询', '2707', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:document:todo', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2730', '待办公文审批', '2707', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:document:approve', '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 会议室管理 (parent=2709)
insert into sys_menu values('2731', '会议室查询', '2709', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meetingRoom:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2732', '会议室新增', '2709', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meetingRoom:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2733', '会议室修改', '2709', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meetingRoom:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2734', '会议室删除', '2709', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meetingRoom:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2735', '会议室导出', '2709', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meetingRoom:export', '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 会议管理 (parent=2710)
insert into sys_menu values('2736', '会议查询', '2710', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meeting:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2737', '会议新增', '2710', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meeting:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2738', '会议修改', '2710', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meeting:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2739', '会议删除', '2710', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meeting:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2740', '会议导出', '2710', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:meeting:export', '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 公告管理 (parent=2712)
insert into sys_menu values('2741', '公告查询', '2712', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:notice:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2742', '公告新增', '2712', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:notice:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2743', '公告修改', '2712', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:notice:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2744', '公告删除', '2712', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:notice:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2745', '公告导出', '2712', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:notice:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2746', '公告发布', '2712', '6', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:notice:publish', '#', 'admin', sysdate(), '', null, '');

-- 按钮权限 - 日程管理 (parent=2714)
insert into sys_menu values('2747', '日程查询', '2714', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:schedule:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2748', '日程新增', '2714', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:schedule:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2749', '日程修改', '2714', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:schedule:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2750', '日程删除', '2714', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:schedule:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2751', '日程导出', '2714', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'oa:schedule:export', '#', 'admin', sysdate(), '', null, '');
