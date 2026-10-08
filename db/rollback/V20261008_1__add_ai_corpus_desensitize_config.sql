-- =====================================================================================
-- 回滚：V20261008.1 —— 移除「AI 垂直语料脱敏」系统参数
-- 对应变更：db/migration/V20261008_1__add_ai_corpus_desensitize_config.sql
-- 影响：sys_config 删除 ai.corpus.desensitize.* 两项。删除后代码回退为合规优先内置默认
--       （enabled=true、mode=MASK），不影响既有知识库数据与问答。
-- 幂等性：是
-- =====================================================================================

DELETE FROM sys_config WHERE config_key LIKE 'ai.corpus.desensitize.%';
