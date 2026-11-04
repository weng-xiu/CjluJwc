package com.yu.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * G5：{@link StringUtils} 的契约测试（192 行，此前仅 3 行被覆盖）。
 *
 * <p>两个必须钉住的语义差异：</p>
 * <ol>
 *   <li>本项目的 {@code isEmpty} 是 <b>trim 语义</b>（{@code " "} 判为空），
 *       而若依原版是 length 语义。全仓大量 {@code if (StringUtils.isEmpty(x))} 依赖它，
 *       一旦有人「修回」原版，所有纯空白入参会突然变成合法值。</li>
 *   <li>{@code isEmpty} 与 {@code hasText} 不等价：{@code "\u00a0"}（不换行空格）在
 *       {@code isEmpty} 下非空，在 {@code hasText} 下也不算空白——两者都要单独断言。</li>
 * </ol>
 */
class StringUtilsTest
{
    @Test
    @DisplayName("nvl：只在 null 时取默认值，空串不替换")
    void nvl()
    {
        assertEquals("x", StringUtils.nvl(null, "x"));
        assertEquals("", StringUtils.nvl("", "x"), "空串不是 null，不能被默认值顶掉");
        assertEquals("a", StringUtils.nvl("a", "x"));
    }

    @Test
    @DisplayName("isEmpty：String 走 trim 语义，集合/数组/Map 只判空")
    void isEmpty()
    {
        assertTrue(StringUtils.isEmpty((String) null));
        assertTrue(StringUtils.isEmpty(""));
        assertTrue(StringUtils.isEmpty("   "), "本项目 isEmpty 带 trim，纯空白判为空（与若依原版不同）");
        assertFalse(StringUtils.isEmpty(" a "));
        assertFalse(StringUtils.isEmpty("\u00a0"), "不换行空格 trim 不掉，判为非空");

        assertTrue(StringUtils.isEmpty((List<String>) null));
        assertTrue(StringUtils.isEmpty(Collections.emptyList()));
        assertFalse(StringUtils.isEmpty(List.of("a")));

        assertTrue(StringUtils.isEmpty((Object[]) null));
        assertTrue(StringUtils.isEmpty(new Object[0]));
        assertFalse(StringUtils.isEmpty(new Object[] { 1 }));

        assertTrue(StringUtils.isEmpty((Map<String, String>) null));
        assertTrue(StringUtils.isEmpty(new HashMap<String, String>()));
        assertFalse(StringUtils.isEmpty(Map.of("k", "v")), "非空 Map 不能判空");

        assertTrue(StringUtils.isNotEmpty("a"));
        assertFalse(StringUtils.isNotEmpty(" "));
    }

    @Test
    @DisplayName("isNull / isArray")
    void isNullAndArray()
    {
        assertTrue(StringUtils.isNull(null));
        assertFalse(StringUtils.isNull(""));
        assertTrue(StringUtils.isNotNull(""));
        assertTrue(StringUtils.isArray(new int[] { 1 }));
        assertFalse(StringUtils.isArray("abc"));
        assertFalse(StringUtils.isArray(null));
    }

    @Test
    @DisplayName("trim：null 归一成空串，不返回 null")
    void trim()
    {
        assertEquals("", StringUtils.trim(null));
        assertEquals("a", StringUtils.trim("  a  "));
        assertEquals("", StringUtils.trim("   "));
    }

    @Test
    @DisplayName("hide：区间内字符替换成 *，越界与倒序区间返回空串")
    void hide()
    {
        assertEquals("138****5678", StringUtils.hide("13812345678", 3, 7), "手机号打码是主要用途");
        assertEquals("", StringUtils.hide(null, 0, 1));
        assertEquals("", StringUtils.hide("", 0, 1));
        assertEquals("", StringUtils.hide("ab", 5, 6), "起点越界必须返回空串，不能抛异常");
        assertEquals("***", StringUtils.hide("abc", 0, 9), "终点越界要收敛到末尾，即整串都被遮盖");
        assertEquals("", StringUtils.hide("abc", 2, 1), "倒序区间不替换，返回空串");
    }

