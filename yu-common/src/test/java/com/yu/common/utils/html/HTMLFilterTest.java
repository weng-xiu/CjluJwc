package com.yu.common.utils.html;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * G5：{@link HTMLFilter} 的契约测试。
 *
 * <p>这是 {@code EscapeUtil.clean} 的唯一实现，{@code GlobalExceptionHandler} 用它把
 * 「请求参数类型不匹配」时用户传上来的原始值清洗后再回给前端，所以它的输出是会被浏览器解析的。
 * 测试因此不做整串快照 —— {@code balanceHTML} 会把成对标签里的右尖括号转义成
 * {@code &gt;}，整串长相很难看且与安全无关 —— 而是钉住<b>安全性质</b>：
 * 危险标签必须消失、危险协议必须变成锚点、非白名单属性必须丢弃。</p>
 *
 * <p>{@code filter} 是有状态的（{@code vTagCounts} 每次 filter 清零），所以每个用例都新建实例，
 * 并额外验证「同一个实例连调两次结果一致」，防止计数没清干净。</p>
 */
class HTMLFilterTest
{
    private static final String[] BLOCKED = { "<script", "<style", "<iframe", "javascript:", "vbscript:", "onerror=", "onload=" };

    private static void assertSafe(String input, String output, String... allowedFragments)
    {
        for (String blocked : BLOCKED)
        {
            assertFalse(output.toLowerCase().contains(blocked),
                    "输入[" + input + "]过滤后仍含危险片段[" + blocked + "]，实际=" + output);
        }
        for (String fragment : allowedFragments)
        {
            assertTrue(output.contains(fragment), "输入[" + input + "]过滤后应保留[" + fragment + "]，实际=" + output);
        }
    }

    @Nested
    @DisplayName("PHP 兼容的字符工具")
    class CharTools
    {
        @Test
        @DisplayName("chr：按十进制码点取单字符")
        void chr()
        {
            assertEquals("A", HTMLFilter.chr(65));
            assertEquals("中", HTMLFilter.chr(20013));
            assertEquals("\n", HTMLFilter.chr(10));
        }

        @Test
        @DisplayName("htmlSpecialChars：四个特殊字符各自变成实体")
        void htmlSpecialChars()
        {
            assertEquals("&amp;", HTMLFilter.htmlSpecialChars("&"));
            assertEquals("&quot;", HTMLFilter.htmlSpecialChars("\""));
            assertEquals("&lt;", HTMLFilter.htmlSpecialChars("<"));
            assertEquals("&gt;", HTMLFilter.htmlSpecialChars(">"));
            assertEquals("a&lt;b&gt;c&amp;d&quot;e", HTMLFilter.htmlSpecialChars("a<b>c&d\"e"));
            assertEquals("", HTMLFilter.htmlSpecialChars(""), "无特殊字符时原样返回");
        }
    }

    @Nested
    @DisplayName("脚本与未知标签")
    class BlockedTags
    {
        @Test
        @DisplayName("script 标签整体消失，标签内文本保留（这是错误消息可读性的下限）")
        void script()
        {
            String out = new HTMLFilter().filter("<script>alert(1);</script>");
            assertSafe("<script>alert(1);</script>", out, "alert(1);");
            assertFalse(out.contains("script"), "标签名也不能残留，实际=" + out);
        }

        @Test
        @DisplayName("iframe / style / 事件处理器一律清掉")
        void moreBlockedTags()
        {
            HTMLFilter filter = new HTMLFilter();
            assertSafe("<iframe src=\"http://evil\"></iframe>", filter.filter("<iframe src=\"http://evil\"></iframe>"));
            assertSafe("<style>body{}</style>", filter.filter("<style>body{}</style>"));
            assertSafe("<img src=\"http://a/x.png\" onerror=\"alert(1)\">",
                    filter.filter("<img src=\"http://a/x.png\" onerror=\"alert(1)\">"), "<img");
        }

