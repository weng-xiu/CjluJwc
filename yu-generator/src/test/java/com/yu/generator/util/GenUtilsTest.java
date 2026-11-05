package com.yu.generator.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.common.constant.GenConstants;
import com.yu.generator.config.GenConfig;
import com.yu.generator.domain.GenTable;
import com.yu.generator.domain.GenTableColumn;

/**
 * 代码生成器工具类测试（G5 覆盖率爬坡 · yu-generator）。
 *
 * <p>{@code GenUtils} 把「数据库表结构」翻译成「Java 字段 + 控件类型 + 查询方式」，
 * 它算错不会报错，只会在生成物里静默写错类型（例如把 {@code decimal} 当 {@code Integer}、
 * 把 500 字的 textarea 当 input），等人工发现时已经生成了一整套 CRUD。
 * 因此这里逐条钉住类型映射、控件映射与「主键/审计列不参与编辑列表查询」的口径。</p>
 *
 * <p>{@code GenConfig} 是静态状态（读 {@code generator.yml}），测试必须显式设置并在
 * {@link AfterEach} 复原，否则会污染同 JVM 内的其它用例。</p>
 *
 * @author G5
 */
class GenUtilsTest
{
    /** 与 generator.yml 默认值同形的测试配置（表前缀故意给两个，覆盖逗号分隔分支） */
    private static final String TEST_PACKAGE_NAME = "com.yu.system";

    private static final String TEST_TABLE_PREFIX = "sys_,tpm_";

    @BeforeEach
    void setUpGenConfig()
    {
        // 字段是 public static，setter 却是 @Value 注入用的实例方法（=Spring 的写法），
        // 单测里直接给静态字段赋值，不为了赋值去起 Spring 容器
        GenConfig.author = "yu";
        GenConfig.packageName = TEST_PACKAGE_NAME;
        GenConfig.autoRemovePre = true;
        GenConfig.tablePrefix = TEST_TABLE_PREFIX;
    }

    @AfterEach
    void resetGenConfig()
    {
        GenConfig.author = null;
        GenConfig.packageName = null;
        GenConfig.autoRemovePre = false;
        GenConfig.tablePrefix = null;
    }

    @Test
    @DisplayName("initTable：类名去前缀、模块名取包尾、功能名去「表」字")
    void initTable_fillsGeneratedNaming()
    {
        GenTable table = new GenTable();
        table.setTableName("sys_student_archive");
        table.setTableComment("学生档案表");

        GenUtils.initTable(table, "admin");

        assertEquals("StudentArchive", table.getClassName());
        assertEquals("com.yu.system", table.getPackageName());
        assertEquals("system", table.getModuleName());
        assertEquals("archive", table.getBusinessName());
        assertEquals("学生档案", table.getFunctionName());
        assertEquals("yu", table.getFunctionAuthor());
        assertEquals("admin", table.getCreateBy());
    }

    @Test
    @DisplayName("convertClassName：关闭自动去前缀时保留原前缀")
    void convertClassName_withoutAutoRemovePrefix()
    {
        GenConfig.autoRemovePre = false;

        assertEquals("SysStudent", GenUtils.convertClassName("sys_student"));
    }

    @Test
    @DisplayName("convertClassName：前缀不在名单内时不去前缀")
    void convertClassName_prefixNotInList()
    {
        assertEquals("AemGradeRecord", GenUtils.convertClassName("aem_grade_record"));
    }

    @Test
    @DisplayName("replaceFirst：按名单顺序命中即停，未命中返回原值")
    void replaceFirst_matchesFirstPrefixOnly()
    {
        assertEquals("course", GenUtils.replaceFirst("tpm_course", new String[] { "sys_", "tpm_" }));
        assertEquals("sys_course", GenUtils.replaceFirst("sys_course", new String[] { "tpm_", "xxx_" }));
        assertEquals("course", GenUtils.replaceFirst("course", new String[] { "sys_", "tpm_" }));
    }

    @Test
    @DisplayName("replaceText：去掉「表」「若依」两种占位词")
    void replaceText_stripsPlaceholderWords()
    {
        assertEquals("用户", GenUtils.replaceText("若依用户表"));
        assertEquals("学生档案", GenUtils.replaceText("学生档案表"));
        assertNull(GenUtils.replaceText(null));
    }

