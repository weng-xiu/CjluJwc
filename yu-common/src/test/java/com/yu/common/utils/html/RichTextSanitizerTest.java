package com.yu.common.utils.html;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * S-2 存储型 XSS：富文本白名单清洗器单元验证。
 *
 * <p>断言口径：危险标签/属性必须消失，排版标签与站内相对地址必须原样保留。</p>
 *
 * @author S-2
 */
class RichTextSanitizerTest
{
    @Test
    @DisplayName("script 标签与内容被整段丢弃")
    void scriptTagDropped()
    {
        String cleaned = RichTextSanitizer.sanitize("<p>正文</p><script>alert(1)</script>");

        assertEquals("<p>正文</p>", cleaned);
    }

    @Test
    @DisplayName("on* 事件属性被剥离，img 的 src 保留")
    void eventHandlersStripped()
    {
        String cleaned = RichTextSanitizer.sanitize(
                "<p onclick=\"alert(1)\" onmouseover=\"alert(2)\">正文</p><img src=x onerror=\"alert(3)\">");

        assertFalse(cleaned.contains("alert"), "不得残留任何事件属性：" + cleaned);
        assertFalse(cleaned.contains("onclick"), cleaned);
        assertFalse(cleaned.contains("onerror"), cleaned);
        assertTrue(cleaned.contains("<p>正文</p>"), cleaned);
        assertTrue(cleaned.contains("<img src=\"x\">"), cleaned);
    }

    @Test
    @DisplayName("javascript: / data: / 带制表符的伪协议链接被剥除")
    void dangerousProtocolsRemoved()
    {
        String cleaned = RichTextSanitizer.sanitize(
                "<a href=\"javascript:alert(1)\">a</a>"
                        + "<a href=\"java\tscript:alert(2)\">b</a>"
                        + "<a href=\"data:text/html;base64,PHNjcmlwdD4=\">c</a>"
                        + "<a href=\"vbscript:msgbox(1)\">d</a>");

        assertFalse(cleaned.contains("javascript"), cleaned);
        assertFalse(cleaned.contains("vbscript"), cleaned);
        assertFalse(cleaned.contains("data:"), cleaned);
        // href 被剥除后仍会保留 a 标签（rel 为强制属性），因此按链接个数断言
        assertEquals(4, countOf(cleaned, "<a "), "链接文本保留但 href 被剥除：" + cleaned);
    }

    @Test
    @DisplayName("http/https/mailto 与站内相对地址保留，并强制 rel=noopener")
    void safeProtocolsKept()
    {
        String cleaned = RichTextSanitizer.sanitize(
                "<a href=\"https://www.yangtzeu.edu.cn/jwc\">教务</a>"
                        + "<a href=\"/portal/article/1\">站内</a>"
                        + "<a href=\"mailto:jwc@yangtzeu.edu.cn\">邮箱</a>"
                        + "<a href=\"#top\">锚点</a>");

        assertTrue(cleaned.contains("href=\"https://www.yangtzeu.edu.cn/jwc\""), cleaned);
        assertTrue(cleaned.contains("href=\"/portal/article/1\""), cleaned);
        assertTrue(cleaned.contains("href=\"mailto:jwc@yangtzeu.edu.cn\""), cleaned);
        assertTrue(cleaned.contains("href=\"#top\""), cleaned);
        assertEquals(4, countOf(cleaned, "rel=\"noopener noreferrer\""), cleaned);
    }

    @Test
    @DisplayName("iframe/style/form 等危险容器连同内容一并丢弃，style 属性被剥离")
    void dangerousContainersDropped()
    {
        String cleaned = RichTextSanitizer.sanitize(
                "<iframe src=\"https://evil.example\"></iframe>"
                        + "<style>body{display:none}</style>"
                        + "<form action=\"/x\"><input name=\"p\"></form>"
                        + "<p style=\"position:fixed;top:0\">正文</p>");

        assertEquals("<p>正文</p>", cleaned);
    }

    @Test
    @DisplayName("Quill 排版类名保留，任意其他类名被剥离")
    void onlyQuillClassesKept()
    {
        String cleaned = RichTextSanitizer.sanitize(
                "<p class=\"ql-align-center fake-overlay\">居中</p><span class=\"ql-size-large\">大号</span>");

        assertTrue(cleaned.contains("class=\"ql-align-center\""), cleaned);
        assertTrue(cleaned.contains("class=\"ql-size-large\""), cleaned);
        assertFalse(cleaned.contains("fake-overlay"), cleaned);
    }

    @Test
    @DisplayName("正常富文本排版不被破坏")
    void normalRichTextSurvives()
    {
        String source = "<h2>选课通知</h2><p>请于<strong>本周五</strong>前完成<em>选课</em></p>"
                + "<ul><li>第一轮</li><li>第二轮</li></ul><blockquote>逾期不再受理</blockquote><br>";

        assertEquals(source, RichTextSanitizer.sanitize(source));
    }

    @Test
    @DisplayName("幂等：清洗结果再清洗一次不变")
    void idempotent()
    {
        String once = RichTextSanitizer.sanitize("<p onclick=\"alert(1)\">正文</p><script>alert(1)</script>");

        assertEquals(once, RichTextSanitizer.sanitize(once));
    }

    @Test
    @DisplayName("空值原样返回，不抛异常")
    void nullAndEmptyPassThrough()
    {
        assertNull(RichTextSanitizer.sanitize(null));
        assertEquals("", RichTextSanitizer.sanitize(""));
    }

    private static int countOf(String text, String fragment)
    {
        int count = 0;
        int from = 0;
        while ((from = text.indexOf(fragment, from)) >= 0)
        {
            count++;
            from += fragment.length();
        }
        return count;
    }
}
