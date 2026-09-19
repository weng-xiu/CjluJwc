-- ===============================================
-- phase12：T3 培养方案发布状态字典补齐
-- 修复列表"发布状态"列空白（tpm_plan_publish_status 字典缺失）
-- 本脚本幂等，可重复执行
-- ===============================================

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '方案发布状态', 'tpm_plan_publish_status', '0', 'admin', sysdate(), '培养方案发布状态列表'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'tpm_plan_publish_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '草稿', '0', 'tpm_plan_publish_status', '', 'info', 'Y', '0', 'admin', sysdate(), '方案未发布'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'tpm_plan_publish_status' AND dict_value = '0');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '已发布', '1', 'tpm_plan_publish_status', '', 'success', 'N', '0', 'admin', sysdate(), '方案已发布锁定'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'tpm_plan_publish_status' AND dict_value = '1');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已废止', '2', 'tpm_plan_publish_status', '', 'danger', 'N', '0', 'admin', sysdate(), '方案已废止'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'tpm_plan_publish_status' AND dict_value = '2');

-- 验证
SELECT dict_label, dict_value, list_class FROM sys_dict_data WHERE dict_type = 'tpm_plan_publish_status' ORDER BY dict_sort;
