-- =====================================================================================
-- V20261021.1 —— F3 OBE 整改闭环：新增持续改进台账表 sam_obe_improvement
-- 背景：达成度未达标（< 目标值）后需登记改进措施、跟踪责任人/期限，并回填改进后达成度
--       形成"评价 → 反馈 → 改进"闭环（§7 F3 剩余项）。
-- 内容：1) 建 sam_obe_improvement 表（幂等 CREATE TABLE IF NOT EXISTS）
-- 影响表：sam_obe_improvement（新建）
-- 幂等性：是（IF NOT EXISTS）
-- 回滚：db/rollback/V20261021_1__add_obe_improvement_ledger.sql
-- =====================================================================================

CREATE TABLE IF NOT EXISTS sam_obe_improvement (
  imp_id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '改进台账ID',
  plan_id           BIGINT       NOT NULL                COMMENT '培养方案ID',
  semester_id       BIGINT       DEFAULT NULL            COMMENT '学期ID（可空=不限学期）',
  gr_id             BIGINT       DEFAULT NULL            COMMENT '毕业要求ID（与 ip_id 二选一）',
  ip_id             BIGINT       DEFAULT NULL            COMMENT '指标点ID（可空=整条毕业要求）',
  source_term       VARCHAR(32)  DEFAULT NULL            COMMENT '发现周期（如 2025-2026-2）',
  attainment        DECIMAL(6,4) DEFAULT NULL            COMMENT '发现时达成度',
  target_value      DECIMAL(6,4) DEFAULT NULL            COMMENT '发现时目标值',
  problem_desc      VARCHAR(500) DEFAULT NULL            COMMENT '问题分析',
  measure           VARCHAR(500) NOT NULL                COMMENT '改进措施',
  owner             VARCHAR(64)  DEFAULT NULL            COMMENT '责任人',
  due_date          DATE         DEFAULT NULL            COMMENT '计划完成日期',
  status            CHAR(1)      DEFAULT '0'             COMMENT '状态（0进行中 1已完成 2已验证）',
  effect_attainment DECIMAL(6,4) DEFAULT NULL            COMMENT '效果反馈：改进后达成度',
  effect_note       VARCHAR(500) DEFAULT NULL            COMMENT '效果反馈说明',
  del_flag          CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0存在 2删除）',
  create_by         VARCHAR(64)  DEFAULT ''              COMMENT '创建者',
  create_time       DATETIME     DEFAULT NULL            COMMENT '创建时间',
  update_by         VARCHAR(64)  DEFAULT ''              COMMENT '更新者',
  update_time       DATETIME     DEFAULT NULL            COMMENT '更新时间',
  remark            VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (imp_id),
  KEY idx_obe_imp_plan (plan_id, status),
  KEY idx_obe_imp_gr (gr_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OBE持续改进台账';
