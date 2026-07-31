-- =====================================================================
-- BRM 基础信息管理模块字典
-- 学历层次 brm_education_level、年级 brm_grade
-- 通过系统自带字典模块管理，可在「系统管理 → 字典管理」维护
-- 可重复执行：先按 dict_type 清理再插入
-- =====================================================================

-- 清理旧数据（幂等）
DELETE FROM sys_dict_data WHERE dict_type IN ('brm_education_level', 'brm_grade');
DELETE FROM sys_dict_type WHERE dict_type IN ('brm_education_level', 'brm_grade');

-- 字典类型
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark) VALUES
(120, '学历层次', 'brm_education_level', '0', 'admin', sysdate(), '专业学历层次（专科/本科/硕士/博士）'),
(121, '年级',     'brm_grade',           '0', 'admin', sysdate(), '班级年级（入学年份）');

-- 学历层次字典数据
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES
(310, 1, '专科', '专科', 'brm_education_level', '', 'info',    'N', '0', 'admin', sysdate(), ''),
(311, 2, '本科', '本科', 'brm_education_level', '', 'primary', 'Y', '0', 'admin', sysdate(), ''),
(312, 3, '硕士', '硕士', 'brm_education_level', '', 'success', 'N', '0', 'admin', sysdate(), ''),
(313, 4, '博士', '博士', 'brm_education_level', '', 'warning', 'N', '0', 'admin', sysdate(), '');

-- 年级字典数据
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES
(314, 1, '2023级', '2023', 'brm_grade', '', 'default', 'N', '0', 'admin', sysdate(), ''),
(315, 2, '2024级', '2024', 'brm_grade', '', 'default', 'N', '0', 'admin', sysdate(), ''),
(316, 3, '2025级', '2025', 'brm_grade', '', 'default', 'N', '0', 'admin', sysdate(), ''),
(317, 4, '2026级', '2026', 'brm_grade', '', 'default', 'Y', '0', 'admin', sysdate(), '');