    @Test
    @DisplayName("getDbType / getColumnLength：截括号、取长度")
    void getDbTypeAndColumnLength()
    {
        assertEquals("varchar", GenUtils.getDbType("varchar(64)"));
        assertEquals("datetime", GenUtils.getDbType("datetime"));
        assertEquals(64, GenUtils.getColumnLength("varchar(64)"));
        assertEquals(0, GenUtils.getColumnLength("text"));

        // 左括号在首位（非法列类型）按原实现原样返回，不做第二次猜测
        assertEquals("(x)", GenUtils.getDbType("(x)"));
    }

    @Test
    @DisplayName("getColumnLength：多参类型只取整数部分，不得抛 NumberFormatException")
    void getColumnLength_multiArgumentType()
    {
        // 反例自证：修复前 Integer.valueOf("10,2") 抛 NumberFormatException，
        // 即「列类型带逗号」会把导入表结构整条链路打成「导入失败：For input string: \"10,2\"」
        assertEquals(10, GenUtils.getColumnLength("decimal(10,2)"));
        assertEquals(8, GenUtils.getColumnLength("double(8,3)"));
        // 非数字参数（enum）回退 0，不抛异常
        assertEquals(0, GenUtils.getColumnLength("enum('a','b')"));
    }

    @Test
    @DisplayName("arraysContains：命中与未命中")
    void arraysContains_basic()
    {
        assertTrue(GenUtils.arraysContains(GenConstants.COLUMNTYPE_STR, "varchar"));
        assertFalse(GenUtils.arraysContains(GenConstants.COLUMNTYPE_STR, "bigint"));
    }

    @Test
    @DisplayName("initColumnField：字符串列 —— 短串 input、≥500 与 text 走 textarea")
    void initColumnField_stringColumns()
    {
        GenTable table = table("sys_student", "学生信息表");
        GenTableColumn name = column("student_name", "varchar(64)");
        GenUtils.initColumnField(name, table);
        assertEquals("studentName", name.getJavaField());
        assertEquals(GenConstants.TYPE_STRING, name.getJavaType());
        assertEquals(GenConstants.HTML_INPUT, name.getHtmlType());
        assertEquals(GenConstants.QUERY_LIKE, name.getQueryType());
        assertEquals(GenConstants.REQUIRE, name.getIsInsert());
        assertEquals(GenConstants.REQUIRE, name.getIsEdit());
        assertEquals(GenConstants.REQUIRE, name.getIsList());
        assertEquals(GenConstants.REQUIRE, name.getIsQuery());
        assertEquals(table.getTableId(), name.getTableId());
        assertEquals(table.getCreateBy(), name.getCreateBy());

        GenTableColumn boundary = column("remark_text", "varchar(499)");
        GenUtils.initColumnField(boundary, table);
        assertEquals(GenConstants.HTML_INPUT, boundary.getHtmlType());

        GenTableColumn wide = column("description", "varchar(500)");
        GenUtils.initColumnField(wide, table);
        assertEquals(GenConstants.HTML_TEXTAREA, wide.getHtmlType());

        GenTableColumn text = column("content_text", "text");
        GenUtils.initColumnField(text, table);
        assertEquals(GenConstants.HTML_TEXTAREA, text.getHtmlType());
    }

    @Test
    @DisplayName("initColumnField：时间列 → Date + datetime")
    void initColumnField_timeColumn()
    {
        GenTableColumn column = column("create_time", "datetime");
        GenUtils.initColumnField(column, table("sys_student", "学生信息表"));

        assertEquals(GenConstants.TYPE_DATE, column.getJavaType());
        assertEquals(GenConstants.HTML_DATETIME, column.getHtmlType());
    }

