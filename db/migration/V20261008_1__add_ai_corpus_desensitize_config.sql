-- =====================================================================================
-- 版本：V20261008.1
-- 标题：新增「AI 垂直语料脱敏」系统参数（K4 AI 治理 · 知识库入库去标识化口径）
-- 目的：知识库(sys_ai_knowledge)正文是智能问答 RAG 的唯一事实来源，会被注入检索上下文乃至大模型提示；
--       等保与《人工智能+教育行动计划》要求训练/检索语料去标识化。本批登记语料脱敏器的开关与模式参数，
--       使机构可在「系统管理-参数配置」中调整口径而无需改代码。后端见 AiCorpusDesensitizer 与
--       SysAiKnowledgeServiceImpl.applyCorpusDesensitize。
-- 做法：sys_config 按 WHERE NOT EXISTS 幂等写入，重复执行不产生重复行；不改写既有配置。
-- 兼容：参数缺省时代码按合规优先默认（enabled=true、mode=MASK），未应用本脚本亦不影响既有问答与知识库；
--       MASK 为入库前自动掩码（非破坏、可解释），BLOCK 为命中即拒绝入库（需编辑先去标识化）。
-- 影响表：sys_config（追加 2 项）。
-- 关联事项：V4.x §7 K4（AI 治理 · 语料治理）
-- 幂等性：是
-- 回滚脚本：db/rollback/V20261008_1__add_ai_corpus_desensitize_config.sql
-- 执行说明：本机存量库按既有约定「人工应用 + 带回滚脚本」，FLYWAY_ENABLED 默认 false。
-- =====================================================================================

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT * FROM (
  SELECT 'AI语料脱敏开关' AS a, 'ai.corpus.desensitize.enabled' AS b, 'true' AS c, 'Y' AS d, 'system' AS e, NOW() AS f, 'true/false，知识库条目入库前是否对正文内嵌个人信息(身份证/手机/邮箱)执行去标识化；缺省视为开启' AS g
  UNION ALL SELECT 'AI语料脱敏模式', 'ai.corpus.desensitize.mode', 'MASK', 'Y', 'system', NOW(), 'MASK=命中即自动掩码后入库(默认，非破坏)；BLOCK=命中即拒绝入库，需编辑显式去除个人信息后再提交'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_config c2 WHERE c2.config_key = t.b);

-- ----------------------------
-- 验证
-- ----------------------------
SELECT '语料脱敏参数' AS item, COUNT(1) AS cnt FROM sys_config WHERE config_key LIKE 'ai.corpus.desensitize.%';
