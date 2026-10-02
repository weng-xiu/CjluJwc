-- =====================================================================================
-- 回滚：V20261001.1 —— 恢复「工具 / 表单构建」菜单
-- 对应变更：db/migration/V20261001_1__retire_tool_build_menu.sql
-- 逆向操作：status '1' → '0'，并剥离变更脚本追加的 remark 标记
-- 影响表：sys_menu（至多 1 行，menu_id=115）
-- 数据丢失风险：无（变更未删除任何行，sys_role_menu 关联全程保留）
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261001.1';
--            否则 Flyway 仍认为该版本已应用，下次 migrate 不会重放。
-- 注意：恢复后 Vue3 端点击该菜单会重新命中 Unmigrated.vue，
--       属预期行为——回滚仅表示「撤销停用决策」，不代表页面已迁移完成。
-- =====================================================================================

UPDATE sys_menu
   SET status      = '0',
       update_by   = 'flyway-rollback',
       update_time = NOW(),
       remark      = TRIM(REPLACE(IFNULL(remark, ''),
                                  ' [V20261001.1 停用：Vue3 端未迁移表单构建，功能由 tool/gen 覆盖]', ''))
 WHERE menu_id = 115
   AND component = 'tool/build';

-- 校验：应返回 1 行且 status=0
SELECT menu_id, menu_name, path, component, status
  FROM sys_menu
 WHERE menu_id = 115
   AND component = 'tool/build';
