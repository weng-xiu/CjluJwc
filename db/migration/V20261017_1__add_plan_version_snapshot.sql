-- =====================================================================================
-- 版本：V20261017.1
-- 标题：新增人才培养方案版本快照表 tpm_plan_snapshot（F2 培养方案版本快照与按版审核后端地基）
-- 目的：政策变更后可按旧版方案追溯核验。方案每次"发布"即生成一条不可变内容快照
--       （记录实质字段内容指纹 + 学分结构 + 课程清单 JSON），使毕业审核可选定"按某一历史版本比对"，
--       而非只能比对实时 tpm_credit_structure。复用 yu-tpm TrainingPlanSnapshotService 的 SHA-256 指纹。
-- 做法：CREATE TABLE IF NOT EXISTS（幂等，可重复执行）；content_hash 为快照实质内容指纹，
--       credit_structure_json / course_list_json 为发布时刻的子表定格内容（供按版比对，与实时表解耦）。
-- 影响表：新增 tpm_plan_snapshot；不改动任何既有表。
-- 关联事项：《技术架构与开发优化计划 V5.0》§7.1 F2；产品对标本方"版本控制/学分核验可追溯"评估点。
-- 幂等性：是（表已存在则跳过）
-- 回滚脚本：db/rollback/V20261017_1__add_plan_version_snapshot.sql
-- =====================================================================================

CREATE TABLE IF NOT EXISTS tpm_plan_snapshot (
  snapshot_id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '快照ID(主键)',
  plan_id              BIGINT        NOT NULL                COMMENT '培养方案ID(关联 tpm_training_plan.plan_id)',
  version              VARCHAR(20)   DEFAULT NULL            COMMENT '发布时的方案版本号',
  content_hash         VARCHAR(64)   DEFAULT NULL            COMMENT '实质内容指纹(SHA-256,十六进制小写,复用 TrainingPlanSnapshotService)',
  total_credit         DECIMAL(6,2)  DEFAULT NULL            COMMENT '快照时的要求总学分',
  credit_structure_json LONGTEXT     COMMENT '学分结构定格快照JSON(发布时刻 tpm_credit_structure 分项)',
  course_list_json     LONGTEXT     COMMENT '课程清单定格快照JSON(发布时刻 tpm_course_library 摘要)',
  publish_date         DATETIME      DEFAULT NULL            COMMENT '方案发布日期',
  snapshot_time        DATETIME      DEFAULT NULL            COMMENT '快照生成时间',
  create_by            VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  create_time          DATETIME      DEFAULT NULL            COMMENT '创建时间',
  remark               VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (snapshot_id),
  KEY idx_plan_snapshot_plan (plan_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='人才培养方案版本快照表';

-- 验证
SELECT '方案版本快照表' AS item, COUNT(1) AS cnt FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'tpm_plan_snapshot';
