-- =====================================================================================
-- 版本：V20261004.1
-- 标题：新增「字段级数据变更流水」表 data_change_audit
-- 目的：V4.0 §6.2 K1 合规③——等保数据新规要求关键数据操作「可追溯、可核查」。
--       既有 sys_oper_log 仅到操作级（谁调了哪个接口），无法回答「某条成绩的分数
--       由 85 改成了 90、谁改的、何时改的」。本表下沉到字段粒度：每次业务修改对
--       变更字段逐条记录「旧值 → 新值 + 操作人 + IP + traceId + 时间」。
--       首批由成绩记录（aem_grade_record）修改链路写入（见 AemGradeRecordServiceImpl），
--       后续学籍/培养方案等实体按同一 recordDiff 约定接入。
-- 做法：CREATE TABLE IF NOT EXISTS，纯新增表，不触碰任何存量表结构与数据。
-- 影响表：新增 data_change_audit（原不存在）
-- 敏感字段：old_value/new_value 对命中脱敏的字段（身份证/手机号等）已在写入前掩码，
--       成绩类字段非个人敏感标识，按明文留存以供审计核查。
-- 关联事项：V4.0 §6.2 K1、§5.3 合规差距 #4（全量数据操作审计）
-- 幂等性：是（CREATE TABLE IF NOT EXISTS；重复执行不报错、不产生副作用）
-- 回滚脚本：db/rollback/V20261004_1__add_data_change_audit.sql
-- 执行说明：本机存量库按既有约定「人工应用 + 带回滚脚本」，FLYWAY_ENABLED 默认 false；
--       新环境或发布流程注入 FLYWAY_ENABLED=true 时由 Flyway 自动重放。
-- =====================================================================================

CREATE TABLE IF NOT EXISTS data_change_audit (
  audit_id           BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '流水主键',
  entity_type        VARCHAR(64)  NOT NULL                 COMMENT '实体类型（表名，如 aem_grade_record）',
  entity_type_label  VARCHAR(64)  DEFAULT NULL             COMMENT '实体中文名（如 成绩记录）',
  biz_id             VARCHAR(64)  NOT NULL                 COMMENT '业务主键值',
  field_name         VARCHAR(64)  NOT NULL                 COMMENT '变更字段名',
  field_label        VARCHAR(64)  DEFAULT NULL             COMMENT '变更字段中文名',
  old_value          VARCHAR(500) DEFAULT NULL             COMMENT '旧值（敏感字段已脱敏）',
  new_value          VARCHAR(500) DEFAULT NULL             COMMENT '新值（敏感字段已脱敏）',
  oper_name          VARCHAR(64)  DEFAULT NULL             COMMENT '操作人登录名',
  oper_ip            VARCHAR(128) DEFAULT NULL             COMMENT '操作人IP',
  trace_id           VARCHAR(64)  DEFAULT NULL             COMMENT '链路追踪ID',
  change_time        DATETIME     NOT NULL                 COMMENT '变更时间',
  remark             VARCHAR(500) DEFAULT NULL             COMMENT '备注',
  PRIMARY KEY (audit_id),
  KEY idx_dca_biz (entity_type, biz_id),
  KEY idx_dca_time (change_time),
  KEY idx_dca_oper (oper_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字段级数据变更流水表';

-- 校验：表存在即返回 1
SELECT COUNT(*) AS tbl_exists FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name = 'data_change_audit';
