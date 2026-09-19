-- ===============================================
-- phase11：A4 成绩权重配置化 建表 + 菜单
-- 本脚本幂等，可重复执行
-- 关联需求：长江大学教务管理系统需求优化计划 A4
-- ===============================================

-- ----------------------------
-- 1. 成绩权重配置表（平时成绩/考试成绩占比）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `aem_grade_weight` (
  `weight_id` bigint NOT NULL AUTO_INCREMENT COMMENT '权重ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID（关联tpm_course_library，NULL表示非课程级）',
  `course_category` varchar(50) DEFAULT NULL COMMENT '课程类别（关联字典tpm_course_category，NULL表示全局默认）',
  `regular_ratio` decimal(5,2) NOT NULL DEFAULT '30.00' COMMENT '平时成绩占比（%）',
  `exam_ratio` decimal(5,2) NOT NULL DEFAULT '70.00' COMMENT '考试成绩占比（%）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`weight_id`),
  KEY `idx_grade_weight_course` (`course_id`),
  KEY `idx_grade_weight_category` (`course_category`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成绩权重配置表（A4）';

-- ----------------------------
-- 2. 菜单：成绩权重配置（挂在 2205 成绩管理 下，order_num=5 避开 2600 GPA配置）
--    ID 段 2920-2925（2910+ 未被任何脚本占用）
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2920, '成绩权重配置', 2205, 5, 'gradeWeight', 'aem/gradeWeight/index', '', '', 1, 0, 'C', '0', '0', 'aem:gradeWeight:list', 'slider', 'admin', sysdate(), '', null, '成绩权重配置菜单（A4）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2920);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2921, '权重查询', 2920, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gradeWeight:query', '#', 'admin', sysdate(), '', null, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2921);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2922, '权重新增', 2920, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gradeWeight:add', '#', 'admin', sysdate(), '', null, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2922);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2923, '权重修改', 2920, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gradeWeight:edit', '#', 'admin', sysdate(), '', null, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2923);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2924, '权重删除', 2920, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gradeWeight:remove', '#', 'admin', sysdate(), '', null, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2924);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2925, '权重导出', 2920, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'aem:gradeWeight:export', '#', 'admin', sysdate(), '', null, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2925);

-- ----------------------------
-- 3. 角色授权：凡是拥有 2206（成绩记录）菜单的角色，同步授予 2920-2925
-- ----------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, m.menu_id
FROM sys_role_menu rm
JOIN (SELECT 2920 AS menu_id UNION SELECT 2921 UNION SELECT 2922 UNION SELECT 2923 UNION SELECT 2924 UNION SELECT 2925) m
WHERE rm.menu_id = 2206
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = m.menu_id);

-- ----------------------------
-- 4. 验证
-- ----------------------------
SELECT menu_id, menu_name, parent_id, perms FROM sys_menu WHERE menu_id BETWEEN 2920 AND 2925;
SHOW CREATE TABLE aem_grade_weight;
