# 历史脚本基线清单（legacy MANIFEST）

> 本文件是 `sql/` 目录下**已执行过的历史脚本**的权威顺序表，作为 Flyway 基线（version `0`）
> 的收编依据。`sql/` 内的文件本身不改动、不重命名、不删除（新增不动旧），
> 顺序由本清单固定；`bin/db-baseline.ps1` 直接读取本清单驱动导入。
>
> 版本号规则：`0.<序号>`（如 `0.09` = 第 9 个），全部归属于「基线」阶段——
> Flyway 层面不会重跑它们，只用于对账「这台库导到了第几步」。
> 从此文件之后的一切变更，走 `db/migration/`。

## A. 框架与工作流基础

| # | 脚本 | 内容 | 幂等 | 备注 |
|---|---|---|---|---|
| 0.01 | `sql/ry_20260417.sql` | RuoYi 基础表（sys_* / gen_*）+ 原生菜单 1–1199 + 初始用户/角色/字典 | 否（建表 IF NOT EXISTS，但菜单为裸 INSERT） | 只能对空库执行；含 `admin` 初始账号 |
| 0.02 | `sql/quartz.sql` | QRTZ_* 定时任务表 | 否 | |
| 0.03 | `sql/flowable_create_all.sql` | Flowable 7.1.0 ACT_* / FLW_* 引擎表（官方脚本，utf8/utf8_bin） | 否 | 与业务库同库共存，勿改成 utf8mb4 |

## B. 业务模块建表

| # | 脚本 | 内容 | 幂等 |
|---|---|---|---|
| 0.04 | `sql/brm.sql` | BRM 基础资源（学年学期/校区/教学楼/教室/借用/设备/院系/专业/班级/教师） | 是 |
| 0.05 | `sql/tpm.sql` | TPM 培养过程（课程库/培养方案/开课计划/排课/调停课/选课轮次与名单） | 是 |
| 0.06 | `sql/aem.sql` | AEM 考核评价（考试计划/座位/监考/成绩/复核/GPA/评教/督导） | 是 |
| 0.07 | `sql/sam.sql` | SAM 学籍学位（学籍/异动/预警/毕业审核/证书/离校） | 是 |
| 0.08 | `sql/dis.sql` | DIS 数据对接（外部系统/接口配置/同步任务/交换日志） | 是 |
| 0.09 | `sql/oa.sql` | OA 办公（流程定义/待办/公文/会议/公告/日程） | 是 |
| 0.10 | `sql/portal.sql` | Portal 学生/教师门户业务表 | 是 |
| 0.11 | `sql/portal_cms.sql` | Portal CMS（栏目/文章/轮播/通知公告） | 是 |

## C. 菜单与字典补丁（依赖 A/B 已建表）

| # | 脚本 | 内容 | 幂等 |
|---|---|---|---|
| 0.12 | `sql/ry_brm_menu.sql` | BRM 菜单（2000 段） | 是 |
| 0.13 | `sql/ry_brm_dict.sql` | BRM 字典 | 是 |
| 0.14 | `sql/ry_tpm_menu.sql` | TPM 菜单（2100–2299 段） | 是 |
| 0.15 | `sql/ry_aem_menu.sql` | AEM 菜单（2300 段） | 是 |
| 0.16 | `sql/ry_sam_menu.sql` | SAM 菜单（2400–2599 段） | 是 |
| 0.17 | `sql/ry_dis_menu.sql` | DIS 菜单（2600 段） | 是 |
| 0.18 | `sql/ry_oa_menu.sql` | OA 菜单（2600 段） | 是 |
| 0.19 | `sql/ry_oa_menu_2700.sql` | OA 菜单补充（2700 段，与 0.18 分段避让，**勿调换顺序**） | 是 |
| 0.20 | `sql/ry_oa_dict.sql` | OA 字典 | 是 |
| 0.21 | `sql/ry_portal_menu.sql` | 门户菜单（2800 段） | 是 |
| 0.22 | `sql/ry_portal_cms_menu.sql` | 门户 CMS 菜单（2800 段） | 是 |
| 0.23 | `sql/ry_portal_role_menu.sql` | 门户角色-菜单授权 | 是 |

## D. 结构性升级补丁

| # | 脚本 | 内容 | 幂等 |
|---|---|---|---|
| 0.24 | `sql/user_mgmt_upgrade.sql` | 用户管理增强（教师/学生账号字段） | 是 |
| 0.25 | `sql/brm_dept_sync_from_sys.sql` | BRM 院系从 sys_dept 同步逻辑 | 是 |
| 0.26 | `sql/fix_schedule_opt_menu.sql` | 排课优化菜单修正 | 是 |
| 0.27 | `sql/fix_gpa_config_menu.sql` | GPA 配置菜单修正 | 是 |

