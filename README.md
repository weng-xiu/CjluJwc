# 长江大学教务管理系统 (CjluJwc)

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-2.6-blue" alt="Vue">
  <img src="https://img.shields.io/badge/JDK-17%2B-orange" alt="JDK">
  <img src="https://img.shields.io/badge/MySQL-8.x-blue" alt="MySQL">
  <img src="https://img.shields.io/badge/Redis-5.x%2B-red" alt="Redis">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
</p>

> 长江大学教务处综合管理系统，面向高校教务管理场景，基于若依（RuoYi 3.9.2）快速开发框架构建，提供后台管理与前台师生服务双端能力。

## 系统架构

本项目采用前后端分离的双端架构：

- **后台管理端**：面向教务管理人员，覆盖基础资源、培养过程、考核评价、学籍学位、办公自动化、系统权限与数据对接七大领域。
- **前台用户端（师生互动服务门户）**：面向在校学生与教师，提供教务通知、课表、成绩、选课、考试、评教、学籍服务等互动入口，并内置移动端 H5（`/mobile`）。

后端为 Spring Boot 4 多模块分层设计（13 个 Maven 模块），管理端前端与门户端前端相互独立，均代理至同一后端服务。

## 技术栈

| 层级 | 技术 |
|---|---|
| 后端 | Spring Boot 4.0.3（要求 **JDK 17+**）、Spring Security + JWT、MyBatis 4.0.1、Druid 1.2.28、PageHelper、Redis（Lettuce） |
| 工作流 | Flowable 7.1.0（OA 公文流程，BPMN 引擎，关闭 CMMN/DMN 等子引擎） |
| 管理端前端 | Vue 2.6.12、Element UI 2.15.14、@vue/cli-service 4.4.6（webpack 4） |
| 门户端前端 | Vue 2、Element UI（含移动端 H5 页面） |
| 数据库 | MySQL 8.x（utf8mb4；Flowable 引擎表为官方 utf8/utf8_bin，同库共存） |
| 缓存 | Redis 5.x+（仓库 `bin/redis/` 内置 Windows 版） |
| 接口文档 | springdoc-openapi 3.0.2（Swagger UI） |
| 构建工具 | Maven 3.8+、Node.js / npm |
| 核心组件 | FastJSON2、Apache POI、Velocity、OSHI、Kaptcha、Yauaa |

## 模块说明

| 模块 | 说明 |
|---|---|
| `yu-admin` | 后台管理入口，Spring Boot 启动模块（唯一可执行 jar） |
| `yu-framework` | 核心框架，安全认证、数据源、AOP 等 |
| `yu-system` | 系统管理，用户、角色、部门、字典、菜单权限等 |
| `yu-common` | 通用工具，注解、工具类、异常、过滤器等 |
| `yu-quartz` | 定时任务调度（已落地学业预警等业务任务） |
| `yu-generator` | 代码生成器 |
| `yu-brm` | 基础资源管理：校区、教学楼、教室、院系、专业、班级、教师等 |
| `yu-tpm` | 培养过程管理：课程库、培养方案、学分结构、排课、排课优化、选课等 |
| `yu-aem` | 考核评价管理：考试计划、考场座位编排、监考、成绩与 GPA、评教等 |
| `yu-sam` | 学籍与学位管理：学籍、异动、学业预警、毕业/学位审核、证书、离校等 |
| `yu-dis` | 数据对接与共享：外部系统接口、同步任务、交换日志 |
| `yu-portal` | 师生互动服务门户后端（含门户移动端接口） |
| `yu-oa` | 办公自动化：Flowable 工作流、公文、会议、通知公告、日程 |
| `yu-ui` | 管理端前端（Vue 2） |
| `yu-portal-ui` | 门户端前端（Vue 2，含移动端 H5） |

## 核心业务功能

### 后台管理端（yu-ui）

