-- 修复测试中发现的菜单缺失问题：
-- 1. scheduleOpt 菜单(2620)未导入数据库，导致前端访问 /tpm/schedule-group/scheduleOpt 404
-- 2. graduationProcedure 菜单需要确认是否存在（截图虽然 200，但实际可能也是默认 404 组件）
-- 注：G4/V6.5 起不再写 USE `yu-CjluJwc`;——基线重放的库名由 -DbName 指定，
-- 硬编码库名在 CI 与 Linux（库名大小写敏感）下会直接报 Unknown database。
--
-- G4/V6.5 处置：原 2621-2625 五个按钮权限（tpm:scheduleOpt:query/execute/edit/remove/export）删除，
-- 判据是实测无引用，而非随口取舍——
--   · 后端：ScheduleOptimizationController 全类走 tpm:schedule:* （detectConflict / autoAssign / edit …），
--     `tpm:scheduleOpt:` 前缀在 yu-*/src/main/java 下 0 处 @PreAuthorize 命中；
--   · 前端：tpm/scheduleOpt/index.vue（vue2 与 vue3 两套）整个文件 0 处 hasPermi，按钮不做权限点控制；
--   · 实态：db/ci/02-meta.sql 里 2621-2626 是 ry_portal_cms_menu.sql 的「文章管理」按钮，
--     本脚本这 5 行因 INSERT IGNORE 从未落库。
-- 保留 2620：页面是活的（组件、api、controller 均在），且实态库里它就是 2620。

-- 排课优化菜单（如果已存在则跳过）
INSERT IGNORE INTO sys_menu VALUES('2620', '排课优化', '2105', '4', 'scheduleOpt', 'tpm/scheduleOpt/index', '', '', 1, 0, 'C', '0', '0', 'tpm:scheduleOpt:list', 'tool', 'admin', sysdate(), '', null, '');

-- 清理历史上可能已落库的死按钮权限（幂等；没落过就是 0 行）
DELETE FROM sys_menu WHERE perms IN
  ('tpm:scheduleOpt:query','tpm:scheduleOpt:execute','tpm:scheduleOpt:edit',
   'tpm:scheduleOpt:remove','tpm:scheduleOpt:export');

-- 验证
SELECT menu_id, menu_name, path, component FROM sys_menu WHERE menu_id = 2620;
