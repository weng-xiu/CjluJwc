-- ============================================================
-- V20261101_2 —— 抽签任务表（T-5 异步化 + P-3 进度查询）
--
-- 解决的问题：
--   1) T-5/P-3 余面：抽签原本在 HTTP 请求线程里同步跑完（单事务 + 万级记录批量落库），
--      请求会长时间挂起，前端无法知道“跑到哪了”，也没有重试/审计载体。
--   2) 任务级幂等：同一轮次在任意时刻只允许存在一个「待执行/执行中」任务，
--      由生成列 active_round + 唯一索引在数据库层强制，比 Redis 锁更硬（锁会过期、会被误删）。
--   3) 锁的隐式依赖已在代码侧解决（DistributedLock 全链路原始字节 + 看门狗续期），
--      本表是第二道闸门：即使锁失效，重复任务也插不进来。
--
-- 状态机：'0' 待执行 → '1' 执行中 → '2' 成功 / '3' 失败
--   迁移一律用「带旧状态谓词的条件 update + affected rows」，不做 read-then-write。
--
-- 幂等：CREATE TABLE IF NOT EXISTS。生成列不参与业务读写，仅承载唯一约束。
-- 回滚：db/rollback/V20261101_2__add_lottery_task.sql（DROP TABLE，须先回滚应用版本）
-- ============================================================

CREATE TABLE IF NOT EXISTS `tpm_lottery_task` (
  `task_id` bigint NOT NULL AUTO_INCREMENT COMMENT '抽签任务ID',
  `round_id` bigint NOT NULL COMMENT '选课轮次ID',
  `task_status` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '任务状态（0待执行 1执行中 2成功 3失败）',
  `lottery_seed` bigint DEFAULT NULL COMMENT '本次抽签随机种子（成功后可复现审计）',
  `lottery_courses` int NOT NULL DEFAULT '0' COMMENT '参与抽签课程数（超容量需洗牌的课程数）',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '待抽签记录数（任务快照，非实时）',
  `processed_count` int NOT NULL DEFAULT '0' COMMENT '已落库记录数（进度快照，按分块推进）',
  `admitted_count` int NOT NULL DEFAULT '0' COMMENT '中签人数',
  `rejected_count` int NOT NULL DEFAULT '0' COMMENT '落选人数',
  `error_msg` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因（超长截断）',
  `start_time` datetime DEFAULT NULL COMMENT '开始执行时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间（成功或失败）',
  `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `active_round` bigint GENERATED ALWAYS AS ((case when ((`del_flag` = '0') and (`task_status` in ('0','1'))) then `round_id` else NULL end)) STORED COMMENT '活动任务轮次（仅用于唯一约束，业务不读写）',
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_lottery_task_active` (`active_round`),
  KEY `idx_lottery_task_round` (`round_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='选课抽签任务表';

-- 说明：存量环境的抽签历史不回填。本表只承载「本版本起」的任务执行轨迹，
-- 历史抽签结果的审计依据仍是 tpm_selection_round.lottery_seed / lottery_time。