## E. 增量 phase 脚本（按编号严格递增，同编号内按标注顺序）

| # | 脚本 | 关联事项 | 内容 | 回滚物 |
|---|---|---|---|---|
| 0.28 | `sql/phase1_upgrade.sql` | — | 一期升级主体 | `sql/phase1_rollback.sql`（**已存在的成对回滚，作为规范范本**） |
| 0.29 | `sql/phase1_rollback.sql` | — | phase1 的逆向脚本；正常基线导入时**不执行**，仅在对账/回滚时使用 | — |
| 0.30 | `sql/phase1_warning_job.sql` | — | 学业预警定时任务 | 需补写 |
| 0.31 | `sql/phase3_governance.sql` | — | 数据治理 | 需补写 |
| 0.32 | `sql/phase4_tpm_enhancement.sql` | — | TPM 增强 | 需补写 |
| 0.33 | `sql/phase5_aem_optimization.sql` | — | AEM 优化 | 需补写 |
| 0.34 | `sql/phase6_portal_mobile.sql` | — | 门户移动端结构 | 需补写 |
| 0.35 | `sql/phase6_portal_mobile_seed.sql` | — | 门户移动端种子数据（依赖 0.34） | 需补写 |
| 0.36 | `sql/phase7_portal_selection_seed.sql` | — | 门户选课种子数据 | 需补写 |
| 0.37 | `sql/phase8_d1_sync.sql` | D1 | 同步改造 | 需补写 |
| 0.38 | `sql/phase9_s5_o1_p1.sql` | S5/O1/P1 | 三事项合并增量 | 需补写 |
| 0.39 | `sql/phase10_s1_audit_menu.sql` | S1 | 审核菜单 | 需补写 |
| 0.40 | `sql/phase11_a4_grade_weight.sql` | A4 | 成绩加权 | 需补写 |
| 0.41 | `sql/phase12_t3_publish_status_dict.sql` | T3 | 发布状态字典 | 需补写 |
| 0.42 | `sql/phase13_p7_course_import.sql` | P7 | 课程导入 | 需补写 |
| 0.43 | `sql/phase14_d2_sync_reliability.sql` | D2 | 同步可靠性 | 需补写 |
| 0.44 | `sql/phase15_s4_graduation_prereview.sql` | S4 | 毕业预审 | 需补写 |
| 0.45 | `sql/phase16_p1_a5_s3_t6_a1a2.sql` | P1/A5/S3/T6/A1/A2 | 多事项合并增量（**跨模块，回滚需拆分执行**） | 需补写 |
| 0.46 | `sql/phase17_a6_b2_stat.sql` | A6/B2 | 统计 | 需补写 |
| 0.47 | `sql/phase18_b1_borrow.sql` | B1 | 教室借用 | 需补写 |
| 0.48 | `sql/phase19_p7_a7_import_stat.sql` | P7/A7 | 导入统计 | 需补写 |
| 0.49 | `sql/phase20_p7_plan_import.sql` | P7 | 方案导入 | 需补写 |
| 0.50 | `sql/phase21_s8_registration.sql` | S8 | 学籍注册 | 需补写 |
| 0.51 | `sql/phase22_s7_certificate_procedure.sql` | S7 | 证书办理 | 需补写 |
| 0.52 | `sql/phase23_p4_print_credential.sql` | P4 | 打印凭证 | 需补写 |
| 0.53 | `sql/phase24_p6_portal_apply_loop.sql` | P6 | 门户申请闭环 | 需补写 |
| 0.54 | `sql/phase25_tpm_dict_restore.sql` | — | TPM 字典修复 | 需补写 |
| 0.55 | `sql/phase26_sam_student_user_id.sql` | — | SAM 学生-用户关联 | 需补写 |
| 0.56 | `sql/phase27_tpm_adjustment_approve_comment.sql` | — | 调停课审批意见 | 需补写 |
| 0.57 | `sql/phase28_p3_subject_stat.sql` | P3 | 学科统计 | 需补写 |
| 0.58 | `sql/phase29_s6_warning_assist.sql` | S6 | 预警帮扶 | 需补写 |
| 0.59 | `sql/phase30_o1_grade_review_flow.sql` | O1 | 成绩复核流程 | 需补写 |
| 0.60 | `sql/phase31_o1_workflow_cosign.sql` | O1 | 流程会签 | 需补写 |
| 0.61 | `sql/phase32_thesis_management.sql` | — | 论文管理 | 需补写 |
| 0.62 | `sql/phase33_status_report.sql` | — | 教育部状态数据上报 | 需补写 |
| 0.63 | `sql/phase34_ai_pilot.sql` | — | AI 试点（问答/画像/推荐） | 需补写 |
| 0.64 | `sql/phase35_ui_theme.sql` | — | UI 主题/暗色模式令牌 | 需补写 |

