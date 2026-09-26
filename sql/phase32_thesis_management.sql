-- =============================================================
-- Phase 32：毕业论文（设计）全过程管理（合规缺口补齐）
-- 覆盖：
--   1) sam_thesis_topic  选题库（题目来源、指导教师、容量、审核状态）
--   2) sam_thesis        论文全过程主表（选题→开题→中期→查重→答辩→成绩归档）
--   3) sam_thesis_process 环节留痕（提交、审核、成绩、抽检，一人多环节可追溯）
--   4) 字典 sam_thesis_stage / sam_thesis_stage_status / sam_thesis_topic_source
--      sam_thesis_topic_status / sam_thesis_grade / sam_thesis_sample_status
--   5) 菜单：管理端「毕业论文管理」目录（选题库、论文过程）+ 门户「毕业论文」
--   6) 学位审核接线说明：本表落库后，SamDegreeReviewServiceImpl.autoReview 的
--      论文分项改为读取 sam_thesis.is_qualified（require_thesis=1 时生效），
--      不再恒置合格。
-- 菜单ID：现有最大为 3100，本脚本使用 3110—3142 段。
-- 本脚本幂等，可重复执行。
-- =============================================================

-- ----------------------------
-- 1. 选题库
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sam_thesis_topic` (
  `topic_id`        bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '题目ID',
  `plan_year`       varchar(10)  NOT NULL                COMMENT '届别（如2026）',
  `topic_name`      varchar(200) NOT NULL                COMMENT '论文（设计）题目',
  `topic_source`    char(1)      DEFAULT '0'             COMMENT '题目来源（0教师科研课题 1生产社会实践 2学生自拟 3学科竞赛）',
  `major_id`        bigint(20)   DEFAULT NULL            COMMENT '适用专业ID',
  `dept_id`         bigint(20)   DEFAULT NULL            COMMENT '所属学院ID',
  `advisor`         varchar(50)  DEFAULT NULL            COMMENT '指导教师登录名',
  `advisor_name`    varchar(50)  DEFAULT NULL            COMMENT '指导教师姓名',
  `capacity`        int(4)       DEFAULT 1               COMMENT '可选题人数',
  `elected_count`   int(4)       DEFAULT 0               COMMENT '已选人数',
  `difficulty`      char(1)      DEFAULT '2'             COMMENT '难度（1基础 2中等 3较高）',
  `intro`           varchar(1000) DEFAULT NULL           COMMENT '题目简介与完成要求',
  `status`          char(1)      DEFAULT '0'             COMMENT '状态（0待审核 1可选题 2已选满 3已下架）',
  `audit_opinion`   varchar(500) DEFAULT NULL            COMMENT '审核意见',
  `create_by`       varchar(50)  DEFAULT ''              COMMENT '创建者',
  `create_time`     datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`       varchar(50)  DEFAULT ''              COMMENT '更新者',
  `update_time`     datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`          varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`topic_id`),
  KEY `idx_topic_year` (`plan_year`),
  KEY `idx_topic_advisor` (`advisor`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='毕业论文选题库';

-- ----------------------------
-- 2. 论文全过程主表（一名学生一届一条记录，含最终成绩与合格结论）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sam_thesis` (
  `thesis_id`       bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '论文ID',
  `plan_year`       varchar(10)  NOT NULL                COMMENT '届别（如2026）',
  `student_id`      bigint(20)   NOT NULL                COMMENT '学生ID（sam_student）',
  `topic_id`        bigint(20)   DEFAULT NULL            COMMENT '选题库题目ID（自拟题目为空）',
  `topic_name`      varchar(200) DEFAULT NULL            COMMENT '论文题目快照',
  `advisor`         varchar(50)  DEFAULT NULL            COMMENT '指导教师登录名',
  `advisor_name`    varchar(50)  DEFAULT NULL            COMMENT '指导教师姓名',
  `current_stage`   char(1)      NOT NULL DEFAULT '1'    COMMENT '当前环节（1选题 2开题 3中期检查 4查重 5答辩 6成绩归档）',
  `stage_status`    char(1)      NOT NULL DEFAULT '0'    COMMENT '当前环节状态（0待提交 1待审核 2已通过 3已退回）',
  `check_rate`      decimal(5,2) DEFAULT NULL            COMMENT '查重重复率（%）',
  `check_pass`      char(1)      DEFAULT NULL            COMMENT '查重是否达标（0否 1是）',
  `defense_score`   decimal(5,2) DEFAULT NULL            COMMENT '答辩成绩',
  `total_score`     decimal(5,2) DEFAULT NULL            COMMENT '总评成绩',
  `grade_level`     char(1)      DEFAULT NULL            COMMENT '成绩等级（0优秀 1良好 2中等 3及格 4不及格）',
  `is_qualified`    char(1)      DEFAULT '0'             COMMENT '论文是否合格（0否 1是）——学位审核读取该字段',
  `sample_status`   char(1)      DEFAULT '0'             COMMENT '抽检状态（0未抽检 1已送抽检待结果 2抽检合格 3抽检不合格）',
  `archive_time`    datetime     DEFAULT NULL            COMMENT '成绩归档时间',
  `status`          char(1)      DEFAULT '0'             COMMENT '记录状态（0正常 1作废）',
  `create_by`       varchar(50)  DEFAULT ''              COMMENT '创建者',
  `create_time`     datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`       varchar(50)  DEFAULT ''              COMMENT '更新者',
  `update_time`     datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`          varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`thesis_id`),
  UNIQUE KEY `uk_thesis_student_year` (`student_id`, `plan_year`),
  KEY `idx_thesis_advisor` (`advisor`),
  KEY `idx_thesis_stage` (`current_stage`, `stage_status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='毕业论文（设计）全过程主表';

-- ----------------------------
-- 3. 环节留痕（提交与审核过程记录，支撑教育部毕业论文抽检数据对接）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sam_thesis_process` (
  `process_id`      bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `thesis_id`       bigint(20)   NOT NULL                COMMENT '论文ID',
  `stage`           char(1)      NOT NULL                COMMENT '环节（1选题 2开题 3中期检查 4查重 5答辩 6成绩归档）',
  `action`          varchar(20)  NOT NULL DEFAULT 'submit' COMMENT '动作（submit提交 audit审核 record登记）',
  `title`           varchar(200) DEFAULT NULL            COMMENT '材料/环节名称',
  `content`         varchar(2000) DEFAULT NULL           COMMENT '提交内容或审核结论说明',
  `attachment`      varchar(500) DEFAULT NULL            COMMENT '附件地址（报告/查重单/答辩记录）',
  `result`          char(1)      DEFAULT '2'             COMMENT '结果（0退回 1通过 2仅记录）',
  `score`           decimal(5,2) DEFAULT NULL            COMMENT '本环节成绩（如有）',
  `opinion`         varchar(500) DEFAULT NULL            COMMENT '意见',
  `operator`        varchar(50)  DEFAULT NULL            COMMENT '操作人登录名',
  `operator_name`   varchar(50)  DEFAULT NULL            COMMENT '操作人姓名',
  `operate_time`    datetime     DEFAULT NULL            COMMENT '操作时间',
  `create_by`       varchar(50)  DEFAULT ''              COMMENT '创建者',
  `create_time`     datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`       varchar(50)  DEFAULT ''              COMMENT '更新者',
  `update_time`     datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`          varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`process_id`),
  KEY `idx_process_thesis` (`thesis_id`, `stage`),
  KEY `idx_process_operator` (`operator`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='毕业论文环节留痕表';

-- ----------------------------
-- 4. 字典
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '毕业论文环节', 'sam_thesis_stage', '0', 'admin', sysdate(), '毕业论文全过程六个环节'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='sam_thesis_stage');
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '毕业论文环节状态', 'sam_thesis_stage_status', '0', 'admin', sysdate(), '当前环节处理状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='sam_thesis_stage_status');
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '毕业论文题目来源', 'sam_thesis_topic_source', '0', 'admin', sysdate(), '选题库题目来源分类'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='sam_thesis_topic_source');
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '毕业论文选题状态', 'sam_thesis_topic_status', '0', 'admin', sysdate(), '选题库题目审核与可用状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='sam_thesis_topic_status');
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '毕业论文成绩等级', 'sam_thesis_grade', '0', 'admin', sysdate(), '毕业论文（设计）总评等级'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='sam_thesis_grade');
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '毕业论文抽检状态', 'sam_thesis_sample_status', '0', 'admin', sysdate(), '上级论文抽检报送与结果状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='sam_thesis_sample_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '选题', '1', 'sam_thesis_stage', '', 'primary', 'Y', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_stage' AND dict_value='1');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '开题', '2', 'sam_thesis_stage', '', 'primary', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_stage' AND dict_value='2');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '中期检查', '3', 'sam_thesis_stage', '', 'warning', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_stage' AND dict_value='3');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '查重', '4', 'sam_thesis_stage', '', 'warning', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_stage' AND dict_value='4');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '答辩', '5', 'sam_thesis_stage', '', 'success', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_stage' AND dict_value='5');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, '成绩归档', '6', 'sam_thesis_stage', '', 'info', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_stage' AND dict_value='6');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '待提交', '0', 'sam_thesis_stage_status', '', 'info', 'Y', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_stage_status' AND dict_value='0');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '待审核', '1', 'sam_thesis_stage_status', '', 'warning', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_stage_status' AND dict_value='1');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已通过', '2', 'sam_thesis_stage_status', '', 'success', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_stage_status' AND dict_value='2');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '已退回', '3', 'sam_thesis_stage_status', '', 'danger', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_stage_status' AND dict_value='3');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '教师科研课题', '0', 'sam_thesis_topic_source', '', 'primary', 'Y', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_topic_source' AND dict_value='0');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '生产社会实践', '1', 'sam_thesis_topic_source', '', 'success', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_topic_source' AND dict_value='1');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '学生自拟', '2', 'sam_thesis_topic_source', '', 'info', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_topic_source' AND dict_value='2');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '学科竞赛', '3', 'sam_thesis_topic_source', '', 'warning', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_topic_source' AND dict_value='3');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '待审核', '0', 'sam_thesis_topic_status', '', 'info', 'Y', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_topic_status' AND dict_value='0');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '可选题', '1', 'sam_thesis_topic_status', '', 'success', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_topic_status' AND dict_value='1');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已选满', '2', 'sam_thesis_topic_status', '', 'warning', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_topic_status' AND dict_value='2');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '已下架', '3', 'sam_thesis_topic_status', '', 'danger', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_topic_status' AND dict_value='3');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '优秀', '0', 'sam_thesis_grade', '', 'success', 'N', '0', 'admin', sysdate(), '总评≥90'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_grade' AND dict_value='0');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '良好', '1', 'sam_thesis_grade', '', 'primary', 'N', '0', 'admin', sysdate(), '80—89'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_grade' AND dict_value='1');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '中等', '2', 'sam_thesis_grade', '', 'warning', 'N', '0', 'admin', sysdate(), '70—79'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_grade' AND dict_value='2');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '及格', '3', 'sam_thesis_grade', '', 'info', 'N', '0', 'admin', sysdate(), '60—69'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_grade' AND dict_value='3');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '不及格', '4', 'sam_thesis_grade', '', 'danger', 'N', '0', 'admin', sysdate(), '<60'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_grade' AND dict_value='4');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '未抽检', '0', 'sam_thesis_sample_status', '', 'info', 'Y', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_sample_status' AND dict_value='0');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '已送抽检', '1', 'sam_thesis_sample_status', '', 'warning', 'N', '0', 'admin', sysdate(), '数据已报送，等待结果'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_sample_status' AND dict_value='1');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '抽检合格', '2', 'sam_thesis_sample_status', '', 'success', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_sample_status' AND dict_value='2');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '抽检不合格', '3', 'sam_thesis_sample_status', '', 'danger', 'N', '0', 'admin', sysdate(), '存在学术不端或质量不达标'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='sam_thesis_sample_status' AND dict_value='3');

