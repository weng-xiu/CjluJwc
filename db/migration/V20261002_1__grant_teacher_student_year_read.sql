-- =====================================================================================
-- 版本：V20261002.1
-- 标题：为「教师 / 学生」角色补授基础学籍数据（学年 / 学期）只读权限
-- 目的：门户教师端与 student 端页面（个人课表 portal/teacherSchedule、教学任务查询
--       portal/teachingTask、成绩查询 portal/grade、选课中心 portal/selection、考试安排
--       portal/exam 等）在筛选区直接调用管理端字典接口 /brm/year/list 与 /brm/semester/list
--       拉取学年/学期下拉。这两个接口分别受 @ss.hasPermi('brm:year:list') 与
--       'brm:semester:list' 保护，而 role_id=6（teacher）、role_id=7（student）在
--       sys_role_menu 中对菜单 2002/2019（学年管理/学年查询）、2003/2024（学期管理/学期查询）
--       均无任何授权记录 → 教师登录后这些页面的学年筛选恒为 403「没有权限，请联系管理员授权」，
--       下拉为空、按学期筛选不可用（2026-10-02 界面级功能测试实测复现，管理员不受影响，
--       因 admin 走 SecurityUtils.isAdmin() 超级权限短路）。
-- 做法：仅追加 sys_role_menu 关联行（教师/学生 × 4 个菜单 = 8 行），且只授「只读」两档
--       （list + query），不授 add/edit/remove/export，管理端基础数据维护权仍收敛在教务角色。
--       不改动 sys_menu 结构、不改动后端 @PreAuthorize 表达式：避免放宽接口本身的权限语义。
-- 影响表：sys_role_menu（新增至多 8 行）
-- 连带影响：菜单 2002/2003 虽 visible='0'（显示），但其父菜单 2001（基础数据管理）并未授权
--       给教师/学生，/getRouters 的路由树按已授权集合自顶向下组装，父缺失则子成孤儿不入树，
--       故授权后教师/学生侧边栏不会新增「学年/学期管理」条目，仅鉴权字符串经 selectMenuPermsByUserId
--       平铺查询后生效、接口 403 解除；Vue2 归档端共用该数据，行为一致，代码零改动。
-- 关联事项：V4.0 §6.1 N7（门户页面落地质量）、§7.3 快速可交付清单
-- 幂等性：是（INSERT ... SELECT WHERE NOT EXISTS；重复执行不产生重复行，主键 (role_id, menu_id) 亦兜底）
-- 回滚脚本：db/rollback/V20261002_1__grant_teacher_student_year_read.sql
-- 执行说明：本机存量库按既有约定「人工应用 + 带回滚脚本」，FLYWAY_ENABLED 默认 false；
--       新环境或发布流程注入 FLYWAY_ENABLED=true 时由 Flyway 自动重放。
-- =====================================================================================

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
  FROM (SELECT 6 AS role_id UNION ALL SELECT 7) r
 CROSS JOIN (SELECT 2002 AS menu_id UNION ALL SELECT 2019 UNION ALL SELECT 2003 UNION ALL SELECT 2024) m
 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.role_id AND x.menu_id = m.menu_id);

-- 校验：应返回 8 行（teacher/student × 学年管理/学年查询/学期管理/学期查询）
SELECT rm.role_id, ro.role_key, rm.menu_id, sm.menu_name, sm.perms
  FROM sys_role_menu rm
  JOIN sys_role ro ON ro.role_id = rm.role_id
  JOIN sys_menu sm ON sm.menu_id = rm.menu_id
 WHERE rm.menu_id IN (2002, 2019, 2003, 2024)
 ORDER BY rm.role_id, rm.menu_id;