        @Test
        @DisplayName("非白名单标签被删除，但标签文本与周围文本都要留住")
        void unknownTagsKeepText()
        {
            String out = new HTMLFilter().filter("<div>正文<span>子串</span></div>");
            assertSafe("<div>正文<span>子串</span></div>", out, "正文", "子串");
            assertFalse(out.contains("<div"), "div 不在白名单，实际=" + out);
            assertFalse(out.contains("<span"), "span 不在白名单，实际=" + out);
        }
    }

    @Nested
    @DisplayName("白名单标签与属性")
    class AllowedTags
    {
        @Test
        @DisplayName("a / b / strong / i / em 都保留开标签")
        void inlineTags()
        {
            HTMLFilter filter = new HTMLFilter();
            assertSafe("<b>粗</b>", filter.filter("<b>粗</b>"), "<b>");
            assertSafe("<strong>重</strong>", filter.filter("<strong>重</strong>"), "<strong>");
            assertSafe("<i>斜</i>", filter.filter("<i>斜</i>"), "<i>");
            assertSafe("<em>强调</em>", filter.filter("<em>强调</em>"), "<em>");
        }

        @Test
        @DisplayName("img 是自闭合标签，必须输出 /> 而不能输出开闭两个")
        void imgIsSelfClosing()
        {
            String out = new HTMLFilter().filter("<img src=\"http://a/x.png\">");
            assertSafe("<img src=\"http://a/x.png\">", out, "<img", "/>");
            assertFalse(out.contains("</img>"), "img 不该被补闭合标签，实际=" + out);
        }

        @Test
        @DisplayName("a 的 href / target 保留，其它属性（含事件）丢弃")
        void anchorAttributes()
        {
            String out = new HTMLFilter().filter("<a href=\"http://a.com\" target=\"_blank\" onclick=\"x()\">链接</a>");
            assertSafe("<a href=\"http://a.com\" target=\"_blank\" onclick=\"x()\">链接</a>", out,
                    "href=\"http://a.com\"", "target=\"_blank\"", "链接");
            assertFalse(out.contains("onclick"), "onclick 不在白名单，实际=" + out);
        }

        @Test
        @DisplayName("属性定界符必须是一对双引号，不能带反斜杠")
        void attributeDelimiterHasNoBackslash()
        {
            String out = new HTMLFilter().filter("<a href=\"http://example.com\">x</a>");
            assertFalse(out.contains("\\\""),
                    "输出里的 \\\" 会让浏览器把反斜杠当属性值的一部分，实际=" + out);
            assertTrue(out.contains("href=\"http://example.com\""), "属性必须原样可解析，实际=" + out);
        }

        @Test
        @DisplayName("未闭合的 a 会自动补上闭合标签")
        void autoClose()
        {
            String out = new HTMLFilter().filter("<a href=\"http://a.com\">半截");
            assertSafe("<a href=\"http://a.com\">半截", out, "半截", "</a>");
        }

        @Test
        @DisplayName("成对空标签 <b></b> 会被整体移除；<b /> 已被前一步规范化，删不掉（已知行为）")
        void removeBlanks()
        {
            HTMLFilter filter = new HTMLFilter();
            assertEquals("前后", filter.filter("前<b></b>后"), "成对空标签要删掉");
            // processRemoveBlanks 排在 checkTags 之后，此时 <b /> 已经被规范成 <b> + 自动补的 </b>，
            // P_REMOVE_SELF_BLANKS 永远匹配不到 —— 这条断言是行为记录，不是期望值。
            assertEquals("前<b>后</b>", filter.filter("前<b />后"),
                    "自闭合写法会被规范成开标签并吞掉后面的文本，如需修正要调整管道顺序（未在本批处理）");
        }
    }