| 领域 | 功能 |
|---|---|
| 基础资源管理 | 学年学期、校区/教学楼/教室、教室借用、设备资产与维保、院系（支持从系统部门同步）、专业、班级、教师信息与岗位资质 |
| 培养过程管理 | 课程库、培养方案（子表一次保存/发布/废止）、开课计划、排课与冲突检测、排课优化（自动分配教室）、调停课审批、选课轮次/规则/名单（冲突预检、替代建议、抽签、退课） |
| 考核评价管理 | 考试计划与发布、一键自动排座（蛇形）、一键派监考、成绩录入与导入、成绩复核、成绩统计、GPA 多算法配置与批量重算、评教问卷与结果、教学督导 |
| 学籍与学位管理 | 学籍维护、学籍异动、学业预警引擎（GPA/学分/出勤规则批量生成）、毕业/学位审核、证书管理、离校手续 |
| 办公自动化 | 流程定义与部署、待办/已办审批、公文流转、会议与会议室、通知公告、日程 |
| 数据对接与共享 | 外部系统登记、接口配置、同步任务、数据交换日志 |
| 系统管理 | 用户（含教师/学生账号批量同步）、角色、部门、岗位、字典、菜单、参数、日志、定时任务、代码生成、缓存/服务器监控 |

### 师生互动服务门户（yu-portal-ui，端口 81）

| 角色 | 功能 |
|---|---|
| 学生端 | 信息公开与通知、个人课表、成绩查询、选课中心（轮次/冲突预检/选课）、考试安排、评教、学籍查看与异动申请、学业预警查询 |
| 教师端 | 教师课表、成绩录入、教学任务、监考安排、评教结果、调停课申请 |
| 移动端 H5 | 访问 `/mobile`（或 `/m`），含首页、课表、成绩、考试、评教、监考、选课、预警 8 页，窄屏自动切换提示 |

## 环境要求

| 依赖 | 版本要求 | 说明 |
|---|---|---|
| JDK | **17+** | Spring Boot 4 强制要求，低于 17 无法编译/启动 |
| Maven | 3.8+ | 已配置阿里云镜像仓库 |
| MySQL | 8.x | 库名注意：配置文件默认 `yu-CjluJwc`，本机实际为全小写 `yu-cjlujwc`（详见下文） |
| Redis | 5.x+ | 默认 `127.0.0.1:6379` 无密码；仓库 `bin/redis/` 内置 Windows 版可直接启动 |
| Node.js | **16 LTS（推荐）** | `package.json` 声明 `>=8.9`，但 @vue/cli 4 / webpack 4 在 Node 17+ 会报 `ERR_OSSL_EVP_UNSUPPORTED`，需设置 `NODE_OPTIONS=--openssl-legacy-provider` 或改用 Node 16 |
| npm | 8+ | 两个前端工程各自独立安装依赖 |

## 端口说明

| 服务 | 端口 | 地址 / 说明 |
|---|---|---|
| 后端 yu-admin | 8080 | `http://localhost:8080`，管理端与门户端共用同一后端 |
| Swagger 接口文档 | 8080 | `http://localhost:8080/swagger-ui.html` |
| Druid 监控控制台 | 8080 | `http://localhost:8080/druid`（账号 `ruoyi` / `123456`） |
| 管理端前端 yu-ui | 80 | `http://localhost`（被占用时可设 `port` 环境变量） |
| 门户端前端 yu-portal-ui | 81 | `http://localhost:81`，移动端入口 `http://localhost:81/mobile` |
| MySQL | 3306 | — |
| Redis | 6379 | — |

## 快速开始

### 1. 环境准备

启动前请确保 MySQL、Redis 可用：

```powershell
# 方式一：使用仓库内置 Redis（Windows）
bin\redis\redis-server.exe bin\redis\redis.windows.conf

# 方式二：已注册为 Windows 服务时
net start Redis
```

### 2. 数据库初始化

默认库名 `yu-CjluJwc`、账号 `root/123456`，均可通过环境变量覆盖（见配置说明）。

> **库名大小写陷阱**：本机 MySQL 实际库名为全小写 `yu-cjlujwc`。Windows 下 `lower_case_table_names=1` 大小写不敏感所以能连上；**迁移到 Linux（大小写敏感）会直接连不上库**，请统一使用小写库名或设置 `DB_NAME` 环境变量。

两种初始化方式（任选其一）：

**方式一：整库迁移导入（推荐，含全部业务数据）**

```powershell
# 在源机器导出（产物在 bin/db-backup/，含结构+数据，自动完整性校验）
.\bin\db-export.ps1

# 在新机器导入（自动建库、utf8mb4 流式导入、打印表数量）
.\bin\db-import.ps1 -SqlFile D:\backup\yu-cjlujwc-<时间戳>.sql -DbUser root -DbPassword 新密码
# MySQL 未加入 PATH 时追加：-MysqlBin 'D:\MySQL\bin'
```

