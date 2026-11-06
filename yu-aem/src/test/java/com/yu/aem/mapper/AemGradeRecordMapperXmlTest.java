package com.yu.aem.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
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

import com.yu.aem.domain.AemGradeRecord;

/**
 * E 档：成绩重复导入 SQL 的结构测试（不连数据库）。
 *
 * <p>「重复导入即覆盖」由两条约束共同保证：唯一索引 uk_grade_scset 让重复行落到
 * {@code on duplicate key update}，状态谓词让已定稿成绩不被覆盖。唯一索引由
 * db/migration/V20261106_1 负责，本测试锁住后半段——任何一列漏掉
 * {@code if(ifnull(submit_status,'0') in ('0','3'), 新值, 原值)} 谓词，
 * 一次重复导入就能绕过审核把「已提交待审/已锁定」的成绩改掉。</p>
 */
class AemGradeRecordMapperXmlTest
{
    private static final String NS = "com.yu.aem.mapper.AemGradeRecordMapper.";

    /** 允许被覆盖的成绩状态：未提交(0) / 已驳回(3)；1 待审、2 已锁定不可覆盖 */
    private static final String GUARD = "if(ifnull(submit_status, '0') in ('0', '3'),";

    /** ODKU 里必须逐列受状态谓词保护的字段（顺序与 XML 一致） */
    private static final List<String> GUARDED_COLUMNS = List.of(
            "regular_score", "exam_score", "total_score", "grade_point",
            "grade_level", "is_pass", "update_by", "update_time");

    private static Configuration configuration;

    @BeforeAll
    static void parseMapperXml() throws Exception
    {
        configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("AemGradeRecord", AemGradeRecord.class);
        String resource = "mapper/aem/AemGradeRecordMapper.xml";
        try (InputStream in = Resources.getResourceAsStream(resource))
        {
            new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    private BoundSql boundSqlOf(String statementId, Object parameter)
    {
        MappedStatement ms = configuration.getMappedStatement(NS + statementId);
        return ms.getBoundSql(parameter);
    }

    /**
     * batchInsert 的入参是 List，直连 getBoundSql 时没有 MapperMethod 的 ParamNameResolver 包装，
     * 需手动补上 foreach 引用的 collection 键（生产链路由 MyBatis 自动包裹为 list/collection）。
     */
    private BoundSql boundSqlOfList(String statementId, List<AemGradeRecord> rows)
    {
        Map<String, Object> param = new HashMap<>();
        param.put("list", rows);
        param.put("collection", rows);
        return boundSqlOf(statementId, param);
    }

    private String normalize(String sql)
    {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private List<AemGradeRecord> oneRow()
    {
        AemGradeRecord record = new AemGradeRecord();
        record.setStudentId(101L);
        record.setCourseId(201L);
        record.setSemesterId(1L);
        record.setExamType("0");
        record.setRegularScore(80.0);
        record.setExamScore(90.0);
        record.setTotalScore(87.0);
        record.setGradePoint(3.0);
        record.setGradeLevel("C");
        record.setIsPass("1");
        record.setIsReviewed("0");
        record.setStatus("0");
        record.setCreateBy("tester");
        List<AemGradeRecord> list = new ArrayList<>();
        list.add(record);
        return list;
    }

    @Test
    @DisplayName("batchInsert：ODKU 每列都套状态谓词，没有一列是裸 values()")
    void batchInsert_guardsEveryColumnWithSubmitStatus()
    {
        String raw = boundSqlOfList("batchInsert", oneRow()).getSql();
        String normalized = normalize(raw);

        assertTrue(normalized.contains("on duplicate key update"), normalized);

        String odku = raw.substring(raw.indexOf("on duplicate key update") + "on duplicate key update".length());
        List<String> guarded = new ArrayList<>();
        for (String line : odku.split("\\r?\\n"))
        {
            String t = line.trim();
            if (t.isEmpty())
            {
                continue;
            }
            // 形如 `regular_score = if(ifnull(submit_status, '0') in ('0', '3'), values(regular_score), regular_score),`
            // XML 里等号做了纵向对齐，先压空白再比对
            String compact = t.replaceAll("\\s+", " ");
            int eq = compact.indexOf('=');
            assertTrue(eq > 0, "无法识别的 ODKU 片段：" + t);
            String column = compact.substring(0, eq).trim();
            assertTrue(compact.startsWith(column + " = " + GUARD),
                    "该列未被状态谓词保护，重复导入会覆盖已定稿成绩：" + t);
            guarded.add(column);
        }
        assertEquals(GUARDED_COLUMNS, guarded, "受保护字段清单被改动：新增/删减列都会破坏「已锁定不可覆盖」的判定面");

        // update_by 必须写操作人（values(create_by)），不能沿用被覆盖行的原值
        assertTrue(normalized.contains("update_by = if(ifnull(submit_status, '0') in ('0', '3'), values(create_by), update_by)"),
                normalized);
        // update_time 只在真正覆盖时才刷新，跳过时保留原值
        assertTrue(normalized.contains("update_time = if(ifnull(submit_status, '0') in ('0', '3'), CURRENT_TIMESTAMP, update_time)"),
                normalized);
    }

    @Test
    @DisplayName("batchInsert：插入列清单不含 submit_status（新行走库默认 '0'）")
    void batchInsert_doesNotWriteSubmitStatus()
    {
        String raw = boundSqlOfList("batchInsert", oneRow()).getSql();
        String insertPart = raw.substring(0, raw.indexOf("on duplicate key update"));

        assertFalse(insertPart.contains("submit_status"),
                "导入不得自行写入 submit_status，否则可以伪造「未提交」绕过锁定：" + normalize(insertPart));
    }

    @Test
    @DisplayName("selectLockedGradeKeys：只取待审/锁定行，按学期批量过滤，exam_type 归一为 '0'")
    void selectLockedGradeKeys_filtersFinalizedRowsBySemester()
    {
        Map<String, Object> param = new HashMap<>();
        param.put("semesterIds", List.of(1L, 2L));
        String sql = normalize(boundSqlOf("selectLockedGradeKeys", param).getSql());

        assertTrue(sql.contains("ifnull(exam_type, '0') as exam_type"),
                "exam_type 需归一：唯一索引不约束 NULL，历史 NULL 行否则会漏出跳过判定：" + sql);
        assertTrue(sql.contains("where semester_id in ( ? , ? )"), sql);
        assertTrue(sql.contains("and submit_status in ('1', '2')"), sql);
        assertFalse(sql.contains("submit_status = ?"), "待审/锁定必须一次取两种状态，不能按单值过滤：" + sql);
    }
}