> `phase2_*` 不存在（历史缺失编号，非遗漏）。E 段共 37 个文件、覆盖 phase1–phase35。

## 基线重放的已知阻塞项（由 `bin/db-menu-check.ps1` 自动发现）

把 0.01–0.64 按序导入一个**全新库**目前会失败：`sys_menu` 存在 **64 处 `menu_id` 撞号**
（同一 ID 在不同脚本里对应不同菜单，第二个 INSERT 触发主键冲突中断）：

| ID 段 | 冲突脚本对 | 撞号数 | 示例 |
|---|---|---|---|
| 2300–2344 | `ry_oa_menu.sql` ↔ `ry_sam_menu.sql` | 45 | 2300 = `OA管理` vs `学籍与学位管理` |
| 2600–2625 | `ry_portal_cms_menu.sql` ↔ `phase1_upgrade.sql` | 13 | 2600 两处各自建目录菜单 |
| 2700–2705 | `ry_oa_menu_2700.sql` ↔ `phase4_tpm_enhancement.sql` | 6 | 2700 两处各自建菜单 |

成因：这些脚本都是在**已含全量库的存量环境**上增量编写的，作者按当时本地库的
空闲号段就地取号，彼此不感知。存量库不受影响（它只落地了其中一个版本），
但**「从 SQL 脚本手工初始化」这条路今天走不通**——这正是必须基线化的理由。

处置约定：

1. 不在本清单里粉饰（不把顺序改成“看起来能跑”），阻塞项显式存在；
2. 修复方式是给**后执行**的脚本改派 3000+ 保留段，并以 `db/migration/` + `db/rollback/` 成对脚本
   的形式提交（而不是直接改 `sql/` 里的历史文件），然后 `bin/db-baseline.ps1 -Resume` 续跑；
3. 存量库若存在“该菜单缺失”现象，属于当年静默插入失败的残留，需单独对账；
4. CI 以 `-IgnoreLegacy` 模式跑本项检查：**历史 64 处降为不计**，
   但 `db/migration/` 引入的任何新撞号直接 FAIL（详见 `.github/workflows/ci.yml`）。

完整实时清单：`powershell -File bin\db-menu-check.ps1`（不带参数）。

## 附录：`sys_menu` 实际占用（由脚本扫描得出，非设计意图）

| 区间 | 实际使用数 | 来源脚本 |
|---|---|---|
| 1–116、500–501、1000–1060 | 85 | `ry_20260417.sql` |
| 2000–2093 | 94 | `ry_brm_menu.sql` |
| 2100–2157 | 58 | `ry_tpm_menu.sql` |
| 2200–2256 | 57 | `ry_aem_menu.sql` |
| 2300–2351 | 52 | `ry_oa_menu.sql`（与 `ry_sam_menu.sql` 的 2300–2344 全量重叠） |
| 2400–2423 | 24 | `ry_dis_menu.sql` |
| 2500–2540、2550–2562 | 52 | `ry_portal_menu.sql`、`user_mgmt_upgrade.sql` |
| 2600–2634 | 23 | `ry_portal_cms_menu.sql`（与 `phase1_upgrade.sql` 的 2600–2625 重叠） |
| 2700–2751 | 52 | `ry_oa_menu_2700.sql`（与 `phase4_tpm_enhancement.sql` 的 2700–2705 重叠） |
| 2900–2906 | 7 | `fix_gpa_config_menu.sql` |
| 3000+ | 0 | 基线之后的新增变更保留段 |

> AEM 在 **2200** 段而非 2300；DIS 在 **2400** 段；SAM 与 OA 共用 2300 段。`db/README.md` 的分段表已按此修正。

## F. 不纳入基线的脚本（明确排除）

| 脚本 | 排除原因 |
|---|---|
| `sql/_drop_act.sql`、`sql/_drop_act_wrapped.sql`、`sql/_drop_flow.sql`、`sql/_drop_flow_wrapped.sql` | **破坏性运维脚本**（DROP Flowable 引擎表），只用于本地重放官方建表，绝不进入自动导入链路 |
| `sql/yangtze_seed_data.sql`、`sql/seed_feature_test_data.sql` | 演示/测试数据，与结构无关；由各环境按需手工导入 |
| `sql/备份/*.sql` | 含真实个人信息的整库 dump，**已出库并被 `.gitignore` 忽略**（V4.0 §7.3/C1），仅本机保留 |

## 校验

清单是否与 `sql/` 实际内容脱节，由脚本判定，不靠人眼：

```powershell
powershell -File bin\db-manifest-check.ps1     # 报告未登记文件与清单中已失效的条目
powershell -File bin\db-menu-check.ps1         # 扫描菜单 INSERT 的 menu_id，报告分段冲突/重复
powershell -File bin\db-baseline.ps1 -DryRun   # 打印将要执行的导入顺序，不实际连库
```
