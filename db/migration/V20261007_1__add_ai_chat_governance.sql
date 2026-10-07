-- =====================================================================================
-- 版本：V20261007.1
-- 标题：AI 问答留痕表新增「内容治理」列 + 治理参数与字典（K4 AI 治理 · 输入/输出审核与算法留痕）
-- 目的：为智能问答补齐合规治理地基——① 记录每次作答所依据的大模型名称/版本（模型版本登记）；
--       ② 记录内容审核结果码与命中类别（越狱/提示词注入、敏感信息套取、输入超限、输出泄露改写），
--       使「AI 内容审核与算法留痕」可审计、可复盘。对应等保与《人工智能+教育行动计划》治理要求。
-- 做法：information_schema + PREPARE 幂等 ADD COLUMN（MySQL 8 不支持 ADD COLUMN IF NOT EXISTS）；
--       参数与字典按 WHERE NOT EXISTS 幂等写入，重复执行不产生重复行。
-- 影响表：sys_ai_chat_record（新增 3 可空列，存量行取默认值，不影响任何既有问答与统计）；
--       sys_config / sys_dict_type / sys_dict_data（追加，不改写既有项）。
-- 兼容：审核器默认开启，未显式配置 ai.moderation.enabled 时按合规优先默认放行正常问答、仅拦截明显风险；
--       存量数据 moderation_result 为 NULL 或 '0' 均视为通过，统计口径不受影响。
-- 关联事项：V4.x §7 K4（AI 治理：内容审核/留痕策略/模型版本登记/语料治理）
-- 幂等性：是（列已存在则跳过；配置/字典已存在则跳过）
-- 回滚脚本：db/rollback/V20261007_1__add_ai_chat_governance.sql
-- 执行说明：本机存量库按既有约定「人工应用 + 带回滚脚本」，FLYWAY_ENABLED 默认 false。
-- =====================================================================================

-- ----------------------------
-- 1、sys_ai_chat_record 新增治理列（非破坏，可空，存量行为 NULL）
-- ----------------------------
SET @c1 = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_ai_chat_record' AND column_name = 'model_version');
SET @s1 = IF(@c1 = 0,
    'ALTER TABLE `sys_ai_chat_record` ADD COLUMN `model_version` varchar(100) DEFAULT NULL COMMENT ''作答依据的大模型名称/版本（空表示本地抽取）'' AFTER `error_msg`',
    'SELECT 1');
PREPARE stmt FROM @s1; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c2 = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_ai_chat_record' AND column_name = 'moderation_result');
SET @s2 = IF(@c2 = 0,
    'ALTER TABLE `sys_ai_chat_record` ADD COLUMN `moderation_result` char(1) DEFAULT ''0'' COMMENT ''内容治理结果码（0通过 1输入拦截 2输出改写）'' AFTER `model_version`',
    'SELECT 1');
PREPARE stmt FROM @s2; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c3 = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_ai_chat_record' AND column_name = 'moderation_note');
SET @s3 = IF(@c3 = 0,
    'ALTER TABLE `sys_ai_chat_record` ADD COLUMN `moderation_note` varchar(100) DEFAULT NULL COMMENT ''内容治理命中类别说明（供审计）'' AFTER `moderation_result`',
    'SELECT 1');
PREPARE stmt FROM @s3; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 索引：便于管理端按审核结果筛查被拦截/改写的问答
SET @idx = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'sys_ai_chat_record' AND index_name = 'idx_ai_chat_moderation');
SET @sidx = IF(@idx = 0,
    'ALTER TABLE `sys_ai_chat_record` ADD INDEX `idx_ai_chat_moderation` (`moderation_result`)',
    'SELECT 1');
PREPARE stmt FROM @sidx; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 2、系统参数（内容治理开关；未配置即合规优先默认开启，仅拦截明显风险，不影响正常问答）
-- ----------------------------
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT * FROM (
  SELECT 'AI内容治理开关' AS a, 'ai.moderation.enabled' AS b, 'true' AS c, 'Y' AS d, 'system' AS e, NOW() AS f, 'true/false，控制问答输入审核与输出防泄露改写；缺省视为开启' AS g
  UNION ALL SELECT 'AI提问最大长度','ai.moderation.maxLength','200','Y','system',NOW(),'单次提问允许的最大字符数，超出即拦截，防止超长注入与资源滥用'
  UNION ALL SELECT 'AI违禁词清单','ai.moderation.blockKeywords','','Y','system',NOW(),'机构自定义违禁词，逗号/分号分隔，命中即拦截（越狱与套取特征已内置，无需在此重复）'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_config c2 WHERE c2.config_key = t.b);

-- ----------------------------
-- 3、字典：回答来源新增「内容审核拦截」值
-- ----------------------------
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '审核拦截', 'MODERATED', 'sys_ai_answer_source', '', 'warning', 'N', '0', 'system', NOW(), '输入命中内容治理规则，未触达检索与模型即被安全话术拦截'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sys_ai_answer_source' AND dict_value = 'MODERATED');

-- ----------------------------
-- 4、字典：内容治理结果码
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT 'AI内容治理结果', 'sys_ai_moderation_result', '0', 'system', NOW(), '智能问答内容审核结果码'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'sys_ai_moderation_result');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT * FROM (
  SELECT 1 AS a, '通过' AS b, '0' AS c, 'sys_ai_moderation_result' AS d, '' AS e, 'success' AS f, 'Y' AS g, '0' AS h, 'system' AS i, NOW() AS j, '问答正常放行' AS k
  UNION ALL SELECT 2,'输入拦截','1','sys_ai_moderation_result','','danger','N','0','system',NOW(),'提问命中越狱/套取/超限等风险，直接以安全话术回复'
  UNION ALL SELECT 3,'输出改写','2','sys_ai_moderation_result','','warning','N','0','system',NOW(),'回答疑似泄露密钥/连接串，已按安全策略屏蔽'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sys_ai_moderation_result' AND dict_value = '0');

-- ----------------------------
-- 5、验证
-- ----------------------------
SELECT '治理列' AS item, COUNT(1) AS cnt FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'sys_ai_chat_record'
   AND column_name IN ('model_version','moderation_result','moderation_note')
UNION ALL SELECT '治理参数', COUNT(1) FROM sys_config WHERE config_key LIKE 'ai.moderation.%'
UNION ALL SELECT '来源字典(MODERATED)', COUNT(1) FROM sys_dict_data WHERE dict_type = 'sys_ai_answer_source' AND dict_value = 'MODERATED'
UNION ALL SELECT '结果码字典', COUNT(1) FROM sys_dict_data WHERE dict_type = 'sys_ai_moderation_result';
