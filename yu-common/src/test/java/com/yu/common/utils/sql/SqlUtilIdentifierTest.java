package com.yu.common.utils.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.common.exception.UtilException;

/**
 * S-7：${} 动态片段的取值护栏。
 *
 * <p>MyBatis 的 ${} 是字面拼接，值只要是「一个名字」就安全，变成「一段 SQL」就危险。
 * 这组断言把边界钉在标识符上：反引号、点号、空格、分号、括号一律不接受——
 * 它们的唯一用途就是让一个名字逃逸成语句片段。</p>
 */
class SqlUtilIdentifierTest
{
    @Test
    @DisplayName("S-7：合法表名/列名原样放行")
    void acceptsPlainIdentifiers()
    {
        assertEquals("sys_user", SqlUtil.assertIdentifier("sys_user", "表名"));
        assertEquals("_tmp1", SqlUtil.assertIdentifier("_tmp1", "表名"));
        assertEquals("a", SqlUtil.assertIdentifier("a", "列名"));
    }

    @Test
    @DisplayName("S-7：空值放行（CARD 分支不用列名，由调用方决定是否必填）")
    void allowsEmpty()
    {
        assertNull(SqlUtil.assertIdentifier(null, "列名"));
        assertEquals("", SqlUtil.assertIdentifier("", "列名"));
    }

    @Test
    @DisplayName("S-7：语句片段一律拒绝")
    void rejectsSqlFragments()
    {
        List<String> payloads = Arrays.asList(
                "sys_user; drop table sys_user",
                "sys_user -- ",
                "`sys_user`",
                "sys_user.col",
                "sys user",
                "(select 1)",
                "sys_user)`",
                "1=1 or user_id");
        for (String payload : payloads)
        {
            UtilException ex = assertThrows(UtilException.class,
                    () -> SqlUtil.assertIdentifier(payload, "表名"), "必须拒绝：" + payload);
            assertEquals(true, ex.getMessage().contains("表名"), "报错要能定位到是哪类值：" + ex.getMessage());
        }
    }

    @Test
    @DisplayName("S-7：超长标识符拒绝（MySQL 上限 64，超长必是拼出来的）")
    void rejectsOverlong()
    {
        String longName = "a".repeat(65);
        assertThrows(UtilException.class, () -> SqlUtil.assertIdentifier(longName, "列名"));
        assertEquals("a".repeat(64), SqlUtil.assertIdentifier("a".repeat(64), "列名"));
    }

    @Test
    @DisplayName("S-7：批量校验遇到任一非法列名即抛")
    void rejectsAnyBadColumnInBatch()
    {
        SqlUtil.assertIdentifiers(Arrays.asList("student_id", "score"), "列名");
        assertThrows(UtilException.class,
                () -> SqlUtil.assertIdentifiers(Arrays.asList("student_id", "score) values (1) -- "), "列名"));
        SqlUtil.assertIdentifiers(null, "列名");
    }

    @Test
    @DisplayName("S-7：createTable 只接受 CREATE TABLE 开头")
    void createTablePrefixOnly()
    {
        SqlUtil.assertCreateTableStatement("CREATE TABLE `demo` (`id` int)");
        SqlUtil.assertCreateTableStatement("  create table demo (id int)");

        for (String bad : Arrays.asList("DROP TABLE demo", "select 1", "", "   ", null))
        {
            assertThrows(UtilException.class, () -> SqlUtil.assertCreateTableStatement(bad), "必须拒绝：" + bad);
        }
    }
}
