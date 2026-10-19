-- =====================================================================================
-- 回滚：V20261018.1 选课容量水位告警（删除 sys_job 与 tpm.selection.alert.* 参数）
-- 幂等性：是（按 invoke_target / config_key 精确删除，无则不报错）
-- =====================================================================================

DELETE FROM sys_job WHERE invoke_target = 'selectionCapacityTask.check()';

DELETE FROM sys_config WHERE config_key IN ('tpm.selection.alert.capacityPercent', 'tpm.selection.alert.cooldownMinutes');