    @Test
    @DisplayName("substring：负下标从末尾算，越界收敛而不是抛异常")
    void substring()
    {
        assertEquals("", StringUtils.substring(null, 0));
        assertEquals("bc", StringUtils.substring("abc", 1));
        assertEquals("c", StringUtils.substring("abc", -1), "负起点从末尾数");
        assertEquals("", StringUtils.substring("abc", 10), "起点越界返回空串");

        assertEquals("bc", StringUtils.substring("abcdef", 1, 3));
        assertEquals("de", StringUtils.substring("abcdef", -3, -1));
        assertEquals("abc", StringUtils.substring("abc", 0, 99), "终点越界收敛到末尾");
        assertEquals("", StringUtils.substring("abc", 2, 1), "起点大于终点返回空串");
    }

    @Test
    @DisplayName("substringBetweenLast：取第一个 open 与最后一个 close 之间")
    void substringBetweenLast()
    {
        assertEquals("b]c", StringUtils.substringBetweenLast("a[b]c]d", "[", "]"));
        assertEquals("", StringUtils.substringBetweenLast("abc", "[", "]"), "标记缺失返回空串");
        assertEquals("", StringUtils.substringBetweenLast(null, "[", "]"));
    }

    @Test
    @DisplayName("hasText：空白字符不算文本")
    void hasText()
    {
        assertFalse(StringUtils.hasText(null));
        assertFalse(StringUtils.hasText(""));
        assertFalse(StringUtils.hasText("   "));
        assertFalse(StringUtils.hasText("\t\n"));
        assertTrue(StringUtils.hasText(" a "));
    }

    @Test
    @DisplayName("format：{} 顺序占位，无参数或空模板原样返回")
    void format()
    {
        assertEquals("this is a for b", StringUtils.format("this is {} for {}", "a", "b"));
        assertEquals("x", StringUtils.format("x"), "无参数必须原样返回，不能丢掉 {}");
        assertEquals("", StringUtils.format("", "a"));
        assertEquals("1 2 3", StringUtils.format("{} {} {}", 1, 2, 3));
    }

    @Test
    @DisplayName("ishttp：只认 http/https 前缀")
    void ishttp()
    {
        assertTrue(StringUtils.ishttp("http://a.com"));
        assertTrue(StringUtils.ishttp("https://a.com"));
        assertFalse(StringUtils.ishttp("ftp://a.com"));
        assertFalse(StringUtils.ishttp("/tmp/a"));
    }

    @Test
    @DisplayName("str2List / str2Set：默认过滤空白项")
    void str2ListSet()
    {
        assertEquals(List.of("a", "b"), StringUtils.str2List("a,b", ","));
        assertEquals(List.of(), StringUtils.str2List(null, ","));
        assertEquals(List.of(), StringUtils.str2List("   ", ","), "纯空白整体判空");

        assertEquals(List.of("a", "b"), StringUtils.str2List("a, ,b", ",", true, true), "空白项要被过滤");
        assertEquals(List.of(" a", " b"), StringUtils.str2List(" a, b", ",", true, false), "trim=false 保留首尾空格");

        assertEquals(Set.of("a", "b"), StringUtils.str2Set("a,b,a", ","), "set 天然去重");
    }

    @Test
    @DisplayName("contains 系列：忽略大小写的版本不能退化成区分大小写")
    void contains()
    {
        assertTrue(StringUtils.contains("abc", "b"));
        assertFalse(StringUtils.contains("abc", "B"));
        assertTrue(StringUtils.containsIgnoreCase("abc", "B"));
        assertTrue(StringUtils.containsAny("abc", "x", "b"));
        assertFalse(StringUtils.containsAny("abc", "x", "y"));
        assertTrue(StringUtils.containsAny(List.of("abc", "def"), "def", "xyz"));
        assertTrue(StringUtils.containsAnyIgnoreCase("abc", "B"));
    }

    @Test
    @DisplayName("startsWith / endsWith / equals 系列")
    void prefixSuffixEquals()
    {
        assertTrue(StringUtils.startsWithAny("abc", "x", "ab"));
        assertFalse(StringUtils.startsWithAny("abc", "x", "y"));
        assertTrue(StringUtils.startsWithIgnoreCase("abc", "AB"));

        assertTrue(StringUtils.endsWith("abc", "bc"));
        assertTrue(StringUtils.endsWithAny("abc", "x", "bc"));
        assertTrue(StringUtils.endsWithIgnoreCase("abc", "BC"));

        assertTrue(StringUtils.equals("a", "a"));
        assertTrue(StringUtils.equals(null, null), "两端都 null 视为相等");
        assertFalse(StringUtils.equals("a", "A"));
        assertTrue(StringUtils.equalsAny("a", "x", "a"));
        assertTrue(StringUtils.equalsAnyIgnoreCase("a", "A"));
    }

