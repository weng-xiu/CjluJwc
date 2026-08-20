# 长江大学教务管理系统 (CjluJwc)

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-2.x-blue" alt="Vue">
  <img src="https://img.shields.io/badge/JDK-17%2B-orange" alt="JDK">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
</p>

> 长江大学教务处综合管理系统，面向高校教务管理场景，基于若依（RuoYi）快速开发框架构建，提供后台管理与前台师生服务双端能力。

## 系统架构

本项目采用前后端分离的双端架构：

- **后台管理端**：面向教务管理人员，覆盖基础资源、培养过程、考核评价、学籍学位、系统权限与数据对接六大领域。
- **前台用户端**：面向在校学生与教师，提供教务通知、课表、成绩、选课、考试、评教、学籍服务等互动服务入口。

后端采用 Spring Boot 多模块分层设计，前端管理端与门户端相互独立。

## 技术栈

| 层级 | 技术 |
|---|---|
| 后端 | Spring Boot 4.x（JDK 17+）、Spring Security、MyBatis、Druid、Redis、JWT |
| 管理端前端 | Vue 2、Element UI |
| 门户端前端 | Vue 2、Element UI |
| 数据库 | MySQL |
| 构建工具 | Maven、Node.js/npm |
| 核心组件 | PageHelper、FastJSON2、Apache POI、Velocity、OSHI、Kaptcha |

## 模块说明

| 模块 | 说明 |
|---|---|
| `yu-admin` | 后台管理入口，Spring Boot 启动模块 |
| `yu-framework` | 核心框架，安全认证、数据源、AOP 等 |
| `yu-system` | 系统管理，用户、角色、部门、字典、菜单权限等 |
| `yu-common` | 通用工具，注解、工具类、异常、过滤器等 |
| `yu-quartz` | 定时任务调度 |
| `yu-generator` | 代码生成器 |
| `yu-brm` | 基础资源管理：校区、教学楼、教室、院系、专业、班级、教师等 |
| `yu-tpm` | 培养过程管理：课程库、培养方案、学分结构、排课、选课等 |
| `yu-aem` | 考核评价管理：考试安排、监考、成绩、教学评价等 |
| `yu-sam` | 学籍与学位管理：学籍、毕业审核、学位评审、证书等 |
| `yu-dis` | 数据对接与共享：外部系统接口、同步任务等 |
| `yu-portal` | 师生互动服务门户后端 |
| `yu-ui` | 管理端前端（Vue 2） |
| `yu-portal-ui` | 门户端前端（Vue 2） |

## 核心业务功能（后台管理端）

| 领域 | 功能 |
|---|---|
| 基础资源管理 | 校区、教学楼、教室、院系、专业、班级、教师信息维护 |
| 培养过程管理 | 课程库、培养方案、学分结构、排课、选课管理 |
| 考核评价管理 | 考试安排、监考安排、成绩录入与查询、教学评价 |
| 学籍与学位管理 | 学籍信息、毕业审核、学位评审、证书管理 |
| 系统权限管理 | 用户、角色、部门、岗位、字典、菜单与操作日志 |
| 数据对接与共享 | 外部系统接口配置、数据同步任务 |

## 师生互动服务（前台用户端）

| 角色 | 功能 |
|---|---|
| 学生端 | 教务通知、个人课表、成绩查询、选课中心、考试安排、评教入口、学籍服务 |
| 教师端 | 教务通知、教师课表、成绩录入、监考安排、评教结果、教学任务查询、调停课申请 |

## 环境要求

- JDK 17+
- MySQL 8.x
- Redis 5.x+
- Maven 3.8+
- Node.js 16+
- npm 8+

## 快速开始

### 环境准备

启动前请确保已安装并运行以下服务：

- **JDK 17+**、**Maven 3.8+**
- **MySQL 8.x**：创建数据库（默认库名 `yu-CjluJwc`），并导入 `sql/` 目录下的 SQL 脚本。
- **Redis 5.x+**：默认连接 `127.0.0.1:6379`，无密码。
- **Node.js 16+**、**npm 8+**

### 配置说明

后端核心配置位于 `yu-admin/src/main/resources/`：

- `application-druid.yml`：数据库连接配置，默认 `localhost:3306`，库名 `yu-CjluJwc`，用户名/密码 `root/123456`。
- `application.yml`：服务端口、Redis、文件上传路径等配置，后端默认端口 `8080`。

以上配置均支持通过环境变量覆盖，无需改动文件即可适配不同环境：

| 环境变量 | 说明 | 默认值 |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | 数据库地址、端口、库名 | `localhost` / `3306` / `yu-CjluJwc` |
| `DB_USER` / `DB_PASSWORD` | 数据库用户名、密码 | `root` / `123456` |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 地址、端口、密码 | `127.0.0.1` / `6379` / （空） |
| `RUIYI_PROFILE` | 文件上传存储路径 | Windows 下为项目内 `ruoyi/uploadPath` |

### 后端启动

**方式一：Maven 命令**

在项目根目录执行：

```bash
mvn clean install
mvn spring-boot:run -pl yu-admin
```

**方式二：启动脚本（Windows）**

```bash
ry.bat
```

或使用 `bin/start-backend.bat`。

启动成功后，后端服务运行在 `http://localhost:8080`：

- 接口文档（Swagger）：`http://localhost:8080/swagger-ui.html`
- Druid 监控控制台：`http://localhost:8080/druid`（默认账号 `ruoyi` / `123456`）

### 前端启动

项目包含两个独立的前端工程，分别对应后台管理端与师生门户端，均可独立启动。前端默认将 `/dev-api` 前缀的请求代理至 `http://localhost:8080`，因此**请先启动后端**。

#### 管理端（yu-ui）

```bash
cd yu-ui
npm install
npm run dev
```

- 默认端口：`80`，访问地址：`http://localhost`
- 页面标题：长江大学教务处

#### 门户端（yu-portal-ui）

```bash
cd yu-portal-ui
npm install
npm run dev
```

- 默认端口：`81`，访问地址：`http://localhost:81`
- 页面标题：师生互动服务门户

#### 前端构建

```bash
# 管理端生产构建（产物在 yu-ui/dist）
cd yu-ui
npm run build:prod

# 门户端生产构建（产物在 yu-portal-ui/dist）
cd yu-portal-ui
npm run build:prod
```

> 提示：若 `80` / `81` 端口被占用，可设置 `port` 环境变量，或修改对应 `vue.config.js` 中的 `port`。

## 项目结构

```
CjluJwc/
├── yu-admin/          # 后台启动入口
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
├── yu-portal/         # 门户后端
├── yu-ui/             # 管理端前端
├── yu-portal-ui/      # 门户端前端
└── sql/               # 数据库脚本
```

## 开源协议

本项目基于 [MIT License](LICENSE) 开源。
