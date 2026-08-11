# 数据库与环境无缝迁移说明

适用场景：把当前开发机的 MySQL 数据库 `yu-cjlujwc`（含全部表结构与业务数据）完整迁移到另一台机器/新环境，使项目在新环境直接运行。

> **本机实测环境（已验证通过）**
> - MySQL 8.0.12，phpstudy 绿色版，路径 `F:\phpstudy_pro\Extensions\MySQL8.0.12\bin`
>   （**未注册为 Windows 服务、未加入 PATH**，故脚本默认已内置该路径；新机如不同请用 `-MysqlBin` 指定）
> - 源库 `yu-cjlujwc`：**163 张表、31242 行数据、12.25 MB、utf8mb4**，无存储过程/视图/触发器
> - 已完成端到端演练：导出 → 导入临时库 → **163 张表逐表行数比对全部一致、中文无乱码**
>
> **重要：库名实际为全小写 `yu-cjlujwc`**
> 项目配置里写的是 `yu-CjluJwc`，在 Windows 下因 MySQL 大小写不敏感而能连上；
> 但迁移到 **Linux（大小写敏感）后会直接连不上库**。迁到 Linux 时请统一使用小写库名，
> 或设置 `DB_NAME` 环境变量为实际库名。

迁移方式：**mysqldump 全量导出 → 传输 → 新机建库导入 → 配置外部化（环境变量）→ 启动验证**。
优点：跨 MySQL 版本/跨操作系统通用，不依赖物理文件拷贝，数据一致性由 dump 保证。

---

## 一、涉及改动文件清单

| 文件 | 改动 | 说明 |
|------|------|------|
| `bin/db-export.ps1` | 新增 | 当前机全量导出脚本 |
| `bin/db-import.ps1` | 新增 | 新机建库+导入+校验脚本 |
| `yu-admin/src/main/resources/application-druid.yml` | 改 | 主库/从库连接改为 `${DB_*:}` 占位符 |
| `yu-admin/src/main/resources/application.yml` | 改 | Redis 地址、上传路径改为 `${REDIS_*:}`/`${RUIYI_PROFILE:}` 占位符 |
| `yu-admin/src/main/resources/logback.xml` | 改 | 日志路径改为 `${LOG_PATH:-默认}` 占位符 |
| `pom.xml` | 改 | `<build><directory>` 由绝对路径改回 `${project.basedir}/target`（修复换机器构建失败） |

---

## 二、当前机：导出数据

1. 确认本机 MySQL 已启动。脚本会自动定位 `mysqldump`：优先用内置的 phpstudy 路径，找不到再回退 PATH。
2. 在项目根目录执行（默认 localhost/root/123456，库名 `yu-cjlujwc`）：

   ```powershell
   .\bin\db-export.ps1
   ```

   如需指定连接或 MySQL 目录：

   ```powershell
   .\bin\db-export.ps1 -DbHost 127.0.0.1 -DbUser root -DbPassword 123456 -MysqlBin 'D:\MySQL\bin' -OutFile D:\backup\yu-cjlujwc.sql
   ```

3. 脚本在 `bin/db-backup/` 生成 `yu-cjlujwc-<时间戳>.sql`（含结构与全部业务数据，utf8mb4，`--set-gtid-purged=OFF` 防 GTID 导入报错）。
4. 脚本结尾会自动做完整性校验，正常输出如下（**表数应为 163**）：

   ```
   [2/3] 导出完成：
          文件：H:\project_open\CjluJwc\bin\db-backup\yu-cjlujwc-20260807-084526.sql
          大小：3.69 MB
   [3/3] 完整性校验：
          头部标记：通过
          结束标记：通过
          建表语句：163 张表
   ```

   若「结束标记」显示缺失，说明导出被中断，脚本会以非 0 退出，**该文件不可用于迁移**。

---

## 三、新环境：准备与导入

### 3.1 前置条件
- 安装同版本或更高版本的 MySQL（建议 8.0+），启动服务。
- 安装 Redis（若需缓存/会话；本项目 Redis 无密码，可按需设密码）。
- （可选）安装 JDK 17 与 Maven，用于构建运行。

### 3.2 导入
将导出的 `.sql` 传到新机，执行：

```powershell
.\bin\db-import.ps1 -SqlFile D:\backup\yu-cjlujwc-20260807-084526.sql -DbHost 127.0.0.1 -DbUser root -DbPassword 新密码
```

若新机 MySQL 未加入 PATH（如 phpstudy/绿色版），追加 `-MysqlBin`：

```powershell
.\bin\db-import.ps1 -SqlFile D:\backup\yu-cjlujwc.sql -MysqlBin 'F:\phpstudy_pro\Extensions\MySQL8.0.12\bin'
```

脚本会：定位客户端 → 检测库是否存在（存在则 DROP 重建，需确认，加 `-Force` 跳过）→ 以 utf8mb4 建库 → 通过 stdin 流式导入 → 打印表数量。

**验收标准：表数量应为 163**，与源库一致。若为 0，说明导入未真正执行，请检查报错。

> 导入采用 .NET Process 将 SQL 文件字节流直接写入 mysql 的 stdin，
> 而非 `-e "source xxx"` 或 cmd 的 `<` 重定向——后两者在路径含空格、
> 或参数含空格分号（如 `--init-command`）时会被拆错，导致**库建好但一张表都没有的静默失败**。

---

## 四、连接配置（关键：无需改代码）

项目配置已改为 **环境变量占位符**，默认值保留当前开发机值。换环境只需在启动应用前设置环境变量，或直接在 yml 中改默认值。

### 4.1 数据库（application-druid.yml）