**方式二：从 SQL 脚本手工初始化**

按顺序导入 `sql/` 目录脚本：

1. `ry_20260417.sql` —— RuoYi 基础表（用户/角色/菜单/字典等）
2. `quartz.sql` —— 定时任务表
3. `flowable_create_all.sql` —— Flowable 工作流引擎表
4. 各业务模块脚本：`brm.sql`、`tpm.sql`、`aem.sql`、`sam.sql`、`dis.sql`、`oa.sql`、`portal.sql`、`portal_cms.sql`
5. 菜单/字典补丁：`ry_*_menu.sql`、`ry_*_dict.sql` 系列
6. 增量升级脚本：`phase*.sql` 系列（按编号顺序执行，新库若已含最新备份可跳过；规范见记忆中的幂等性要求）

导入后校验：`SHOW TABLES` 应不少于 163 张表，`SELECT menu_name FROM sys_menu LIMIT 5` 中文无乱码。

### 3. 配置说明

后端核心配置位于 `yu-admin/src/main/resources/`：

- `application-druid.yml`：数据库连接（默认 `localhost:3306/yu-CjluJwc`，`root/123456`），从库默认关闭。
- `application.yml`：服务端口 `8080`、Redis、文件上传路径、Token、Flowable 等。

所有环境相关项均支持**环境变量覆盖，无需改动文件**：

| 环境变量 | 说明 | 默认值 |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | 数据库地址、端口、库名 | `localhost` / `3306` / `yu-CjluJwc` |
| `DB_USER` / `DB_PASSWORD` | 数据库用户名、密码 | `root` / `123456` |
| `DB_USE_SSL` / `DB_ALLOW_PUBLIC_KEY` | 连远程库无 SSL 时建议 `false` / `true` | `true` / `false` |
| `SLAVE_ENABLED` | 从库数据源开关 | `false` |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` / `REDIS_DATABASE` | Redis 地址、端口、密码、库索引 | `127.0.0.1` / `6379` / （空） / `0` |
| `RUIYI_PROFILE` | 文件上传存储路径 | 项目内 `ruoyi/uploadPath`（Linux 建议 `/home/ruoyi/uploadPath`） |
| `LOG_PATH` | 日志输出目录 | 启动目录下 `logs/`（Linux 建议 `/home/ruoyi/logs`） |

PowerShell 设置示例（启动后端前执行）：

```powershell
$env:DB_PASSWORD="yourPassword"
$env:REDIS_HOST="127.0.0.1"
$env:RUIYI_PROFILE="D:/app/ruoyi/uploadPath"
```

### 4. 后端启动

**方式一：Maven 命令（开发）**

```bash
# 首次或依赖变更后，先构建全部模块
mvn clean install -DskipTests

# 启动 yu-admin（注意：根 pom 将构建输出目录重定向到 build-temp/，而非各模块 target/）
mvn spring-boot:run -pl yu-admin
```

**方式二：运行 jar 包**

```bash
mvn clean package -Dmaven.test.skip=true    # 或 bin/package.bat
java -jar build-temp/yu-admin/yu-admin.jar --server.port=8080
```

**方式三：脚本管理（Windows，针对已打好的 jar）**

```bash
ry.bat        # 菜单式：启动/停止/重启/状态（需在 jar 所在目录或使用 bin/start-backend.bat）
```

> ⚠️ `bin/start-all.ps1`、`bin/start-backend.ps1`、`bin/start-all-backend.ps1` 等脚本内含**旧开发机的硬编码绝对路径**（`h:\project_open`、`F:\web`、`C:\Users\q\...`），在新机器上不可直接使用，请按上述方式手动启动，或先修正脚本中的路径与 `JAVA_HOME`。

启动成功后：

- 后端服务：`http://localhost:8080`
- Swagger 文档：`http://localhost:8080/swagger-ui.html`
- Druid 监控：`http://localhost:8080/druid`（`ruoyi` / `123456`）

### 5. 前端启动

两个前端工程独立启动，默认将 `/dev-api` 前缀请求代理至 `http://localhost:8080`，**请先启动后端**。

```bash
# 管理端（默认端口 80，标题：长江大学教务处）
cd yu-ui
npm install
npm run dev

# 门户端（默认端口 81，标题：师生互动服务门户）
cd yu-portal-ui
npm install
npm run dev
```

