-- ===============================================
-- phase22：S7 证书与离校增强 数据库升级脚本（幂等）
-- 覆盖需求 S7：证书编号规则自动生成、补办流程、离校环节可配置并自动判定完成
-- 内容：
--   1) sam_certificate 增列 reissue_type/cert_source_id + 证书编号唯一索引
--   2) 新建 sam_cert_reissue_apply（证书补办申请）
--   3) 新建 sam_procedure_step（离校环节配置）、sam_procedure_item（学生环节办理明细）+ 默认四环节
--   4) 字典 sam_cert_reissue_type / sam_cert_reissue_status / sam_step_auto_check
--   5) 管理端菜单与按钮权限（证书生成、证书补办、离校环节配置、离校办理/自动判定），授权超级管理员
-- 幂等：列/索引用 information_schema + PREPARE；建表用 CREATE TABLE IF NOT EXISTS；字典/菜单用 INSERT ... WHERE NOT EXISTS
-- menu_id 采用 3020 段（3010 已被 phase21 占用）
-- 应用参数（application.yml，可选覆盖，均有默认值）：
--   sam.cert.schoolCode: 10489    # 证书编号前缀（院校码）
--   sam.cert.seqLen: 4            # 证书编号流水号位数
-- ===============================================

-- 1) sam_certificate 增列 + 编号唯一索引 ------------------------------
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sam_certificate' AND column_name = 'reissue_type');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `sam_certificate` ADD COLUMN `reissue_type` char(1) DEFAULT ''0'' COMMENT ''证书来源（0原始 1补办）'' AFTER `receiver`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sam_certificate' AND column_name = 'cert_source_id');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `sam_certificate` ADD COLUMN `cert_source_id` bigint DEFAULT NULL COMMENT ''来源证书ID（补办指向原证书）'' AFTER `reissue_type`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'sam_certificate' AND index_name = 'uk_cert_number');
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE `sam_certificate` ADD UNIQUE INDEX `uk_cert_number` (`cert_number`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 证书补办申请表 --------------------------------------------------
CREATE TABLE IF NOT EXISTS `sam_cert_reissue_apply` (
  `apply_id`      bigint       NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `student_id`    bigint       NOT NULL COMMENT '学生ID（关联 sam_student）',
  `orig_cert_id`  bigint       NOT NULL COMMENT '原证书ID（关联 sam_certificate）',
  `new_cert_id`   bigint       DEFAULT NULL COMMENT '补办生成的新证书ID（受理后回填）',
  `reason`        varchar(500) DEFAULT NULL COMMENT '补办原因',
  `apply_status`  char(1)      NOT NULL DEFAULT '0' COMMENT '申请状态（0待受理 1已补办 2已驳回）',
  `audit_by`      varchar(64)  DEFAULT NULL COMMENT '受理人',
  `audit_time`    datetime     DEFAULT NULL COMMENT '受理时间',
  `audit_opinion` varchar(500) DEFAULT NULL COMMENT '受理意见',
  `create_by`     varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
  `update_by`     varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
  `remark`        varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`apply_id`),
  KEY `idx_orig_cert` (`orig_cert_id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_status` (`apply_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='证书补办申请（S7）';

-- 3) 离校环节配置 + 办理明细表 ---------------------------------------
CREATE TABLE IF NOT EXISTS `sam_procedure_step` (
  `step_id`         bigint      NOT NULL AUTO_INCREMENT COMMENT '环节ID',
  `step_key`        varchar(50) NOT NULL COMMENT '环节编码（LIBRARY/FINANCE/DORMITORY/CARD 或自定义）',
  `step_name`       varchar(50) NOT NULL COMMENT '环节名称',
  `order_num`       int         DEFAULT 0 COMMENT '排序',
  `required_flag`   char(1)     DEFAULT '1' COMMENT '是否必办（0否 1是）',
  `auto_check_type` varchar(20) DEFAULT 'NONE' COMMENT '自动判定数据源（NONE手动/CARD一卡通/CERT_PICKUP证书发放/GRAD_REVIEW毕业审核/DEGREE_REVIEW学位审核/LEGACY旧字段列）',
  `status`          char(1)     DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `create_by`       varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`     datetime    DEFAULT NULL COMMENT '创建时间',
  `update_by`       varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`     datetime    DEFAULT NULL COMMENT '更新时间',
  `remark`          varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`step_id`),
  UNIQUE KEY `uk_step_key` (`step_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='离校环节配置（S7）';

CREATE TABLE IF NOT EXISTS `sam_procedure_item` (
  `item_id`       bigint      NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `procedure_id`  bigint      NOT NULL COMMENT '离校手续ID（关联 sam_graduation_procedure）',
  `student_id`    bigint      NOT NULL COMMENT '学生ID',
  `step_id`       bigint      NOT NULL COMMENT '环节ID（关联 sam_procedure_step）',
  `item_status`   char(1)     DEFAULT '0' COMMENT '办理状态（0未办 1已办）',
  `check_type`    char(1)     DEFAULT '0' COMMENT '判定方式（0人工登记 1自动判定）',
  `check_time`    datetime    DEFAULT NULL COMMENT '办理时间',
  `check_by`      varchar(64) DEFAULT NULL COMMENT '办理人/数据源',
  `create_time`   datetime    DEFAULT NULL COMMENT '创建时间',
  `update_time`   datetime    DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uk_proc_step` (`procedure_id`, `step_id`),
  KEY `idx_step` (`step_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生离校环节办理明细（S7）';

-- 默认四环节（与旧硬编码列一致，LEGACY 自动判定）
INSERT INTO `sam_procedure_step` (`step_key`,`step_name`,`order_num`,`required_flag`,`auto_check_type`,`status`,`create_by`,`create_time`)
SELECT 'LIBRARY','图书馆清还',1,'1','LEGACY','0','admin',sysdate() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sam_procedure_step` WHERE `step_key`='LIBRARY');
INSERT INTO `sam_procedure_step` (`step_key`,`step_name`,`order_num`,`required_flag`,`auto_check_type`,`status`,`create_by`,`create_time`)
SELECT 'FINANCE','财务结算',2,'1','LEGACY','0','admin',sysdate() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sam_procedure_step` WHERE `step_key`='FINANCE');
INSERT INTO `sam_procedure_step` (`step_key`,`step_name`,`order_num`,`required_flag`,`auto_check_type`,`status`,`create_by`,`create_time`)
SELECT 'DORMITORY','宿舍退宿',3,'1','LEGACY','0','admin',sysdate() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sam_procedure_step` WHERE `step_key`='DORMITORY');
INSERT INTO `sam_procedure_step` (`step_key`,`step_name`,`order_num`,`required_flag`,`auto_check_type`,`status`,`create_by`,`create_time`)
SELECT 'CARD','一卡通退还',4,'1','CARD','0','admin',sysdate() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sam_procedure_step` WHERE `step_key`='CARD');
-- 可选环节：证书领取（CERT_PICKUP 自动判定，默认非必办，供配置启用）
INSERT INTO `sam_procedure_step` (`step_key`,`step_name`,`order_num`,`required_flag`,`auto_check_type`,`status`,`create_by`,`create_time`)
SELECT 'CERT_PICKUP','证书领取',5,'0','CERT_PICKUP','1','admin',sysdate() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sam_procedure_step` WHERE `step_key`='CERT_PICKUP');

-- 4) 字典 ------------------------------------------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '证书来源', 'sam_cert_reissue_type', '0', 'admin', sysdate(), 'S7 证书原始/补办'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'sam_cert_reissue_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '原始', '0', 'sam_cert_reissue_type', '', 'primary', 'Y', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_cert_reissue_type' AND dict_value = '0');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '补办', '1', 'sam_cert_reissue_type', '', 'warning', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_cert_reissue_type' AND dict_value = '1');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '证书补办申请状态', 'sam_cert_reissue_status', '0', 'admin', sysdate(), 'S7 补办受理状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'sam_cert_reissue_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '待受理', '0', 'sam_cert_reissue_status', '', 'info', 'Y', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_cert_reissue_status' AND dict_value = '0');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '已补办', '1', 'sam_cert_reissue_status', '', 'success', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_cert_reissue_status' AND dict_value = '1');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已驳回', '2', 'sam_cert_reissue_status', '', 'danger', 'N', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_cert_reissue_status' AND dict_value = '2');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '离校环节自动判定', 'sam_step_auto_check', '0', 'admin', sysdate(), 'S7 离校环节数据源'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'sam_step_auto_check');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '手动登记', 'NONE', 'sam_step_auto_check', '', 'info', 'Y', '0', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_step_auto_check' AND dict_value = 'NONE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '一卡通退还', 'CARD', 'sam_step_auto_check', '', 'success', 'N', '0', 'admin', sysdate(), '按 sam_graduation_procedure.card_returned 判定'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_step_auto_check' AND dict_value = 'CARD');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '证书发放', 'CERT_PICKUP', 'sam_step_auto_check', '', 'success', 'N', '0', 'admin', sysdate(), '按 sam_certificate.is_issued 判定'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_step_auto_check' AND dict_value = 'CERT_PICKUP');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '毕业审核通过', 'GRAD_REVIEW', 'sam_step_auto_check', '', 'success', 'N', '0', 'admin', sysdate(), '按 sam_graduation_review.review_status=1 判定'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_step_auto_check' AND dict_value = 'GRAD_REVIEW');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '学位审核通过', 'DEGREE_REVIEW', 'sam_step_auto_check', '', 'success', 'N', '0', 'admin', sysdate(), '按 sam_degree_review.review_status=1 判定'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_step_auto_check' AND dict_value = 'DEGREE_REVIEW');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, '旧字段列', 'LEGACY', 'sam_step_auto_check', '', 'success', 'N', '0', 'admin', sysdate(), '按环节编码对应的旧列判定'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sam_step_auto_check' AND dict_value = 'LEGACY');

-- 5) 菜单与按钮权限 --------------------------------------------------
-- 5.1 证书管理(2308) 新增"生成证书"按钮
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3020,'生成证书',2308,6,'','','','',1,0,'F','0','0','sam:certificate:generate','#','admin',sysdate(),'',NULL,'S7a 批量生成证书'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3020);

-- 5.2 证书补办（parent=2305 学籍与学位管理目录）
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3021,'证书补办',2305,5,'certReissue','sam/certReissue/index','','',1,0,'C','0','0','sam:certReissue:list','refresh','admin',sysdate(),'',NULL,'S7b 证书补办申请与受理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3021);
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3022,'补办查询',3021,1,'','','','',1,0,'F','0','0','sam:certReissue:query','#','admin',sysdate(),'',NULL,'' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3022);
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3023,'补办申请',3021,2,'','','','',1,0,'F','0','0','sam:certReissue:add','#','admin',sysdate(),'',NULL,'' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3023);
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3024,'补办受理',3021,3,'','','','',1,0,'F','0','0','sam:certReissue:audit','#','admin',sysdate(),'',NULL,'受理通过/驳回' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3024);
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3025,'补办导出',3021,4,'','','','',1,0,'F','0','0','sam:certReissue:export','#','admin',sysdate(),'',NULL,'' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3025);
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3026,'补办删除',3021,5,'','','','',1,0,'F','0','0','sam:certReissue:remove','#','admin',sysdate(),'',NULL,'' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3026);

-- 5.3 离校环节配置（parent=2305）
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3030,'离校环节配置',2305,6,'procedureStep','sam/procedureStep/index','','',1,0,'C','0','0','sam:procedureStep:list','tree-table','admin',sysdate(),'',NULL,'S7c 离校环节可配置'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3030);
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3031,'环节查询',3030,1,'','','','',1,0,'F','0','0','sam:procedureStep:query','#','admin',sysdate(),'',NULL,'' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3031);
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3032,'环节新增',3030,2,'','','','',1,0,'F','0','0','sam:procedureStep:add','#','admin',sysdate(),'',NULL,'' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3032);
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3033,'环节修改',3030,3,'','','','',1,0,'F','0','0','sam:procedureStep:edit','#','admin',sysdate(),'',NULL,'' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3033);
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3034,'环节删除',3030,4,'','','','',1,0,'F','0','0','sam:procedureStep:remove','#','admin',sysdate(),'',NULL,'' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3034);
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3035,'环节导出',3030,5,'','','','',1,0,'F','0','0','sam:procedureStep:export','#','admin',sysdate(),'',NULL,'' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3035);

-- 5.4 离校手续(2309) 新增办理/初始化/自动判定按钮
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3040,'手续初始化',2309,6,'','','','',1,0,'F','0','0','sam:graduationProcedure:init','#','admin',sysdate(),'',NULL,'S7c 为毕业生生成手续与环节明细' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3040);
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3041,'环节办理',2309,7,'','','','',1,0,'F','0','0','sam:graduationProcedure:handle','#','admin',sysdate(),'',NULL,'S7c 人工勾选环节' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3041);
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3042,'自动判定',2309,8,'','','','',1,0,'F','0','0','sam:graduationProcedure:autoCheck','#','admin',sysdate(),'',NULL,'S7c 按数据源自动完成' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3042);

-- 授权：超级管理员(1)
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`) SELECT 1,m.menu_id FROM (
  SELECT 3020 menu_id UNION ALL SELECT 3021 UNION ALL SELECT 3022 UNION ALL SELECT 3023 UNION ALL SELECT 3024
  UNION ALL SELECT 3025 UNION ALL SELECT 3026 UNION ALL SELECT 3030 UNION ALL SELECT 3031 UNION ALL SELECT 3032
  UNION ALL SELECT 3033 UNION ALL SELECT 3034 UNION ALL SELECT 3035 UNION ALL SELECT 3040 UNION ALL SELECT 3041 UNION ALL SELECT 3042
) m WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.role_id=1 AND rm.menu_id=m.menu_id);

-- 验证
SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 3020 AND 3042 ORDER BY menu_id;
