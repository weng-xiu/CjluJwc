-- =====================================================================================
-- 回滚：V20261007.1 —— 移除 AI 问答「内容治理」列、治理参数与字典
-- 对应变更：db/migration/V20261007_1__add_ai_chat_governance.sql
-- 前置动作：回滚前建议将 ai.moderation.enabled 置为 false，并确保后端已回退到不引用治理列的版本，
--          避免 insert 语句命中缺失列。
-- 逆向操作：information_schema + PREPARE 幂等 DROP INDEX / DROP COLUMN；按 key/type DELETE 配置与字典。
-- 影响表：sys_ai_chat_record（删除 3 列与索引，问答主数据不受影响）；sys_config / sys_dict_* 追加项清除。
-- 回滚后果：AI 问答退回无内容治理状态（正常问答与历史留痕不受损，仅失去审核与模型版本记录能力）。
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261007.1';
-- =====================================================================================

-- 1、删除索引（存在则删）
SET @idx = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'sys_ai_chat_record' AND index_name = 'idx_ai_chat_moderation');
SET @sidx = IF(@idx = 1, 'ALTER TABLE `sys_ai_chat_record` DROP INDEX `idx_ai_chat_moderation`', 'SELECT 1');
PREPARE stmt FROM @sidx; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2、删除治理列（存在则删；顺序：note -> result -> version，逆向于新增）
SET @c3 = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_ai_chat_record' AND column_name = 'moderation_note');
SET @sql3 = IF(@c3 = 1, 'ALTER TABLE `sys_ai_chat_record` DROP COLUMN `moderation_note`', 'SELECT 1');
PREPARE stmt FROM @sql3; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c2 = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_ai_chat_record' AND column_name = 'moderation_result');
SET @sql2 = IF(@c2 = 1, 'ALTER TABLE `sys_ai_chat_record` DROP COLUMN `moderation_result`', 'SELECT 1');
PREPARE stmt FROM @sql2; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c1 = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_ai_chat_record' AND column_name = 'model_version');
SET @sql1 = IF(@c1 = 1, 'ALTER TABLE `sys_ai_chat_record` DROP COLUMN `model_version`', 'SELECT 1');
PREPARE stmt FROM @sql1; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3、清理治理参数
DELETE FROM sys_config WHERE config_key IN ('ai.moderation.enabled', 'ai.moderation.maxLength', 'ai.moderation.blockKeywords');

-- 4、清理字典（来源新增项 + 结果码字典）
DELETE FROM sys_dict_data WHERE dict_type = 'sys_ai_answer_source' AND dict_value = 'MODERATED';
DELETE FROM sys_dict_data WHERE dict_type = 'sys_ai_moderation_result';
DELETE FROM sys_dict_type WHERE dict_type = 'sys_ai_moderation_result';

-- 5、校验：应返回 0
SELECT COUNT(*) AS remaining_cols FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'sys_ai_chat_record'
   AND column_name IN ('model_version','moderation_result','moderation_note');
