package com.yu.common.utils.html;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * G5：{@link EscapeUtil} 的契约测试。
 *
 * <p>这个类有两个坑：① {@code escape} 用的是 {@code %XX} / {@code %uXXXX} 的 URL 式编码，
 * 而不是 {@code &#039;} 实体编码（类里那张 {@code TEXT[][]} 表是死代码，别被误导）；
 * ② {@code escape(null)} 返回空串、{@code decode(null)} 返回 null —— 两端不对称，
 * 调用方若把 escape 的结果直接当「非空」判据就会漏判。</p>
 */
class EscapeUtilTest
{
    @Test
    @DisplayName("escape：按 %XX 编码，宽字符用 %uXXXX 且不足 4 位补 0")
    void escape()
    {
        assertEquals("%61", EscapeUtil.escape("a"), "ASCII 用两位十六进制");
        // 本项目 StringUtils.isEmpty 是 trim 语义（不是若依原版的 length 语义），纯空格被视为空串。
        assertEquals("", EscapeUtil.escape(" "), "空格判定要跟 StringUtils.isEmpty 保持一致");
        assertEquals("%61%20%62", EscapeUtil.escape("a b"), "中间的空格照常编码成 %20");
        // 码点 <16 要补 0。注意必须夹在可见字符之间：单独一个 \u000f 会被 trim 判空而整体丢弃。
        assertEquals("%61%0f%62", EscapeUtil.escape("a\u000fb"), "小于 16 的码点必须补 0，否则解码错位");
        assertEquals("%u4e2d", EscapeUtil.escape("中"), "超出 0xfff 的码点直接四位");
        assertEquals("%u0800", EscapeUtil.escape(String.valueOf((char) 0x0800)), "0xfff 以内要补 0 凑四位");
    }

    @Test
    @DisplayName("escape(null/空串)：返回空串而不是 null")
    void escapeEmpty()
    {
        assertEquals("", EscapeUtil.escape(null));
        assertEquals("", EscapeUtil.escape(""));
    }

    @Test
    @DisplayName("decode：%XX 与 %uXXXX 混合、纯文本前缀都能还原")
    void decode()
    {
        assertEquals("a", EscapeUtil.decode("%61"));
        assertEquals("中", EscapeUtil.decode("%u4e2d"));
        assertEquals("abc", EscapeUtil.decode("abc"), "不含 % 的纯文本原样返回");
        assertEquals("xay", EscapeUtil.decode("x%61y"), "% 之前的前缀不能被吞掉");
        assertEquals("中a文", EscapeUtil.decode("%u4e2d%61%u6587"));
    }

    @Test
    @DisplayName("decode(null/空串)：原样返回 null 与空串（与 escape 不对称）")
    void decodeEmpty()
    {
        assertNull(EscapeUtil.decode(null), "decode(null) 必须仍是 null，不能变成空串");
        assertEquals("", EscapeUtil.decode(""));
    }

    @Test
    @DisplayName("escape / unescape 互逆：含中文与特殊字符的文本能完整回环")
    void roundTrip()
    {
        String raw = "<script>alert('x')</script> 中文 & 符号";
        assertEquals(raw, EscapeUtil.unescape(EscapeUtil.escape(raw)));
    }

    @Test
    @DisplayName("clean：剥掉 HTML 标签但保留标签内文本")
    void clean()
    {
        String out = EscapeUtil.clean("<script>alert(1);</script>");
        assertFalse(out.contains("<script>"), "标签必须被清掉，实际=" + out);
        assertFalse(out.contains("</script>"), "闭合标签也要清掉，实际=" + out);
        assertTrue(out.contains("alert(1);"), "标签内文本不能被一起删掉，实际=" + out);
    }
}
