-- =====================================================================================
-- 回滚：V20261024.4 P2 残项告警参数（删除抽签耗时/超卖尝试两个 sys_config 参数）
-- 说明：仅删参数；阈值缺省值已内置于 MetricsAlertService（抽签 30 秒 / 超卖 20 次每轮），
--       删除后代码仍按缺省判定，行为不报错。resourceAlertTask job 为既有项，不在此删除。
-- 幂等性：是（按 config_key 精确删除，无则不报错）
-- =====================================================================================

DELETE FROM sys_config WHERE config_key IN ('sys.observability.alert.lotteryMaxSeconds', 'sys.observability.alert.oversellCount');
