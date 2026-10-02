-- =====================================================================================
-- 版本：V20261001.1
-- 标题：停用「工具 / 表单构建」菜单（Vue3 端不再提供该功能）
-- 目的：sys_menu.menu_id=115（表单构建，component=tool/build）源自 RuoYi 框架原生菜单，
--       仅存在于已归档的 Vue2 管理端（yu-ui/src/views/tool/build，约 2200 行拖拽式表单生成器）。
--       现役 Vue3 管理端未迁移该页面，导致菜单落地率卡在 116/117 且点击后命中 Unmigrated.vue 占位页。
--       该功能为开发者工具（生成表单 JSON），其产出用途已由 tool/gen（代码生成，Vue3 已具备）覆盖，
--       故按 V4.0 §6.1 N7「菜单级 100% 落地」要求停用，而非长期留占位页。
-- 做法：status 置为 '1'（停用）。后端 SysMenuMapper.selectMenuTreeByUserId 过滤 m.status = 0，
--       停用后 /getRouters 不再返回该节点，Vue3 前端不会注册对应路由，占位页无从命中。
--       不采用 visible='1'（隐藏）方案：隐藏菜单仍会下发路由，直接输入 URL 依旧命中占位页。
--       不采用 DELETE 方案：保留数据行使回滚零成本，且不破坏 sys_role_menu 关联。
-- 影响表：sys_menu（1 行，menu_id=115）
-- 连带影响：菜单为两端共享数据，Vue2 归档端同步不再显示该入口；Vue2 工程代码零改动。
-- 关联事项：V4.0 §6.1 N7、§7.3
-- 幂等性：是（重复执行结果一致；WHERE 带 component 双重限定，避免误伤同 ID 其他环境数据）
-- 回滚脚本：db/rollback/V20261001_1__retire_tool_build_menu.sql
-- =====================================================================================

UPDATE sys_menu
   SET status      = '1',
       update_by   = 'flyway',
       update_time = NOW(),
       -- 幂等：已带本版本标记则不重复追写 remark
       remark      = IF(IFNULL(remark, '') LIKE '%[V20261001.1%',
                        remark,
                        CONCAT(IFNULL(remark, ''), ' [V20261001.1 停用：Vue3 端未迁移表单构建，功能由 tool/gen 覆盖]'))
 WHERE menu_id = 115
   AND component = 'tool/build';
