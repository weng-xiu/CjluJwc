-- =============================================================================
-- Phase 37：Q3 可观测性（后续优化方案 V3.0 Q3 全部子项）
--
-- 覆盖：
--   Q3-1 应用监控：actuator + Micrometer Prometheus 注册表（代码/配置交付，本脚本仅登记配套参数）
--   Q3-2 业务埋点：@BizMetric + BizMetrics（选课尖峰/审核耗时、成败计数、预警触发计数）
--   Q3-3 告警闭环：新增 resourceAlertTask 运行态指标告警定时任务（JVM 堆水位 / HTTP 5xx 错误率），
--        复用系统通知公告(sys_notice)通道，与既有 JobFailureAlertService 共同构成告警闭环。
--
-- 幂等：参数与定时任务用 INSERT ... WHERE NOT EXISTS；可重复执行。
-- 依赖：resourceAlertTask 已由 yu-quartz 提供 Spring Bean（bean-name 调用，无需类白名单）。
-- =============================================================================

-- ----------------------------
-- 1、告警阈值参数（sys_config，运维可调整、免重启）
-- ----------------------------
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'JVM堆水位告警阈值', 'sys.observability.alert.heapPercent', '85', 'Y', 'system', NOW(),
       'Q3 告警：JVM 堆内存使用率百分比达到该值即告警（默认 85）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.observability.alert.heapPercent');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'HTTP错误率告警阈值', 'sys.observability.alert.httpErrorRatePercent', '5', 'Y', 'system', NOW(),
       'Q3 告警：HTTP 5xx 错误率百分比达到该值即告警（默认 5）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.observability.alert.httpErrorRatePercent');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '错误率最小样本数', 'sys.observability.alert.httpMinRequests', '50', 'Y', 'system', NOW(),
       'Q3 告警：累计请求样本数低于该值不做错误率判定，避免抖动误报（默认 50）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.observability.alert.httpMinRequests');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '指标告警冷却分钟', 'sys.observability.alert.cooldownMinutes', '15', 'Y', 'system', NOW(),
       'Q3 告警：同一指标两次告警的最小间隔分钟数，防止刷屏（默认 15）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.observability.alert.cooldownMinutes');

-- ----------------------------
-- 2、运行态指标告警定时任务（每 5 分钟一次）
--    status='0' 默认启用；misfire_policy='3' 错过不补跑；concurrent='1' 禁止并发
-- ----------------------------
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '运行态指标告警检查', 'SYSTEM', 'resourceAlertTask.check()', '0 0/5 * * * ?', '3', '1', '0', 'system', NOW(),
       'Q3 可观测性：JVM 堆水位/HTTP 5xx 错误率超阈值时写入系统通知公告'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'resourceAlertTask.check()');

-- ----------------------------
-- 3、验证
-- ----------------------------
SELECT 'Q3告警参数' AS item, COUNT(1) AS cnt FROM sys_config WHERE config_key LIKE 'sys.observability.alert.%'
UNION ALL SELECT 'Q3告警定时任务', COUNT(1) FROM sys_job WHERE invoke_target = 'resourceAlertTask.check()';
