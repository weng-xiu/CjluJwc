package com.yu.tpm.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.tpm.domain.TpmSelectionEnrollment;

/**
 * P-3：抽签批量落库 SQL 的结构测试（不连数据库）。
 *
 * <p>批量 UPDATE 是 `CASE enroll_id WHEN ... END` 动态拼接，单测里 mock 掉 Mapper 只能证明
 * “调用了一次”，证明不了拼出来的 SQL 合法。这里用 MyBatis 自己的解析器把 XML 跑一遍并取 BoundSql，
 * 锁定三件事：① 每条记录三列各一个 WHEN；② WHERE 的 IN 与 CASE 覆盖同一批 id；
 * ③ waitlist_rank 为 null 时参数带 jdbcType=BIGINT（否则运行期会抛 “setting null for parameter”）。</p>
 */
class TpmSelectionEnrollmentMapperXmlTest {

    private static Configuration configuration;

    @BeforeAll
    static void parseMapperXml() throws Exception {
        configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("TpmSelectionEnrollment", TpmSelectionEnrollment.class);
        String resource = "mapper/tpm/TpmSelectionEnrollmentMapper.xml";
        try (InputStream in = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    private TpmSelectionEnrollment enroll(long id, String lotteryResult, String resultStatus, Integer rank) {
        TpmSelectionEnrollment e = new TpmSelectionEnrollment();
        e.setEnrollId(id);
        e.setLotteryResult(lotteryResult);
        e.setResultStatus(resultStatus);
        e.setWaitlistRank(rank);
        return e;
    }

    private BoundSql boundSqlOf(List<TpmSelectionEnrollment> list) {
        MappedStatement ms = configuration.getMappedStatement(
                "com.yu.tpm.mapper.TpmSelectionEnrollmentMapper.batchUpdateLotteryResult");
        Map<String, Object> param = new HashMap<>();
        param.put("list", list);
        param.put("updateTime", new Date());
        return ms.getBoundSql(param);
    }

    @Test
    @DisplayName("批量更新：三列各按记录数展开 CASE WHEN，IN 覆盖同一批 id")
    void batchUpdate_expandsAllColumns() {
        List<TpmSelectionEnrollment> list = new ArrayList<>(Arrays.asList(
                enroll(1L, "1", "1", null),
                enroll(2L, "2", "2", 1),
                enroll(3L, "2", "2", 2)));

        BoundSql bs = boundSqlOf(list);
        String sql = bs.getSql().toLowerCase();
        String flat = sql.replaceAll("\\s+", " ").trim();

        // 3 列 × 1 个 CASE = 3 个 CASE 块，每块 3 个 WHEN，共 9 个 "when ? then ?"
        assertEquals(3, sql.split("case enroll_id", -1).length - 1, "三列必须各有一个 CASE enroll_id 块：" + sql);
        assertEquals(9, sql.split("when \\? then \\?", -1).length - 1, "每条记录三列各一个 WHEN：" + sql);
        assertTrue(flat.contains("where enroll_id in"), "缺少 IN 子句：" + sql);
        // 不匹配空白：“IN (?, ?, ?)” 的确切空格数取决于 foreach 正文缩进，属排版细节；
        // 真正要锁的是“占位个数 = 同一批 id 数”
        int inIdx = flat.indexOf("where enroll_id in");
        int inEnd = flat.indexOf(")", inIdx);
        assertEquals(3, flat.substring(inIdx, inEnd).split("\\?", -1).length - 1,
                "IN 必须覆盖同一批 3 个 id：" + sql);
        // 18（CASE 内 3列×3条×2）+ 1（update_time）+ 3（IN）
        assertEquals(22, bs.getParameterMappings().size(), "参数占位数量与语句结构不符：" + sql);
    }

    @Test
    @DisplayName("批量更新：候补排名为 null 时参数必须声明 jdbcType=BIGINT")
    void batchUpdate_nullWaitlistRankHasJdbcType() {
        List<TpmSelectionEnrollment> list = new ArrayList<>(Arrays.asList(
                enroll(1L, "1", "1", null),
                enroll(2L, "2", "2", 1)));

        BoundSql bs = boundSqlOf(list);

        List<ParameterMapping> nullRankMappings = new ArrayList<>();
        for (ParameterMapping pm : bs.getParameterMappings()) {
            if (pm.getProperty().endsWith(".waitlistRank")) {
                nullRankMappings.add(pm);
            }
        }
        assertEquals(2, nullRankMappings.size(), "每条记录都要有 waitlist_rank 的 WHEN 参数");
        for (ParameterMapping pm : nullRankMappings) {
            assertEquals(JdbcType.BIGINT, pm.getJdbcType(),
                    "waitlist_rank 为 null 时必须显式 jdbcType，否则 MyBatis 抛 setting null for parameter：" + pm.getProperty());
        }
    }

    // ==================== T-3：容量口径（落选/候补不算占名额） ====================

    private BoundSql boundSqlOf(String statementId, Map<String, Object> param) {
        MappedStatement ms = configuration.getMappedStatement(
                "com.yu.tpm.mapper.TpmSelectionEnrollmentMapper." + statementId);
        return ms.getBoundSql(param);
    }

    private static String normalized(String sql) {
        return sql.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    @Test
    @DisplayName("T-3：selectByStudentAndRound 只返回在册（result_status='1'），落选/候补不占门数")
    void selectByStudentAndRound_onlyAdmitted() {
        Map<String, Object> param = new HashMap<>();
        param.put("studentId", 101L);
        param.put("roundId", 1L);

        BoundSql bs = boundSqlOf("selectByStudentAndRound", param);
        String sql = normalized(bs.getSql());

        assertTrue(sql.contains("e.result_status = '1'"), "必须只认在册记录：" + sql);
        assertFalse(sql.contains("!= '3'"), "落选/候补（result_status='2'）不能再算作已选：" + sql);
        assertEquals(2, bs.getParameterMappings().size(), "studentId/roundId 两个参数：" + sql);
    }

    @Test
    @DisplayName("T-3：在册计数语句只数 result_status='1'，与 enrolled_count 同口径")
    void countEnrolledByOffering_onlyEnrolled() {
        Map<String, Object> param = new HashMap<>();
        param.put("courseOfferingId", 20L);

        String sql = normalized(boundSqlOf("countEnrolledByOffering", param).getSql());

        assertTrue(sql.contains("result_status = '1'"), "容量判定口径必须是 result_status='1'：" + sql);
        assertTrue(sql.contains("del_flag = '0'"), "必须排除已删除行：" + sql);
    }

    @Test
    @DisplayName("T-3：报名行计数保留 != '3' 口径（删除前校验用，含落选也算报名痕迹）")
    void selectCountByOffering_keepsRegistrationCaliber() {
        Map<String, Object> param = new HashMap<>();
        param.put("courseOfferingId", 20L);

        String sql = normalized(boundSqlOf("selectCountByOffering", param).getSql());

        assertTrue(sql.contains("result_status != '3'"), "删除前校验要拦住有落选记录的开课：" + sql);
    }

    // ==================== T-3 并发收口：状态迁移必须带 WHERE 栅栏 ====================

    @Test
    @DisplayName("T-3 并发：退课条件迁移限定 result_status='1'，否则释放名额会重计")
    void dropIfEnrolled_isConditionalTransition() {
        Map<String, Object> param = new HashMap<>();
        param.put("enrollId", 7L);
        param.put("dropTime", new Date());

        BoundSql bs = boundSqlOf("dropIfEnrolled", param);
        String sql = normalized(bs.getSql());

        assertTrue(sql.contains("set result_status = '3'"), "必须把退课状态写进 set：" + sql);
        assertTrue(sql.contains("and result_status = '1'"), "状态判定必须在 WHERE（Java 侧先查后改会重复释放名额）：" + sql);
        assertTrue(sql.contains("and del_flag = '0'"), "已删除行不得参与迁移：" + sql);
        assertEquals(3, bs.getParameterMappings().size(),
                "drop_time + update_time + enrollId 三个占位（两个时间列共用同一个参数值）：" + sql);
    }

    @Test
    @DisplayName("T-3 并发：改选条件迁移带旧开课栅栏，且不得写 enrolled_count")
    void swapIfEnrolled_fencesOnOldOffering() {
        Map<String, Object> param = new HashMap<>();
        param.put("enrollId", 7L);
        param.put("oldOfferingId", 20L);
        param.put("newOfferingId", 21L);
        param.put("updateTime", new Date());

        BoundSql bs = boundSqlOf("swapIfEnrolled", param);
        String sql = normalized(bs.getSql());

        assertTrue(sql.contains("and course_offering_id = ?"), "旧开课必须是栅栏条件：" + sql);
        assertTrue(sql.contains("and result_status = '1'"), "只允许迁在册记录：" + sql);
        assertTrue(sql.contains("lottery_result = '0'") && sql.contains("waitlist_rank = null"),
                "改选后重置为未抽签、非候补：" + sql);
        assertFalse(sql.contains("enrolled_count"), "名额只能由 occupySeat/releaseSeat 增量维护：" + sql);
        assertEquals(5, bs.getParameterMappings().size(),
                "newOfferingId + select_time + update_time + enrollId + oldOfferingId：" + sql);
    }

    @Test
    @DisplayName("T-3 并发：候补递补只允许把 '2' 升为 '1'，否则名额会被占两次")
    void promoteFromWaitlist_isConditional() {
        Map<String, Object> param = new HashMap<>();
        param.put("enrollId", 7L);
        param.put("updateTime", new Date());

        BoundSql bs = boundSqlOf("promoteFromWaitlist", param);
        String sql = normalized(bs.getSql());

        assertTrue(sql.contains("and result_status = '2'"), "递补必须限定候补态：" + sql);
        assertEquals(2, bs.getParameterMappings().size(), "enrollId + updateTime 两个参数：" + sql);
    }
}
