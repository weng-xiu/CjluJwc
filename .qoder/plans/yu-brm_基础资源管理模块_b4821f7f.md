# yu-brm 基础资源管理模块开发计划

## 概述
创建 yu-brm Maven 模块，实现 "基础与资源管理" 三大功能共 15 张数据表的完整 CRUD，代码风格遵循 `yu-generator/src/main/resources/vm` 下的 Velocity 模板。

---

## Task 1: 创建 Maven 模块骨架

**文件清单：**
- `d:\project\CjluJwc\CjluJwc\yu-brm\pom.xml` — Maven 模块配置，继承 parent pom，依赖 yu-common
- 修改 `d:\project\CjluJwc\CjluJwc\pom.xml` — 在 `<modules>` 添加 `<module>yu-brm</module>`，在 `<dependencyManagement>` 添加 yu-brm 声明
- 修改 `d:\project\CjluJwc\CjluJwc\yu-admin\pom.xml` — 添加 yu-brm 依赖

**验证：** `mvn clean install -DskipTests` 成功通过

---

## Task 2: 数据库建表 SQL

创建 `d:\project\CjluJwc\CjluJwc\sql\brm.sql` 包含以下 15 张表：

### 基础信息管理 (8张)
| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `brm_academic_year` | 学年 | year_id, year_name, start_date, end_date, status |
| `brm_semester` | 学期 | semester_id, semester_name, academic_year_id, start_date, end_date, semester_order, status |
| `brm_department` | 院系(树形) | dept_id, parent_id, dept_code, dept_name, leader, phone, email, order_num, status |
| `brm_major` | 专业 | major_id, major_code, major_name, dept_id, education_level, duration, status |
| `brm_class` | 班级 | class_id, class_code, class_name, major_id, grade, student_count, status |
| `brm_campus` | 校区 | campus_id, campus_name, campus_address, order_num, status |
| `brm_building` | 教学楼 | building_id, building_name, building_code, campus_id