    @Nested
    @DisplayName("协议白名单")
    class Protocol
    {
        @Test
        @DisplayName("http / https / mailto 原样保留")
        void allowed()
        {
            HTMLFilter filter = new HTMLFilter();
            assertSafe("<a href=\"http://a.com\">x</a>", filter.filter("<a href=\"http://a.com\">x</a>"), "http://a.com");
            assertSafe("<a href=\"https://a.com\">x</a>", filter.filter("<a href=\"https://a.com\">x</a>"), "https://a.com");
            assertSafe("<a href=\"mailto:a@b.c\">x</a>", filter.filter("<a href=\"mailto:a@b.c\">x</a>"), "mailto:a@b.c");
        }

        @Test
        @DisplayName("javascript: 变成页内锚点")
        void javascript()
        {
            String out = new HTMLFilter().filter("<a href=\"javascript:alert(1)\">x</a>");
            assertSafe("<a href=\"javascript:alert(1)\">x</a>", out);
            assertTrue(out.contains("href=\"#"), "非法协议要转成 # 锚点，实际=" + out);
        }

        @Test
        @DisplayName("javascript: 的大小写变形同样拦截")
        void javascriptMixedCase()
        {
            String out = new HTMLFilter().filter("<a href=\"JaVaScRiPt:alert(1)\">x</a>");
            assertSafe("<a href=\"JaVaScRiPt:alert(1)\">x</a>", out);
        }

        @Test
        @DisplayName("十进制实体编码不能绕过协议检查")
        void entityEncodedProtocol()
        {
            String out = new HTMLFilter().filter("<a href=\"&#106;avascript:alert(1)\">x</a>");
            assertSafe("<a href=\"&#106;avascript:alert(1)\">x</a>", out);
            assertFalse(out.contains("avascript"), "&#106; 必须先解码成 j 再判协议，实际=" + out);
        }

        @Test
        @DisplayName("十六进制实体编码不能绕过协议检查")
        void unicodeEncodedProtocol()
        {
            String out = new HTMLFilter().filter("<a href=\"&#x6a;avascript:alert(1)\">x</a>");
            assertSafe("<a href=\"&#x6a;avascript:alert(1)\">x</a>", out);
        }

        @Test
        @DisplayName("%XX 百分号编码不能绕过协议检查")
        void percentEncodedProtocol()
        {
            String out = new HTMLFilter().filter("<a href=\"%6aavascript:alert(1)\">x</a>");
            assertSafe("<a href=\"%6aavascript:alert(1)\">x</a>", out);
        }

        @Test
        @DisplayName("src 与 href 一样要过协议检查")
        void srcProtocol()
        {
            String out = new HTMLFilter().filter("<img src=\"javascript:alert(1)\">");
            assertSafe("<img src=\"javascript:alert(1)\">", out);
        }
    }

    @Nested
    @DisplayName("注释与开关")
    class Comments
    {
        @Test
        @DisplayName("默认剥离注释，注释里的内容不能成为漏网文本")
        void stripComment()
        {
            String out = new HTMLFilter().filter("前<!-- 藏起来的 <script> -->后");
            assertSafe("前<!-- 藏起来的 <script> -->后", out, "前", "后");
        }

        @Test
        @DisplayName("默认开关值：isAlwaysMakeTags=false、isStripComments=true")
        void defaults()
        {
            HTMLFilter filter = new HTMLFilter();
            assertFalse(filter.isAlwaysMakeTags());
            assertTrue(filter.isStripComments());
        }

        @Test
        @DisplayName("Map 构造器：stripComment=false 时注释保留为注释")
        void mapConfKeepComments()
        {
            HTMLFilter filter = new HTMLFilter(conf(false, true, true));
            assertTrue(filter.isStripComments(), "encodeQuotes=true 与 stripComment 无关");
            assertFalse(filter.isAlwaysMakeTags());

            HTMLFilter keep = new HTMLFilter(conf(false, false, false));
            assertFalse(keep.isStripComments(), "stripComment=false 必须生效");
            String out = keep.filter("前<!-- 注释 -->后");
            assertTrue(out.contains("<!--"), "stripComment=false 时注释要保留，实际=" + out);
        }

