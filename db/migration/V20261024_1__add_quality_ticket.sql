-- =====================================================================================
-- V20261024.1 —— F5 质量看板异常工单化：新增质量异常工单表 dis_quality_ticket
-- 背景：数据对接质量看板（N2 DisQualityService）此前仅只读汇总各同步任务失败率，
--       异常无法沉淀为可指派、可跟踪、可闭环的处置工单（§7 F5 剩余项"质量看板异常工单化"）。
-- 内容：1) 建 dis_quality_ticket 表（幂等 CREATE TABLE IF NOT EXISTS）
--       2) 工单由"异常检测"扫描同步任务失败率自动生成，或人工登记；含处置状态机与结论回填
-- 影响表：dis_quality_ticket（新建）
-- 幂等性：是（IF NOT EXISTS）
-- 回滚：db/rollback/V20261024_1__add_quality_ticket.sql
-- =====================================================================================

CREATE TABLE IF NOT EXISTS dis_quality_ticket (
  ticket_id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  task_id        BIGINT       DEFAULT NULL            COMMENT '关联同步任务ID（dis_sync_task，可空=人工登记）',
  task_name      VARCHAR(100) DEFAULT NULL            COMMENT '任务名称（快照，任务改名不影响工单）',
  anomaly_type   CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '异常类型（0失败率超标 1连续失败 2长时间未同步 3其他）',
  severity       CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '严重程度（0低 1中 2高）',
  anomaly_detail VARCHAR(500) DEFAULT NULL            COMMENT '异常描述',
  fail_rate      DECIMAL(5,2) DEFAULT NULL            COMMENT '生成时失败率（百分比，0~100）',
  status         CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '处置状态（0待处理 1处理中 2已解决 3已关闭）',
  assignee       VARCHAR(64)  DEFAULT NULL            COMMENT '处置责任人',
  resolution     VARCHAR(500) DEFAULT NULL            COMMENT '处置结论',
  resolved_time  DATETIME     DEFAULT NULL            COMMENT '解决时间',
  del_flag       CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0存在 2删除）',
  create_by      VARCHAR(64)  DEFAULT ''              COMMENT '创建者',
  create_time    DATETIME     DEFAULT NULL            COMMENT '创建时间',
  update_by      VARCHAR(64)  DEFAULT ''              COMMENT '更新者',
  update_time    DATETIME     DEFAULT NULL            COMMENT '更新时间',
  remark         VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (ticket_id),
  KEY idx_dqt_task (task_id),
  KEY idx_dqt_status (status),
  KEY idx_dqt_severity (severity, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据对接质量异常工单（F5 异常-工单-处置闭环）';

-- 验证
SELECT ticket_id, task_id, task_name, anomaly_type, severity, fail_rate, status
FROM dis_quality_ticket ORDER BY ticket_id DESC;
