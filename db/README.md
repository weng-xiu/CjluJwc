# 数据库版本管理规范（V4.0 §7.3 / K1）

本目录是 `yu-cjlujwc` 库的**版本化管理入口**。引入 Flyway 前，全部结构变更以
`sql/` 下 70 个手工脚本 + `phase*.sql` 增量脚本的「口头顺序」执行，存在三个问题：

1. 顺序只存在于文档与老同事记忆里，新环境重建一次就要踩一次坑；
2. 无执行记录，无法回答「这台库到底升到了第几 phase」；
3. 无回滚物，出问题只能反向手工补数据。

现在的约定：**历史脚本基线化收编，新增变更一律走 `db/migration/`。**

## 目录结构

```
db/
├── README.md                 本文件（规范）
├── legacy/
│   └── MANIFEST.md           历史 70 个脚本的权威执行顺序与版本映射
├── migration/                Flyway 版本化脚本（新增变更唯一入口）
│   ├── V1__legacy_baseline.sql
│   └── V20261001_1__*.sql
└── rollback/                 与 migration 同名配对的回滚脚本（人工执行）
    └── V20261001_1__*.sql
```

`sql/` 保持原样不动（历史资产与 Vue2 时代环境仍在用），**新的变更不要再往 `sql/` 里加**。

## 命名

```
V<版本>__<snake_case_描述>.sql
```

* 版本号优先用日期段，避免与 `phaseN` 抢序号：`V20261001_1__add_exam_seat_map.sql`
  * `V20261001_1` 表示 2026-10-01 的第 1 个变更，同日多变更靠 `_n` 递增定序。
* 描述里带上业务标识，便于 `flyway info` 一眼看懂：
  `V20261002_3__tpm_T6_waitlist_promote_index.sql`（T6 是优化报告里的事项编号）
* **禁止**修改已发布（已在任何环境执行过）的脚本内容 —— 需要更正就再加一个新版本，
  Flyway 的 checksum 校验会直接让启动失败，这是特性不是故障。

## 回滚（强制配对）

Flyway 社区版不提供 `undo`，因此约定：**每个 `db/migration/Vx__d.sql` 必须有同名
`db/rollback/Vx__d.sql`**，两者一同提交，缺一不得合并。

* 回滚脚本写「逆向操作」，而不是「注释掉」：`ADD COLUMN` ↔ `DROP COLUMN`；
  `INSERT ... sys_menu` ↔ `DELETE ... WHERE menu_id = ...`；
  `UPDATE set status='1'` ↔ `UPDATE set status='0'`。
* 不可逆变更（DROP TABLE / DROP COLUMN 且含数据）必须在脚本头声明
  `-- IRREVERSIBLE: <原因>` 并在评审时单独确认；此类回滚脚本只能恢复结构，数据不保证。
* 执行方式（人工，需 DBA 在场）：

  ```powershell
  mysql -h127.0.0.1 -P3306 -uroot -p yu-cjlujwc < db\rollback\V20261001_1__xxx.sql
  # 回滚后手工清理版本记录，否则 Flyway 会认为该版本仍已应用
  DELETE FROM flyway_schema_history WHERE version = '20261001.1';
  ```

* 回滚脚本本身要幂等：MySQL 8 没有 `DROP COLUMN IF EXISTS`，用
  `information_schema` 判断 + `PREPARE/EXECUTE` 动态语句（参考
  `db/migration/V1__legacy_baseline.sql` 的写法）。

## 幂等与菜单 ID 段

所有脚本必须可重复执行（`CREATE TABLE IF NOT EXISTS`、
`INSERT ... ON DUPLICATE KEY UPDATE` 或先 `DELETE` 再 `INSERT`）。

`sys_menu.menu_id` 分段登记。下表是**实际占用**（由 `bin/db-menu-check.ps1` 扫描
`sql/` 与 `db/migration/` 得出，非当初的设计意图），完整数据见
`db/legacy/MANIFEST.md` 附录：

| ID 段 | 归属 | 来源 |
|---|---|---|
| 1 – 1199 | RuoYi 框架原生（系统/监控/工具，含 115 表单构建） | `ry_20260417.sql` |
| 2000 – 2099 | BRM 基础资源 | `ry_brm_menu.sql` |
| 2100 – 2199 | TPM 培养过程 | `ry_tpm_menu.sql` |
| 2200 – 2299 | AEM 考核评价 | `ry_aem_menu.sql` |
| 2300 – 2399 | ⚠ OA 办公 与 SAM 学籍 **共用段**（历史撞号 45 处） | `ry_oa_menu.sql`、`ry_sam_menu.sql` |
| 2400 – 2499 | DIS 数据对接 | `ry_dis_menu.sql` |
| 2500 – 2599 | Portal 门户 / 用户管理增强 | `ry_portal_menu.sql`、`user_mgmt_upgrade.sql` |
| 2600 – 2699 | ⚠ Portal CMS 与 phase1 增量 **共用段**（历史撞号 13 处） | `ry_portal_cms_menu.sql`、`phase1_upgrade.sql` |
| 2700 – 2799 | ⚠ OA 补充与 phase4 增量 **共用段**（历史撞号 6 处） | `ry_oa_menu_2700.sql`、`phase4_tpm_enhancement.sql` |
| 2800 – 2899 | 空闲（历史上被当作设计意图登记，实际未使用） | — |
| 2900 – 2999 | 菜单修正补丁 | `fix_gpa_config_menu.sql`、`fix_schedule_opt_menu.sql` |
| **3000 以上** | **基线之后的新增变更专用段** | `db/migration/` |

> **新变更一律从 3000 往上取号，并先跑冲突校验**：
> `powershell -File bin\db-menu-check.ps1 -Files db\migration\V20261002_1__xxx.sql`
>
> 带 ⚠ 的三段是历史遗留冲突，CI 以 `-IgnoreLegacy` 降为不计，但**不允许**
> `db/migration/` 再往这些段里插菜单（会被判为与历史脚本的新增冲突）。

## 如何执行

Flyway 配置在 `yu-admin/src/main/resources/application.yml`，**默认关闭**
（`FLYWAY_ENABLED=false`），以免影响开发机上已有库。

| 场景 | 做法 |
|---|---|
| 已有库（本机 / 测试 / 生产） | 保持 `FLYWAY_ENABLED=false`，或设 `baseline-on-migrate=true`（已默认）后开启：Flyway 会以版本 0 打基线标记，只执行 `V1` 之后的脚本 |
| 全新空库 | `FLYWAY_ENABLED=true` 启动后端，先建 Flyway 历史表，再按 `db/legacy/MANIFEST.md` 顺序导入历史脚本（见 `bin/db-baseline.ps1`），后续版本自动接续 |
| 只做检查不执行 | `mvn -pl yu-admin flyway:info -Dflyway.url=... -Dflyway.user=...` |

`spring.flyway.locations` 默认 `filesystem:db/migration`（相对后端进程工作目录，
即仓库根）。CI 里通过 `FLYWAY_LOCATIONS` 覆盖。

## 评审清单（提 PR 前自查）

- [ ] `db/migration/` 与 `db/rollback/` 同名成对
- [ ] 脚本头注释写清：目的、影响表、关联需求编号（A1/T6/N7…）、是否 IRREVERSIBLE
- [ ] 全脚本幂等；`sys_menu` 变更未撞已有 ID 段
- [ ] 未改动 `sql/` 与任何已发布脚本
- [ ] `mvn -B -q -pl yu-admin -am compile` 通过；有条件时本地 `FLYWAY_ENABLED=true` 跑一次空库演练
