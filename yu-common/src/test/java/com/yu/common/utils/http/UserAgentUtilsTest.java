package com.yu.common.utils.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.yu.common.utils.reflect.ReflectUtils;

/**
 * G5：{@link UserAgentUtils} 的契约测试。
 *
 * <p>公开入口 {@code getBrowser / getOperatingSystem} 先用 yauaa 解析，解析不出（空值或含 {@code ??}）
 * 才落到本类自己写的一串正则兜底。yauaa 的结果随版本变化，所以公开入口只断言「能识别出大类」，
 * 精确行为全部钉在四个 private 兜底方法上 —— 拿虚构 UA 直接调，才是稳定的契约。</p>
 *
 * <p>四个 private 方法用 {@link ReflectUtils#invokeMethod} 调用，顺带验证反射工具本身可用。
 * 兜底逻辑里最容易错的是<b>判断顺序</b>：Chrome 的 UA 里同时含 {@code Version/} 与 {@code Safari/}，
 * Edge 的 UA 里同时含 {@code Chrome/}，顺序一改就会张冠李戴。</p>
 */
class UserAgentUtilsTest
{
    private static final String CHROME_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final String WINDOWS_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final UserAgentUtils INSTANCE = new UserAgentUtils();

    private static String formatBrowser(String ua)
    {
        return ReflectUtils.invokeMethod(INSTANCE, "formatBrowser", new Class[] { String.class }, new Object[] { ua });
    }

    private static String formatOperatingSystem(String ua)
    {
        return ReflectUtils.invokeMethod(INSTANCE, "formatOperatingSystem", new Class[] { String.class },
                new Object[] { ua });
    }

    private static String extractMajorVersion(String version)
    {
        return ReflectUtils.invokeMethod(INSTANCE, "extractMajorVersion", new Class[] { String.class },
                new Object[] { version });
    }

    private static String windowsVersion(String version)
    {
        return ReflectUtils.invokeMethod(INSTANCE, "getWindowsVersionDisplay", new Class[] { String.class },
                new Object[] { version });
    }

    @Nested
    @DisplayName("getBrowser / getOperatingSystem：先走 yauaa")
    class Analyzer
    {
        @Test
        @DisplayName("真实 Chrome UA 能识别出 Chrome")
        void chrome()
        {
            assertTrue(UserAgentUtils.getBrowser(CHROME_UA).contains("Chrome"),
                    "实际=" + UserAgentUtils.getBrowser(CHROME_UA));
        }

        @Test
        @DisplayName("真实 Windows UA 能识别出 Windows")
        void windows()
        {
            assertTrue(UserAgentUtils.getOperatingSystem(WINDOWS_UA).contains("Windows"),
                    "实际=" + UserAgentUtils.getOperatingSystem(WINDOWS_UA));
        }

        @Test
        @DisplayName("解析不出时不抛异常，回落到自有正则")
        void fallbackDoesNotThrow()
        {
            assertNotNull(UserAgentUtils.getBrowser(""));
            assertNotNull(UserAgentUtils.getOperatingSystem(""));
            assertNotNull(UserAgentUtils.getBrowser("SomeBot/1.0"));
            assertNotNull(UserAgentUtils.getOperatingSystem("SomeBot/1.0"));
        }
    }

    @Nested
    @DisplayName("formatBrowser：兜底正则的判定顺序")
    class BrowserFallback
    {
        @Test
        @DisplayName("Chrome 优先于 Safari：Chrome 的 UA 同样含 Version/ 与 Safari/")
        void chromeBeforeSafari()
        {
            assertEquals("Chrome120", formatBrowser("Chrome/120.0.0.0 Safari/537.36"));
            assertEquals("Chrome120", formatBrowser("Mozilla/5.0 ... Chrome/120.0 ... Version/17.0 Safari/605.1"),
                    "顺序错了就会被 Safari 抢走");
        }

        @Test
        @DisplayName("Firefox / Edge / Safari")
        void mainstream()
        {
            assertEquals("Firefox121", formatBrowser("Firefox/121.0"));
            assertEquals("Edge120", formatBrowser("Mozilla/5.0 Edge/120.0.0.0"),
                    "Edge 的新 UA 里带 Chrome/，这里用不含 Chrome/ 的形态验证 Edge 分支本身");
            assertEquals("Safari17", formatBrowser("Mozilla/5.0 Version/17.0 Safari/605.1"),
                    "不含 Chrome 时 Version/ 才能归到 Safari");
        }

        @Test
        @DisplayName("国产与移动端浏览器")
        void domestic()
        {
            assertEquals("WeChat8", formatBrowser("MicroMessenger/8.0.42"));
            assertEquals("UC Browser13", formatBrowser("UCBrowser/13.4.0"));
            assertEquals("QQ Browser14", formatBrowser("QQBrowser/14.1.0"));
            assertEquals("Baidu Browser13", formatBrowser("baidubrowser/13.1.0"));
            assertEquals("Samsung Browser21", formatBrowser("SamsungBrowser/21.0"));
        }

        @Test
        @DisplayName("Opera 与 IE（含 Trident 形态）")
        void legacy()
        {
            assertEquals("Opera9", formatBrowser("Opera/9.80"));
            assertEquals("Internet Explorer11", formatBrowser("MSIE 11.0"));
            assertEquals("Internet Explorer11", formatBrowser("Trident/7.0; rv:11.0"),
                    "IE11 在兼容模式下只报 Trident/...rv:");
        }

