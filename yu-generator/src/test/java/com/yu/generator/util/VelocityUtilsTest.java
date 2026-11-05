package com.yu.generator.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSONObject;
import com.yu.common.constant.GenConstants;
import com.yu.generator.domain.GenTable;
import com.yu.generator.domain.GenTableColumn;

/**
 * 模板上下文与文件名映射测试（G5 覆盖率爬坡 · yu-generator）。
 *
 * <p>{@code VelocityUtils} 决定「生成什么模板、写到哪个路径、模板里拿到的变量叫什么」。
 * 它一旦漏键，Velocity 对未定义变量是**静默输出空串**（不报错、不失败），
 * 生成物看起来正常但内容缺失，例如 {@code packageName} 空 → 生成的 Java 没有 package 行。
 * 因此这里除正向断言外，还专门锁住「options 为 null 不抛异常」与「三套前端模板各自取对目录」。</p>
 *
 * @author G5
 */
class VelocityUtilsTest
{
    @Test
    @DisplayName("prepareContext：单表上下文的关键变量与派生值")
    void prepareContext_crud()
    {
        GenTable table = crudTable();

        VelocityContext context = VelocityUtils.prepareContext(table);

        assertEquals(GenConstants.TPL_CRUD, context.get("tplCategory"));
        assertEquals("sys_student", context.get("tableName"));
        assertEquals("学生信息", context.get("functionName"));
        assertEquals("Student", context.get("ClassName"));
        assertEquals("student", context.get("className"));
        assertEquals("system", context.get("moduleName"));
        assertEquals("Student", context.get("BusinessName"));
        assertEquals("student", context.get("businessName"));
        assertEquals("com.yu", context.get("basePackage"));
        assertEquals("com.yu.system", context.get("packageName"));
        assertEquals("yu", context.get("author"));
        // 双列布局 → 12
        assertEquals("12", context.get("colSpan"));
        assertEquals("system:student", context.get("permissionPrefix"));
        assertEquals("3", context.get("parentMenuId"));
        assertNotNull(context.get("datetime"));
        assertEquals(table.getPkColumn(), context.get("pkColumn"));
        assertEquals(table.getColumns(), context.get("columns"));

        @SuppressWarnings("unchecked")
        HashSet<String> imports = (HashSet<String>) context.get("importList");
        assertTrue(imports.contains("java.util.Date"));
        assertTrue(imports.contains("com.fasterxml.jackson.annotation.JsonFormat"));
        assertTrue(imports.contains("java.math.BigDecimal"));

        assertEquals("'sys_normal_disable'", context.get("dicts"));
    }

    @Test
    @DisplayName("prepareContext：functionName 为空时用占位文案（模板里不会出现空白标题）")
    void prepareContext_blankFunctionName()
    {
        GenTable table = crudTable();
        table.setFunctionName("");

        assertEquals("【请填写功能名称】", VelocityUtils.prepareContext(table).get("functionName"));
    }

    @Test
    @DisplayName("prepareContext：options 为 null 不抛异常，上级菜单回落默认值 3")
    void prepareContext_nullOptions()
    {
        GenTable table = crudTable();
        table.setOptions(null);

        VelocityContext context = VelocityUtils.prepareContext(table);

        assertEquals("3", context.get("parentMenuId"));
        assertEquals(Boolean.FALSE, context.get("genView"));
    }

    @Test
    @DisplayName("prepareContext：options 带 parentMenuId 与 genView 时按配置生效")
    void prepareContext_optionsOverrides()
    {
        GenTable table = crudTable();
        table.setOptions("{\"parentMenuId\":\"2000\",\"genView\":true}");

        VelocityContext context = VelocityUtils.prepareContext(table);

        assertEquals("2000", context.get("parentMenuId"));
        assertEquals(Boolean.TRUE, context.get("genView"));
    }

    @Test
    @DisplayName("prepareContext：树表补 treeCode/treeParentCode/treeName 与展开列序号")
    void prepareContext_tree()
    {
        GenTable table = crudTable();
        table.setTplCategory(GenConstants.TPL_TREE);
        table.setOptions("{\"treeCode\":\"dept_id\",\"treeParentCode\":\"parent_id\",\"treeName\":\"dept_name\"}");
        table.setColumns(new ArrayList<>(Arrays.asList(
                column("dept_id", GenConstants.TYPE_LONG, GenConstants.REQUIRE),
                column("dept_name", GenConstants.TYPE_STRING, GenConstants.REQUIRE),
                column("order_num", GenConstants.TYPE_INTEGER, GenConstants.REQUIRE))));

        VelocityContext context = VelocityUtils.prepareContext(table);

        assertEquals("deptId", context.get("treeCode"));
        assertEquals("parentId", context.get("treeParentCode"));
        assertEquals("deptName", context.get("treeName"));
        // 带下划线的两个键给模板直接用（列名原样，不转驼峰）
        assertEquals("parent_id", context.get("tree_parent_code"));
        assertEquals("dept_name", context.get("tree_name"));
        // dept_name 是第 2 个「列表列」
        assertEquals(2, context.get("expandColumn"));
    }

