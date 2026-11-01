package com.yu.common.core.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * S-6：BaseEntity 审计字段只读契约。
 *
 * <p>背景：控制器长期直接以领域实体接 {@code @RequestBody}，而 Mapper 的 update 语句会写
 * {@code update_by = #{updateBy}}（实测 97 条）、insert 会写 {@code create_by}。
 * 客户端只要在请求体里自带 createBy/updateBy，就能把「这条数据算谁的」一起改掉，
 * 使按 createBy 判归属的过滤（S-5）失效。故审计四字段标注 Jackson READ_ONLY。</p>
 *
 * <p>本测试同时覆盖「读侧不能误伤」：开放出去时仍要序列化出 createBy，
 * 否则前端列表里的创建人会凭空消失。</p>
 */
class BaseEntityAuditReadOnlyTest
{
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("S-6：请求体里的 createBy/updateBy 不得写入实体")
    void auditFieldsIgnoreRequestBody() throws Exception
    {
        String body = "{\"createBy\":\"forged\",\"updateBy\":\"forged\","
                + "\"createTime\":1700000000000,\"updateTime\":1700000000000,"
                + "\"remark\":\"ok\"}";

        BaseEntity entity = mapper.readValue(body, BaseEntity.class);

        assertNull(entity.getCreateBy(), "createBy 必须丢弃，否则可伪造归属");
        assertNull(entity.getUpdateBy(), "updateBy 必须丢弃，否则改审计留痕");
        assertNull(entity.getCreateTime());
        assertNull(entity.getUpdateTime());
        // 业务字段照常绑定，不能顺手把整个反序列化关掉
        assertEquals("ok", entity.getRemark());
    }

    @Test
    @DisplayName("S-6：params（查询参数容器）仍可从请求体写入，不扩大封堵面")
    void queryParamsStillWritable() throws Exception
    {
        BaseEntity entity = mapper.readValue("{\"params\":{\"beginTime\":\"2026-01-01\"}}", BaseEntity.class);

        Map<String, Object> params = entity.getParams();
        assertNotNull(params, "params 是列表查询的入参容器，误封会打断日期区间筛选");
        assertEquals("2026-01-01", params.get("beginTime"));
    }

    @Test
    @DisplayName("S-6：序列化仍输出审计字段（读侧不受影响）")
    void auditFieldsStillSerialized() throws Exception
    {
        BaseEntity entity = new BaseEntity();
        entity.setCreateBy("admin");

        String json = mapper.writeValueAsString(entity);

        assertEquals(true, json.contains("\"createBy\":\"admin\""), "读侧封了会让列表丢掉创建人，json=" + json);
    }

    @Test
    @DisplayName("反例自证：去掉注解的等价实体确实会被写入，证明上面的断言有牙齿")
    void plainEntityAcceptsForgedAuditFields() throws Exception
    {
        PlainEntity plain = mapper.readValue("{\"createBy\":\"forged\"}", PlainEntity.class);

        assertEquals("forged", plain.createBy,
                "若此处也为 null，说明失败原因不在注解而在测试环境，断言失去意义");
        assertNull(mapper.readValue("{\"createBy\":\"forged\"}", BaseEntity.class).getCreateBy());
    }

    /** 与 BaseEntity 审计字段等价、但不带 READ_ONLY 的对照体 */
    static class PlainEntity
    {
        @JsonProperty
        private String createBy;
    }
}
