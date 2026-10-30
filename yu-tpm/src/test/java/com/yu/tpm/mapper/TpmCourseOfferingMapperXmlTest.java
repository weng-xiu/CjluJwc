package com.yu.tpm.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.tpm.domain.TpmCourseOffering;

/**
 * T-3：容量条件更新 SQL 的结构测试（不连数据库）。
 *
 * <p>容量的正确性完全押在这三条 SQL 上，单测里 mock 掉 Mapper 只能证明“调用了一次”，
 * 证明不了 SQL 里还留着并发守卫。这里用 MyBatis 自己的解析器把 XML 跑一遍并取 BoundSql，锁定：
 * ① occupySeat 必须同时带 {@code enrolled_count < max_students} 与自增（缺守卫就会超卖）；
 * ② releaseSeat 必须带 {@code enrolled_count > 0} 守卫（否则计数会减成负数）；
 * ③ recountEnrolled 的 IN 占位个数必须等于传入的开课数；
 * ④ updateTpmCourseOffering 绝不能写入 enrolled_count（会把旧值覆盖到并发增量上）。</p>
 */
class TpmCourseOfferingMapperXmlTest {

    private static Configuration configuration;

    @BeforeAll
    static void parseMapperXml() throws Exception {
        configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("TpmCourseOffering", TpmCourseOffering.class);
        String resource = "mapper/tpm/TpmCourseOfferingMapper.xml";
        try (InputStream in = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    private BoundSql boundSqlOf(String statement, Object param) {
        MappedStatement ms = configuration.getMappedStatement("com.yu.tpm.mapper.TpmCourseOfferingMapper." + statement);
        return ms.getBoundSql(param);
    }

    private Map<String, Object> offeringIdParam(long offeringId) {
        Map<String, Object> param = new HashMap<>();
        param.put("offeringId", offeringId);
        return param;
    }

    private String flatten(String sql) {
        return sql.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    @Test
    @DisplayName("occupySeat：占名额与余量判断压在同一条件更新里")
    void occupySeat_isConditionalIncrement() {
        BoundSql bs = boundSqlOf("occupySeat", offeringIdParam(9721L));
        String sql = flatten(bs.getSql());

        assertTrue(sql.contains("set enrolled_count = enrolled_count + 1"),
                "必须是原子自增，不能先查后改：" + sql);
        assertTrue(sql.contains("enrolled_count < max_students"),
                "缺少余量守卫，满员课程也会被占名额（超卖）：" + sql);
        assertTrue(sql.contains("del_flag = '0'"), "已删除开课不应再产生名额：" + sql);
        assertEquals(1, bs.getParameterMappings().size(), "occupySeat 只应有一个 offeringId 参数：" + sql);
        assertEquals("offeringId", bs.getParameterMappings().get(0).getProperty());
    }

    @Test
    @DisplayName("releaseSeat：释放名额带 > 0 守卫，不会减成负数")
    void releaseSeat_isGuardedDecrement() {
        BoundSql bs = boundSqlOf("releaseSeat", offeringIdParam(9721L));
        String sql = flatten(bs.getSql());

        assertTrue(sql.contains("set enrolled_count = enrolled_count - 1"),
                "必须是原子自减：" + sql);
        assertTrue(sql.contains("enrolled_count > 0"),
                "缺少 > 0 守卫，重复退课会把计数减成负数：" + sql);
        assertEquals(1, bs.getParameterMappings().size(), "releaseSeat 只应有一个 offeringId 参数：" + sql);
    }

    @Test
    @DisplayName("recountEnrolled：按报名表在册行数重算，IN 覆盖全部开课")
    void recountEnrolled_recomputesFromEnrollmentTable() {
        List<Long> ids = new ArrayList<>(Arrays.asList(20L, 21L, 22L));
        Map<String, Object> param = new HashMap<>();
        param.put("offeringIds", ids);

        BoundSql bs = boundSqlOf("recountEnrolled", param);
        String sql = flatten(bs.getSql());

        assertTrue(sql.contains("set enrolled_count = (select count(1) from tpm_selection_enrollment"),
                "重算必须回报名表取真相，不能读旧值做加减：" + sql);
        assertTrue(sql.contains("result_status = '1'"), "只统计选中状态：" + sql);
        assertTrue(sql.contains("del_flag = '0'"), "只统计未删除记录：" + sql);

        int inIdx = sql.indexOf("where offering_id in");
        assertTrue(inIdx >= 0, "缺少 IN 子句：" + sql);
        int inEnd = sql.indexOf(")", inIdx);
        assertEquals(ids.size(), sql.substring(inIdx, inEnd).split("\\?", -1).length - 1,
                "IN 占位个数必须等于开课数：" + sql);
        assertEquals(ids.size(), bs.getParameterMappings().size(),
                "参数个数必须等于开课数：" + sql);
        // foreach 会把参数重命名为 __frch_offeringId_N，锁前缀即可确认参数确实来自 IN 遍历
        for (int i = 0; i < ids.size(); i++) {
            assertTrue(bs.getParameterMappings().get(i).getProperty().startsWith("__frch_offeringId_"),
                    "第 " + i + " 个参数应来自 IN 遍历，实际为 " + bs.getParameterMappings().get(i).getProperty());
        }
    }

    @Test
    @DisplayName("updateTpmCourseOffering：绝不能写入 enrolled_count（否则覆盖并发增量）")
    void updateStatement_mustNotWriteEnrolledCount() {
        TpmCourseOffering offering = new TpmCourseOffering();
        offering.setOfferingId(9721L);
        offering.setMaxStudents(80);
        offering.setEnrolledCount(99); // 管理员表单里可能带着读到的旧值

        String sql = flatten(boundSqlOf("updateTpmCourseOffering", offering).getSql());

        assertFalse(sql.contains("enrolled_count"),
                "updateTpmCourseOffering 一旦带上 enrolled_count，编辑容量的低并发操作就会把并发选课的增量覆盖掉：" + sql);
    }
}
