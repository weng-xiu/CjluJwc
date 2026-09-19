package com.yu.sam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.yu.sam.domain.SamGraduationReview;
import com.yu.sam.mapper.SamGraduationReviewMapper;
import com.yu.sam.mapper.SamWarningDataMapper;

/**
 * 毕业资格审核单元测试（S1 接线 / S2 判定修正）。
 * 使用 Mockito 隔离数据库，覆盖核心判定分支。
 */
@ExtendWith(MockitoExtension.class)
class SamGraduationReviewServiceImplTest {

    @Mock
    private SamGraduationReviewMapper samGraduationReviewMapper;

    @Mock
    private SamWarningDataMapper samWarningDataMapper;

    @InjectMocks
    private SamGraduationReviewServiceImpl service;

    private SamGraduationReview legacyPassBase() {
        // 旧口径：required 取自已修学分之和，与 earned 相等 → 学分恒达标
        when(samWarningDataMapper.selectStudentEarnedCredits(eq(1L), isNull())).thenReturn(200.0);
        when(samWarningDataMapper.selectStudentRequiredCredits(eq(1L), isNull())).thenReturn(200.0);
        when(samWarningDataMapper.selectStudentFailCourseCount(eq(1L), isNull())).thenReturn(0);
        when(samWarningDataMapper.selectPlanIdByStudent(eq(1L))).thenReturn(null);
        return null;
    }

    @Test
    @DisplayName("S1/旧口径：全部条件满足应自动审核通过并落库")
    void autoReview_legacyAllPass() {
        ReflectionTestUtils.setField(service, "usePlanCredit", false);
        legacyPassBase();
        // 英语/体育：属性与关键字均无课程 → 0 未通过
        when(samWarningDataMapper.countCoursesByCourseCategory(eq(1L), isNull(), any())).thenReturn(0);
        when(samWarningDataMapper.countCoursesByNameKeyword(eq(1L), isNull(), any())).thenReturn(0);

        SamGraduationReview r = service.autoReview(1L);

        assertEquals("1", r.getReviewStatus(), "全部合格应为通过");
        assertEquals("1", r.getIsCreditQualified());
        assertEquals("1", r.getIsCourseQualified());
        assertEquals("1", r.getIsEnglishQualified());
        assertEquals("1", r.getIsPeQualified());
        assertTrue(r.getReviewOpinion().contains("旧口径"), "应记录口径来源");
        verify(samGraduationReviewMapper, times(1)).insertSamGraduationReview(any(SamGraduationReview.class));
    }

    @Test
    @DisplayName("S2：启用培养方案口径且应修学分>已修学分应判定不通过")
    void autoReview_planCreditNotReach() {
        ReflectionTestUtils.setField(service, "usePlanCredit", true);
        when(samWarningDataMapper.selectStudentEarnedCredits(eq(1L), isNull())).thenReturn(150.0);
        when(samWarningDataMapper.selectPlanIdByStudent(eq(1L))).thenReturn(9L);
        when(samWarningDataMapper.selectPlanRequiredCredits(eq(9L))).thenReturn(300.0);
        when(samWarningDataMapper.selectStudentFailCourseCount(eq(1L), isNull())).thenReturn(0);
        when(samWarningDataMapper.selectPlanCreditSections(eq(9L))).thenReturn(null);
        when(samWarningDataMapper.countCoursesByCourseCategory(eq(1L), isNull(), any())).thenReturn(0);
        when(samWarningDataMapper.countCoursesByNameKeyword(eq(1L), isNull(), any())).thenReturn(0);

        SamGraduationReview r = service.autoReview(1L);

        assertEquals("0", r.getIsCreditQualified(), "已修150<方案300，学分不达标");
        assertEquals("2", r.getReviewStatus());
        assertEquals(300.0, r.getRequiredCredits(), 0.001);
        assertTrue(r.getReviewOpinion().contains("培养方案"), "应记录培养方案来源");
    }

    @Test
    @DisplayName("S2：英语课程属性标记优先于关键字判定")
    void autoReview_englishByAttributeMarker() {
        ReflectionTestUtils.setField(service, "usePlanCredit", false);
        when(samWarningDataMapper.selectStudentEarnedCredits(eq(1L), isNull())).thenReturn(200.0);
        when(samWarningDataMapper.selectStudentRequiredCredits(eq(1L), isNull())).thenReturn(200.0);
        when(samWarningDataMapper.selectStudentFailCourseCount(eq(1L), isNull())).thenReturn(0);
        when(samWarningDataMapper.selectPlanIdByStudent(eq(1L))).thenReturn(null);
        // 英语：属性标记存在(2门)且其中1门不及格；体育：无课程
        when(samWarningDataMapper.countCoursesByCourseCategory(eq(1L), isNull(), eq("FOREIGN_LANGUAGE"))).thenReturn(2);
        when(samWarningDataMapper.countFailByCourseCategory(eq(1L), isNull(), eq("FOREIGN_LANGUAGE"))).thenReturn(1);
        when(samWarningDataMapper.countCoursesByCourseCategory(eq(1L), isNull(), eq("PE"))).thenReturn(0);
        when(samWarningDataMapper.countCoursesByNameKeyword(eq(1L), isNull(), eq("体育"))).thenReturn(0);

        SamGraduationReview r = service.autoReview(1L);

        assertEquals("0", r.getIsEnglishQualified(), "属性标记命中1门不及格，英语不合格");
        assertEquals("1", r.getIsPeQualified());
        assertEquals("2", r.getReviewStatus());
        // 英语走属性，不应再走关键字
        verify(samWarningDataMapper, times(0)).countFailByCourseNameKeyword(eq(1L), isNull(), eq("英语"));
    }

