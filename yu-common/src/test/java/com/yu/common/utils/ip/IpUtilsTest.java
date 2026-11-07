package com.yu.common.utils.ip;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * G5：{@link IpUtils} 的契约测试。
 *
 * <p>这些方法全部是「拿字符串算布尔值 / 字节数组」的纯函数，只有 {@code getIpAddr} 依赖请求对象，
 * 用 Mockito 桩住 {@code HttpServletRequest} 就能跑，不需要容器。</p>
 *
 * <p>重点钉两条：① 反向代理头有 6 级兜底，顺序错了会让审计日志记成代理 IP；
 * ② 所有把 IP 拆成 4 段再按下标取值的地方，都必须先看段数 —— 之前只有调用方做了格式校验，
 * 一旦从「IP 过滤白名单」这种配置串直接调进来就会数组越界。</p>
 */
class IpUtilsTest
{
    @AfterEach
    void clearRequestContext()
    {
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    @DisplayName("getIpAddr：反向代理头兜底顺序")
    class GetIpAddr
    {
        @Test
        @DisplayName("x-forwarded-for 优先，命中就不再看后面的头")
        void firstHeaderWins()
        {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("x-forwarded-for")).thenReturn("1.1.1.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn("2.2.2.2");
            when(request.getRemoteAddr()).thenReturn("3.3.3.3");
            assertEquals("1.1.1.1", IpUtils.getIpAddr(request));
        }

        @Test
        @DisplayName("逐级降级：Proxy-Client-IP → X-Forwarded-For → WL-Proxy-Client-IP → X-Real-IP → remoteAddr")
        void fallbackChain()
        {
            assertEquals("2.2.2.2", IpUtils.getIpAddr(requestWith("Proxy-Client-IP", "2.2.2.2")));
            assertEquals("3.3.3.3", IpUtils.getIpAddr(requestWith("X-Forwarded-For", "3.3.3.3")));
            assertEquals("4.4.4.4", IpUtils.getIpAddr(requestWith("WL-Proxy-Client-IP", "4.4.4.4")));
            assertEquals("5.5.5.5", IpUtils.getIpAddr(requestWith("X-Real-IP", "5.5.5.5")));
        }

        @Test
        @DisplayName("\"unknown\" 与空串都算没取到，继续降级")
        void unknownFallsThrough()
        {
            HttpServletRequest blank = mock(HttpServletRequest.class);
            when(blank.getHeader("x-forwarded-for")).thenReturn("");
            when(blank.getHeader("Proxy-Client-IP")).thenReturn("unknown");
            when(blank.getHeader("X-Forwarded-For")).thenReturn("UNKNOWN");
            when(blank.getHeader("WL-Proxy-Client-IP")).thenReturn("10.0.0.1");
            assertEquals("10.0.0.1", IpUtils.getIpAddr(blank));
        }

        @Test
        @DisplayName("一个头都没有时落到 remoteAddr")
        void remoteAddr()
        {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRemoteAddr()).thenReturn("192.168.9.9");
            assertEquals("192.168.9.9", IpUtils.getIpAddr(request));
        }

        @Test
        @DisplayName("IPv6 回环统一显示成 127.0.0.1")
        void ipv6Loopback()
        {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("x-forwarded-for")).thenReturn("0:0:0:0:0:0:0:1");
            assertEquals("127.0.0.1", IpUtils.getIpAddr(request));
        }