-- ----------------------------
-- 5. 管理端菜单：毕业论文管理目录 + 选题库 + 论文过程
-- ----------------------------
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3110,'毕业论文管理',2300,3,'thesis-group','','','',1,0,'M','0','0','','education','admin',sysdate(),'',NULL,'毕业论文（设计）全过程管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3110);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3111,'选题库管理',3110,1,'thesisTopic','sam/thesisTopic/index','','',1,0,'C','0','0','sam:thesisTopic:list','#','admin',sysdate(),'',NULL,'毕业论文选题库'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3111);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3112,'题目查询',3111,1,'','','','',1,0,'F','0','0','sam:thesisTopic:query','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3112);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3113,'题目新增',3111,2,'','','','',1,0,'F','0','0','sam:thesisTopic:add','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3113);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3114,'题目修改',3111,3,'','','','',1,0,'F','0','0','sam:thesisTopic:edit','#','admin',sysdate(),'',NULL,'含题目审核上架'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3114);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3115,'题目删除',3111,4,'','','','',1,0,'F','0','0','sam:thesisTopic:remove','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3115);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3116,'题目导出',3111,5,'','','','',1,0,'F','0','0','sam:thesisTopic:export','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3116);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3120,'论文过程管理',3110,2,'thesis','sam/thesis/index','','',1,0,'C','0','0','sam:thesis:list','#','admin',sysdate(),'',NULL,'开题/中期/查重/答辩/成绩归档'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3120);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3121,'论文查询',3120,1,'','','','',1,0,'F','0','0','sam:thesis:query','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3121);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3122,'论文登记',3120,2,'','','','',1,0,'F','0','0','sam:thesis:add','#','admin',sysdate(),'',NULL,'管理员代学生建立论文档案'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3122);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3123,'论文修改',3120,3,'','','','',1,0,'F','0','0','sam:thesis:edit','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3123);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3124,'论文删除',3120,4,'','','','',1,0,'F','0','0','sam:thesis:remove','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3124);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3125,'论文导出',3120,5,'','','','',1,0,'F','0','0','sam:thesis:export','#','admin',sysdate(),'',NULL,'含抽检数据导出'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3125);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3126,'环节审核',3120,6,'','','','',1,0,'F','0','0','sam:thesis:audit','#','admin',sysdate(),'',NULL,'开题/中期/查重/答辩审核与成绩归档'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3126);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3127,'抽检管理',3120,7,'','','','',1,0,'F','0','0','sam:thesis:sample','#','admin',sysdate(),'',NULL,'送检与抽检结果回填'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3127);

