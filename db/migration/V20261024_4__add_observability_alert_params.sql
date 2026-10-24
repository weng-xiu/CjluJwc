-- =====================================================================================
-- 版本：V20261024.4
-- 标题：选课抽签耗时/超卖尝试告警参数上线（V5.0 §7.4 P2 残项）
-- 目的：P2「容量水位告警」主体已随 V5.0 §7.4 交付（selectionCapacityTask），本迁移补齐两个残项：
--       ①抽签耗时——单次全量抽签最大耗时超阈值（名单异常膨胀/慢查询）可感知；
--       ②超卖尝试——本轮窗口内「抢占已满课程名额」次数达阈值（热门课容量持续被击穿）可感知。
--       两项均由既有 resourceAlertTask.check()（每 5 分钟）→ MetricsAlertService 新增的
--       checkLotteryLatency()/checkOversell() 读取 Micrometer 埋点判定，无需新增 job。
-- 埋点来源：@BizMetric("lottery") 记录 yu.biz.duration{event=lottery}；
--           BizMetrics.recordOversell() 在选课/改选容量扣减触达负值处记录 yu.selection.oversell。
-- 做法：sys_config 幂等写入（config_key 判重）。bean 名调用沿用既有 resourceAlertTask 约定，无需类白名单。
-- 影响表：追加 sys_config 两行。
-- 兼容：无存量行为变更；阈值可在参数管理页随时调整，无需发版。缺省兜底：抽签 30 秒 / 超卖 20 次每轮。
-- 关联事项：V5.0 §7.4 P2；后端 yu-quartz com.yu.quartz.util.MetricsAlertService、
--           yu-common com.yu.common.monitor.BizMetrics、yu-tpm TpmSelectionEnrollmentServiceImpl
-- 幂等性：是（NOT EXISTS 判重）
-- 回滚脚本：db/rollback/V20261024_4__add_observability_alert_params.sql
-- 执行说明：本机存量库按既有约定「人工应用 + 带回滚脚本」，FLYWAY_ENABLED 默认 false。
-- =====================================================================================

-- ----------------------------
-- 抽签最大耗时告警阈值（秒）
-- ----------------------------
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '抽签最大耗时告警阈值(秒)', 'sys.observability.alert.lotteryMaxSeconds', '30', 'Y', 'system', NOW(),
       'P2：单次抽签最大耗时超过该秒数即写系统通知（读取 yu.biz.duration{event=lottery}，阈值缺省 30）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.observability.alert.lotteryMaxSeconds');

-- ----------------------------
-- 本轮窗口超卖尝试告警阈值（次）
-- ----------------------------
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '超卖尝试告警阈值(次/轮)', 'sys.observability.alert.oversellCount', '20', 'Y', 'system', NOW(),
       'P2：本轮检查窗口内选课超卖尝试增量达到该次数即写系统通知（读取 yu.selection.oversell 计数增量，阈值缺省 20）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.observability.alert.oversellCount');

-- ----------------------------
-- 验证
-- ----------------------------
SELECT 'P2残项告警参数' AS item, COUNT(1) AS cnt FROM sys_config
WHERE config_key IN ('sys.observability.alert.lotteryMaxSeconds', 'sys.observability.alert.oversellCount');