| 环境变量 | 默认值 | 含义 |
|----------|--------|------|
| `DB_HOST` | `localhost` | MySQL 主机 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_NAME` | `yu-CjluJwc` | 库名（实际库为小写 `yu-cjlujwc`，迁 Linux 必须改小写） |
| `DB_USER` | `root` | 用户名 |
| `DB_PASSWORD` | `123456` | 密码 |
| `DB_USE_SSL` | `true` | 连远程库建议设 `false` |
| `DB_ALLOW_PUBLIC_KEY` | `false` | 远程库无 SSL 时设 `true` |
| `SLAVE_ENABLED` | `false` | 从库开关 |

### 4.2 Redis（application.yml）

| 环境变量 | 默认值 | 含义 |
|----------|--------|------|
| `REDIS_HOST` | `127.0.0.1` | Redis 主机 |
| `REDIS_PORT` | `6379` | 端口 |
| `REDIS_DATABASE` | `0` | 库索引 |
| `REDIS_PASSWORD` | 空 | 密码（若新环境设了密码必填） |

### 4.3 路径（application.yml / logback.xml）

| 环境变量 | 默认值 | 含义 |
|----------|--------|------|
| `RUIYI_PROFILE` | `H:/project_open/CjluJwc/ruoyi/uploadPath` | 文件上传目录（Linux 设如 `/home/ruoyi/uploadPath`） |
| `LOG_PATH` | `H:/project_open/CjluJwc/ruoyi/logs` | 日志目录（Linux 设如 `/home/ruoyi/logs`） |

### 4.4 设置方式示例（Windows PowerShell 启动前）

```powershell
$env:DB_HOST="192.168.1.100"
$env:DB_PASSWORD="NewPass123"
$env:DB_USE_SSL="false"
$env:DB_ALLOW_PUBLIC_KEY="true"
$env:REDIS_HOST="192.168.1.100"
$env:RUIYI_PROFILE="D:/app/ruoyi/uploadPath"
$env:LOG_PATH="D:/app/ruoyi/logs"
```

Linux 则：`export DB_HOST=192.168.1.100` 等。

---

## 五、启动验证清单

1. **数据库连通性**：`mysql -h $DB_HOST -u $DB_USER -p -e "USE \`yu-cjlujwc\`; SHOW TABLES;"` 能列出表。
2. **数据完整性**：`db-import.ps1` 结尾打印的表数量应为 **163**，与源库一致。
3. **逐表行数比对（推荐，可发现"表在但数据缺"）**：在新机执行下述脚本，与源库总行数 **31242** 对照：

   ```powershell
   $m='mysql'  # 或 MySQL bin 下的 mysql.exe 完整路径
   $a='--host=127.0.0.1','--port=3306','--user=root','--password=密码','-N','-B'
   $t = & $m @a -e "SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema='yu-cjlujwc' AND table_type='BASE TABLE';"
   $sum=0; foreach($x in $t){ if($x){ $sum += [int](& $m @a -e "SELECT COUNT(*) FROM ``yu-cjlujwc``.``$x``;") } }
   "TOTAL_ROWS=$sum"   # 期望 31242
   ```

4. **中文校验**：`SELECT menu_name FROM sys_menu ORDER BY menu_id LIMIT 5;` 应显示「系统管理/系统监控/系统工具/若依官网/用户管理」，无乱码。
5. **Redis 连通性**：`redis-cli -h $REDIS_HOST ping` 返回 `PONG`。
   （注意：**当前开发机 6379 端口未监听，即本机没有运行 Redis**。若应用启动时报 Redis 连接失败，需先在新环境安装并启动 Redis。）
6. **应用启动**：`mvn clean package` 后运行 `yu-admin`，观察日志 `com.yu` 级别无数据库连接异常、Druid 连接池初始化成功。
7. **功能验证**：登录后台，检查菜单、业务数据是否完整；上传文件验证 `RUIYI_PROFILE` 路径可写。

---

## 六、注意事项

- **库名含连字符** `yu-cjlujwc`，所有 SQL 引用须加反引号，脚本已处理。
- **脚本必须保存为 UTF-8 with BOM**。`bin/*.ps1` 含中文，Windows PowerShell 5.1 在无 BOM 时会按 GBK 解码，
  导致中文字节被拆错、引号配对失败，报 "缺少右大括号 / 字符串缺少终止符" 等假语法错误。
  用编辑器另存时请确认带 BOM；若被工具改成无 BOM，可执行：

  ```powershell
  $f='bin\db-export.ps1'
  $t=[System.IO.File]::ReadAllText($f,[System.Text.UTF8Encoding]::new($false))
  [System.IO.File]::WriteAllText($f,$t,(New-Object System.Text.UTF8Encoding($true)))
  ```

- **mysql/mysqldump 的密码告警会写入 stderr**。脚本中若设 `$ErrorActionPreference='Stop'`，
  PowerShell 会把它当成终止性错误，造成"命令明明成功却报失败"。两个脚本均已改为
  以 `$LASTEXITCODE` / `Process.ExitCode` 作为唯一成功判据。
- **字符集混合**：业务表为 utf8mb4，Flowable 引擎表为 utf8/utf8_bin（官方原生）。同库共存，迁移后一般无碍；避免对两类表做混排 JOIN（会触发 collation 冲突）。
- **不要把导出的 .sql 提交进 Git**（含数据/可能含敏感信息）。`bin/db-backup/` 建议加入 `.gitignore`。
- **安全加固（上新环境建议）**：Druid 监控台默认 `ruoyi/123456` 且白名单为空、`token.secret` 为弱口令，公网部署前请修改 `application-druid.yml` 与 `application.yml` 相关项。
- **`bin/extract-flowable-sql.ps1`、`bin/verify-oa.ps1` 中仍有旧机器的绝对路径**（D 盘/F 盘），仅用于特定本机运维，迁移无关，但若需在新机复用请先修正路径。
