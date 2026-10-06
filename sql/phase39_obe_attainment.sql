-- =============================================================
-- Phase 39：N1 OBE 毕业要求达成度（产品最大空白格补齐）
-- 覆盖：
--   1) sam_obe_graduation_requirement  毕业要求（挂在培养方案下，含达成度目标值）
--   2) sam_obe_indicator_point         指标点（拆分毕业要求，含在毕业要求内的权重与目标值）
--   3) sam_obe_course_support          课程支撑矩阵（课程→指标点 支撑强度权重）
--   4) 达成度计算：指标点达成度 = Σ(课程归一化得分×支撑权重)/Σ支撑权重；
--      毕业要求达成度 = Σ(指标点达成度×指标点权重)/Σ指标点权重；达成 = 达成度 ≥ 目标值。
--      课程归一化得分取自 aem_grade_record.total_score/100（跨模块 JOIN，与 SamWarningDataMapper 同口径）。
--   5) 字典 obe_attain_status（达成判定）
--   6) 菜单：管理端「OBE达成度管理」目录（毕业要求、指标点、课程支撑矩阵、达成度看板）
-- 前置：N2 版本快照（tpm_training_plan.version/publish_status）已就绪，OBE 按 plan_id 绑定培养方案版本。
-- 菜单ID：现有最大为 3211，本脚本使用 3220—3259 段。
-- 本脚本幂等，可重复执行。
-- =============================================================

-- ----------------------------
-- 1. 毕业要求
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sam_obe_graduation_requirement` (
  `gr_id`         bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '毕业要求ID',
  `plan_id`       bigint(20)   NOT NULL                COMMENT '培养方案ID（tpm_training_plan）',
  `gr_code`       varchar(30)  NOT NULL                COMMENT '毕业要求编号（如GR-01）',
  `gr_name`       varchar(200) NOT NULL                COMMENT '毕业要求名称',
  `gr_desc`       varchar(1000) DEFAULT NULL           COMMENT '毕业要求描述',
  `target_value`  decimal(5,4) NOT NULL DEFAULT 0.7000 COMMENT '达成度目标值（0~1）',
  `sort_order`    int(4)       DEFAULT 0               COMMENT '排序',
  `status`        char(1)      DEFAULT '0'             COMMENT '状态（0正常 1停用）',
  `del_flag`      char(1)      DEFAULT '0'             COMMENT '删除标志（0存在 2删除）',
  `create_by`     varchar(50)  DEFAULT ''              COMMENT '创建者',
  `create_time`   datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`     varchar(50)  DEFAULT ''              COMMENT '更新者',
  `update_time`   datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`        varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`gr_id`),
  UNIQUE KEY `uk_gr_plan_code` (`plan_id`, `gr_code`),
  KEY `idx_gr_plan` (`plan_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='OBE毕业要求';

-- ----------------------------
-- 2. 指标点（拆分毕业要求）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sam_obe_indicator_point` (
  `ip_id`         bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '指标点ID',
  `gr_id`         bigint(20)   NOT NULL                COMMENT '所属毕业要求ID',
  `plan_id`       bigint(20)   NOT NULL                COMMENT '培养方案ID（冗余便于查询）',
  `ip_code`       varchar(30)  NOT NULL                COMMENT '指标点编号（如1.1）',
  `ip_name`       varchar(200) NOT NULL                COMMENT '指标点名称',
  `ip_desc`       varchar(1000) DEFAULT NULL           COMMENT '指标点描述',
  `weight`        decimal(6,3) NOT NULL DEFAULT 1.000  COMMENT '在毕业要求中的权重（>0）',
  `target_value`  decimal(5,4) NOT NULL DEFAULT 0.7000 COMMENT '达成度目标值（0~1）',
  `sort_order`    int(4)       DEFAULT 0               COMMENT '排序',
  `status`        char(1)      DEFAULT '0'             COMMENT '状态（0正常 1停用）',
  `del_flag`      char(1)      DEFAULT '0'             COMMENT '删除标志（0存在 2删除）',
  `create_by`     varchar(50)  DEFAULT ''              COMMENT '创建者',
  `create_time`   datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`     varchar(50)  DEFAULT ''              COMMENT '更新者',
  `update_time`   datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`        varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`ip_id`),
  UNIQUE KEY `uk_ip_gr_code` (`gr_id`, `ip_code`),
  KEY `idx_ip_gr` (`gr_id`),
  KEY `idx_ip_plan` (`plan_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='OBE指标点';

-- ----------------------------
-- 3. 课程支撑矩阵（课程→指标点）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sam_obe_course_support` (
  `support_id`      bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '支撑关系ID',
  `plan_id`         bigint(20)   NOT NULL                COMMENT '培养方案ID',
  `ip_id`           bigint(20)   NOT NULL                COMMENT '指标点ID',
  `course_id`       bigint(20)   NOT NULL                COMMENT '课程ID（tpm_course_library）',
  `support_weight`  decimal(6,3) NOT NULL DEFAULT 1.000  COMMENT '课程对该指标点的支撑强度权重（>0，如高3中2低1）',
  `sort_order`      int(4)       DEFAULT 0               COMMENT '排序',
  `status`          char(1)      DEFAULT '0'             COMMENT '状态（0正常 1停用）',
  `del_flag`        char(1)      DEFAULT '0'             COMMENT '删除标志（0存在 2删除）',
  `create_by`       varchar(50)  DEFAULT ''              COMMENT '创建者',
  `create_time`     datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`       varchar(50)  DEFAULT ''              COMMENT '更新者',
  `update_time`     datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`          varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`support_id`),
  UNIQUE KEY `uk_support_ip_course` (`ip_id`, `course_id`),
  KEY `idx_support_plan` (`plan_id`),
  KEY `idx_support_course` (`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='OBE课程支撑矩阵';

