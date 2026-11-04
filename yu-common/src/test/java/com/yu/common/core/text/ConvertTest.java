package com.yu.common.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * G5：{@link Convert} 的契约测试。
 *
 * <p>这个类是全仓最底层的类型转换器（252 行、零覆盖），几乎每个 Controller 的入参转换、
 * Excel 导出、字典回填都要经过它。它的错误语义是「转换失败不抛异常、返回默认值」，
 * 所以一旦某个分支退化，表现不是崩溃而是**静默变成 null 或 0**，最容易被忽略。
 * 本测试把每条规则的正例与退化分支都钉住。</p>
 */
class ConvertTest
{
    private enum Level
    {
        LOW, HIGH
    }

    @Test
    @DisplayName("toStr：null 返回默认值，非字符串走 toString")
    void toStr()
    {
        assertEquals("x", Convert.toStr(null, "x"), "null 必须回落到默认值");
        assertNull(Convert.toStr(null), "无默认值时必须是 null 而不是字符串 \"null\"");
        assertEquals("abc", Convert.toStr("abc"));
        assertEquals("123", Convert.toStr(123));
        assertEquals("true", Convert.toStr(Boolean.TRUE));
    }

    @Test
    @DisplayName("toChar：字符串取首字符，空串回落默认值")
    void toChar()
    {
        assertEquals(Character.valueOf('a'), Convert.toChar('a'), "Character 直接返回，不做二次转换");
        assertEquals(Character.valueOf('a'), Convert.toChar("abc"), "字符串只取首字符");
        // G5：这里曾因三元表达式把 char/Character 混用而抛 NPE，已改为分支返回。
        assertNull(Convert.toChar(""), "空串必须返回 null，不能抛 NPE，也不能返回 \\0");
        assertEquals(Character.valueOf('z'), Convert.toChar("", 'z'), "空串必须回落到默认值");
        assertEquals(Character.valueOf('z'), Convert.toChar(null, 'z'));
    }

    @Test
    @DisplayName("toByte / toShort / toNumber：数值与字符串双向，越界与脏值回落默认值")
    void byteShortNumber()
    {
        assertEquals(Byte.valueOf((byte) 127), Convert.toByte("127"));
        assertEquals(Byte.valueOf((byte) 9), Convert.toByte(9));
        assertEquals(Byte.valueOf((byte) 1), Convert.toByte(null, (byte) 1), "null 回落到默认值");
        assertEquals(Byte.valueOf((byte) 0), Convert.toByte("abc", (byte) 0), "脏值不能抛异常");

        assertEquals(Short.valueOf((short) 300), Convert.toShort("300"));
        assertEquals(Short.valueOf((short) 7), Convert.toShort(7));
        assertEquals(Short.valueOf((short) 5), Convert.toShort(null, (short) 5));

        assertEquals(1.5d, Convert.toNumber("1.5").doubleValue(), 0.0001d);
        assertEquals(42L, Convert.toNumber(42L).longValue());
        assertEquals(-1L, Convert.toNumber(null, -1L).longValue(), "null 回落到默认值而不抛 NPE");
    }

    @Test
    @DisplayName("toInt：Number 截断小数，字符串解析失败回落默认值")
    void toInt()
    {
        assertEquals(Integer.valueOf(42), Convert.toInt("42"));
        assertEquals(Integer.valueOf(3), Convert.toInt(3.9d), "Number 走 intValue，直接截断而不是四舍五入");
        assertEquals(Integer.valueOf(-1), Convert.toInt("abc", -1), "脏值必须回落默认值");
        assertNull(Convert.toInt("abc"), "无默认值时脏值是 null，不能是 0（0 会被当成合法值继续跑）");
    }

    @Test
    @DisplayName("toLong：支持科学计数法（雪花 ID / Excel 数字文本常出现）")
    void toLong()
    {
        assertEquals(Long.valueOf(123456789012L), Convert.toLong("123456789012"));
        assertEquals(Long.valueOf(1000L), Convert.toLong("1E3"), "科学计数法必须能解析");
        assertEquals(Long.valueOf(9L), Convert.toLong(9.9d));
        assertEquals(Long.valueOf(-1L), Convert.toLong("abc", -1L));
    }

    @Test
    @DisplayName("toDouble / toFloat：脏值回落默认值")
    void toDoubleFloat()
    {
        assertEquals(1.25d, Convert.toDouble("1.25"), 0.0001d);
        assertEquals(2.5d, Convert.toDouble(2.5d), 0.0001d);
        assertEquals(-1d, Convert.toDouble("x", -1d), 0.0001d);
        assertEquals(2.5f, Convert.toFloat("2.5"), 0.0001f);
        assertEquals(0f, Convert.toFloat("x", 0f), 0.0001f);
    }

