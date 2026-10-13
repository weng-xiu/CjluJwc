-- =====================================================================================
-- 回滚：V20261012.1 智能体发布前评测集与发布门禁（K4 AI 治理 · ③）
-- 说明：与 db/migration/V20261012_1__add_ai_eval_publish_gate.sql 反向对应。
--       3 张表为本批新建（无历史业务数据），DROP 安全；配置/字典/菜单按 key/type/perms 精确删除。
-- 影响：删除 sys_ai_eval_case / sys_ai_eval_run / sys_ai_eval_result；清理 ai.eval.% 参数、
--       sys_ai_eval_result 字典、3410~3417 菜单及其角色关联。
-- =====================================================================================

-- 1、删除新建表
DROP TABLE IF EXISTS `sys_ai_eval_result`;
DROP TABLE IF EXISTS `sys_ai_eval_run`;
DROP TABLE IF EXISTS `sys_ai_eval_case`;

-- 2、清理系统参数
DELETE FROM sys_config WHERE config_key IN ('ai.eval.publish.enabled', 'ai.eval.pass.threshold', 'ai.eval.min.confidence');

-- 3、清理字典
DELETE FROM sys_dict_data WHERE dict_type = 'sys_ai_eval_result';
DELETE FROM sys_dict_type WHERE dict_type = 'sys_ai_eval_result';

-- 4、清理菜单与角色关联
DELETE FROM sys_role_menu WHERE menu_id BETWEEN 3410 AND 3417;
DELETE FROM sys_menu WHERE menu_id BETWEEN 3410 AND 3417;

-- 5、验证（回滚后应全为 0）
SELECT '评测表' AS item, COUNT(1) AS cnt FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name IN ('sys_ai_eval_case','sys_ai_eval_run','sys_ai_eval_result')
UNION ALL SELECT '评测参数', COUNT(1) FROM sys_config WHERE config_key LIKE 'ai.eval.%'
UNION ALL SELECT '门禁结果字典', COUNT(1) FROM sys_dict_data WHERE dict_type = 'sys_ai_eval_result'
UNION ALL SELECT '评测菜单', COUNT(1) FROM sys_menu WHERE menu_id BETWEEN 3410 AND 3417;