-- ----------------------------
-- 4. 字典：达成判定
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT 'OBE达成判定', 'obe_attain_status', '0', 'admin', sysdate(), '毕业要求/指标点达成度是否达标判定'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='obe_attain_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '未达成', '0', 'obe_attain_status', '', 'danger', 'N', '0', 'admin', sysdate(), '达成度 < 目标值'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='obe_attain_status' AND dict_value='0');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '达成', '1', 'obe_attain_status', '', 'success', 'N', '0', 'admin', sysdate(), '达成度 ≥ 目标值'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='obe_attain_status' AND dict_value='1');

-- ----------------------------
-- 5. 管理端菜单：OBE达成度管理目录 + 四功能页
-- ----------------------------
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3220,'OBE达成度管理',2300,4,'obe-group','','','',1,0,'M','0','0','','education','admin',sysdate(),'',NULL,'OBE毕业要求达成度管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3220);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3221,'毕业要求',3220,1,'obeRequirement','sam/obeRequirement/index','','',1,0,'C','0','0','sam:obeRequirement:list','#','admin',sysdate(),'',NULL,'毕业要求配置'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3221);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3222,'毕业要求查询',3221,1,'','','','',1,0,'F','0','0','sam:obeRequirement:query','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3222);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3223,'毕业要求新增',3221,2,'','','','',1,0,'F','0','0','sam:obeRequirement:add','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3223);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3224,'毕业要求修改',3221,3,'','','','',1,0,'F','0','0','sam:obeRequirement:edit','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3224);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3225,'毕业要求删除',3221,4,'','','','',1,0,'F','0','0','sam:obeRequirement:remove','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3225);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3230,'指标点',3220,2,'obeIndicator','sam/obeIndicator/index','','',1,0,'C','0','0','sam:obeIndicator:list','#','admin',sysdate(),'',NULL,'毕业要求指标点拆分'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3230);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3231,'指标点查询',3230,1,'','','','',1,0,'F','0','0','sam:obeIndicator:query','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3231);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3232,'指标点新增',3230,2,'','','','',1,0,'F','0','0','sam:obeIndicator:add','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3232);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3233,'指标点修改',3230,3,'','','','',1,0,'F','0','0','sam:obeIndicator:edit','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3233);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3234,'指标点删除',3230,4,'','','','',1,0,'F','0','0','sam:obeIndicator:remove','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3234);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3240,'课程支撑矩阵',3220,3,'obeSupport','sam/obeSupport/index','','',1,0,'C','0','0','sam:obeSupport:list','#','admin',sysdate(),'',NULL,'课程对指标点支撑关系'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3240);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3241,'支撑查询',3240,1,'','','','',1,0,'F','0','0','sam:obeSupport:query','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3241);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3242,'支撑新增',3240,2,'','','','',1,0,'F','0','0','sam:obeSupport:add','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3242);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3243,'支撑修改',3240,3,'','','','',1,0,'F','0','0','sam:obeSupport:edit','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3243);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3244,'支撑删除',3240,4,'','','','',1,0,'F','0','0','sam:obeSupport:remove','#','admin',sysdate(),'',NULL,''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3244);

INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3250,'达成度看板',3220,4,'obeAttainment','sam/obeAttainment/index','','',1,0,'C','0','0','sam:obeAttainment:list','chart','admin',sysdate(),'',NULL,'毕业要求达成度计算与看板'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3250);
INSERT INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
SELECT 3251,'达成度查询',3250,1,'','','','',1,0,'F','0','0','sam:obeAttainment:query','#','admin',sysdate(),'',NULL,'看板/计算'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id`=3251);

-- ----------------------------
-- 6. 角色授权：超级管理员(1)、教务处管理员(3)、学院管理员(4)、教学秘书(5)
-- ----------------------------
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT r.role_id, m.menu_id FROM
  (SELECT 1 AS role_id UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5) r
  CROSS JOIN (SELECT 3220 AS menu_id UNION ALL SELECT 3221 UNION ALL SELECT 3222 UNION ALL SELECT 3223
              UNION ALL SELECT 3224 UNION ALL SELECT 3225 UNION ALL SELECT 3230 UNION ALL SELECT 3231
              UNION ALL SELECT 3232 UNION ALL SELECT 3233 UNION ALL SELECT 3234 UNION ALL SELECT 3240
              UNION ALL SELECT 3241 UNION ALL SELECT 3242 UNION ALL SELECT 3243 UNION ALL SELECT 3244
              UNION ALL SELECT 3250 UNION ALL SELECT 3251) m
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=r.role_id AND rm.`menu_id`=m.menu_id);

-- ----------------------------
-- 验证
-- ----------------------------
SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME IN ('sam_obe_graduation_requirement','sam_obe_indicator_point','sam_obe_course_support') ORDER BY TABLE_NAME;
SELECT menu_id, menu_name, menu_type, perms FROM sys_menu WHERE menu_id BETWEEN 3220 AND 3259 ORDER BY menu_id;