    @Test
    @DisplayName("initColumnField：数字列 —— 小数 BigDecimal、≤10 位整数 Integer、其余 Long")
    void initColumnField_numberColumns()
    {
        GenTable table = table("sys_student", "学生信息表");

        GenTableColumn decimal = column("score", "decimal(10,2)");
        GenUtils.initColumnField(decimal, table);
        assertEquals(GenConstants.TYPE_BIGDECIMAL, decimal.getJavaType());
        assertEquals(GenConstants.HTML_INPUT, decimal.getHtmlType());

        GenTableColumn smallInt = column("age", "int(4)");
        GenUtils.initColumnField(smallInt, table);
        assertEquals(GenConstants.TYPE_INTEGER, smallInt.getJavaType());

        // 与若依原口径一致：int(11) 宽度已超 10 位，按长整型处理
        GenTableColumn wideInt = column("sort_no", "int(11)");
        GenUtils.initColumnField(wideInt, table);
        assertEquals(GenConstants.TYPE_LONG, wideInt.getJavaType());

        GenTableColumn bigInt = column("student_id", "bigint(20)");
        GenUtils.initColumnField(bigInt, table);
        assertEquals(GenConstants.TYPE_LONG, bigInt.getJavaType());
    }

    @Test
    @DisplayName("initColumnField：主键与审计列不参与编辑/列表/查询")
    void initColumnField_pkAndAuditColumns()
    {
        GenTable table = table("sys_student", "学生信息表");

        GenTableColumn pk = column("student_id", "bigint(20)");
        pk.setIsPk("1");
        GenUtils.initColumnField(pk, table);
        assertEquals(GenConstants.REQUIRE, pk.getIsInsert());
        assertNull(pk.getIsEdit());
        assertNull(pk.getIsList());
        assertNull(pk.getIsQuery());

        GenTableColumn audit = column("create_by", "varchar(64)");
        GenUtils.initColumnField(audit, table);
        assertNull(audit.getIsEdit());
        assertNull(audit.getIsList());
        assertNull(audit.getIsQuery());

        GenTableColumn remark = column("remark", "varchar(500)");
        GenUtils.initColumnField(remark, table);
        assertEquals(GenConstants.REQUIRE, remark.getIsList());
        assertNull(remark.getIsQuery());
    }

    @Test
    @DisplayName("initColumnField：状态/类型/图片/文件/内容列的控件映射")
    void initColumnField_htmlTypeByColumnName()
    {
        GenTable table = table("sys_student", "学生信息表");

        GenTableColumn status = column("status", "char(1)");
        GenUtils.initColumnField(status, table);
        assertEquals(GenConstants.HTML_RADIO, status.getHtmlType());

        GenTableColumn type = column("student_type", "char(1)");
        GenUtils.initColumnField(type, table);
        assertEquals(GenConstants.HTML_SELECT, type.getHtmlType());

        GenTableColumn sex = column("sex", "char(1)");
        GenUtils.initColumnField(sex, table);
        assertEquals(GenConstants.HTML_SELECT, sex.getHtmlType());

        GenTableColumn image = column("avatar_image", "varchar(255)");
        GenUtils.initColumnField(image, table);
        assertEquals(GenConstants.HTML_IMAGE_UPLOAD, image.getHtmlType());

        GenTableColumn file = column("attachment_file", "varchar(255)");
        GenUtils.initColumnField(file, table);
        assertEquals(GenConstants.HTML_FILE_UPLOAD, file.getHtmlType());

        GenTableColumn content = column("notice_content", "text");
        GenUtils.initColumnField(content, table);
        assertEquals(GenConstants.HTML_EDITOR, content.getHtmlType());
    }

    @Test
    @DisplayName("getModuleName / getBusinessName：无分隔符时返回整串（现状口径）")
    void moduleAndBusinessName_withoutSeparator()
    {
        assertEquals("system", GenUtils.getModuleName("com.yu.system"));
        assertEquals("system", GenUtils.getModuleName("system"));
        assertEquals("student", GenUtils.getBusinessName("sys_student"));
        assertEquals("student", GenUtils.getBusinessName("student"));
    }

    private static GenTable table(String tableName, String tableComment)
    {
        GenTable table = new GenTable();
        table.setTableId(100L);
        table.setTableName(tableName);
        table.setTableComment(tableComment);
        table.setCreateBy("admin");
        return table;
    }

    private static GenTableColumn column(String columnName, String columnType)
    {
        GenTableColumn column = new GenTableColumn();
        column.setColumnName(columnName);
        column.setColumnType(columnType);
        return column;
    }
}
