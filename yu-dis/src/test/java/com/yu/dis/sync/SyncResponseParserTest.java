package com.yu.dis.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.dis.domain.DisFieldMapping;

/**
 * {@link SyncResponseParser} 单元测试（D1"解析"环节核心逻辑）。
 */
class SyncResponseParserTest
{
    private final SyncResponseParser parser = new SyncResponseParser();

    private DisFieldMapping mapping(String table, String source, String column)
    {
        DisFieldMapping m = new DisFieldMapping();
        m.setTargetTable(table);
        m.setSourceField(source);
        m.setTargetColumn(column);
        m.setStatus("0");
        return m;
    }

    @Test
    @DisplayName("data数组响应按映射解析为目标表行数据")
    void parseDataArrayResponse()
    {
        String json = "{\"code\":200,\"data\":["
                + "{\"studentNo\":\"2024001\",\"name\":\"张三\",\"major\":\"计算机\"},"
                + "{\"studentNo\":\"2024002\",\"name\":\"李四\",\"major\":\"数学\"}]}";
        List<DisFieldMapping> mappings = new ArrayList<>();
        mappings.add(mapping("dis_demo_student", "studentNo", "student_no"));
        mappings.add(mapping("dis_demo_student", "name", "name"));
        mappings.add(mapping("dis_demo_student", "major", "major"));

        Map<String, List<Map<String, Object>>> result = parser.parse(json, mappings);

        assertTrue(result.containsKey("dis_demo_student"));
        List<Map<String, Object>> rows = result.get("dis_demo_student");
        assertEquals(2, rows.size());
        assertEquals("2024001", rows.get(0).get("student_no"));
        assertEquals("李四", rows.get(1).get("name"));
    }

    @Test
    @DisplayName("顶层即为数组的响应可直接解析")
    void parseTopLevelArray()
    {
        String json = "[{\"courseCode\":\"C01\",\"credit\":3},{\"courseCode\":\"C02\",\"credit\":4}]";
        List<DisFieldMapping> mappings = new ArrayList<>();
        mappings.add(mapping("dis_demo_course", "courseCode", "course_code"));
        mappings.add(mapping("dis_demo_course", "credit", "credit"));

        Map<String, List<Map<String, Object>>> result = parser.parse(json, mappings);
        List<Map<String, Object>> rows = result.get("dis_demo_course");
        assertEquals(2, rows.size());
        assertEquals("C02", rows.get(1).get("course_code"));
    }

    @Test
    @DisplayName("支持点分嵌套路径提取源字段")
    void parseNestedSourcePath()
    {
        String json = "{\"data\":[{\"user\":{\"info\":{\"realName\":\"王五\"}},\"age\":20}]}";
        List<DisFieldMapping> mappings = new ArrayList<>();
        mappings.add(mapping("dis_demo_user", "user.info.realName", "real_name"));
        mappings.add(mapping("dis_demo_user", "age", "age"));

        Map<String, List<Map<String, Object>>> result = parser.parse(json, mappings);
        List<Map<String, Object>> rows = result.get("dis_demo_user");
        assertEquals(1, rows.size());
        assertEquals("王五", rows.get(0).get("real_name"));
        assertEquals(20, ((Number) rows.get(0).get("age")).intValue());
    }

    @Test
    @DisplayName("映射到不同目标表时按表分组")
    void parseMultiTable()
    {
        String json = "{\"rows\":[{\"sno\":\"1\",\"cname\":\"高数\",\"ccredit\":5}]}";
        List<DisFieldMapping> mappings = new ArrayList<>();
        mappings.add(mapping("tbl_student", "sno", "student_no"));
        mappings.add(mapping("tbl_course", "cname", "course_name"));
        mappings.add(mapping("tbl_course", "ccredit", "credit"));

        Map<String, List<Map<String, Object>>> result = parser.parse(json, mappings);
        assertEquals(2, result.size());
        assertEquals(1, result.get("tbl_student").size());
        assertEquals(1, result.get("tbl_course").size());
        assertEquals("高数", result.get("tbl_course").get(0).get("course_name"));
    }

    @Test
    @DisplayName("非法表名/列名（疑似注入）被跳过")
    void rejectUnsafeIdentifiers()
    {
        String json = "{\"data\":[{\"a\":1}]}";
        List<DisFieldMapping> mappings = new ArrayList<>();
        mappings.add(mapping("tbl; DROP TABLE x--", "a", "col"));
        mappings.add(mapping("ok_tbl", "a", "col`) -- "));

        Map<String, List<Map<String, Object>>> result = parser.parse(json, mappings);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("无匹配记录或空响应返回空结果")
    void emptyOrNoMatch()
    {
        assertTrue(parser.parse("", new ArrayList<>()).isEmpty());
        assertTrue(parser.parse(null, new ArrayList<>()).isEmpty());
        assertTrue(parser.parse("<html>not json</html>", new ArrayList<>()).isEmpty());

        String json = "{\"data\":[]}";
        List<DisFieldMapping> mappings = new ArrayList<>();
        mappings.add(mapping("t", "x", "y"));
        assertTrue(parser.parse(json, mappings).isEmpty());
    }

    @Test
    @DisplayName("记录缺失全部映射字段时不产出空行")
    void skipRowWithoutAnyMappedValue()
    {
        String json = "{\"data\":[{\"other\":\"zzz\"}]}";
        List<DisFieldMapping> mappings = new ArrayList<>();
        mappings.add(mapping("t", "x", "y"));
        Map<String, List<Map<String, Object>>> result = parser.parse(json, mappings);
        assertFalse(result.containsKey("t"));
    }

    @Test
    @DisplayName("isSafeIdentifier 校验标识符白名单")
    void identifierSafetyCheck()
    {
        assertTrue(SyncResponseParser.isSafeIdentifier("dis_demo_student"));
        assertTrue(SyncResponseParser.isSafeIdentifier("_col1"));
        assertFalse(SyncResponseParser.isSafeIdentifier("col name"));
        assertFalse(SyncResponseParser.isSafeIdentifier("tbl;DROP"));
        assertFalse(SyncResponseParser.isSafeIdentifier("1abc"));
        assertFalse(SyncResponseParser.isSafeIdentifier(null));
    }
}
