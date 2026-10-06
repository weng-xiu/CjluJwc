package com.yu.sam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.yu.sam.domain.SamObeCourseSupport;
import com.yu.sam.domain.SamObeGraduationRequirement;
import com.yu.sam.domain.SamObeIndicatorPoint;
import com.yu.sam.domain.dto.ObeAttainmentVo;
import com.yu.sam.domain.dto.ObeCourseScore;
import com.yu.sam.mapper.SamObeCourseSupportMapper;
import com.yu.sam.mapper.SamObeDataMapper;
import com.yu.sam.mapper.SamObeGraduationRequirementMapper;
import com.yu.sam.mapper.SamObeIndicatorPointMapper;

/**
 * OBE 达成度服务装配单测（N1）：群体/个体口径计算、看板汇总、指标点方案回填、级联删除。
 * 计算器为真实实例，仅隔离 Mapper。
 *
 * @author N1
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamObeAttainmentServiceImplTest
{
    @Mock
    private SamObeGraduationRequirementMapper requirementMapper;

    @Mock
    private SamObeIndicatorPointMapper indicatorMapper;

    @Mock
    private SamObeCourseSupportMapper supportMapper;

    @Mock
    private SamObeDataMapper dataMapper;

    @InjectMocks
    private SamObeAttainmentServiceImpl service;

    private SamObeGraduationRequirement gr(long id, long planId, double target)
    {
        SamObeGraduationRequirement g = new SamObeGraduationRequirement();
        g.setGrId(id);
        g.setPlanId(planId);
        g.setGrCode("GR-0" + id);
        g.setGrName("毕业要求" + id);
        g.setTargetValue(target);
        g.setStatus("0");
        g.setDelFlag("0");
        return g;
    }

    private SamObeIndicatorPoint ip(long id, long grId, double weight, double target)
    {
        SamObeIndicatorPoint p = new SamObeIndicatorPoint();
        p.setIpId(id);
        p.setGrId(grId);
        p.setIpCode(id + "");
        p.setIpName("指标点" + id);
        p.setWeight(weight);
        p.setTargetValue(target);
        p.setStatus("0");
        p.setDelFlag("0");
        return p;
    }

    private SamObeCourseSupport support(long ipId, long courseId, double weight)
    {
        SamObeCourseSupport s = new SamObeCourseSupport();
        s.setIpId(ipId);
        s.setCourseId(courseId);
        s.setSupportWeight(weight);
        s.setStatus("0");
        s.setDelFlag("0");
        return s;
    }

    private ObeCourseScore score(long courseId, double sc)
    {
        return new ObeCourseScore(courseId, sc);
    }

    @Test
    @DisplayName("calculateCohort：群体口径按课程均分计算达成度")
    void calculateCohort_computesAttainment()
    {
        when(requirementMapper.selectByPlanId(1L)).thenReturn(List.of(gr(1L, 1L, 0.7)));
        when(indicatorMapper.selectByPlanId(1L)).thenReturn(List.of(ip(10L, 1L, 1.0, 0.7)));
        when(supportMapper.selectByPlanId(1L)).thenReturn(new ArrayList<>(List.of(
                support(10L, 100L, 1.0), support(10L, 101L, 1.0))));
        when(dataMapper.selectCourseAverageScores(any())).thenReturn(List.of(score(100L, 0.8), score(101L, 0.6)));
        when(dataMapper.selectCourseNames()).thenReturn(List.of());

        List<ObeAttainmentVo> res = service.calculateCohort(1L, null);

        assertEquals(1, res.size());
        // 指标点 = (0.8+0.6)/2 = 0.7 → 达成
        assertEquals(0.7, res.get(0).getAttainment(), 1e-9);
        assertEquals("1", res.get(0).getAchieved());
        assertEquals("cohort", res.get(0).getScope());
    }

    @Test
    @DisplayName("calculateByStudent：使用学生成绩并携带 studentId")
    void calculateByStudent_setsStudentScope()
    {
        when(requirementMapper.selectByPlanId(1L)).thenReturn(List.of(gr(1L, 1L, 0.7)));
        when(indicatorMapper.selectByPlanId(1L)).thenReturn(List.of(ip(10L, 1L, 1.0, 0.7)));
        when(supportMapper.selectByPlanId(1L)).thenReturn(List.of(support(10L, 100L, 1.0)));
        when(dataMapper.selectStudentCourseScores(9L)).thenReturn(List.of(score(100L, 0.9)));
        when(dataMapper.selectCourseNames()).thenReturn(List.of());

        List<ObeAttainmentVo> res = service.calculateByStudent(1L, 9L);

        assertEquals(1, res.size());
        assertEquals(9L, res.get(0).getStudentId());
        assertEquals("student", res.get(0).getScope());
        assertEquals(0.9, res.get(0).getAttainment(), 1e-9);
    }

    @Test
    @DisplayName("compute：无毕业要求时短路返回，不再查询指标点")
    void compute_noRequirementShortCircuit()
    {
        when(requirementMapper.selectByPlanId(1L)).thenReturn(List.of());
        List<ObeAttainmentVo> res = service.calculateCohort(1L, null);
        assertEquals(0, res.size());
        verify(indicatorMapper, never()).selectByPlanId(anyLong());
    }

    @Test
    @DisplayName("dashboard：汇总达成数与平均达成度")
    void dashboard_summary()
    {
        when(requirementMapper.selectByPlanId(1L)).thenReturn(List.of(gr(1L, 1L, 0.7), gr(2L, 1L, 0.7)));
        when(indicatorMapper.selectByPlanId(1L)).thenReturn(List.of(ip(10L, 1L, 1.0, 0.7), ip(20L, 2L, 1.0, 0.7)));
        when(supportMapper.selectByPlanId(1L)).thenReturn(List.of(support(10L, 100L, 1.0), support(20L, 101L, 1.0)));
        when(dataMapper.selectCourseAverageScores(any())).thenReturn(List.of(score(100L, 0.9), score(101L, 0.4)));
        when(dataMapper.selectCourseNames()).thenReturn(List.of());

        Map<String, Object> dash = service.dashboard(1L, null);

        assertEquals(1L, dash.get("planId"));
        assertEquals(2, dash.get("totalRequirements"));
        assertEquals(1, dash.get("achievedRequirements")); // GR1=0.9达成, GR2=0.4未达成
        assertEquals(0.5, dash.get("achievedRate"));
        assertNotNull(dash.get("requirements"));
        assertEquals(0.65, (Double) dash.get("averageAttainment"), 1e-9);
    }

    @Test
    @DisplayName("insertIndicator：planId 缺省时按毕业要求自动回填")
    void insertIndicator_fillsPlanFromRequirement()
    {
        SamObeIndicatorPoint record = ip(10L, 1L, 1.0, 0.7);
        record.setPlanId(null);
        when(requirementMapper.selectSamObeGraduationRequirementByGrId(1L)).thenReturn(gr(1L, 555L, 0.7));
        when(indicatorMapper.insertSamObeIndicatorPoint(any())).thenReturn(1);

        service.insertIndicator(record);

        ArgumentCaptor<SamObeIndicatorPoint> cap = ArgumentCaptor.forClass(SamObeIndicatorPoint.class);
        verify(indicatorMapper).insertSamObeIndicatorPoint(cap.capture());
        assertEquals(555L, cap.getValue().getPlanId());
    }

    @Test
    @DisplayName("deleteRequirementByGrIds：级联软删指标点与课程支撑")
    void deleteRequirement_cascade()
    {
        when(indicatorMapper.selectByGrId(1L)).thenReturn(List.of(ip(10L, 1L, 1.0, 0.7)));
        when(supportMapper.deleteByIpIds(any())).thenReturn(1);
        when(indicatorMapper.deleteByGrIds(any())).thenReturn(1);
        when(requirementMapper.deleteSamObeGraduationRequirementByGrIds(any())).thenReturn(1);

        int n = service.deleteRequirementByGrIds(new Long[] {1L});

        assertEquals(1, n);
        verify(supportMapper).deleteByIpIds(any());
        verify(indicatorMapper).deleteByGrIds(eq(new Long[] {1L}));
    }

    @Test
    @DisplayName("deleteRequirementByGrIds：空入参安全返回0")
    void deleteRequirement_emptySafe()
    {
        assertEquals(0, service.deleteRequirementByGrIds(new Long[0]));
        verify(requirementMapper, never()).deleteSamObeGraduationRequirementByGrIds(any());
    }
}
