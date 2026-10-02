-- =============================================================================
-- Phase 36：F2 智能算法深化（后续优化方案 V3.0 F2 全部子项）
--
-- 覆盖：
--   F2-1 自动排课增强：教师禁排表 tpm_teacher_forbidden（排课硬约束）+ 多方案对比（策略参数）
--   F2-2 选课体验增强：志愿优先级权重抽签（enrollment.priority + round.lottery_mode）、
--        退改选窗口（round.allow_drop_adjust/drop_adjust_start/drop_adjust_end）、
--        弹性扩容（offering.elastic_enabled/elastic_max/elastic_step）
--   F2-3 成绩与学业预测：预测算法阈值参数（aem.prediction.*）+ 预测看板菜单
--
-- 幂等：建表 CREATE TABLE IF NOT EXISTS；列用 information_schema + PREPARE；
--       字典/参数/菜单用 INSERT ... WHERE NOT EXISTS；可重复执行。
-- 菜单 ID：教师禁排 3200-3205；成绩预测 3210-3211（3160-3183 已被 phase34 占用）。
-- =============================================================================

-- ----------------------------
-- 1、教师禁排时间片表（F2-1）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tpm_teacher_forbidden` (
  `forbidden_id`  bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '禁排ID',
  `semester_id`   bigint(20)  DEFAULT NULL            COMMENT '学期ID（NULL表示全学期通用禁排）',
  `teacher_id`    bigint(20)  NOT NULL                COMMENT '教师ID（关联brm_teacher）',
  `week_day`      int(1)      NOT NULL                COMMENT '星期几（1-7）',
  `start_period`  int(2)      NOT NULL                COMMENT '开始节次',
  `end_period`    int(2)      NOT NULL                COMMENT '结束节次',
  `reason`        varchar(200) DEFAULT NULL           COMMENT '禁排原因',
  `status`        char(1)     DEFAULT '0'             COMMENT '状态（0正常 1停用）',
  `del_flag`      char(1)     DEFAULT '0'             COMMENT '删除标志（0存在 2删除）',
  `create_by`     varchar(64) DEFAULT ''              COMMENT '创建者',
  `create_time`   datetime    DEFAULT NULL            COMMENT '创建时间',
  `update_by`     varchar(64) DEFAULT ''              COMMENT '更新者',
  `update_time`   datetime    DEFAULT NULL            COMMENT '更新时间',
  `remark`        varchar(500) DEFAULT NULL           COMMENT '备注',
  PRIMARY KEY (`forbidden_id`),
  KEY `idx_tf_teacher_sem` (`teacher_id`, `semester_id`),
  KEY `idx_tf_status` (`status`, `del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师禁排时间片表（F2-1）';

-- ----------------------------
-- 2、选课志愿优先级（F2-2）：tpm_selection_enrollment.priority
-- ----------------------------
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tpm_selection_enrollment' AND COLUMN_NAME='priority');
SET @sql = IF(@c=0,'ALTER TABLE `tpm_selection_enrollment` ADD COLUMN `priority` int(2) NULL DEFAULT NULL COMMENT ''志愿优先级（1=第一志愿，越小越优先；weighted抽签按此加权）'' AFTER `waitlist_rank`','SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ----------------------------
-- 3、抽签模式与退改选窗口（F2-2）：tpm_selection_round
-- ----------------------------
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tpm_selection_round' AND COLUMN_NAME='lottery_mode');
SET @sql = IF(@c=0,'ALTER TABLE `tpm_selection_round` ADD COLUMN `lottery_mode` varchar(20) NULL DEFAULT ''random'' COMMENT ''抽签模式（random公平随机/weighted志愿权重）'' AFTER `lottery_time`','SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tpm_selection_round' AND COLUMN_NAME='allow_drop_adjust');
SET @sql = IF(@c=0,'ALTER TABLE `tpm_selection_round` ADD COLUMN `allow_drop_adjust` char(1) NULL DEFAULT ''0'' COMMENT ''是否开放退改选窗口（0否 1是）'' AFTER `lottery_mode`','SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tpm_selection_round' AND COLUMN_NAME='drop_adjust_start');
SET @sql = IF(@c=0,'ALTER TABLE `tpm_selection_round` ADD COLUMN `drop_adjust_start` datetime NULL DEFAULT NULL COMMENT ''退改选窗口开始时间'' AFTER `allow_drop_adjust`','SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tpm_selection_round' AND COLUMN_NAME='drop_adjust_end');
SET @sql = IF(@c=0,'ALTER TABLE `tpm_selection_round` ADD COLUMN `drop_adjust_end` datetime NULL DEFAULT NULL COMMENT ''退改选窗口结束时间'' AFTER `drop_adjust_start`','SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ----------------------------
-- 4、弹性扩容（F2-2）：tpm_course_offering
-- ----------------------------
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tpm_course_offering' AND COLUMN_NAME='elastic_enabled');
SET @sql = IF(@c=0,'ALTER TABLE `tpm_course_offering` ADD COLUMN `elastic_enabled` char(1) NULL DEFAULT ''0'' COMMENT ''弹性扩容开关（0关闭 1开启）'' AFTER `offering_status`','SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tpm_course_offering' AND COLUMN_NAME='elastic_max');
SET @sql = IF(@c=0,'ALTER TABLE `tpm_course_offering` ADD COLUMN `elastic_max` int(6) NULL DEFAULT NULL COMMENT ''弹性扩容上限人数'' AFTER `elastic_enabled`','SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tpm_course_offering' AND COLUMN_NAME='elastic_step');
SET @sql = IF(@c=0,'ALTER TABLE `tpm_course_offering` ADD COLUMN `elastic_step` int(4) NULL DEFAULT NULL COMMENT ''弹性扩容步长人数'' AFTER `elastic_max`','SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ----------------------------
-- 5、参数字典（抽签模式 / 排课策略）
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '选课抽签模式', 'tpm_lottery_mode', '0', 'system', NOW(), 'F2-2 选课抽签模式'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'tpm_lottery_mode');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT * FROM (
  SELECT 1 AS a, '公平随机' AS b, 'random' AS c, 'tpm_lottery_mode' AS d, '' AS e, 'primary' AS f, 'Y' AS g, '0' AS h, 'system' AS i, NOW() AS j, '同种子可复现，不考虑志愿' AS k
  UNION ALL SELECT 2,'志愿权重','weighted','tpm_lottery_mode','','success','N','0','system',NOW(),'按志愿优先级加权，第一志愿优先中签') t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'tpm_lottery_mode' AND dict_value = 'random');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '排课候选策略', 'tpm_schedule_strategy', '0', 'system', NOW(), 'F2-1 自动排课多方案对比策略'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'tpm_schedule_strategy');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT * FROM (
  SELECT 1 AS a, '容量降序' AS b, 'capacity' AS c, 'tpm_schedule_strategy' AS d, '' AS e, 'primary' AS f, 'Y' AS g, '0' AS h, 'system' AS i, NOW() AS j, '默认，优先保障大班/热门课' AS k
  UNION ALL SELECT 2,'受限优先','constrainedFirst','tpm_schedule_strategy','','success','N','0','system',NOW(),'实践/实验室课先行占位') t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'tpm_schedule_strategy' AND dict_value = 'capacity');

