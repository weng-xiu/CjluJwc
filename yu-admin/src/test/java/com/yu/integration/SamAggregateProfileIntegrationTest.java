package com.yu.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.yu.sam.service.ISamProgramQualityService;
import com.yu.sam.service.ISamStudentProfileService;

/**
 * G4 集成测试扩面 · 跨模块只读聚合（V5.13）。
 *
 * <p>防护对象：N6 学生数字档案 / F3 专业质量档案一页纸这类"跨 brm/tpm/aem/sam 多表 join"的只读聚合。
 * 分层单测（Service mock Mapper）与 MockMvc standalone（mock Service）都无法发现"Mapper 引用了
 * 库里不存在的列"这类装配缺陷——本类走真实 HikariCP + MyBatis + MySQL，直接执行聚合 SQL。
 *
 * <p>回归背景：{@code SamStudentProfileMapper.selectRecentGrades} 曾以 {@code gr.credit}
 * 引用 {@code aem_grade_record}（该表从无 credit 列，学分在 {@code tpm_course_library}），
 * 导致 {@code GET /sam/studentProfile/{id}} 与档案导出整体 500；运行态参数化补测发现，已修正为
 * {@code cl.credit}，此处用 recentGrades 的字段契约锁住该 join。
 *
 * <p>只读用例不改库，无需 @AfterEach 清场。依赖 fixture：学生 101、专业 1。
 */
class SamAggregateProfileIntegrationTest extends AbstractIntegrationTest
{
    private static final long STUDENT_ID = 101L;

    private static final long MAJOR_ID = 1L;

    @Autowired
    private ISamStudentProfileService studentProfileService;

    @Autowired
    private ISamProgramQualityService programQualityService;

    @Test
    @DisplayName("学生数字档案：聚合可用，近期成绩含课程名与学分（锁 cl.credit join）")
    void studentProfileAggregatesRecentGradesWithCredit()
    {
        Map<String, Object> profile = studentProfileService.buildProfile(STUDENT_ID);

        assertEquals(Boolean.TRUE, profile.get("available"), "学生 101 应能生成数字档案");
        assertNotNull(profile.get("student"), "学籍基础板块不应为空");
        assertNotNull(profile.get("completeness"), "完备度口径应返回");

        Object recent = profile.get("recentGrades");
        assertTrue(recent instanceof List, "recentGrades 应为列表");
        List<?> grades = (List<?>) recent;
        assertFalse(grades.isEmpty(), "学生 101 有成绩记录，近期成绩不应为空");

        Map<?, ?> first = (Map<?, ?>) grades.get(0);
        assertTrue(first.containsKey("courseName"), "近期成绩应含 courseName（来源 tpm_course_library）");
        assertTrue(first.containsKey("credit"), "近期成绩应含 credit（来源 tpm_course_library.cl.credit）");
        assertNotNull(first.get("credit"), "学分取值不应为 null");
        assertTrue(first.containsKey("totalScore"), "近期成绩应含 totalScore（来源 aem_grade_record）");
    }

    @Test
    @DisplayName("学生数字档案：学号不存在时返回 available=false 而非抛异常")
    void studentProfileReturnsUnavailableForUnknownStudent()
    {
        Map<String, Object> profile = studentProfileService.buildProfile(99999999L);

        assertEquals(Boolean.FALSE, profile.get("available"));
        assertNotNull(profile.get("note"));
    }

    @Test
    @DisplayName("专业质量档案：七个板块聚合可用")
    void programQualityAggregatesAllSections()
    {
        Map<String, Object> quality = programQualityService.buildProfile(MAJOR_ID);

        assertEquals(Boolean.TRUE, quality.get("available"), "专业 1 应能生成质量档案");
        assertNotNull(quality.get("major"));
        assertNotNull(quality.get("studentStats"), "培养规模板块应返回");
        assertNotNull(quality.get("faculty"), "师资队伍板块应返回");
        assertNotNull(quality.get("plans"), "培养方案板块应返回");
    }
}