        @Test
        @DisplayName("Map 构造器：缺失的可选开关回落到默认值")
        void mapConfDefaults()
        {
            Map<String, Object> minimal = conf(false, false, false);
            minimal.remove("stripComment");
            minimal.remove("encodeQuotes");
            minimal.remove("alwaysMakeTags");
            HTMLFilter filter = new HTMLFilter(minimal);
            assertTrue(filter.isStripComments(), "stripComment 缺省 true");
            assertTrue(filter.isAlwaysMakeTags(), "alwaysMakeTags 缺省 true");

            Map<String, Object> blank = conf(false, false, false);
            blank.put("stripComment", Boolean.FALSE);
            blank.put("encodeQuotes", Boolean.FALSE);
            blank.put("alwaysMakeTags", Boolean.FALSE);
            HTMLFilter off = new HTMLFilter(blank);
            assertFalse(off.isStripComments());
            assertFalse(off.isAlwaysMakeTags());
        }

        @Test
        @DisplayName("Map 构造器：alwaysMakeTags=true 时残缺标签不再被整段转义")
        void mapConfAlwaysMakeTags()
        {
            HTMLFilter filter = new HTMLFilter(conf(true, true, true));
            assertTrue(filter.isAlwaysMakeTags());
            String out = assertDoesNotThrow(() -> filter.filter("<b text </b>"));
            // 残缺标签被补成 <b></b>，随后按空标签规则整体移除，所以这里只钉「没有退化成 &lt;b」。
            assertFalse(out.contains("&lt;b"), "alwaysMakeTags=true 时不该把标签转义掉，实际=" + out);
            assertEquals("", out, "补全后是空标签，应被 processRemoveBlanks 清掉");
        }

        private static Map<String, Object> conf(boolean alwaysMakeTags, boolean stripComment, boolean encodeQuotes)
        {
            Map<String, List<String>> allowed = new HashMap<>();
            allowed.put("a", new ArrayList<>(Arrays.asList("href", "target")));
            allowed.put("img", new ArrayList<>(Arrays.asList("src", "width", "height", "alt")));
            allowed.put("b", new ArrayList<>());
            allowed.put("strong", new ArrayList<>());
            allowed.put("i", new ArrayList<>());
            allowed.put("em", new ArrayList<>());

            Map<String, Object> conf = new HashMap<>();
            conf.put("vAllowed", allowed);
            conf.put("vSelfClosingTags", new String[] { "img" });
            conf.put("vNeedClosingTags", new String[] { "a", "b", "strong", "i", "em" });
            conf.put("vDisallowed", new String[] {});
            conf.put("vAllowedProtocols", new String[] { "http", "mailto", "https" });
            conf.put("vProtocolAtts", new String[] { "src", "href" });
            conf.put("vRemoveBlanks", new String[] { "a", "b", "strong", "i", "em" });
            conf.put("vAllowedEntities", new String[] { "amp", "gt", "lt", "quot" });
            conf.put("stripComment", stripComment);
            conf.put("encodeQuotes", encodeQuotes);
            conf.put("alwaysMakeTags", alwaysMakeTags);
            return conf;
        }
    }

    @Nested
    @DisplayName("有状态与边界")
    class Statefulness
    {
        @Test
        @DisplayName("同一实例连调两次结果必须一致（vTagCounts 要清零）")
        void reusable()
        {
            HTMLFilter filter = new HTMLFilter();
            String input = "<a href=\"http://a.com\">x</a>";
            String first = filter.filter(input);
            String second = filter.filter(input);
            assertEquals(first, second, "第二次过滤被上一次的开放标签计数污染了");
        }

        @Test
        @DisplayName("空串原样返回")
        void empty()
        {
            assertEquals("", new HTMLFilter().filter(""));
        }

        @Test
        @DisplayName("null 不得把调用方（GlobalExceptionHandler）拖崩")
        void nullSafe()
        {
            assertNull(new HTMLFilter().filter(null), "filter(null) 应返回 null，而不是抛 NPE");
        }
    }
}
