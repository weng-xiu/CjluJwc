-- ===============================================
-- 第二期 P1 升级脚本：A5 成绩提交与锁定 / S3 学位条件配置化 / T6 抽签机制 / A1+A2 考试编排与冲突检测
-- 幂等：列/索引用 information_schema + PREPARE；建表用 CREATE TABLE IF NOT EXISTS；菜单用 INSERT ... WHERE NOT EXISTS
-- 依赖：aem_grade_record / tpm_selection_round / tpm_selection_enrollment 已存在
-- 菜单ID：现有最大为 2906，本脚本使用 2950+ 段避免冲突
-- ===============================================

-- ----------------------------
-- 1. A5：aem_grade_record 增加成绩提交/锁定相关列
-- ----------------------------
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'aem_grade_record' AND column_name = 'submit_status');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `aem_grade_record` ADD COLUMN `submit_status` char(1) DEFAULT ''0'' COMMENT ''提交状态（0未提交 1已提交待审 2已锁定 3已驳回）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'aem_grade_record' AND column_name = 'submit_by');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `aem_grade_record` ADD COLUMN `submit_by` varchar(64) DEFAULT NULL COMMENT ''提交人''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'aem_grade_record' AND column_name = 'submit_time');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `aem_grade_record` ADD COLUMN `submit_time` datetime DEFAULT NULL COMMENT ''提交时间''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'aem_grade_record' AND column_name = 'lock_time');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `aem_grade_record` ADD COLUMN `lock_time` datetime DEFAULT NULL COMMENT ''锁定时间''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 提交状态检索索引
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'aem_grade_record' AND index_name = 'idx_grade_submit_status');
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `aem_grade_record` ADD INDEX `idx_grade_submit_status` (`submit_status`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 2. T6：tpm_selection_round 增加随机种子/抽签时间列（可复现审计）
-- ----------------------------
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tpm_selection_round' AND column_name = 'lottery_seed');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `tpm_selection_round` ADD COLUMN `lottery_seed` bigint DEFAULT NULL COMMENT ''抽签随机种子（可复现审计）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tpm_selection_round' AND column_name = 'lottery_time');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `tpm_selection_round` ADD COLUMN `lottery_time` datetime DEFAULT NULL COMMENT ''抽签执行时间''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 3. T6：tpm_selection_enrollment 增加候补排名列（落选按序递补）
-- ----------------------------
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tpm_selection_enrollment' AND column_name = 'waitlist_rank');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `tpm_selection_enrollment` ADD COLUMN `waitlist_rank` int DEFAULT NULL COMMENT ''候补排名（抽签落选后的递补顺序，null表示非候补）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 4. S3：新建学位审核条件配置表 sam_degree_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sam_degree_config` (
  `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_name` varchar(100) NOT NULL COMMENT '配置名称',
  `gpa_threshold` double DEFAULT '2' COMMENT 'GPA门槛',
  `require_degree_course` char(1) DEFAULT '1' COMMENT '是否要求学位课无不及格（0否 1是）',
  `require_foreign_language` char(1) DEFAULT '0' COMMENT '是否要求外语无不及格（0否 1是）',
  `require_thesis` char(1) DEFAULT '0' COMMENT '是否要求论文合格（0否 1是，需对接论文系统）',
  `require_achievement` char(1) DEFAULT '0' COMMENT '是否要求学术成果（0否 1是，需对接成果数据）',
  `is_default` char(1) DEFAULT '0' COMMENT '是否默认生效配置（0否 1是）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学位审核条件配置';

-- 内置默认配置（GPA 2.0、要求学位课无不及格，其余不强制，保持历史口径）
INSERT INTO `sam_degree_config`
  (`config_name`,`gpa_threshold`,`require_degree_course`,`require_foreign_language`,`require_thesis`,`require_achievement`,`is_default`,`status`,`remark`,`create_by`,`create_time`)
SELECT '默认学位审核条件',2.0,'1','0','0','0','1','0','系统内置默认口径：GPA≥2.0且学位课无不及格','admin',sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sam_degree_config` WHERE `is_default`='1');

-- ----------------------------
-- 5. 菜单权限：A5 成绩提交/审核/解锁（parent=2206 成绩记录）
-- ----------------------------
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2950,'成绩提交',2206,6,'','','','',1,0,'F','0','0','aem:gradeRecord:submit','#','admin',sysdate(),'',NULL,'A5 教师提交成绩'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2950);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2951,'成绩审核锁定',2206,7,'','','','',1,0,'F','0','0','aem:gradeRecord:audit','#','admin',sysdate(),'',NULL,'A5 教研室审核并锁定'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2951);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2952,'成绩解锁',2206,8,'','','','',1,0,'F','0','0','aem:gradeRecord:unlock','#','admin',sysdate(),'',NULL,'A5 解锁回退至驳回'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2952);

-- ----------------------------
-- 6. 菜单权限：A1 考试自动编排（parent=2202 考试安排）；A2 冲突检测复用 list 权限
-- ----------------------------
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2953,'自动编排',2202,6,'','','','',1,0,'F','0','0','aem:examPlan:arrange','#','admin',sysdate(),'',NULL,'A1 考试自动编排'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2953);

-- ----------------------------
-- 7. 菜单权限：S3 学位条件配置（新增菜单 parent=2305 学位管理 + CRUD 按钮）
-- ----------------------------
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2960,'学位条件配置',2305,9,'degreeConfig','sam/degreeConfig/index','','',1,0,'C','0','0','sam:degreeConfig:list','setting','admin',sysdate(),'',NULL,'S3 学位审核条件配置化'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2960);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2961,'配置查询',2960,1,'','','','',1,0,'F','0','0','sam:degreeConfig:query','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2961);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2962,'配置新增',2960,2,'','','','',1,0,'F','0','0','sam:degreeConfig:add','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2962);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2963,'配置修改',2960,3,'','','','',1,0,'F','0','0','sam:degreeConfig:edit','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2963);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 2964,'配置删除',2960,4,'','','','',1,0,'F','0','0','sam:degreeConfig:remove','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=2964);

-- ----------------------------
-- 8. 授权：超级管理员(role_id=1) 获得本批全部新增菜单/按钮
-- ----------------------------
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1,m.menu_id FROM (
  SELECT 2950 AS menu_id UNION ALL SELECT 2951 UNION ALL SELECT 2952 UNION ALL SELECT 2953
  UNION ALL SELECT 2960 UNION ALL SELECT 2961 UNION ALL SELECT 2962 UNION ALL SELECT 2963 UNION ALL SELECT 2964
) m WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=1 AND rm.`menu_id`=m.menu_id);