-- ----------------------------
-- 6、系统参数（预测阈值）
-- ----------------------------
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT * FROM (
  SELECT '学业预测及格线' AS a, 'aem.prediction.lowScoreLine' AS b, '60' AS c, 'Y' AS d, 'system' AS e, NOW() AS f, '风险分级判定中"最近学期表现"的低分阈值（平均分低于此计风险）' AS g
  UNION ALL SELECT '学业预测下滑斜率阈值','aem.prediction.declineSlope','-3.0','Y','system',NOW(),'最小二乘趋势斜率低于此值判为成绩下滑（负值表示下行）') t
WHERE NOT EXISTS (SELECT 1 FROM sys_config c2 WHERE c2.config_key = t.b);

-- ----------------------------
-- 7、菜单与权限
--    教师禁排 3200-3205（父：2105 开课与排课管理）
--    成绩预测 3210-3211（父：2205 成绩管理）
-- ----------------------------
-- 7.1 教师禁排
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3200, '教师禁排', 2105, 5, 'teacherForbidden', 'tpm/teacherForbidden/index', '', '', 1, 0, 'C', '0', '0', 'tpm:teacherForbidden:list', 'ban', 'system', NOW(), 'F2-1 教师禁排时间片维护'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3200);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3201, '禁排查询', 3200, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:teacherForbidden:query', '#', 'system', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3201);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3202, '禁排新增', 3200, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:teacherForbidden:add', '#', 'system', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3202);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3203, '禁排修改', 3200, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:teacherForbidden:edit', '#', 'system', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3203);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3204, '禁排删除', 3200, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:teacherForbidden:remove', '#', 'system', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3204);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3205, '禁排导出', 3200, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'tpm:teacherForbidden:export', '#', 'system', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3205);

-- 7.2 成绩与学业预测看板
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3210, '学业预测', 2205, 8, 'gradePrediction', 'aem/gradePrediction/index', '', '', 1, 0, 'C', '0', '0', 'aem:gradePrediction:list', 'chart', 'system', NOW(), 'F2-3 成绩趋势预测/课程难度画像/风险看板'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3210);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3211, '预测查询', 3210, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gradePrediction:query', '#', 'system', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3211);

-- ----------------------------
-- 8、角色授权
--    拥有排课管理(2107)的角色 → 授予教师禁排 3200-3205
--    拥有成绩记录(2206)的角色 → 授予学业预测 3210-3211
-- ----------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, m.menu_id
FROM sys_role_menu rm
JOIN (SELECT 3200 AS menu_id UNION SELECT 3201 UNION SELECT 3202 UNION SELECT 3203 UNION SELECT 3204 UNION SELECT 3205) m
WHERE rm.menu_id = 2107
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = m.menu_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, m.menu_id
FROM sys_role_menu rm
JOIN (SELECT 3210 AS menu_id UNION SELECT 3211) m
WHERE rm.menu_id = 2206
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = m.menu_id);

-- ----------------------------
-- 9、验证
-- ----------------------------
SELECT '教师禁排表' AS item, COUNT(1) AS cnt FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tpm_teacher_forbidden'
UNION ALL SELECT 'enrollment.priority列', COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tpm_selection_enrollment' AND column_name = 'priority'
UNION ALL SELECT 'round.退改选列', COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tpm_selection_round' AND column_name IN ('lottery_mode','allow_drop_adjust','drop_adjust_start','drop_adjust_end')
UNION ALL SELECT 'offering.弹性列', COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tpm_course_offering' AND column_name IN ('elastic_enabled','elastic_max','elastic_step')
UNION ALL SELECT 'F2参数', COUNT(1) FROM sys_config WHERE config_key LIKE 'aem.prediction.%'
UNION ALL SELECT 'F2字典', COUNT(1) FROM sys_dict_data WHERE dict_type IN ('tpm_lottery_mode','tpm_schedule_strategy')
UNION ALL SELECT 'F2菜单', COUNT(1) FROM sys_menu WHERE menu_id IN (3200,3201,3202,3203,3204,3205,3210,3211);