    @Test
    @DisplayName("replace / removeEnd / indexOf")
    void replaceAndIndex()
    {
        assertEquals("a_b_c", StringUtils.replace("a-b-c", "-", "_"));
        assertEquals("abc", StringUtils.removeEnd("abc.txt", ".txt"));
        assertEquals("abc.txt", StringUtils.removeEnd("abc.txt", ".md"), "后缀不匹配时原样返回");
        assertEquals(1, StringUtils.indexOf("abc", "b"));
        assertEquals(1, StringUtils.indexOfIgnoreCase("aBc", "b"));
    }

    @Test
    @DisplayName("toUnderScoreCase：驼峰转下划线，连续大写按缩写处理")
    void toUnderScoreCase()
    {
        assertNull(StringUtils.toUnderScoreCase(null));
        assertEquals("user_name", StringUtils.toUnderScoreCase("userName"));
        assertEquals("http_server", StringUtils.toUnderScoreCase("HTTPServer"), "连续大写要整体成词");
        assertEquals("a", StringUtils.toUnderScoreCase("A"));
    }

    @Test
    @DisplayName("toCamelCase / convertToCamelCase：两种驼峰风格不能互相混淆")
    void camelCase()
    {
        assertNull(StringUtils.toCamelCase(null));
        assertEquals("userName", StringUtils.toCamelCase("user_name"), "全小写转小驼峰");
        assertEquals("userName", StringUtils.toCamelCase("USER_NAME"), "输入先整体小写");
        assertEquals("username", StringUtils.toCamelCase("username"), "无分隔符原样返回");

        assertEquals("", StringUtils.convertToCamelCase(null));
        assertEquals("", StringUtils.convertToCamelCase(""));
        assertEquals("HelloWorld", StringUtils.convertToCamelCase("HELLO_WORLD"), "大驼峰，且整词转小写后首字母大写");
        assertEquals("Name", StringUtils.convertToCamelCase("name"), "无下划线只把首字母大写");
    }

    @Test
    @DisplayName("matches / isMatch：Ant 风格通配")
    void matches()
    {
        assertTrue(StringUtils.isMatch("/sys/user/*", "/sys/user/list"));
        assertFalse(StringUtils.isMatch("/sys/user/*", "/sys/user/list/1"), "* 不跨层级");
        assertTrue(StringUtils.isMatch("/sys/**", "/sys/user/list/1"), "** 可跨层级");

        assertTrue(StringUtils.matches("/sys/user/list", List.of("/sys/user/*")));
        assertFalse(StringUtils.matches("/sys/user/list", List.of("/other/*")));
        assertFalse(StringUtils.matches(null, List.of("/sys/**")), "空串不匹配任何规则");
        assertFalse(StringUtils.matches("/a", Collections.emptyList()));
    }

    @Test
    @DisplayName("lastStringDel：只删末尾一个字符")
    void lastStringDel()
    {
        assertEquals("a,b", StringUtils.lastStringDel("a,b,", ","));
        assertEquals("a,b", StringUtils.lastStringDel("a,b", ","), "不以分隔符结尾时原样返回");
        assertEquals("a,b,", StringUtils.lastStringDel("a,b,,", ","), "注意：只删末尾 1 个字符，不是删整个 spit");
    }

    @Test
    @DisplayName("padl：左侧补零，超长时截掉开头保留尾部")
    void padl()
    {
        assertEquals("005", StringUtils.padl(5, 3));
        assertEquals("00a", StringUtils.padl("a", 3, '0'));
        assertEquals("bc", StringUtils.padl("abc", 2, '0'), "超长要保留尾部，学号截断依赖它");
        assertEquals("000", StringUtils.padl((String) null, 3, '0'), "null 用补齐字符填满");
    }

    @Test
    @DisplayName("cast：仅做类型擦除转换，不做校验")
    void cast()
    {
        Object o = "abc";
        assertEquals("abc", StringUtils.<String> cast(o));
        assertEquals(Arrays.asList(1, 2), StringUtils.<List<Integer>> cast(Arrays.asList(1, 2)));
    }
}