    @Test
    @DisplayName("toBool：中文/英文/数字三种真值写法都认，无法识别回落默认值")
    void toBool()
    {
        assertTrue(Convert.toBool(Boolean.TRUE));
        assertTrue(Convert.toBool("true"));
        assertTrue(Convert.toBool(" YES "), "要 trim + 忽略大小写");
        assertTrue(Convert.toBool("1"));
        assertTrue(Convert.toBool("是"));

        assertFalse(Convert.toBool("false"));
        assertFalse(Convert.toBool("no"));
        assertFalse(Convert.toBool("0"));
        assertFalse(Convert.toBool("否"));

        assertNull(Convert.toBool("maybe"), "无法识别必须是 null，不能默认 true");
        assertEquals(Boolean.TRUE, Convert.toBool(null, Boolean.TRUE));
    }

    @Test
    @DisplayName("toEnum：精确名匹配（区分大小写），失配回落默认值")
    void toEnum()
    {
        assertEquals(Level.HIGH, Convert.toEnum(Level.class, "HIGH"));
        assertEquals(Level.LOW, Convert.toEnum(Level.class, Level.LOW), "同类型直接返回");
        assertNull(Convert.toEnum(Level.class, "high"), "枚举名区分大小写，失配不能瞎猜");
        assertEquals(Level.LOW, Convert.toEnum(Level.class, "bad", Level.LOW));
    }

    @Test
    @DisplayName("数组/集合：空串返回空数组，脏元素逐个回落 0")
    void arrays()
    {
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Convert.toIntArray("1,2,3"));
        assertArrayEquals(new Integer[] {}, Convert.toIntArray(""), "空串必须返回空数组而不是 [0]");
        assertArrayEquals(new Integer[] { 1, 0 }, Convert.toIntArray("1,abc"), "单个脏元素只影响自己");
        assertArrayEquals(new Integer[] { 1, 2 }, Convert.toIntArray("#", "1#2"));

        assertArrayEquals(new Long[] { 1L, 2L }, Convert.toLongArray("1,2"));
        assertArrayEquals(new Long[] {}, Convert.toLongArray(null));
        assertArrayEquals(new String[] { "a", "b" }, Convert.toStrArray("a,b"));
        assertArrayEquals(new String[] {}, Convert.toStrArray(""));
        assertArrayEquals(new String[] { "a", "b" }, Convert.toStrArray("#", "a#b"));
    }

    @Test
    @DisplayName("toBigDecimal / toBigInteger：精度不被 double 污染")
    void bigNumbers()
    {
        assertEquals(new BigDecimal("1.10"), Convert.toBigDecimal("1.10"), "必须保留尾随零，金额展示依赖它");
        assertEquals(BigDecimal.ONE, Convert.toBigDecimal("abc", BigDecimal.ONE));
        assertEquals(new BigInteger("99"), Convert.toBigInteger("99"));
        assertEquals(BigInteger.ZERO, Convert.toBigInteger(null, BigInteger.ZERO));
    }

    @Test
    @DisplayName("str / utf8Str：字节与字符串按指定字符集互转")
    void str()
    {
        assertEquals("中文", Convert.utf8Str("中文"));
        assertEquals("中文", Convert.str("中文".getBytes(StandardCharsets.UTF_8), "UTF-8"));
        assertEquals("中文", Convert.str("中文".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        assertEquals("中文", Convert.str(ByteBuffer.wrap("中文".getBytes(StandardCharsets.UTF_8)), "UTF-8"));
        assertEquals("中文", Convert.str(ByteBuffer.wrap("中文".getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("toSBC / toDBC：半角与全角互转，排除集里的字符保持原样")
    void sbcDbc()
    {
        assertEquals("ａｂｃ　１２３", Convert.toSBC("abc 123"), "空格要转全角空格 \\u3000");
        assertEquals("abc 123", Convert.toDBC("ａｂｃ　１２３"));
        assertEquals("aｂｃ", Convert.toSBC("abc", Set.of('a')), "排除集里的 a 保持半角，其余照转");
        assertEquals("ａbc", Convert.toDBC("ａｂｃ", Set.of('ａ')), "排除集里的全角 ａ 保持全角，其余照转");
    }

    @Test
    @DisplayName("digitUppercase：金额大写（0 是「零元整」，负数带「负」前缀）")
    void digitUppercase()
    {
        assertEquals("零元整", Convert.digitUppercase(0));
        assertTrue(Convert.digitUppercase(-5).startsWith("负"), "负数必须带负号前缀");

        String s = Convert.digitUppercase(1234.56d);
        assertNotNull(s);
        assertTrue(s.contains("壹仟贰佰叁拾肆元"), "整数部分大写错误，实际=" + s);
        assertTrue(s.contains("伍角"), "角位丢失，实际=" + s);
        assertTrue(s.contains("陆分"), "分位丢失，实际=" + s);
    }
}
