SET NAMES utf8mb4;

-- ============================================================
-- 用户管理模块数据库结构升级脚本
-- 版本：v1.0.0
-- 日期：2026-06-12
-- 说明：为 sys_user 扩展教务字段，新增 sys_student / sys_teacher_link 表，
--       预置教务角色、数据字典及菜单权限（增量脚本，首次执行）
-- ============================================================


-- ----------------------------
-- 1. 扩展 sys_user 表
-- ----------------------------
ALTER TABLE sys_user ADD COLUMN user_category    varchar(10)  DEFAULT 'admin' COMMENT '用户类别(student/teacher/admin/secretary)';
ALTER TABLE sys_user ADD COLUMN identity_id      bigint       DEFAULT NULL    COMMENT '关联业务身份ID';
ALTER TABLE sys_user ADD COLUMN account_status   varchar(20)  DEFAULT 'active' COMMENT '生命周期状态';
ALTER TABLE sys_user ADD COLUMN enrollment_date  date         DEFAULT NULL    COMMENT '入学/入职日期';
ALTER TABLE sys_user ADD COLUMN graduation_date  date         DEFAULT NULL    COMMENT '毕业/离职日期';


-- ----------------------------
-- 2. 创建 sys_student 表（学生信息表）
-- ----------------------------
drop table if exists sys_student;
create table sys_student (
  student_id        bigint(20)      not null auto_increment                    comment '学生ID',
  user_id           bigint(20)      not null                                   comment '关联用户ID',
  student_code      varchar(30)     not null                                   comment '学号',
  class_id          bigint(20)      default null                               comment '班级ID',
  major_id          bigint(20)      default null                               comment '专业ID',
  dept_id           bigint(20)      default null                               comment '院系ID',
  grade             varchar(10)     default null                               comment '年级',
  enrollment_year   int(4)          default null                               comment '入学年份',
  education_level   varchar(20)     default null                               comment '学历层次(undergraduate/master/doctor)',
  student_status    varchar(20)     default 'enrolled'                         comment '学籍状态',
  create_by         varchar(64)     default ''                                 comment '创建者',
  create_time       datetime                                                   comment '创建时间',
  update_by         varchar(64)     default ''                                 comment '更新者',
  update_time       datetime                                                   comment '更新时间',
  remark            varchar(500)    default null                               comment '备注',
  primary key (student_id),
  unique key idx_user_id (user_id),
  unique key idx_student_code (student_code),
  key idx_class_id (class_id),
  key idx_dept_id (dept_id)
) engine=innodb auto_increment=1 comment = '学生信息表';


-- ----------------------------
-- 3. 创建 sys_teacher_link 表（教师账号关联表）
-- ----------------------------
drop table if exists sys_teacher_link;
create table sys_teacher_link (
  link_id           bigint(20)      not null auto_increment                    comment '关联ID',
  user_id           bigint(20)      not null                                   comment '用户ID',
  teacher_id        bigint(20)      not null                                   comment '教师ID(brm_teacher)',
  teacher_code      varchar(50)     not null                                   comment '教师工号',
  create_by         varchar(64)     default ''                                 comment '创建者',
  create_time       datetime                                                   comment '创建时间',
  update_by         varchar(64)     default ''                                 comment '更新者',
  update_time       datetime                                                   comment '更新时间',
  primary key (link_id),
  unique key idx_user_teacher (user_id, teacher_id),
  unique key idx_user_id (user_id),
  key idx_teacher_id (teacher_id)
) engine=innodb auto_increment=1 comment = '教师账号关联表';


-- ----------------------------
-- 4. 预置教务角色
--    role_id 从 3 开始（避免与 1=超级管理员、2=普通角色 冲突）
--    字段顺序：role_id, role_name, role_key, role_sort, data_scope,
--              menu_check_strictly, dept_check_strictly, status, del_flag,
--              create_by, create_time, update_by, update_time, remark
-- ----------------------------
insert into sys_role values(3, '教务处管理员', 'dean',         3, '1', 1, 1, '0', '0', 'admin', sysdate(), '', null, '教务全局管理');
insert into sys_role values(4, '学院管理员',   'college_admin', 4, '4', 1, 1, '0', '0', 'admin', sysdate(), '', null, '管理本学院');
insert into sys_role values(5, '教学秘书',     'secretary',     5, '3', 1, 1, '0', '0', 'admin', sysdate(), '', null, '本系教务管理');
insert into sys_role values(6, '教师',         'teacher',       6, '5', 1, 1, '0', '0', 'admin', sysdate(), '', null, '教师角色');
insert into sys_role values(7, '学生',         'student',       7, '5', 1, 1, '0', '0', 'admin', sysdate(), '', null, '学生角色');


-- ----------------------------
-- 5. 注册数据字典类型
--    dict_id 从 100 开始，避免与系统内置 1-10 冲突
--    字段顺序：dict_id, dict_name, dict_type, status, create_by, create_time,
--              update_by, update_time, remark
-- ----------------------------
insert into sys_dict_type values(100, '用户类别',     'sys_user_category',      '0', 'admin', sysdate(), '', null, '教务系统用户类别');
insert into sys_dict_type values(101, '学籍状态',     'sys_student_status',     '0', 'admin', sysdate(), '', null, '学生学籍状态');
insert into sys_dict_type values(102, '教师账户状态', 'sys_teacher_acct_status','0', 'admin', sysdate(), '', null, '教师账号生命周期状态');


-- ----------------------------
-- 5.1 注册数据字典数据
--     dict_code 从 200 开始，避免与系统内置数据冲突
--     字段顺序：dict_code, dict_sort, dict_label, dict_value, dict_type,
--               css_class, list_class, is_default, status,
--               create_by, create_time, update_by, update_time, remark
-- ----------------------------

