-- =================================================================------------
-- Phase 33：教育部状态数据上报（合规上报，计划文档（三）政策合规第 1/3 项）
--
-- 目标：按教育部状态数据口径组织学籍、课程、成绩、师资四类数据，
--       提供「字段映射说明 → 一键生成批次 → 预览核对 → 导出报盘 → 标记上报」闭环。
--
-- 说明：
--   1) sys_status_report_field 记录每个上报类型的标准字段代码、名称、来源与值域
--      转换规则，既作为页面口径说明，也作为预览列定义，保证「映射可维护、可追溯」。
--   2) sys_status_report_batch 记录每次生成的批次（类型/年度/范围/行数/状态），
--      满足上报留痕与重复上报排查需要。
--   3) 全部语句幂等，可重复执行。
-- =================================================================------------

-- ----------------------------
-- 1、上报批次表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_status_report_batch (
  batch_id       bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  report_type    char(2)      NOT NULL                COMMENT '上报类型（01学生基本信息 02课程基本信息 03成绩信息 04教师基本信息）',
  report_year    varchar(8)   NOT NULL                COMMENT '上报年度（如2026）',
  report_name    varchar(120) DEFAULT NULL            COMMENT '批次名称',
  scope_dept_id  bigint(20)   DEFAULT 0               COMMENT '统计范围院系ID（0表示全校）',
  scope_dept_name varchar(100) DEFAULT NULL           COMMENT '统计范围院系名称',
  row_count      int(11)      DEFAULT 0               COMMENT '生成数据行数',
  batch_status   char(1)      DEFAULT '0'             COMMENT '批次状态（0已生成 1已导出 2已上报 3已作废）',
  gen_time       datetime     DEFAULT NULL            COMMENT '生成时间',
  export_time    datetime     DEFAULT NULL            COMMENT '最近导出时间',
  submit_time    datetime     DEFAULT NULL            COMMENT '上报标记时间',
  remark         varchar(500) DEFAULT NULL            COMMENT '备注',
  create_by      varchar(64)  DEFAULT ''              COMMENT '创建者',
  create_time    datetime     DEFAULT NULL            COMMENT '创建时间',
  update_by      varchar(64)  DEFAULT ''              COMMENT '更新者',
  update_time    datetime     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (batch_id),
  KEY idx_report_type_year (report_type, report_year),
  KEY idx_batch_status (batch_status)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='教育部状态数据上报批次';

-- ----------------------------
-- 2、上报字段映射表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_status_report_field (
  field_id       bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '字段ID',
  report_type    char(2)      NOT NULL                COMMENT '上报类型（01学生 02课程 03成绩 04教师）',
  std_code       varchar(30)  NOT NULL                COMMENT '标准字段代码',
  std_name       varchar(100) NOT NULL                COMMENT '标准字段名称',
  source_expr    varchar(200) DEFAULT NULL            COMMENT '数据来源（表.字段）',
  convert_rule   varchar(300) DEFAULT NULL            COMMENT '值域/转换规则',
  data_type      varchar(20)  DEFAULT 'S'             COMMENT '数据类型（S字符 N数值 D日期）',
  required       char(1)      DEFAULT '1'             COMMENT '是否必填（0否 1是）',
  order_num      int(4)       DEFAULT 0               COMMENT '展示顺序',
  create_by      varchar(64)  DEFAULT ''              COMMENT '创建者',
  create_time    datetime     DEFAULT NULL            COMMENT '创建时间',
  update_by      varchar(64)  DEFAULT ''              COMMENT '更新者',
  update_time    datetime     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (field_id),
  UNIQUE KEY uk_type_std_code (report_type, std_code),
  KEY idx_type_order (report_type, order_num)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='状态数据上报字段映射';

-- ----------------------------
-- 3、字段映射初始化（标准代码取自教育部教育管理信息化状态数据常用字段口径）
-- ----------------------------
INSERT INTO sys_status_report_field
  (report_type, std_code, std_name, source_expr, convert_rule, data_type, required, order_num, create_by, create_time)
SELECT * FROM (
  SELECT '01' AS a, 'XH' AS b, '学号' AS c, 'sam_student.student_no' AS d, '原值直取' AS e, 'S' AS f, '1' AS g, 1 AS h, 'system' AS i, NOW() AS j
  UNION ALL SELECT '01','XM','姓名','sam_student.student_name','原值直取','S','1',2,'system',NOW()
  UNION ALL SELECT '01','XBDM','性别代码','sam_student.gender','0男→1，1女→2（GB/T 2261.1）','S','1',3,'system',NOW()
  UNION ALL SELECT '01','CSRQ','出生日期','sam_student.birth_date','yyyy-MM-dd','D','0',4,'system',NOW()
  UNION ALL SELECT '01','GMSFHM','公民身份号码','sam_student.id_card','原值直取，导出时按权限脱敏','S','0',5,'system',NOW()
  UNION ALL SELECT '01','SZYX','所在院系','brm_department.dept_name','院系名称，代码列另行维护','S','1',6,'system',NOW()
  UNION ALL SELECT '01','ZYMC','专业名称','brm_major.major_name','原值直取','S','1',7,'system',NOW()
  UNION ALL SELECT '01','BJMC','班级名称','brm_class.class_name','原值直取','S','0',8,'system',NOW()
  UNION ALL SELECT '01','RXNJ','入学年份','sam_student.enrollment_year','去除“级”后缀，保留4位年份','S','1',9,'system',NOW()
  UNION ALL SELECT '01','XLCCDM','学历层次代码','sam_student.education_level','博士→10，硕士→20，本科→30，其他→90','S','1',10,'system',NOW()
  UNION ALL SELECT '01','XJZTDM','学籍状态代码','sam_student.student_status','0在读→01在籍，1休学→03，2退学→05，3毕业→09，4转出→10，5保留学籍→02','S','1',11,'system',NOW()
  UNION ALL SELECT '01','ZXNX','修业年限','sam_student.enrollment_year','本科按4年上报（无独立字段，按口径默认值）','N','0',12,'system',NOW()
  UNION ALL SELECT '02','KCH','课程号','tpm_course_library.course_code','原值直取','S','1',1,'system',NOW()
  UNION ALL SELECT '02','KCM','课程名','tpm_course_library.course_name','原值直取','S','1',2,'system',NOW()
  UNION ALL SELECT '02','KCMCYW','课程英文名','tpm_course_library.course_name_en','可空','S','0',3,'system',NOW()
  UNION ALL SELECT '02','XF','学分','tpm_course_library.credit','保留1位小数','N','1',4,'system',NOW()
  UNION ALL SELECT '02','ZXS','总学时','tpm_course_library.total_hours','缺失时取理论+实践学时','N','1',5,'system',NOW()
  UNION ALL SELECT '02','LLXS','理论学时','tpm_course_library.theory_hours','原值直取','N','0',6,'system',NOW()
  UNION ALL SELECT '02','SJXS','实践学时','tpm_course_library.practice_hours','原值直取','N','0',7,'system',NOW()
  UNION ALL SELECT '02','KCXZDM','课程性质代码','tpm_course_library.course_type','必修→1，选修→2，公选→3，其他→9','S','1',8,'system',NOW()
  UNION ALL SELECT '02','KCLB','课程类别','tpm_course_library.course_category','原值直取（通识/学科基础/专业核心/实践环节）','S','0',9,'system',NOW()
  UNION ALL SELECT '02','KSKFMS','考核方式','tpm_course_library.assessment_method','考试→1，考查→2','S','1',10,'system',NOW()
  UNION ALL SELECT '02','SSYX','开设院系','tpm_course_library + brm_department','按课程归属方案院系汇总，缺省为教务处','S','0',11,'system',NOW()
  UNION ALL SELECT '03','XH','学号','sam_student.student_no','原值直取','S','1',1,'system',NOW()
  UNION ALL SELECT '03','XM','姓名','sam_student.student_name','原值直取','S','1',2,'system',NOW()
  UNION ALL SELECT '03','KCH','课程号','tpm_course_library.course_code','原值直取','S','1',3,'system',NOW()
  UNION ALL SELECT '03','KCM','课程名','tpm_course_library.course_name','原值直取','S','1',4,'system',NOW()
  UNION ALL SELECT '03','XF','学分','tpm_course_library.credit','保留1位小数','N','1',5,'system',NOW()
  UNION ALL SELECT '03','CJ','成绩','aem_grade_record.total_score','原始百分制成绩','N','1',6,'system',NOW()
  UNION ALL SELECT '03','JD','绩点','aem_grade_record.grade_point','保留1位小数','N','1',7,'system',NOW()
  UNION ALL SELECT '03','CJDJ','成绩等级','aem_grade_record.grade_level','A优 B良 C中 D及格 F不及格','S','0',8,'system',NOW()
  UNION ALL SELECT '03','JGDM','结果代码','aem_grade_record.is_pass','1通过→Y，0未通过→N','S','1',9,'system',NOW()
  UNION ALL SELECT '03','KCLX','考核类型','aem_grade_record.exam_type','0正考→1，1补考→2，2重修→3','S','1',10,'system',NOW()
  UNION ALL SELECT '03','XQMC','学期','brm_semester.semester_name','原值直取','S','1',11,'system',NOW()
  UNION ALL SELECT '04','JSGH','教工号','brm_teacher.teacher_code','原值直取','S','1',1,'system',NOW()
  UNION ALL SELECT '04','XM','姓名','brm_teacher.teacher_name','原值直取','S','1',2,'system',NOW()
  UNION ALL SELECT '04','XBDM','性别代码','brm_teacher.gender','0男→1，1女→2，2未知→9','S','1',3,'system',NOW()
  UNION ALL SELECT '04','SSYX','所属院系','brm_department.dept_name','原值直取','S','1',4,'system',NOW()
  UNION ALL SELECT '04','ZCDM','职称','brm_teacher.title','原值直取（教授/副教授/讲师/助教）','S','0',5,'system',NOW()
  UNION ALL SELECT '04','XLDM','学历','brm_teacher.education','原值直取，值域转换在导出模板中标注','S','0',6,'system',NOW()
  UNION ALL SELECT '04','LXDH','联系电话','brm_teacher.phone','导出时按权限脱敏','S','0',7,'system',NOW()
  UNION ALL SELECT '04','DZYX','电子邮箱','brm_teacher.email','原值直取','S','0',8,'system',NOW()
  UNION ALL SELECT '04','SFJS','是否在校','brm_teacher.status','0正常→Y，1停用→N','S','1',9,'system',NOW()
) t
WHERE (SELECT COUNT(1) FROM sys_status_report_field) = 0;

-- 补录：成绩信息新增“所在院系”列（用于分院系报送筛选，单独幂等插入）
INSERT INTO sys_status_report_field (report_type, std_code, std_name, source_expr, convert_rule, data_type, required, order_num, create_by, create_time)
SELECT '03', 'SZYX', '所在院系', 'brm_department.dept_name', '学生学籍所属院系', 'S', '1', 12, 'system', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_status_report_field WHERE report_type = '03' AND std_code = 'SZYX');

-- ----------------------------
-- 4、上报字典
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '状态数据上报类型', 'sys_status_report_type', '0', 'system', NOW(), '教育部状态数据上报类型'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'sys_status_report_type');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '状态数据上报批次状态', 'sys_status_report_status', '0', 'system', NOW(), '上报批次流转状态'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'sys_status_report_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT * FROM (
  SELECT 1 AS a, '学生基本信息' AS b, '01' AS c, 'sys_status_report_type' AS d, '' AS e, 'primary' AS f, 'N' AS g, '0' AS h, 'system' AS i, NOW() AS j, 'JYB 学生基本信息表' AS k
  UNION ALL SELECT 2,'课程基本信息','02','sys_status_report_type','','success','N','0','system',NOW(),'JYB 课程基本信息表'
  UNION ALL SELECT 3,'成绩信息','03','sys_status_report_type','','warning','N','0','system',NOW(),'JYB 学生成绩表'
  UNION ALL SELECT 4,'教师基本信息','04','sys_status_report_type','','info','N','0','system',NOW(),'JYB 教师基本信息表'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sys_status_report_type' AND dict_value = '01');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT * FROM (
  SELECT 1 AS a, '已生成' AS b, '0' AS c, 'sys_status_report_status' AS d, '' AS e, 'info' AS f, 'Y' AS g, '0' AS h, 'system' AS i, NOW() AS j, '批次数据已生成待核对' AS k
  UNION ALL SELECT 2,'已导出','1','sys_status_report_status','','primary','N','0','system',NOW(),'已导出报盘文件'
  UNION ALL SELECT 3,'已上报','2','sys_status_report_status','','success','N','0','system',NOW(),'已提交上级平台'
  UNION ALL SELECT 4,'已作废','3','sys_status_report_status','','danger','N','0','system',NOW(),'批次作废不再使用'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sys_status_report_status' AND dict_value = '0');

