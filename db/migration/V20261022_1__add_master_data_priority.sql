-- =====================================================================================
-- V20261022.1 —— F5 主数据裁决前端化：新增数据源优先级配置表 dis_source_priority
-- 背景：MasterDataPriorityPolicy 此前将各外部源优先级硬编码于内存构造函数，改优先级需发版。
--       本表把"以谁为准"的权威优先级沉淀为可配置、可审计的数据（§6.2 D23 前置能力 / §7 F5）。
-- 内容：1) 建 dis_source_priority 表（幂等 CREATE TABLE IF NOT EXISTS）
--       2) 以原内存基线为种子登记 5 个权威源（幂等 INSERT ... WHERE NOT EXISTS，按 source_code 唯一）
-- 影响表：dis_source_priority（新建）
-- 幂等性：是（IF NOT EXISTS / WHERE NOT EXISTS）
-- 回滚：db/rollback/V20261022_1__add_master_data_priority.sql
-- =====================================================================================

CREATE TABLE IF NOT EXISTS dis_source_priority (
  priority_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '优先级配置ID',
  source_code   VARCHAR(50)  NOT NULL                COMMENT '来源系统编码（与裁决候选 sourceCode 对齐）',
  source_name   VARCHAR(100) DEFAULT NULL            COMMENT '来源系统名称',
  priority      INT          DEFAULT 0               COMMENT '权威优先级（数值越大越权威，未登记源回落0）',
  status        CHAR(1)      DEFAULT '0'             COMMENT '状态（0正常 1停用）',
  create_by     VARCHAR(64)  DEFAULT ''              COMMENT '创建者',
  create_time   DATETIME     DEFAULT NULL            COMMENT '创建时间',
  update_by     VARCHAR(64)  DEFAULT ''              COMMENT '更新者',
  update_time   DATETIME     DEFAULT NULL            COMMENT '更新时间',
  remark        VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (priority_id),
  UNIQUE KEY uk_source_code (source_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主数据裁决数据源优先级配置';

-- 种子：迁移原 MasterDataPriorityPolicy 内存基线，保证落库前后裁决口径一致
INSERT INTO dis_source_priority (source_code, source_name, priority, status, create_by, create_time, remark)
SELECT 'GRAD','研究生学籍主系统',100,'0','admin',sysdate(),'F5 主数据裁决权威源基线'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dis_source_priority WHERE source_code='GRAD');
INSERT INTO dis_source_priority (source_code, source_name, priority, status, create_by, create_time, remark)
SELECT 'JW','教务主系统',90,'0','admin',sysdate(),''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dis_source_priority WHERE source_code='JW');
INSERT INTO dis_source_priority (source_code, source_name, priority, status, create_by, create_time, remark)
SELECT 'HR','人事系统（教师主数据）',80,'0','admin',sysdate(),''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dis_source_priority WHERE source_code='HR');
INSERT INTO dis_source_priority (source_code, source_name, priority, status, create_by, create_time, remark)
SELECT 'FINANCE','财务系统',40,'0','admin',sysdate(),''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dis_source_priority WHERE source_code='FINANCE');
INSERT INTO dis_source_priority (source_code, source_name, priority, status, create_by, create_time, remark)
SELECT 'CARD','一卡通',20,'0','admin',sysdate(),''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dis_source_priority WHERE source_code='CARD');

-- 验证
SELECT priority_id, source_code, source_name, priority, status FROM dis_source_priority ORDER BY priority DESC;