-- sys_user_category：用户类别
insert into sys_dict_data values(200, 1, '学生',     'student',   'sys_user_category', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '学生用户');
insert into sys_dict_data values(201, 2, '教师',     'teacher',   'sys_user_category', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '教师用户');
insert into sys_dict_data values(202, 3, '管理员',   'admin',     'sys_user_category', '', 'danger',  'Y', '0', 'admin', sysdate(), '', null, '管理员用户');
insert into sys_dict_data values(203, 4, '教学秘书', 'secretary', 'sys_user_category', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '教学秘书用户');

-- sys_student_status：学籍状态
insert into sys_dict_data values(210, 1, '待入学',   'pending_enrollment', 'sys_student_status', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '待入学');
insert into sys_dict_data values(211, 2, '在读',     'enrolled',           'sys_student_status', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '在读');
insert into sys_dict_data values(212, 3, '休学',     'suspended',          'sys_student_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '休学');
insert into sys_dict_data values(213, 4, '转专业',   'transferred',        'sys_student_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '转专业');
insert into sys_dict_data values(214, 5, '退学',     'withdrawn',          'sys_student_status', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '退学');
insert into sys_dict_data values(215, 6, '已毕业',   'graduated',          'sys_student_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '已毕业');

-- sys_teacher_acct_status：教师账户状态
insert into sys_dict_data values(220, 1, '待入职',   'pending_entry', 'sys_teacher_acct_status', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '待入职');
insert into sys_dict_data values(221, 2, '在职',     'active',        'sys_teacher_acct_status', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '在职');
insert into sys_dict_data values(222, 3, '请假',     'on_leave',      'sys_teacher_acct_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '请假');
insert into sys_dict_data values(223, 4, '调岗',     'transferred',   'sys_teacher_acct_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '调岗');
insert into sys_dict_data values(224, 5, '离职',     'resigned',      'sys_teacher_acct_status', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '离职');
insert into sys_dict_data values(225, 6, '退休',     'retired',       'sys_teacher_acct_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '退休');


-- ----------------------------
-- 6. 菜单权限（menu_id 从 2550 开始，避免与其他模块冲突）
--    字段顺序：menu_id, menu_name, parent_id, order_num, path, component,
--              query, is_frame, is_cache, menu_type, visible, status,
--              perms, icon, create_by, create_time, update_by, update_time, remark
-- ----------------------------

-- 学生账号管理（父菜单：系统管理 = 1）
insert into sys_menu values('2550', '学生账号管理', '1', '10', 'student',     'system/student/index',     '', '', 1, 0, 'C', '0', '0', 'system:student:list',         'student',  'admin', sysdate(), '', null, '学生账号管理菜单');
-- 学生账号管理按钮
insert into sys_menu values('2551', '学生查询',     '2550', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'system:student:query',        '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2552', '学生新增',     '2550', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'system:student:add',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2553', '学生修改',     '2550', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'system:student:edit',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2554', '学生删除',     '2550', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'system:student:remove',       '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2555', '学生导出',     '2550', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'system:student:export',       '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2556', '学生导入',     '2550', '6', '', '', '', '', 1, 0, 'F', '0', '0', 'system:student:import',       '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2557', '状态变更',     '2550', '7', '', '', '', '', 1, 0, 'F', '0', '0', 'system:student:changeStatus', '#', 'admin', sysdate(), '', null, '');

-- 账号同步管理（父菜单：系统管理 = 1）
insert into sys_menu values('2560', '账号同步管理', '1', '11', 'accountSync', 'system/accountSync/index', '', '', 1, 0, 'C', '0', '0', 'system:accountSync:list',     'sync',     'admin', sysdate(), '', null, '账号同步管理菜单');
-- 账号同步管理按钮
insert into sys_menu values('2561', '教师同步',     '2560', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'system:accountSync:teacher',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2562', '学生同步',     '2560', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'system:accountSync:student',  '#', 'admin', sysdate(), '', null, '');


-- ----------------------------
-- 7. 角色菜单授权（sys_role_menu）
--    admin 角色(role_id=1) 和 dean 角色(role_id=3) 分配上述所有菜单
-- ----------------------------

-- admin 角色（role_id=1）授权
insert into sys_role_menu values('1', '2550');
insert into sys_role_menu values('1', '2551');
insert into sys_role_menu values('1', '2552');
insert into sys_role_menu values('1', '2553');
insert into sys_role_menu values('1', '2554');
insert into sys_role_menu values('1', '2555');
insert into sys_role_menu values('1', '2556');
insert into sys_role_menu values('1', '2557');
insert into sys_role_menu values('1', '2560');
insert into sys_role_menu values('1', '2561');
insert into sys_role_menu values('1', '2562');

-- dean 角色（role_id=3）授权
insert into sys_role_menu values('3', '2550');
insert into sys_role_menu values('3', '2551');
insert into sys_role_menu values('3', '2552');
insert into sys_role_menu values('3', '2553');
insert into sys_role_menu values('3', '2554');
insert into sys_role_menu values('3', '2555');
insert into sys_role_menu values('3', '2556');
insert into sys_role_menu values('3', '2557');
insert into sys_role_menu values('3', '2560');
insert into sys_role_menu values('3', '2561');
insert into sys_role_menu values('3', '2562');


-- ----------------------------
-- 8. 教师用户整合约束（确保一个用户只能对应一个教师记录）
-- ----------------------------
ALTER TABLE brm_teacher ADD UNIQUE KEY uk_user_id (user_id);
