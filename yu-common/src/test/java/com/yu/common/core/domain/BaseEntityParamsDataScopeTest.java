package com.yu.common.core.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.ServletRequestDataBinder;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * S-7：params.dataScope 只允许行级权限切面写入，外部绑定必须丢弃。
 *
 * <p>背景：16 处 Mapper 用 {@code ${params.dataScope}} 把该值字面拼进 WHERE，
 * 而 params 是 {@code Map<String,Object>} 属性，会被 JSON 请求体与查询串（{@code params[dataScope]=}）
 * 直接绑定。行级过滤依赖 {@code @DataScope} 切面「先清空再重写」，于是任何漏挂注解的调用路径
 * 都会把请求值原样拼进 SQL（实测 28 个 mapper 调用点里有 11 个不在注解方法内）。</p>
 *
 * <p>关键实测：Spring 的 WebDataBinder 对 Map 属性是「拿 getter 的 Map 直接 put」，
 * 不经过 setParams——所以只在 setter 里剥是假防护，真正的守点在 getParams() 的取用覆盖。</p>
 *
 * <p>本测试同时锁住「不能误伤」的两条：其它 params 键照常绑定、切面走 applyDataScope() 仍可写。</p>
 */
class BaseEntityParamsDataScopeTest
{
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("S-7：请求体里的 params.dataScope 被丢弃，其它参数照常绑定")
    void jsonBodyCannotSetDataScope() throws Exception
    {
        String body = "{\"params\":{\"dataScope\":\" or 1=1 -- \",\"beginTime\":\"2026-01-01\"}}";

        BaseEntity entity = mapper.readValue(body, BaseEntity.class);

        Map<String, Object> params = entity.getParams();
        assertFalse(params.containsKey("dataScope"), "dataScope 必须丢弃，否则未挂注解的路径会把它拼进 WHERE");
        assertEquals("2026-01-01", params.get("beginTime"), "其它查询参数不能被顺手封掉");
    }

    @Test
    @DisplayName("S-7：查询串 params[dataScope]= 也绑定不进实体（列表页 GET 的路径）")
    void queryStringCannotSetDataScope()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("params[dataScope]", " or 1=1 -- ");

        BaseEntity entity = new BaseEntity();
        new ServletRequestDataBinder(entity).bind(request);

        assertFalse(entity.getParams().containsKey("dataScope"), "GET 列表页的绑定路径同样必须被堵");
    }

    @Test
    @DisplayName("反例自证：同形但不剥键的对照体确实被绑上 dataScope，证明上面两条不是空转")
    void plainParamsEntityAcceptsDataScope()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("params[dataScope]", " or 1=1 -- ");

        PlainParamsEntity plain = new PlainParamsEntity();
        new ServletRequestDataBinder(plain).bind(request);

        assertNotNull(plain.getParams(), "若绑定语法本身不成立，上面两条断言就失去意义");
        assertEquals(" or 1=1 -- ", plain.getParams().get("dataScope"),
                "对照体能绑上 → 说明拦截点确实在 BaseEntity.setParams，而不是绑定器不认这个写法");
    }

    @Test
    @DisplayName("S-7：绕过 setter 直接往 getParams() 的 Map 里 put，取用时仍被清掉（WebDataBinder 的实际写法）")
    void directMapPutIsDroppedOnRead()
    {
        BaseEntity entity = new BaseEntity();
        entity.getParams().put("dataScope", " or 1=1 -- ");

        // MyBatis 的 ${params.dataScope} 是“再取一次 params”才读，所以取用点必须自己把关
        assertFalse(entity.getParams().containsKey("dataScope"),
                "绑定器正是走 getter 拿 Map 直接 put，只在 setter 里剥挡不住它");
    }

    @Test
    @DisplayName("S-7：切面写入路径不受影响（DataScopeAspect 走 applyDataScope）")
    void dataScopeAspectCanStillWrite()
    {
        BaseEntity entity = new BaseEntity();
        entity.applyDataScope(" AND (d.dept_id = 1)");

        assertEquals(" AND (d.dept_id = 1)", entity.getParams().get("dataScope"),
                "若这里也丢，行级过滤会整片失效（越权可见）");
    }

    @Test
    @DisplayName("S-7：切面写入后，外部再往 Map 里塞值也盖不回去")
    void externalValueCannotOverrideTrustedOne()
    {
        BaseEntity entity = new BaseEntity();
        entity.applyDataScope(" AND (d.dept_id = 1)");
        entity.getParams().put("dataScope", " or 1=1 -- ");

        assertEquals(" AND (d.dept_id = 1)", entity.getParams().get("dataScope"),
                "可信值必须优先，否则外部值能盖掉行级过滤");
    }

    @Test
    @DisplayName("S-7：setParams 不修改调用方传入的 Map 本身")
    void setParamsDoesNotMutateCallerMap()
    {
        Map<String, Object> given = new HashMap<>();
        given.put("dataScope", " or 1=1 -- ");
        given.put("beginTime", "2026-01-01");

        BaseEntity entity = new BaseEntity();
        entity.setParams(given);

        assertNull(entity.getParams().get("dataScope"));
        assertEquals(" or 1=1 -- ", given.get("dataScope"),
                "原地删键会污染调用方（如多个 Service 复用一个查询对象），必须复制后再删");
    }

    /** 与 BaseEntity.params 同形、但不剥 dataScope 的对照体 */
    static class PlainParamsEntity
    {
        private Map<String, Object> params;

        public Map<String, Object> getParams()
        {
            return params;
        }

        public void setParams(Map<String, Object> params)
        {
            this.params = params;
        }
    }
}