    @Test
    @DisplayName("prepareContext：主子表补子表类名、外键类名与子表导入包")
    void prepareContext_sub()
    {
        GenTable table = crudTable();
        table.setTplCategory(GenConstants.TPL_SUB);
        table.setSubTableName("sys_student_item");
        table.setSubTableFkName("student_id");
        GenTable subTable = crudTable();
        subTable.setClassName("StudentItem");
        subTable.setColumns(new ArrayList<>(Arrays.asList(
                column("item_id", GenConstants.TYPE_LONG, GenConstants.REQUIRE),
                // create_time 是超类字段（BaseEntity 已提供），不会再产生 Date 导入；
                // 要验证子表导入集合，必须用业务日期列
                column("enroll_date", GenConstants.TYPE_DATE, GenConstants.REQUIRE))));
        table.setSubTable(subTable);

        VelocityContext context = VelocityUtils.prepareContext(table);

        assertEquals(subTable, context.get("subTable"));
        assertEquals("sys_student_item", context.get("subTableName"));
        assertEquals("student_id", context.get("subTableFkName"));
        assertEquals("StudentId", context.get("subTableFkClassName"));
        assertEquals("studentId", context.get("subTableFkclassName"));
        assertEquals("StudentItem", context.get("subClassName"));
        assertEquals("studentItem", context.get("subclassName"));

        @SuppressWarnings("unchecked")
        HashSet<String> subImports = (HashSet<String>) context.get("subImportList");
        assertTrue(subImports.contains("java.util.Date"));
        // 主子表时主表本身要 import List
        @SuppressWarnings("unchecked")
        HashSet<String> imports = (HashSet<String>) context.get("importList");
        assertTrue(imports.contains("java.util.List"));
    }

    @Test
    @DisplayName("getTemplateList：crud + 三套前端模板各取对应目录")
    void getTemplateList_webTypes()
    {
        GenTable vue2 = crudTable();
        List<String> vue2Templates = VelocityUtils.getTemplateList(vue2);
        assertTrue(vue2Templates.contains("vm/js/api.js.vm"));
        assertTrue(vue2Templates.contains("vm/vue/index.vue.vm"));
        assertFalse(vue2Templates.contains("vm/vue/v3/index.vue.vm"));

        GenTable plus = crudTable();
        plus.setTplWebType("element-plus");
        List<String> plusTemplates = VelocityUtils.getTemplateList(plus);
        assertTrue(plusTemplates.contains("vm/vue/v3/index.vue.vm"));
        assertFalse(plusTemplates.contains("vm/ts/api.ts.vm"));

        GenTable ts = crudTable();
        ts.setTplWebType("element-plus-typescript");
        List<String> tsTemplates = VelocityUtils.getTemplateList(ts);
        assertTrue(tsTemplates.contains("vm/ts/api.ts.vm"));
        assertTrue(tsTemplates.contains("vm/ts/type.ts.vm"));
        assertTrue(tsTemplates.contains("vm/ts/index.ts.vm"));
        assertTrue(tsTemplates.contains("vm/vue/v3ts/index.vue.vm"));
        assertFalse(tsTemplates.contains("vm/js/api.js.vm"));
    }

    @Test
    @DisplayName("getTemplateList：tree / sub / genView 三种叠加")
    void getTemplateList_categoryAndView()
    {
        GenTable tree = crudTable();
        tree.setTplCategory(GenConstants.TPL_TREE);
        assertTrue(VelocityUtils.getTemplateList(tree).contains("vm/vue/index-tree.vue.vm"));

        GenTable sub = crudTable();
        sub.setTplCategory(GenConstants.TPL_SUB);
        List<String> subTemplates = VelocityUtils.getTemplateList(sub);
        assertTrue(subTemplates.contains("vm/vue/index.vue.vm"));
        assertTrue(subTemplates.contains("vm/java/sub-domain.java.vm"));

        GenTable view = crudTable();
        view.setOptions("{\"genView\":true}");
        assertTrue(VelocityUtils.getTemplateList(view).contains("vm/vue/view.vue.vm"));
    }

