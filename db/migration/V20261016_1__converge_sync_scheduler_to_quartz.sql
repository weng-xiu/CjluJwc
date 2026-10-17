-- =====================================================================================
-- 版本：V20261016.1
-- 标题：数据对接同步任务调度收敛为 Quartz 唯一入口（G7 / D9 调度双轨）
-- 目的：yu-dis 的 SyncTaskScheduler 原以 Spring @Scheduled(fixedDelay=60s) 周期扫描到期同步任务，
--       与 Quartz（ry_task 体系）双轨并存——任务入口不统一、执行日志/启停无法在定时任务管理页统一治理，
--       存在监控盲区。G7 已移除该类 @Scheduled（及冗余的 @EnableScheduling 配置），改由 Quartz 统一调度：
--       本批注册 sys_job，invoke_target=syncTaskJob.scanAndExecute()，按 cron 每分钟触发一次。
-- 做法：sys_job 幂等写入（invoke_target 判重）；bean 名调用无需类白名单（同 resourceAlertTask.check() 约定）。
-- 影响表：追加 sys_job 一行。
-- 兼容：不改 SyncTaskScheduler 内部逻辑，仅切换触发方式；执行链路（扫描—触发—落库—留痕）与下次执行时间计算保持原样。
-- 关联事项：V5.0 §6.2 D9、§7.5 G7；后端 yu-dis com.yu.dis.schedule.SyncTaskScheduler（bean 名 syncTaskJob）
-- 幂等性：是（NOT EXISTS 判重）
-- 回滚脚本：db/rollback/V20261016_1__converge_sync_scheduler_to_quartz.sql
-- 执行说明：本机存量库按既有约定「人工应用 + 带回滚脚本」，FLYWAY_ENABLED 默认 false。
-- =====================================================================================

-- ----------------------------
-- 1、注册 Quartz 同步任务调度 job（每分钟扫描到期同步任务；禁止并发避免同轮重复触发）
-- ----------------------------
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '数据对接同步任务调度', 'SYSTEM', 'syncTaskJob.scanAndExecute()', '0 0/1 * * * ?', '3', '1', '0', 'system', NOW(),
       'G7 调度双轨收敛：yu-dis 同步任务到期扫描由 Quartz 统一调度（原 Spring @Scheduled 已移除）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'syncTaskJob.scanAndExecute()');

-- ----------------------------
-- 2、验证
-- ----------------------------
SELECT '同步调度job' AS item, COUNT(1) AS cnt FROM sys_job WHERE invoke_target = 'syncTaskJob.scanAndExecute()';
