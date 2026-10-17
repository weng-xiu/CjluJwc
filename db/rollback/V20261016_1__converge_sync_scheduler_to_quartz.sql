-- =====================================================================================
-- 回滚：V20261016.1 数据对接同步任务调度收敛为 Quartz 唯一入口（G7 / D9）
-- 说明：与 db/migration/V20261016_1__converge_sync_scheduler_to_quartz.sql 反向对应。
--       仅删除本批注册的 sys_job 行；SyncTaskScheduler 的 @Scheduled 移除属代码变更，
--       不在本回滚范围内（如需完全回退，须同时恢复 Java 代码的 @Scheduled 注解）。
-- 影响：删除 invoke_target=syncTaskJob.scanAndExecute() 的定时任务。
-- =====================================================================================

-- 1、删除同步调度 job
DELETE FROM sys_job WHERE invoke_target = 'syncTaskJob.scanAndExecute()';

-- 2、验证（回滚后应为 0）
SELECT '同步调度job' AS item, COUNT(1) AS cnt FROM sys_job WHERE invoke_target = 'syncTaskJob.scanAndExecute()';
