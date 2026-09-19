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
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.yu.sam.domain.SamDegreeReview;
import com.yu.sam.mapper.SamDegreeReviewMapper;
import com.yu.sam.mapper.SamWarningDataMapper;

/**
 * 学位资格审核单元测试（S1 接线 / GPA 阈值可配置）。
 */
@ExtendWith(MockitoExtension.class)
class SamDegreeReviewServiceImplTest {

    @Mock
    private SamDegreeReviewMapper samDegreeReviewMapper;

    @Mock
    private SamWarningDataMapper samWarningDataMapper;

    @InjectMocks
    private SamDegreeReviewServiceImpl service;

    @Test
    @DisplayName("S1：GPA达标且学位课程全通过应审核通过")
    void autoReview_pass() {
        ReflectionTestUtils.setField(service, "gpaThreshold", 2.0);
        when(samWarningDataMapper.selectStudentGpa(eq(1L), isNull())).thenReturn(3.2);
        when(samWarningDataMapper.countDegreeCourseFail(eq(1L), isNull())).thenReturn(0);

        SamDegreeReview r = service.autoReview(1L);

        assertEquals("1", r.getReviewStatus());
        assertEquals("1", r.getIsGpaQualified());
        assertEquals("1", r.getIsDegreeCourseQualified());
        verify(samDegreeReviewMapper, times(1)).insertSamDegreeReview(any());
    }

    @Test
    @DisplayName("S3基础：GPA低于可配置阈值应判定不通过")
    void autoReview_gpaBelowConfigurableThreshold() {
        ReflectionTestUtils.setField(service, "gpaThreshold", 2.5);
        when(samWarningDataMapper.selectStudentGpa(eq(1L), isNull())).thenReturn(2.2);
        when(samWarningDataMapper.countDegreeCourseFail(eq(1L), isNull())).thenReturn(0);

        SamDegreeReview r = service.autoReview(1L);

        assertEquals("0", r.getIsGpaQualified(), "2.2 < 阈值2.5 应不合格");
        assertEquals("2", r.getReviewStatus());
        assertTrue(r.getReviewOpinion().contains("2.5"), "意见应体现配置阈值");
    }

    @Test
    @DisplayName("S1：批量审核汇总")
    void batchAutoReview_counts() {
        ReflectionTestUtils.setField(service, "gpaThreshold", 2.0);
        when(samWarningDataMapper.selectStudentGpa(eq(1L), isNull())).thenReturn(3.0);
        when(samWarningDataMapper.selectStudentGpa(eq(2L), isNull())).thenReturn(1.0);
        when(samWarningDataMapper.countDegreeCourseFail(any(), isNull())).thenReturn(0);

        Map<String, Object> result = service.batchAutoReview(Arrays.asList(1L, 2L));

        assertEquals(2, result.get("total"));
        assertEquals(1, result.get("passed"));
        assertEquals(1, result.get("rejected"));
        verify(samDegreeReviewMapper, times(2)).insertSamDegreeReview(any());
    }
}