-- ----------------------------
-- 6. 门户菜单：学生/教师毕业论文入口
-- ----------------------------
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3140,'毕业论文',2501,10,'thesis','portal/thesis/index','','',1,0,'C','0','0','portal:thesis:list','#','admin',sysdate(),'',NULL,'学生选题与过程提交、教师指导审核'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3140);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3141,'论文过程提交',3140,1,'','','','',1,0,'F','0','0','portal:thesis:submit','#','admin',sysdate(),'',NULL,'学生选题与材料提交'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3141);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3142,'论文指导审核',3140,2,'','','','',1,0,'F','0','0','portal:thesis:audit','#','admin',sysdate(),'',NULL,'指导教师环节审核'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3142);

-- ----------------------------
-- 7. 角色授权
--    管理端：超级管理员(1)、教务处管理员(3)、学院管理员(4)、教学秘书(5)、教师(6)
--    门户端：学生(7)、教师(6)、超级管理员(1)
-- ----------------------------
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT r.role_id, m.menu_id FROM
  (SELECT 1 AS role_id UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) r
  CROSS JOIN (SELECT 3110 AS menu_id UNION ALL SELECT 3111 UNION ALL SELECT 3112 UNION ALL SELECT 3113
              UNION ALL SELECT 3114 UNION ALL SELECT 3115 UNION ALL SELECT 3116 UNION ALL SELECT 3120
              UNION ALL SELECT 3121 UNION ALL SELECT 3122 UNION ALL SELECT 3123 UNION ALL SELECT 3124
              UNION ALL SELECT 3125 UNION ALL SELECT 3126 UNION ALL SELECT 3127) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=r.role_id AND rm.`menu_id`=m.menu_id);

INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT r.role_id, m.menu_id FROM
  (SELECT 1 AS role_id UNION ALL SELECT 6 UNION ALL SELECT 7) r
  CROSS JOIN (SELECT 3140 AS menu_id UNION ALL SELECT 3141 UNION ALL SELECT 3142) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=r.role_id AND rm.`menu_id`=m.menu_id);

-- ----------------------------
-- 8. 示例选题（仅当选题库为空时写入，便于功能验证）
-- ----------------------------
INSERT INTO sam_thesis_topic
  (plan_year, topic_name, topic_source, major_id, dept_id, advisor, advisor_name, capacity, elected_count, difficulty, intro, status, create_by, create_time, remark)
SELECT * FROM (
  SELECT '2026' plan_year, '基于Spring Cloud的教务微服务性能优化研究' topic_name, '0' topic_source, NULL major_id, NULL dept_id,
         'admin' advisor, '示例指导教师' advisor_name, 2 capacity, 0 elected_count, '3' difficulty,
         '要求完成链路压测与优化方案对比，提交可复现实验数据' intro, '1' status, 'admin' create_by, sysdate() create_time, '示例数据' remark
  UNION ALL
  SELECT '2026', '高校毕业论文全过程管理机制与系统设计', '0', NULL, NULL, 'admin', '示例指导教师', 2, 0, '2',
         '调研毕业论文选题—开题—查重—答辩全流程，输出设计方案', '1', 'admin', sysdate(), '示例数据'
  UNION ALL
  SELECT '2026', '本科生学业预警干预策略实证研究', '1', NULL, NULL, 'admin', '示例指导教师', 1, 0, '2',
         '结合预警与帮扶数据开展实证分析', '1', 'admin', sysdate(), '示例数据'
) t
WHERE (SELECT COUNT(1) FROM sam_thesis_topic) = 0;

-- ----------------------------
-- 验证
-- ----------------------------
SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME IN ('sam_thesis','sam_thesis_topic','sam_thesis_process') ORDER BY TABLE_NAME;
SELECT dict_type, COUNT(1) FROM sys_dict_data
  WHERE dict_type LIKE 'sam_thesis%' GROUP BY dict_type ORDER BY dict_type;
SELECT menu_id, menu_name, menu_type, perms FROM sys_menu WHERE menu_id BETWEEN 3110 AND 3142 ORDER BY menu_id;