-- ----------------------------
-- 5、菜单与权限（挂在「系统管理」下，紧随主题分析）
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3150, '状态数据上报', 1, 12, 'statusReport', 'system/statusReport/index', '', '', 1, 0, 'C', '0', '0', 'system:statusReport:list', 'upload', 'system', NOW(), '教育部状态数据字段映射与报盘导出'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3150);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3151, '上报查询', 3150, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:statusReport:query', '#', 'system', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3151);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3152, '批次生成', 3150, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:statusReport:generate', '#', 'system', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3152);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3153, '报盘导出', 3150, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:statusReport:export', '#', 'system', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3153);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3154, '批次作废', 3150, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:statusReport:remove', '#', 'system', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3154);

-- 授权：教务处管理员（role 3）可见并使用；超级管理员默认全量
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 3, m.menu_id FROM sys_menu m
WHERE m.menu_id IN (3150, 3151, 3152, 3153, 3154)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 3 AND rm.menu_id = m.menu_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 4, m.menu_id FROM sys_menu m
WHERE m.menu_id IN (3150, 3151)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 4 AND rm.menu_id = m.menu_id);

-- ----------------------------
-- 6、验证
-- ----------------------------
SELECT '上报批次表' AS item, COUNT(1) AS cnt FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_status_report_batch'
UNION ALL SELECT '上报字段映射行数', COUNT(1) FROM sys_status_report_field
UNION ALL SELECT '上报字典明细', COUNT(1) FROM sys_dict_data WHERE dict_type IN ('sys_status_report_type', 'sys_status_report_status')
UNION ALL SELECT '上报菜单', COUNT(1) FROM sys_menu WHERE menu_id BETWEEN 3150 AND 3154;
