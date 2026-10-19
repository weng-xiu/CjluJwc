-- =====================================================================================
-- 版本：V20261018.1
-- 标题：选课容量水位告警上线（V5.0 §7.4 P2 / 容量接近上限可感知）
-- 目的：选课尖峰期间热门开课容量水位无感知——满员/接近满员只能事后从名单页翻查。
--       P2 复用 resourceAlertTask 的告警通道（sys_notice + 冷却去重 + 参数化阈值），
--       新增 Quartz job：selectionCapacityTask.check()，每 5 分钟扫描"进行中"轮次，
--       开课已选人数达到阈值（默认 90%）或已满即发通知；超卖防护仍由原子扣减+分布式锁兜底。
-- 做法：sys_job / sys_config 幂等写入（invoke_target、config_key 判重）；
--       bean 名调用无需类白名单（同 resourceAlertTask.check() / syncTaskJob.scanAndExecute() 约定）。
-- 影响表：追加 sys_job 一行、sys_config 两行。
-- 兼容：无存量行为变更；阈值与冷却期可在参数管理页随时调整，无需发版。
-- 关联事项：V5.0 §7.4 P2；后端 yu-tpm com.yu.tpm.task.SelectionCapacityTask（bean 名 selectionCapacityTask）
-- 幂等性：是（NOT EXISTS 判重）
-- 回滚脚本：db/rollback/V20261018_1__add_selection_capacity_alert_job.sql
-- 执行说明：本机存量库按既有约定「人工应用 + 带回滚脚本」，FLYWAY_ENABLED 默认 false。
-- =====================================================================================

-- ----------------------------
-- 1、注册 Quartz 选课容量水位告警 job（每 5 分钟；禁止并发避免同轮重复告警）
-- ----------------------------
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '选课容量水位告警检查', 'SYSTEM', 'selectionCapacityTask.check()', '0 0/5 * * * ?', '3', '1', '0', 'system', NOW(),
       'P2 容量水位告警：扫描进行中选课轮次，开课容量使用率达阈值或满员即写系统通知（阈值/冷却期见参数配置 tpm.selection.alert.*）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'selectionCapacityTask.check()');

-- ----------------------------
-- 2、告警参数（阈值 90%、同轮次冷却 30 分钟，可在参数管理页调整）
-- ----------------------------
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '选课容量水位告警阈值(百分比)', 'tpm.selection.alert.capacityPercent', '90', 'Y', 'system', NOW(),
       'P2：进行中轮次内开课已选人数达到容量该百分比即计入水位告警明细'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'tpm.selection.alert.capacityPercent');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '选课容量告警冷却分钟数', 'tpm.selection.alert.cooldownMinutes', '30', 'Y', 'system', NOW(),
       'P2：同一选课轮次两次容量告警的最小间隔（分钟），防刷屏'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'tpm.selection.alert.cooldownMinutes');

-- ----------------------------
-- 3、验证
-- ----------------------------
SELECT '选课容量告警job' AS item, COUNT(1) AS cnt FROM sys_job WHERE invoke_target = 'selectionCapacityTask.check()'
UNION ALL SELECT '选课告警参数', COUNT(1) FROM sys_config WHERE config_key LIKE 'tpm.selection.alert.%';
