-- ----------------------------
-- 师生互动门户(portal) 角色-菜单授权 SQL
-- 将门户菜单(2500-2540)分配给教师角色(6)和学生角色(7)
-- 管理员角色(1)拥有 *:*:* 通配权限，无需单独授权
-- ----------------------------

-- ====================
-- 教师角色 (role_id=6) 授权
-- 包含：门户根菜单 + 教师服务目录 + 教师功能菜单 + 按钮权限
-- ====================
insert into sys_role_menu values('6', '2500');  -- 师生互动门户(根)
insert into sys_role_menu values('6', '2509');  -- 教师服务(目录)
insert into sys_role_menu values('6', '2510');  -- 个人课表
insert into sys_role_menu values('6', '2511');  -- 成绩录入
insert into sys_role_menu values('6', '2512');  -- 教学任务查询
insert into sys_role_menu values('6', '2513');  -- 监考安排
insert into sys_role_menu values('6', '2514');  -- 评教结果查询
insert into sys_role_menu values('6', '2515');  -- 调停课申请
-- 教师按钮权限
insert into sys_role_menu values('6', '2527');  -- 课表查询(按钮)
insert into sys_role_menu values('6', '2528');  -- 成绩查询(按钮)
insert into sys_role_menu values('6', '2529');  -- 成绩录入(按钮)
insert into sys_role_menu values('6', '2530');  -- 成绩修改(按钮)
insert into sys_role_menu values('6', '2531');  -- 任务查询(按钮)
insert into sys_role_menu values('6', '2532');  -- 监考查询(按钮)
insert into sys_role_menu values('6', '2533');  -- 结果查询(按钮)
insert into sys_role_menu values('6', '2534');  -- 申请查询(按钮)
insert into sys_role_menu values('6', '2535');  -- 提交申请(按钮)
-- 教师也可查看教务通知
insert into sys_role_menu values('6', '2508');  -- 教务通知
insert into sys_role_menu values('6', '2526');  -- 通知查看(按钮)

-- ====================
-- 学生角色 (role_id=7) 授权
-- 包含：门户根菜单 + 学生服务目录 + 学生功能菜单 + 按钮权限
-- ====================
insert into sys_role_menu values('7', '2500');  -- 师生互动门户(根)
insert into sys_role_menu values('7', '2501');  -- 学生服务(目录)
insert into sys_role_menu values('7', '2502');  -- 课表查询
insert into sys_role_menu values('7', '2503');  -- 成绩查询
insert into sys_role_menu values('7', '2504');  -- 选课中心
insert into sys_role_menu values('7', '2505');  -- 考试安排
insert into sys_role_menu values('7', '2506');  -- 评教入口
insert into sys_role_menu values('7', '2507');  -- 学籍服务
insert into sys_role_menu values('7', '2508');  -- 教务通知
-- 学生按钮权限
insert into sys_role_menu values('7', '2517');  -- 课表查询(按钮)
insert into sys_role_menu values('7', '2518');  -- 成绩查询(按钮)
insert into sys_role_menu values('7', '2519');  -- 选课查询(按钮)
insert into sys_role_menu values('7', '2520');  -- 选课操作(按钮)
insert into sys_role_menu values('7', '2521');  -- 考试查询(按钮)
insert into sys_role_menu values('7', '2522');  -- 评教查询(按钮)
insert into sys_role_menu values('7', '2523');  -- 提交评教(按钮)
insert into sys_role_menu values('7', '2524');  -- 学籍查询(按钮)
insert into sys_role_menu values('7', '2525');  -- 异动申请(按钮)
insert into sys_role_menu values('7', '2526');  -- 通知查看(按钮)