        @Test
        @DisplayName("多级代理取第一个非 unknown 的地址")
        void multistage()
        {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("x-forwarded-for")).thenReturn("unknown, 10.1.1.1, 2.2.2.2");
            assertEquals("10.1.1.1", IpUtils.getIpAddr(request));
        }

        @Test
        @DisplayName("request 为 null 时返回 unknown，不抛 NPE")
        void nullRequest()
        {
            assertEquals("unknown", IpUtils.getIpAddr((HttpServletRequest) null));
        }

        @Test
        @DisplayName("无参版本从 RequestContextHolder 取请求")
        void fromRequestContext()
        {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("x-forwarded-for", "6.6.6.6");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
            assertEquals("6.6.6.6", IpUtils.getIpAddr());
        }

        private static HttpServletRequest requestWith(String headerName, String value)
        {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader(headerName)).thenReturn(value);
            when(request.getRemoteAddr()).thenReturn("9.9.9.9");
            return request;
        }
    }

    @Nested
    @DisplayName("internalIp：内网段判定")
    class InternalIp
    {
        @Test
        @DisplayName("10.x / 172.16-31.x / 192.168.x / 127.0.0.1 都是内网")
        void inner()
        {
            assertTrue(IpUtils.internalIp("10.0.0.1"));
            assertTrue(IpUtils.internalIp("172.16.0.1"));
            assertTrue(IpUtils.internalIp("172.31.255.255"), "172.16/12 的上边界");
            assertTrue(IpUtils.internalIp("192.168.1.1"));
            assertTrue(IpUtils.internalIp("127.0.0.1"));
        }

        @Test
        @DisplayName("172.32 已经出了 172.16/12 范围，8.8.8.8 是公网")
        void outer()
        {
            assertFalse(IpUtils.internalIp("172.32.0.1"));
            assertFalse(IpUtils.internalIp("8.8.8.8"));
            assertFalse(IpUtils.internalIp("11.0.0.1"), "10 段只匹配首字节 10，11 不算");
        }

        @Test
        @DisplayName("非法 IP 解析不出字节，按内网处理（失败即收紧，不是失败即放行）")
        void unparsableIsInner()
        {
            assertTrue(IpUtils.internalIp("not-an-ip"), "解析失败时 textToNumericFormatV4 返回 null，internalIp 视为内网");
            assertTrue(IpUtils.internalIp("256.1.1.1"));
        }
    }

    @Nested
    @DisplayName("textToNumericFormatV4：IPv4 字面量转字节")
    class NumericFormat
    {
        @Test
        @DisplayName("标准四段")
        void fourSegments()
        {
            assertArrayEquals(new byte[] { (byte) 0xC0, (byte) 0xA8, 1, 1 }, IpUtils.textToNumericFormatV4("192.168.1.1"));
            assertArrayEquals(new byte[] { 127, 0, 0, 1 }, IpUtils.textToNumericFormatV4("127.0.0.1"));
        }

        @Test
        @DisplayName("三段 / 两段 / 一段：末段按剩余位宽左对齐")
        void fewerSegments()
        {
            assertArrayEquals(new byte[] { 1, 2, 0, 3 }, IpUtils.textToNumericFormatV4("1.2.3"));
            assertArrayEquals(new byte[] { 1, 0, 0, 2 }, IpUtils.textToNumericFormatV4("1.2"));
            assertArrayEquals(new byte[] { 0, 0, 0, 1 }, IpUtils.textToNumericFormatV4("1"));
            assertArrayEquals(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF },
                    IpUtils.textToNumericFormatV4("4294967295"), "单段上界");
        }

        @Test
        @DisplayName("越界、段数过多、非数字一律返回 null")
        void invalid()
        {
            assertNull(IpUtils.textToNumericFormatV4(""));
            assertNull(IpUtils.textToNumericFormatV4("4294967296"), "单段超过 2^32-1");
            assertNull(IpUtils.textToNumericFormatV4("256.1.1.1"));
            assertNull(IpUtils.textToNumericFormatV4("1.1.1.256"));
            assertNull(IpUtils.textToNumericFormatV4("1.16777216"), "两段时末段上界是 0xFFFFFF");
            assertNull(IpUtils.textToNumericFormatV4("1.1.65536"), "三段时末段上界是 0xFFFF");
            assertNull(IpUtils.textToNumericFormatV4("1.1.1.1.1"));
            assertNull(IpUtils.textToNumericFormatV4("a.b.c.d"));
        }

        @Test
        @DisplayName("null 入参返回 null 而不是 NPE")
        void nullInput()
        {
            assertNull(IpUtils.textToNumericFormatV4(null),
                    "这个方法被 internalIp 与白名单解析共用，null 必须安全");
        }
    }

    @Nested
    @DisplayName("格式判定")
    class FormatCheck
    {
        @Test
        @DisplayName("isIP：只认四段 0-255")
        void isIp()
        {
            assertTrue(IpUtils.isIP("192.168.1.1"));
            assertTrue(IpUtils.isIP("0.0.0.0"));
            assertTrue(IpUtils.isIP("255.255.255.255"));
            assertFalse(IpUtils.isIP("256.1.1.1"));
            assertFalse(IpUtils.isIP("1.1.1"));
            assertFalse(IpUtils.isIP(""));
            assertFalse(IpUtils.isIP(null));
            assertFalse(IpUtils.isIP(" 192.168.1.1"));
        }

        @Test
        @DisplayName("isIpWildCard：支持首段定值、两段定值、全星号三种写法")
        void isIpWildCard()
        {
            assertTrue(IpUtils.isIpWildCard("*.*.*.*"));
            assertTrue(IpUtils.isIpWildCard("10.*.*.*"));
            assertTrue(IpUtils.isIpWildCard("10.10.*.*"));
            assertFalse(IpUtils.isIpWildCard("10.10.10.1"), "全定值应走 isIP，不算通配");
            assertFalse(IpUtils.isIpWildCard("10.10.*"), "星号必须占满剩余段");
            assertFalse(IpUtils.isIpWildCard(null));
        }

        @Test
        @DisplayName("isIPSegment：区间两端都必须是合法 IP")
        void isIpSegment()
        {
            assertTrue(IpUtils.isIPSegment("10.0.0.1-10.0.0.9"));
            assertTrue(IpUtils.isIPSegment("10.0.0.9-10.0.0.1"), "正则不校验大小顺序，倒序也判合法");
            assertFalse(IpUtils.isIPSegment("10.0.0.1"));
            assertFalse(IpUtils.isIPSegment("10.0.0.1-"));
            assertFalse(IpUtils.isIPSegment(null));
        }

        @Test
        @DisplayName("isUnknown：空白与 unknown 都算未知，大小写不敏感")
        void isUnknown()
        {
            assertTrue(IpUtils.isUnknown(null));
            assertTrue(IpUtils.isUnknown(""));
            assertTrue(IpUtils.isUnknown("   "));
            assertTrue(IpUtils.isUnknown("unknown"));
            assertTrue(IpUtils.isUnknown("UNKNOWN"));
            assertFalse(IpUtils.isUnknown("1.1.1.1"));
        }
    }

    @Nested
    @DisplayName("区间与通配匹配")
    class Matching
    {
        @Test
        @DisplayName("ipIsInWildCardNoCheck：前缀相同即命中，遇到 * 立刻停下")
        void wildcard()
        {
            assertTrue(IpUtils.ipIsInWildCardNoCheck("10.10.*.*", "10.10.5.6"));
            assertTrue(IpUtils.ipIsInWildCardNoCheck("*.*.*.*", "1.2.3.4"));
            assertFalse(IpUtils.ipIsInWildCardNoCheck("10.11.*.*", "10.10.5.6"));
        }

        @Test
        @DisplayName("ipIsInWildCardNoCheck：目标 IP 段数不足时返回 false 而不是越界")
        void wildcardShortIp()
        {
            // 前缀段全命中（10.10.* 对 10.10）是允许的；只有通配串定值段多于目标 IP 段数时才会越界
            assertTrue(IpUtils.ipIsInWildCardNoCheck("10.10.*.*", "10.10"), "遇到 * 就停，前缀相同即命中");
            assertFalse(IpUtils.ipIsInWildCardNoCheck("10.10.5.*", "10.10"),
                    "配置串来自白名单，目标 IP 来自请求头，长度不可信");
        }

        @Test
        @DisplayName("ipIsInNetNoCheck：区间内命中，倒序区间自动交换端点")
        void net()
        {
            assertTrue(IpUtils.ipIsInNetNoCheck("10.0.0.1-10.0.0.9", "10.0.0.5"));
            assertTrue(IpUtils.ipIsInNetNoCheck("10.0.0.9-10.0.0.1", "10.0.0.5"));
            assertTrue(IpUtils.ipIsInNetNoCheck("10.0.0.1-10.0.0.9", "10.0.0.1"), "左闭");
            assertTrue(IpUtils.ipIsInNetNoCheck("10.0.0.1-10.0.0.9", "10.0.0.9"), "右闭");
            assertFalse(IpUtils.ipIsInNetNoCheck("10.0.0.1-10.0.0.9", "10.0.0.10"));
        }

        @Test
        @DisplayName("ipIsInNetNoCheck：目标 IP 段数不足时返回 false 而不是越界")
        void netShortIp()
        {
            assertFalse(IpUtils.ipIsInNetNoCheck("10.0.0.1-10.0.0.9", "10.0"));
        }

        @Test
        @DisplayName("isMatchedIp：精确 / 通配 / 网段三种写法混在一个分号串里")
        void isMatchedIp()
        {
            String filter = "192.168.1.1;10.0.0.0-10.0.0.255;172.16.*.*";
            assertTrue(IpUtils.isMatchedIp(filter, "192.168.1.1"), "精确命中");
            assertTrue(IpUtils.isMatchedIp(filter, "10.0.0.5"), "网段命中");
            assertTrue(IpUtils.isMatchedIp(filter, "172.16.3.4"), "通配命中");
            assertFalse(IpUtils.isMatchedIp(filter, "8.8.8.8"));
        }

        @Test
        @DisplayName("isMatchedIp：过滤串或 IP 为空直接 false")
        void isMatchedIpEmpty()
        {
            assertFalse(IpUtils.isMatchedIp(null, "1.1.1.1"));
            assertFalse(IpUtils.isMatchedIp("", "1.1.1.1"));
            assertFalse(IpUtils.isMatchedIp("1.1.1.1", null));
            assertFalse(IpUtils.isMatchedIp("1.1.1.1", ""));
        }

        @Test
        @DisplayName("isMatchedIp：目标 IP 不是合法 IPv4 时一律不匹配")
        void isMatchedIpShortIp()
        {
            // 以前只校验 ip 非空就交给按 4 段取下标的方法：要么 AIOOBE，要么被当成 "*.*.*.*" 的前缀误放行。
            assertFalse(IpUtils.isMatchedIp("10.1.5.*", "10.1"));
            assertFalse(IpUtils.isMatchedIp("10.0.0.1-10.0.0.9", "10.1"));
            assertFalse(IpUtils.isMatchedIp("*.*.*.*", "10.1"), "段数不足不能被当成通配命中");
            assertFalse(IpUtils.isMatchedIp("*.*.*.*", "10"), "单段同样不匹配");
        }
    }

    @Nested
    @DisplayName("本机信息与代理串清洗")
    class Host
    {
        @Test
        @DisplayName("getHostIp / getHostName 永远有返回值（取不到时回退字面量）")
        void hostInfo()
        {
            assertTrue(IpUtils.getHostIp() != null && !IpUtils.getHostIp().isEmpty());
            assertTrue(IpUtils.getHostName() != null && !IpUtils.getHostName().isEmpty());
        }

        @Test
        @DisplayName("getMultistageReverseProxyIp：无逗号原样返回，有逗号取第一个非 unknown 并去掉首尾空格")
        void reverseProxy()
        {
            assertEquals("1.2.3.4", IpUtils.getMultistageReverseProxyIp("1.2.3.4"));
            assertEquals("10.1.1.1", IpUtils.getMultistageReverseProxyIp("unknown, 10.1.1.1, 2.2.2.2"),
                    "逗号后的空格必须去掉，否则落进日志的 IP 带空格，无法跟白名单比对");
            assertEquals("", IpUtils.getMultistageReverseProxyIp(null),
                    "本项目 StringUtils.substring 对 null 返回空串，调用方按「无地址」处理即可");
        }

        @Test
        @DisplayName("getMultistageReverseProxyIp：超过 255 字符截断（防止脏头把日志撑爆）")
        void reverseProxyTruncate()
        {
            String longIp = "1".repeat(300);
            assertEquals(255, IpUtils.getMultistageReverseProxyIp(longIp).length());
        }
    }

    @Nested
    @DisplayName("已知未修行为")
    class KnownGaps
    {
        @Test
        @DisplayName("ipIsInWildCardNoCheck 不校验通配串本身格式（由调用方的 isIpWildCard 兜）")
        void wildcardNoFormatCheck()
        {
            // 直接调用（绕过 isMatchedIp）时没有格式校验，段数一样就按下标比，这是方法名 NoCheck 的语义。
            assertTrue(IpUtils.ipIsInWildCardNoCheck("10.x.*.*", "10.x.5.6"), "前缀字面相同就算命中");
        }
    }

    @Test
    @DisplayName("清理：RequestContextHolder 用完必须复位，否则污染同 JVM 的其它用例")
    void cleanupSandbox()
    {
        RequestContextHolder.resetRequestAttributes();
        assertThrows(NullPointerException.class, () -> IpUtils.getIpAddr(),
                "没有请求上下文时无参版本按现有实现会 NPE（内部 API，调用方都在请求线程内）");
    }
}
