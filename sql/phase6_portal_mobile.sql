-- ============================================================
-- 阶段六：移动端门户（选课/成绩/课表/预警）优化 - 数据库升级
-- 幂等脚本，可重复执行
-- ============================================================

-- ---------- 1. 补齐 del_flag 列 ----------
-- tpm_course_offering / tpm_selection_enrollment / tpm_schedule / tpm_selection_round
-- 这些表的管理端 Mapper 已引用 del_flag，但实际建表缺失，导致列表查询报错。
DROP PROCEDURE IF EXISTS portal_add_del_flag;
DELIMITER $$
CREATE PROCEDURE portal_add_del_flag(IN tbl VARCHAR(64))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = tbl AND column_name = 'del_flag'
    ) THEN
        SET @s = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN del_flag char(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0代表存在 2代表删除）''');
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;
CALL portal_add_del_flag('tpm_course_offering');
CALL portal_add_del_flag('tpm_selection_enrollment');
CALL portal_add_del_flag('tpm_schedule');
CALL portal_add_del_flag('tpm_selection_round');
CALL portal_add_del_flag('tpm_course_library');
CALL portal_add_del_flag('tpm_credit_structure');
CALL portal_add_del_flag('tpm_schedule_adjustment');
CALL portal_add_del_flag('tpm_selection_rule');
CALL portal_add_del_flag('tpm_training_plan');
DROP PROCEDURE IF EXISTS portal_add_del_flag;

-- ---------- 2. 学业预警门户菜单 ----------
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2541, '学业预警', 2501, 5, 'warning', 'portal/warning/index', '', '', 1, 0, 'C', '0', '0', 'portal:warning:list', 'warning', 'admin', NOW(), '', NULL, '移动端学业预警菜单');

INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2542, '预警查询', 2541, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'portal:warning:query', '#', 'admin', NOW(), '', NULL, '');

-- ---------- 3. 角色授权（角色7=学生） ----------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (7, 2541);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (7, 2542);