        @Test
        @DisplayName("完全不认识时返回空串（UNKNOWN 常量）")
        void unknown()
        {
            assertEquals("", formatBrowser("SomeBot/1.0"));
            assertEquals("", formatBrowser(""));
            assertEquals(UserAgentUtils.UNKNOWN, formatBrowser("SomeBot/1.0"));
        }

        @Test
        @DisplayName("只取主版本号，小数位丢弃")
        void majorOnly()
        {
            assertEquals("Chrome120", formatBrowser("Chrome/120.0.6099.110"));
            assertFalse(formatBrowser("Chrome/120.0.6099.110").contains(".0"), "多版本号不能带出来");
        }
    }

    @Nested
    @DisplayName("formatOperatingSystem：兜底正则")
    class OperatingSystemFallback
    {
        @Test
        @DisplayName("Windows NT 版本号映射成产品名")
        void windowsVersions()
        {
            assertEquals("Windows10", formatOperatingSystem("Windows NT 10.0; Win64; x64"));
            assertEquals("Windows8.1", formatOperatingSystem("Windows NT 6.3"));
            assertEquals("Windows8", formatOperatingSystem("Windows NT 6.2"));
            assertEquals("Windows7", formatOperatingSystem("Windows NT 6.1"));
            assertEquals("WindowsVista", formatOperatingSystem("Windows NT 6.0"));
            assertEquals("WindowsXP", formatOperatingSystem("Windows NT 5.1"));
            assertEquals("Windows2000", formatOperatingSystem("Windows NT 5.0"));
            assertEquals("Windows4", formatOperatingSystem("Windows NT 4.0"), "未映射的版本号退回主版本");
        }

        @Test
        @DisplayName("macOS 下划线版本号转成主版本")
        void macOS()
        {
            assertEquals("macOS10", formatOperatingSystem("Macintosh; Intel Mac OS X 10_15_7"));
        }

        @Test
        @DisplayName("Android / iOS / Linux / Chrome OS")
        void others()
        {
            assertEquals("Android13", formatOperatingSystem("Linux; Android 13; Pixel 7"));
            assertEquals("iOS16", formatOperatingSystem("iPhone OS 16_0 like Mac OS X"));
            assertEquals("Linux", formatOperatingSystem("Linux x86_64"));
            assertEquals("Chrome OS", formatOperatingSystem("CrOS x86_64 14541.0.0"));
        }

        @Test
        @DisplayName("Android 的 UA 里含 Linux，不能被 Linux 分支抢走")
        void androidNotLinux()
        {
            assertEquals("Android13", formatOperatingSystem("Android 13; Linux"));
        }

        @Test
        @DisplayName("不认识时返回空串")
        void unknown()
        {
            assertEquals("", formatOperatingSystem("SomeBot/1.0"));
            assertEquals("", formatOperatingSystem(""));
        }
    }

    @Nested
    @DisplayName("extractMajorVersion：主版本号提取")
    class MajorVersion
    {
        @Test
        @DisplayName("常规三段式只取第一段；三位数版本号截掉末位")
        void normal()
        {
            assertEquals("12", extractMajorVersion("120"), "120 >= 100 会被当成三位数截成 12");
            assertEquals("17", extractMajorVersion("17.0"));
            assertEquals("10", extractMajorVersion("10.15.7"));
        }

        @Test
        @DisplayName("三位数版本号截掉末位（142 → 14）")
        void threeDigits()
        {
            assertEquals("14", extractMajorVersion("142"));
            assertEquals("10", extractMajorVersion("100"));
            assertEquals("99", extractMajorVersion("99"), "两位数不受影响，99 原样返回");
        }

        @Test
        @DisplayName("非数字字符先清掉再取")
        void cleaning()
        {
            assertEquals("16", extractMajorVersion("16_0"), "下划线会被 replaceAll 清掉，剩下 160");
            assertEquals("12", extractMajorVersion("v120"), "v120 → 120 → 三位数截成 12");
        }

        @Test
        @DisplayName("空值与纯字母降级返回原串/空串")
        void degenerate()
        {
            assertEquals("", extractMajorVersion(""));
            assertEquals("", extractMajorVersion(null));
            assertEquals("abc", extractMajorVersion("abc"), "清理后空串不满足 \\d+，返回原值");
        }

        @Test
        @DisplayName("超出 int 范围时退回原串，不抛异常")
        void overflow()
        {
            String huge = "2147483648";
            assertEquals(huge, extractMajorVersion(huge));
        }
    }

    @Nested
    @DisplayName("getWindowsVersionDisplay：版本号 → 产品名")
    class WindowsDisplay
    {
        @Test
        @DisplayName("已映射的七个版本")
        void mapped()
        {
            assertEquals("10", windowsVersion("10.0"));
            assertEquals("8.1", windowsVersion("6.3"));
            assertEquals("8", windowsVersion("6.2"));
            assertEquals("7", windowsVersion("6.1"));
            assertEquals("Vista", windowsVersion("6.0"));
            assertEquals("XP", windowsVersion("5.1"));
            assertEquals("2000", windowsVersion("5.0"));
        }

        @Test
        @DisplayName("未映射的版本号退回主版本号")
        void unmapped()
        {
            assertEquals("4", windowsVersion("4.0"));
            assertEquals("20", windowsVersion("200.0"), "200.0 取第一段 200，三位数再截成 20");
        }
    }
}