    @Test
    @DisplayName("getFileName：后端五件套 + mapper XML + 菜单 SQL")
    void getFileName_backend()
    {
        GenTable table = crudTable();

        assertEquals("main/java/com/yu/system/domain/Student.java",
                VelocityUtils.getFileName("vm/java/domain.java.vm", table));
        assertEquals("main/java/com/yu/system/mapper/StudentMapper.java",
                VelocityUtils.getFileName("vm/java/mapper.java.vm", table));
        assertEquals("main/java/com/yu/system/service/IStudentService.java",
                VelocityUtils.getFileName("vm/java/service.java.vm", table));
        assertEquals("main/java/com/yu/system/service/impl/StudentServiceImpl.java",
                VelocityUtils.getFileName("vm/java/serviceImpl.java.vm", table));
        assertEquals("main/java/com/yu/system/controller/StudentController.java",
                VelocityUtils.getFileName("vm/java/controller.java.vm", table));
        assertEquals("main/resources/mapper/system/StudentMapper.xml",
                VelocityUtils.getFileName("vm/xml/mapper.xml.vm", table));
        assertEquals("studentMenu.sql", VelocityUtils.getFileName("vm/sql/sql.vm", table));
        assertEquals("", VelocityUtils.getFileName("vm/unknown.vm", table));
    }

    @Test
    @DisplayName("getFileName：前端模板路径 + 子表 domain 覆写")
    void getFileName_frontendAndSubDomain()
    {
        GenTable table = crudTable();

        assertEquals("vue/api/system/student.js", VelocityUtils.getFileName("vm/js/api.js.vm", table));
        assertEquals("vue/api/system/student.ts", VelocityUtils.getFileName("vm/ts/api.ts.vm", table));
        assertEquals("vue/types/api/system/student.ts", VelocityUtils.getFileName("vm/ts/type.ts.vm", table));
        assertEquals("vue/types/api/index-bak.ts", VelocityUtils.getFileName("vm/ts/index.ts.vm", table));
        assertEquals("vue/views/system/student/index.vue",
                VelocityUtils.getFileName("vm/vue/v3/index.vue.vm", table));
        assertEquals("vue/views/system/student/index.vue",
                VelocityUtils.getFileName("vm/vue/v3ts/index-tree.vue.vm", table));
        assertEquals("vue/views/system/student/view.vue",
                VelocityUtils.getFileName("vm/vue/view.vue.vm", table));

        GenTable sub = crudTable();
        sub.setTplCategory(GenConstants.TPL_SUB);
        GenTable subTable = crudTable();
        subTable.setClassName("StudentItem");
        sub.setSubTable(subTable);
        assertEquals("main/java/com/yu/system/domain/StudentItem.java",
                VelocityUtils.getFileName("vm/java/sub-domain.java.vm", sub));
    }

    @Test
    @DisplayName("getImportList：主键与超类列不计、无特殊列返回空集合")
    void getImportList_edgeCases()
    {
        GenTable table = crudTable();
        table.setColumns(new ArrayList<>(Arrays.asList(column("student_name", GenConstants.TYPE_STRING, GenConstants.REQUIRE))));
        assertTrue(VelocityUtils.getImportList(table).isEmpty());

        // 超类字段（createTime 等）由 BaseEntity 提供，不应再 import Date
        GenTableColumn superColumn = column("create_time", GenConstants.TYPE_DATE, GenConstants.REQUIRE);
        superColumn.setJavaField("createTime");
        table.setColumns(new ArrayList<>(Arrays.asList(superColumn)));
        assertTrue(VelocityUtils.getImportList(table).isEmpty());
    }

    @Test
    @DisplayName("getDicts：只收 select/radio/checkbox 且带字典类型的列，去重后加引号")
    void getDicts_onlyDictControls()
    {
        GenTable table = crudTable();
        GenTableColumn select = column("status", GenConstants.TYPE_STRING, GenConstants.REQUIRE);
        select.setHtmlType(GenConstants.HTML_SELECT);
        select.setDictType("sys_normal_disable");
        GenTableColumn radio = column("sex", GenConstants.TYPE_STRING, GenConstants.REQUIRE);
        radio.setHtmlType(GenConstants.HTML_RADIO);
        radio.setDictType("sys_user_sex");
        GenTableColumn input = column("remark2", GenConstants.TYPE_STRING, GenConstants.REQUIRE);
        input.setHtmlType(GenConstants.HTML_INPUT);
        input.setDictType("should_be_ignored");
        GenTableColumn noDict = column("grade", GenConstants.TYPE_STRING, GenConstants.REQUIRE);
        noDict.setHtmlType(GenConstants.HTML_SELECT);
        table.setColumns(new ArrayList<>(Arrays.asList(select, radio, input, noDict)));

        String dicts = VelocityUtils.getDicts(table);

        assertTrue(dicts.contains("'sys_normal_disable'"));
        assertTrue(dicts.contains("'sys_user_sex'"));
        assertFalse(dicts.contains("should_be_ignored"));
    }

