# MySQL 首启初始化脚本目录

本目录用于 `docker compose up` 时 MySQL 容器**首次启动**自动导入数据库。

## 为什么目录里没有任何 .sql

原先 `docker-compose.yml` 直接挂载仓库内的
`sql/备份/yu-cjlujwc_20260821.sql`（整库 dump，含学生/教师真实个人信息与成绩）。
按 V4.0 报告 §7.3「C1 合规止血」与《个人信息保护法》最小必要原则，
含真实数据的 dump 一律不得进入版本库，因此：

- 该 dump 已从 git 索引移除（本地文件保留，便于个人开发库重建）；
- `.gitignore` 已忽略 `/sql/备份/` 与本目录下的 `*.sql`；
- 编排不再依赖任何仓库内的数据文件。

## 本机如何准备初始化脚本

按需选择一种，把生成的 `.sql` 放进本目录，文件名前缀数字决定执行顺序。

### 方案 A：结构化基线（推荐，全新环境）

不携带任何业务数据，只建表 + 若依基础字典/菜单：

```powershell
# 1) 用 docker 起一个空 mysql 后，按顺序执行仓库内脚本
#    顺序以 sql/README.md 与 Flyway 基线 db/migration 为准
sql/ry_20260417.sql
sql/quartz.sql
sql/flowable_create_all.sql
sql/brm.sql sql/tpm.sql sql/aem.sql sql/sam.sql sql/dis.sql sql/oa.sql sql/portal.sql sql/portal_cms.sql
sql/ry_*_menu.sql
```

也可以直接开启 Flyway（见根 `README.md`「数据库版本管理」一节），
让后端启动时自动执行 `db/migration` 下的基线与增量脚本，
此时本目录留空即可，`docker compose up` 只负责起一个空实例。

### 方案 B：从现有开发库导出脱敏 dump

```powershell
mysqldump -h 127.0.0.1 -P 3306 -uroot -p `
  --single-transaction --routines --triggers --default-character-set=utf8mb4 `
  --databases yu-cjlujwc --result-file=./docker/mysql-init/01_baseline.sql
```

导出后**必须先脱敏**再使用：`sys_user`（密码散列/手机号/邮箱）、
`sam_student`、`brm_teacher`、`tpm_*` 选课与成绩明细等业务表中的真实个人信息
需替换为测试数据或清空；可参考仓库内 `sql/yangtze_seed_data.sql`、
`sql/seed_feature_test_data.sql` 的构造方式重建演示数据。

## 注意事项

- MySQL 官方镜像只在数据卷 `mysql-data` **为空**时执行 `docker-entrypoint-initdb.d`；
  改动脚本后需 `docker compose down -v` 或删除该卷才会重新导入。
- Navicat 全量导出无 `USE` 语句，依赖 compose 中 `MYSQL_DATABASE=yu-cjlujwc` 作为默认库；
  自行导出的 dump 若带 `CREATE DATABASE`/`USE`，请去掉 `--databases` 参数重新导出。
- 本目录内容属于本机开发资产，不要提交。