    @Test
    @DisplayName("S2：属性未治理时英语降级为课程名关键字")
    void autoReview_englishFallbackKeyword() {
        ReflectionTestUtils.setField(service, "usePlanCredit", false);
        when(samWarningDataMapper.selectStudentEarnedCredits(eq(1L), isNull())).thenReturn(200.0);
        when(samWarningDataMapper.selectStudentRequiredCredits(eq(1L), isNull())).thenReturn(200.0);
        when(samWarningDataMapper.selectStudentFailCourseCount(eq(1L), isNull())).thenReturn(0);
        when(samWarningDataMapper.selectPlanIdByStudent(eq(1L))).thenReturn(null);
        // 英语：无属性标记但有名字含英语的课程且不及格
        when(samWarningDataMapper.countCoursesByCourseCategory(eq(1L), isNull(), eq("FOREIGN_LANGUAGE"))).thenReturn(0);
        when(samWarningDataMapper.countCoursesByNameKeyword(eq(1L), isNull(), eq("英语"))).thenReturn(3);
        when(samWarningDataMapper.countFailByCourseNameKeyword(eq(1L), isNull(), eq("英语"))).thenReturn(1);
        when(samWarningDataMapper.countCoursesByCourseCategory(eq(1L), isNull(), eq("PE"))).thenReturn(0);
        when(samWarningDataMapper.countCoursesByNameKeyword(eq(1L), isNull(), eq("体育"))).thenReturn(0);

        SamGraduationReview r = service.autoReview(1L);

        assertEquals("0", r.getIsEnglishQualified(), "降级关键字命中不及格，英语不合格");
        assertEquals("2", r.getReviewStatus());
    }

    @Test
    @DisplayName("S1：批量审核汇总通过/不通过计数")
    void batchAutoReview_counts() {
        ReflectionTestUtils.setField(service, "usePlanCredit", false);
        // 学生1 通过
        when(samWarningDataMapper.selectStudentEarnedCredits(eq(1L), isNull())).thenReturn(200.0);
        when(samWarningDataMapper.selectStudentRequiredCredits(eq(1L), isNull())).thenReturn(200.0);
        // 学生2 学分不达标（属性/关键字均无课程）
        when(samWarningDataMapper.selectStudentEarnedCredits(eq(2L), isNull())).thenReturn(50.0);
        when(samWarningDataMapper.selectStudentRequiredCredits(eq(2L), isNull())).thenReturn(200.0);
        when(samWarningDataMapper.selectStudentFailCourseCount(any(), isNull())).thenReturn(0);
        when(samWarningDataMapper.selectPlanIdByStudent(any())).thenReturn(null);
        when(samWarningDataMapper.countCoursesByCourseCategory(any(), isNull(), any())).thenReturn(0);
        when(samWarningDataMapper.countCoursesByNameKeyword(any(), isNull(), any())).thenReturn(0);

        List<Long> ids = Arrays.asList(1L, 2L);
        Map<String, Object> result = service.batchAutoReview(ids);

        assertEquals(2, result.get("total"));
        assertEquals(1, result.get("passed"));
        assertEquals(1, result.get("rejected"));
        verify(samGraduationReviewMapper, times(2)).insertSamGraduationReview(any());
    }

    @Test
    @DisplayName("S1：批量审核null列表安全返回空汇总")
    void batchAutoReview_nullSafe() {
        Map<String, Object> result = service.batchAutoReview(null);
        assertNotNull(result);
        assertEquals(0, result.get("total"));
    }

    @Test
    @DisplayName("S2：启用方案且方案有学分结构时，审核意见附分项达成")
    void autoReview_sectionBreakdownInOpinion() {
        ReflectionTestUtils.setField(service, "usePlanCredit", true);
        when(samWarningDataMapper.selectStudentEarnedCredits(eq(1L), isNull())).thenReturn(160.0);
        when(samWarningDataMapper.selectPlanIdByStudent(eq(1L))).thenReturn(9L);
        when(samWarningDataMapper.selectPlanRequiredCredits(eq(9L))).thenReturn(160.0);
        when(samWarningDataMapper.selectStudentFailCourseCount(eq(1L), isNull())).thenReturn(0);
        when(samWarningDataMapper.countCoursesByCourseCategory(eq(1L), isNull(), any())).thenReturn(0);
        when(samWarningDataMapper.countCoursesByNameKeyword(eq(1L), isNull(), any())).thenReturn(0);
        Map<String, Object> section = new HashMap<>();
        section.put("creditType", "GENERAL");
        section.put("creditTypeName", "通识教育");
        section.put("requiredCredit", 60.0);
        when(samWarningDataMapper.selectPlanCreditSections(eq(9L))).thenReturn(Arrays.asList(section));
        when(samWarningDataMapper.sumEarnedCreditByCourseCategory(eq(1L), isNull(), eq("GENERAL"))).thenReturn(60.0);

        SamGraduationReview r = service.autoReview(1L);

        assertEquals("1", r.getReviewStatus());
        assertTrue(r.getReviewOpinion().contains("通识教育:60.0/60.0✓"), "应附分项达成明细");
    }
}
