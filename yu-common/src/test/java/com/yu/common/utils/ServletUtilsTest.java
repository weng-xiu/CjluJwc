package com.yu.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * G5：{@link ServletUtils} 的契约测试。
 *
 * <p>这个类的所有方法都只是「从 {@code RequestContextHolder} 里把 request/response 摸出来再转发」，
 * 因此用 {@code MockHttpServletRequest/Response} + {@code ServletRequestAttributes} 就能完整覆盖，
 * 不需要 Spring 容器，也不需要 MockMvc。</p>
 *
 * <p>唯一的契约风险是「没有请求上下文时直接 NPE」：这不是缺陷，而是它只允许在请求线程内调用。
 * 这里显式把这条写死，避免有人把它挪到异步线程里用还以为是线程安全的。</p>
 */
class ServletUtilsTest
{
    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    @BeforeEach
    void bindRequest()
    {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
    }

    @AfterEach
    void unbindRequest()
    {
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    @DisplayName("参数取值")
    class Parameter
    {
        @Test
        @DisplayName("String 参数：有值取值、无值给默认值")
        void stringParameter()
        {
            request.addParameter("name", "yu");
            assertEquals("yu", ServletUtils.getParameter("name"));
            assertEquals("fallback", ServletUtils.getParameter("missing", "fallback"));
        }

        @Test
        @DisplayName("Integer 参数：缺失时返回 null（不返回 0）")
        void intParameter()
        {
            request.addParameter("page", "3");
            assertEquals(3, ServletUtils.getParameterToInt("page"));
            assertEquals(9, ServletUtils.getParameterToInt("missing", 9));
            assertEquals(null, ServletUtils.getParameterToInt("missing"), "无默认值时必须给 null，0 会被当成有效页码");
        }

        @Test
        @DisplayName("Boolean 参数：true 算真；缺失时无默认值返回 null（不是 false）")
        void boolParameter()
        {
            request.addParameter("flag", "true");
            assertEquals(Boolean.TRUE, ServletUtils.getParameterToBool("flag"));
            assertEquals(null, ServletUtils.getParameterToBool("missing"),
                    "Convert.toBool(null) 返回 null，调用方必须自己给兜底");
            assertEquals(Boolean.TRUE, ServletUtils.getParameterToBool("missing", Boolean.TRUE));
            assertEquals(Boolean.FALSE, ServletUtils.getParameterToBool("missing", Boolean.FALSE));
        }

        @Test
        @DisplayName("getParams 返回只读 Map，写入要抛异常")
        void paramsReadOnly()
        {
            request.addParameter("a", "1");
            Map<String, String[]> params = ServletUtils.getParams(request);
            assertEquals(1, params.get("a").length);
            assertThrows(UnsupportedOperationException.class, () -> params.put("b", new String[] { "2" }),
                    "必须让调用方改不动原始请求参数");
        }

        @Test
        @DisplayName("getParamMap 把多值参数用逗号拼起来")
        void paramMap()
        {
            request.addParameter("tags", "a", "b", "c");
            request.addParameter("single", "x");
            Map<String, String> map = ServletUtils.getParamMap(request);
            assertEquals("a,b,c", map.get("tags"));
            assertEquals("x", map.get("single"));
        }
    }

    @Nested
    @DisplayName("上下文对象")
    class Context
    {
        @Test
        @DisplayName("getRequest / getResponse / getSession 拿到的就是绑定的那一个")
        void getters()
        {
            assertSame(request, ServletUtils.getRequest());
            assertSame(response, ServletUtils.getResponse());
            assertNotNull(ServletUtils.getSession());
            assertNotNull(ServletUtils.getRequestAttributes());
        }

        @Test
        @DisplayName("没有请求上下文时 NPE：只允许在请求线程内调用")
        void noContext()
        {
            RequestContextHolder.resetRequestAttributes();
            assertThrows(NullPointerException.class, ServletUtils::getRequest);
        }
    }

    @Nested
    @DisplayName("renderString")
    class Render
    {
        @Test
        @DisplayName("状态码 200、application/json、utf-8，内容原样写出")
        void render()
        {
            ServletUtils.renderString(response, "{\"code\":200}");
            assertEquals(200, response.getStatus());
            // MockHttpServletResponse 会把 charset 拼进 contentType，真实容器同样如此
            assertTrue(response.getContentType().startsWith("application/json"), "实际=" + response.getContentType());
            assertEquals("utf-8", response.getCharacterEncoding());
            // getContentAsString(Charset) 同样声明了 UnsupportedEncodingException，直接读字节数组更省事
            assertEquals("{\"code\":200}",
                    new String(response.getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("重复渲染是追加而不是覆盖（Servlet 语义，调用方不能复用 response）")
        void renderTwice()
        {
            ServletUtils.renderString(response, "A");
            ServletUtils.renderString(response, "B");
            assertEquals("AB",
                    new String(response.getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    @Nested
    @DisplayName("isAjaxRequest")
    class Ajax
    {
        @Test
        @DisplayName("accept 含 application/json 即判定为异步")
        void byAccept()
        {
            request.addHeader("accept", "application/json;charset=utf-8");
            assertTrue(ServletUtils.isAjaxRequest(request));
        }

        @Test
        @DisplayName("X-Requested-With 为 XMLHttpRequest 即判定为异步")
        void byRequestedWith()
        {
            request.addHeader("X-Requested-With", "XMLHttpRequest");
            assertTrue(ServletUtils.isAjaxRequest(request));
        }

        @Test
        @DisplayName("URI 以 .json / .xml 结尾即判定为异步")
        void byUri()
        {
            request.setRequestURI("/api/notice.json");
            assertTrue(ServletUtils.isAjaxRequest(request));

            MockHttpServletRequest xml = new MockHttpServletRequest();
            xml.setRequestURI("/api/notice.xml");
            assertTrue(ServletUtils.isAjaxRequest(xml));
        }

        @Test
        @DisplayName("__ajax=json / xml 参数也算（浏览器地址栏直接访问时的兜底）")
        void byQueryParam()
        {
            request.addParameter("__ajax", "json");
            assertTrue(ServletUtils.isAjaxRequest(request));

            MockHttpServletRequest xml = new MockHttpServletRequest();
            xml.addParameter("__ajax", "XML");
            assertTrue(ServletUtils.isAjaxRequest(xml), "比较要忽略大小写");
        }

        @Test
        @DisplayName("普通页面请求一律不是异步")
        void plainRequest()
        {
            request.setRequestURI("/system/notice");
            request.addHeader("accept", "text/html");
            assertFalse(ServletUtils.isAjaxRequest(request));
        }
    }

    @Nested
    @DisplayName("URL 编解码")
    class Url
    {
        @Test
        @DisplayName("中文与空格的编码 / 解码可回环")
        void roundTrip()
        {
            String raw = "长江大学 教务";
            String encoded = ServletUtils.urlEncode(raw);
            assertEquals("%E9%95%BF%E6%B1%9F%E5%A4%A7%E5%AD%A6+%E6%95%99%E5%8A%A1", encoded,
                    "URLEncoder 会把空格编码成 +");
            assertEquals(raw, ServletUtils.urlDecode(encoded));
        }

        @Test
        @DisplayName("空串原样返回")
        void empty()
        {
            assertEquals("", ServletUtils.urlEncode(""));
            assertEquals("", ServletUtils.urlDecode(""));
        }
    }
}