    @Test
    @DisplayName("getPackagePrefix / getPermissionPrefix / getColSpan")
    void simpleDerivations()
    {
        assertEquals("com.yu", VelocityUtils.getPackagePrefix("com.yu.system"));
        assertEquals("system:student", VelocityUtils.getPermissionPrefix("system", "student"));
        assertEquals("24", VelocityUtils.getColSpan(1));
        assertEquals("12", VelocityUtils.getColSpan(2));
        assertEquals("8", VelocityUtils.getColSpan(3));
        assertEquals("24", VelocityUtils.getColSpan(0));
    }

    @Test
    @DisplayName("getExpandColumn：树名不是列表列时返回全部列表列数（编号落在末列）")
    void getExpandColumn_notFound()
    {
        GenTable table = crudTable();
        table.setOptions("{\"treeName\":\"not_exists\"}");
        // crudTable 里 isList='1' 的只有 student_name 与 status 两列（见 buildColumns）
        assertEquals(2, VelocityUtils.getExpandColumn(table));
    }

    @Test
    @DisplayName("getParentMenuId / genView / 树字段解析：null 与缺键分支")
    void optionParsers_edgeCases()
    {
        assertEquals("3", VelocityUtils.getParentMenuId(null));
        assertEquals("3", VelocityUtils.getParentMenuId(new JSONObject()));
        assertEquals("3", VelocityUtils.getParentMenuId(JSONObject.parseObject("{\"parentMenuId\":\"\"}")));
        assertEquals("2000", VelocityUtils.getParentMenuId(JSONObject.parseObject("{\"parentMenuId\":\"2000\"}")));

        assertFalse(VelocityUtils.genView(null));
        assertFalse(VelocityUtils.genView(new JSONObject()));
        assertTrue(VelocityUtils.genView(JSONObject.parseObject("{\"genView\":\"true\"}")));

        assertEquals("", VelocityUtils.getTreecode(new JSONObject()));
        assertEquals("deptId", VelocityUtils.getTreecode(JSONObject.parseObject("{\"treeCode\":\"dept_id\"}")));
        assertEquals("", VelocityUtils.getTreeParentCode(new JSONObject()));
        assertEquals("parentId",
                VelocityUtils.getTreeParentCode(JSONObject.parseObject("{\"treeParentCode\":\"parent_id\"}")));
        assertEquals("", VelocityUtils.getTreeName(new JSONObject()));
        assertEquals("deptName", VelocityUtils.getTreeName(JSONObject.parseObject("{\"treeName\":\"dept_name\"}")));
    }

    /**
     * 单表 fixture：主键列 + 2 个列表列 + 业务日期列 + 小数列 + 字典列 + 超类审计列（create_time）
     */
    static GenTable crudTable()
    {
        GenTable table = new GenTable();
        table.setTableId(1L);
        table.setTableName("sys_student");
        table.setClassName("Student");
        table.setPackageName("com.yu.system");
        table.setModuleName("system");
        table.setBusinessName("student");
        table.setFunctionName("学生信息");
        table.setFunctionAuthor("yu");
        table.setTplCategory(GenConstants.TPL_CRUD);
        table.setFormColNum(2);
        table.setColumns(buildColumns());
        table.setPkColumn(table.getColumns().get(0));
        return table;
    }

    static List<GenTableColumn> buildColumns()
    {
        GenTableColumn pk = column("student_id", GenConstants.TYPE_LONG, null);
        pk.setIsPk("1");
        GenTableColumn name = column("student_name", GenConstants.TYPE_STRING, GenConstants.REQUIRE);
        // 超类字段：由 BaseEntity 提供，不应产生 Date/JsonFormat 导入
        GenTableColumn createTime = column("create_time", GenConstants.TYPE_DATE, null);
        // 业务日期列：这才是真正需要 Date 导入的那一类
        GenTableColumn enrollDate = column("enroll_date", GenConstants.TYPE_DATE, null);
        GenTableColumn score = column("score", GenConstants.TYPE_BIGDECIMAL, null);
        GenTableColumn status = column("status", GenConstants.TYPE_STRING, GenConstants.REQUIRE);
        status.setHtmlType(GenConstants.HTML_RADIO);
        status.setDictType("sys_normal_disable");
        return new ArrayList<>(Arrays.asList(pk, name, createTime, enrollDate, score, status));
    }

    static GenTableColumn column(String columnName, String javaType, String isList)
    {
        GenTableColumn column = new GenTableColumn();
        column.setColumnName(columnName);
        column.setJavaType(javaType);
        column.setJavaField(com.yu.common.utils.StringUtils.toCamelCase(columnName));
        column.setIsList(isList);
        return column;
    }
}