- 端口被占用时：`$env:port=8081; npm run dev`，或修改对应 `vue.config.js` 中 `port`。
- Node 17+ 报 OpenSSL 错误时：`$env:NODE_OPTIONS="--openssl-legacy-provider"`。

### 6. 登录与默认账号

| 入口 | 地址 | 默认账号 |
|---|---|---|
| 管理端 | `http://localhost` | `admin` / `admin123` |
| 门户端 | `http://localhost:81` | `admin` / `admin123` |

> 登录页已默认填充上述凭证（含记住密码）。连续输错密码 5 次会锁定 10 分钟（锁定计数存于 Redis，误锁时可清除对应 Redis 键）。

### 7. 前端构建（生产）

```bash
cd yu-ui          && npm run build:prod    # 产物 yu-ui/dist
cd yu-portal-ui   && npm run build:prod    # 产物 yu-portal-ui/dist
```

## 常见问题排查

| 现象 | 原因与处理 |
|---|---|
| 后端启动即报数据库连不上 | 库名大小写（Linux 下用 `yu-cjlujwc`）或 `DB_PASSWORD` 不匹配；检查 MySQL 服务与 3306 端口 |
| 启动报 Redis 连接失败 | Redis 未运行，先执行 `bin\redis\redis-server.exe bin\redis\redis.windows.conf` |
| Maven 编译报 `invalid target release: 17` | `JAVA_HOME` 未指向 JDK 17+，修正后重开终端 |
| 前端 `npm run dev` 报 `ERR_OSSL_EVP_UNSUPPORTED` | Node ≥17 与 webpack 4 不兼容，改用 Node 16 或设 `NODE_OPTIONS=--openssl-legacy-provider` |
| 执行 `bin/*.ps1` 报"字符串缺少终止符"等假语法错误 | 脚本含中文，必须为 **UTF-8 with BOM** 编码（PowerShell 5.1 无 BOM 按 GBK 解码），见 `bin/migrate-readme.md` |
| 找不到构建产物 `target/*.jar` | 根 `pom.xml` 将输出目录重定向到 `build-temp/<模块名>/`，jar 位于 `build-temp/yu-admin/yu-admin.jar` |
| 接口 403 / 菜单缺失 | 检查对应角色是否分配了门户/管理端菜单权限；菜单平台双端隔离 |

## 项目结构

```
CjluJwc/
├── yu-admin/          # 后台启动入口（端口 8080）
├── yu-framework/      # 核心框架
├── yu-system/         # 系统管理
├── yu-common/         # 通用工具
├── yu-quartz/         # 定时任务
├── yu-generator/      # 代码生成器
├── yu-brm/            # 基础资源管理
├── yu-tpm/            # 培养过程管理
├── yu-aem/            # 考核评价管理
├── yu-sam/            # 学籍与学位管理
├── yu-dis/            # 数据对接与共享
├── yu-portal/         # 门户后端（含移动端接口）
├── yu-oa/             # 办公自动化（Flowable 工作流）
├── yu-ui/             # 管理端前端（端口 80）
├── yu-portal-ui/      # 门户端前端（端口 81，/mobile 为移动端）
├── sql/               # 数据库脚本（基础建库、模块脚本、phase* 增量升级、备份）
├── bin/               # 运维脚本与内置 Redis（redis/、db-export/import.ps1、启动脚本）
├── build-temp/        # Maven 构建输出目录（各模块 jar）
├── doc/               # 项目文档（含《长江大学教务管理系统需求优化计划》docx/md）
├── logs/              # 运行日志（sys-info/sys-error/sys-user，路径可用 LOG_PATH 覆盖）
└── ruoyi/uploadPath   # 文件上传目录（运行时生成，路径可用 RUIYI_PROFILE 覆盖）
```

## 相关文档

- [bin/migrate-readme.md](bin/migrate-readme.md)：数据库与环境无缝迁移说明（导出/导入、环境变量、验收清单）
- [doc/长江大学教务管理系统需求优化计划.md](doc/长江大学教务管理系统需求优化计划.md)：功能级现状盘点、竞品对标与需求优化路线（V2.0）
- [doc/若依环境使用手册.docx](doc/若依环境使用手册.docx)：若依基础环境手册

## 开源协议

本项目基于 [MIT License](LICENSE) 开源。
