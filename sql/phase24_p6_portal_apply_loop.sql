-- =============================================================
-- Phase 24：P6 门户申请流程闭环（异动/调停课）
-- 为审批状态字典 tpm_approve_status 增加 "3"=已撤销
-- （门户端申请人撤销调停课申请后落库 approve_status='3'，
--   管理端 yu-ui/src/views/tpm/adjust 页依赖该字典渲染）
-- 学籍异动页(sam/statusChange)使用前端内联字典，无需 SQL 变更。
-- 幂等：可重复执行。
-- =============================================================

-- 清理旧数据（幂等）
DELETE FROM sys_dict_data WHERE dict_type = 'tpm_approve_status' AND dict_value = '3';

-- 新增 已撤销
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
VALUES (9093, 4, '已撤销', '3', 'tpm_approve_status', '', 'info', 'N', '0', 'admin', sysdate());

-- 更新字典类型备注
UPDATE sys_dict_type SET remark = '0待审 1通过 2驳回 3已撤销' WHERE dict_type = 'tpm_approve_status';
