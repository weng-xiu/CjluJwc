-- =============================================================================
-- phase35：U1 主题色机构自定义（sys.ui.themeColor）
--
-- 所属优化项：《长江大学教务管理系统后续优化方案》U1 设计系统统一与主题化（待补项收口）
-- 内容：
--   1) 新增系统参数 sys.ui.themeColor（机构主题色，十六进制），管理端 yu-ui-vue3
--      登录后读取该参数并派生 Element Plus 色阶写入 CSS 变量，实现全站换色；
--   2) 默认值取长江大学品牌主色 #007ab8，与前端内置默认一致；
--   3) 全部语句幂等，可重复执行（WHERE NOT EXISTS 防重）。
-- =============================================================================

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '界面-机构主题色', 'sys.ui.themeColor', '#007ab8', 'Y', 'admin', sysdate(),
       '管理端全站主题色（十六进制，如 #007ab8），前端自动派生 light/dark 色阶；留空或非法值时回退品牌默认色'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.ui.themeColor');

-- 验证：参数存在且值合法
SELECT '主题色参数' AS item, COUNT(1) AS cnt FROM sys_config WHERE config_key = 'sys.ui.themeColor';
