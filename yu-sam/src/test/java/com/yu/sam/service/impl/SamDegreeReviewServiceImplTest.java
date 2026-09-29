package com.yu.sam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yu.sam.domain.SamDegreeConfig;
import com.yu.sam.domain.SamDegreeReview;
import com.yu.sam.domain.SamThesis;
import com.yu.sam.mapper.SamDegreeReviewMapper;
import com.yu.sam.mapper.SamThesisMapper;
import com.yu.sam.mapper.SamWarningDataMapper;
import com.yu.sam.service.ISamDegreeConfigService;

/**
 * 学位资格审核单元测试（S3：审核条件全部来自后台可配置的学位授予条件配置）。
 */
@ExtendWith(MockitoExtension.class)
class SamDegreeReviewServiceImplTest {

    @Mock
    private SamDegreeReviewMapper samDegreeReviewMapper;

    @Mock
    private SamWarningDataMapper samWarningDataMapper;

    @Mock
    private SamThesisMapper samThesisMapper;

    @Mock
    private ISamDegreeConfigService samDegreeConfigService;

    @InjectMocks
    private SamDegreeReviewServiceImpl service;

    /** 构造生效配置：GPA 阈值可指定，学位课程/外语/论文分项均要求且数据可满足。 */
    private SamDegreeConfig config(double gpaThreshold) {
        SamDegreeConfig cfg = new SamDegreeConfig();
        cfg.setConfigName("测试配置");
        cfg.setGpaThreshold(gpaThreshold);
        cfg.setRequireDegreeCourse("1");
        cfg.setRequireForeignLanguage("1");
        cfg.setRequireThesis("1");
        cfg.setRequireAchievement("0");
        return cfg;
    }

    /** 构造已归档且判定合格的论文记录，满足 requireThesis=1 的分项校验。 */
    private SamThesis qualifiedArchivedThesis() {
        SamThesis thesis = new SamThesis();
        thesis.setTopicName("测试论文");
        thesis.setTotalScore(85.0);
        thesis.setIsQualified("1");
        thesis.setArchiveTime(new Date());
        return thesis;
    }

    @Test
    @DisplayName("S3：GPA达标且各分项满足配置要求应审核通过")
    void autoReview_pass() {
        when(samDegreeConfigService.resolveEffective()).thenReturn(config(2.0));
        when(samWarningDataMapper.selectStudentGpa(eq(1L), isNull())).thenReturn(3.2);
        when(samWarningDataMapper.countDegreeCourseFail(eq(1L), isNull())).thenReturn(0);
        when(samWarningDataMapper.countFailByCourseNameKeyword(eq(1L), isNull(), eq("英语"))).thenReturn(0);
        when(samThesisMapper.selectLatestByStudentId(eq(1L))).thenReturn(qualifiedArchivedThesis());

        SamDegreeReview r = service.autoReview(1L);

        assertEquals("1", r.getReviewStatus());
        assertEquals("1", r.getIsGpaQualified());
        assertEquals("1", r.getIsDegreeCourseQualified());
        assertEquals("1", r.getIsThesisQualified());
        verify(samDegreeReviewMapper, times(1)).insertSamDegreeReview(any());
    }

    @Test
    @DisplayName("S3：GPA低于配置阈值应判定不通过且意见体现阈值")
    void autoReview_gpaBelowConfigurableThreshold() {
        when(samDegreeConfigService.resolveEffective()).thenReturn(config(2.5));
        when(samWarningDataMapper.selectStudentGpa(eq(1L), isNull())).thenReturn(2.2);
        when(samWarningDataMapper.countDegreeCourseFail(eq(1L), isNull())).thenReturn(0);
        when(samWarningDataMapper.countFailByCourseNameKeyword(eq(1L), isNull(), eq("英语"))).thenReturn(0);
        when(samThesisMapper.selectLatestByStudentId(eq(1L))).thenReturn(qualifiedArchivedThesis());

        SamDegreeReview r = service.autoReview(1L);

        assertEquals("0", r.getIsGpaQualified(), "2.2 < 阈值2.5 应不合格");
        assertEquals("2", r.getReviewStatus());
        assertTrue(r.getReviewOpinion().contains("2.5"), "意见应体现配置阈值");
    }

    @Test
    @DisplayName("S3：要求论文但无已归档合格论文应不通过并给出原因")
    void autoReview_thesisMissingRejects() {
        when(samDegreeConfigService.resolveEffective()).thenReturn(config(2.0));
        when(samWarningDataMapper.selectStudentGpa(eq(1L), isNull())).thenReturn(3.5);
        when(samWarningDataMapper.countDegreeCourseFail(eq(1L), isNull())).thenReturn(0);
        when(samWarningDataMapper.countFailByCourseNameKeyword(eq(1L), isNull(), eq("英语"))).thenReturn(0);
        when(samThesisMapper.selectLatestByStudentId(eq(1L))).thenReturn(null);

        SamDegreeReview r = service.autoReview(1L);

        assertEquals("0", r.getIsThesisQualified());
        assertEquals("2", r.getReviewStatus());
        assertTrue(r.getReviewOpinion().contains("无毕业论文"), "意见应说明缺少论文档案");
    }

    @Test
    @DisplayName("S3：预审试算不落库")
    void simulateReview_doesNotPersist() {
        when(samDegreeConfigService.resolveEffective()).thenReturn(config(2.0));
        when(samWarningDataMapper.selectStudentGpa(eq(1L), isNull())).thenReturn(3.0);
        when(samWarningDataMapper.countDegreeCourseFail(eq(1L), isNull())).thenReturn(0);
        when(samWarningDataMapper.countFailByCourseNameKeyword(eq(1L), isNull(), eq("英语"))).thenReturn(0);
        when(samThesisMapper.selectLatestByStudentId(eq(1L))).thenReturn(qualifiedArchivedThesis());

        SamDegreeReview r = service.simulateReview(1L);

        assertEquals("1", r.getReviewStatus());
        verify(samDegreeReviewMapper, times(0)).insertSamDegreeReview(any());
    }

    @Test
    @DisplayName("S3：批量审核汇总通过/不通过计数")
    void batchAutoReview_counts() {
        when(samDegreeConfigService.resolveEffective()).thenReturn(config(2.0));
        when(samWarningDataMapper.selectStudentGpa(eq(1L), isNull())).thenReturn(3.0);
        when(samWarningDataMapper.selectStudentGpa(eq(2L), isNull())).thenReturn(1.0);
        when(samWarningDataMapper.countDegreeCourseFail(any(), isNull())).thenReturn(0);
        when(samWarningDataMapper.countFailByCourseNameKeyword(any(), isNull(), eq("英语"))).thenReturn(0);
        when(samThesisMapper.selectLatestByStudentId(any())).thenReturn(qualifiedArchivedThesis());

        Map<String, Object> result = service.batchAutoReview(Arrays.asList(1L, 2L));

        assertEquals(2, result.get("total"));
        assertEquals(1, result.get("passed"));
        assertEquals(1, result.get("rejected"));
        verify(samDegreeReviewMapper, times(2)).insertSamDegreeReview(any());
    }
}
