package com.yu.sam.obe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.sam.domain.SamObeCourseSupport;
import com.yu.sam.domain.SamObeGraduationRequirement;
import com.yu.sam.domain.SamObeIndicatorPoint;
import com.yu.sam.domain.dto.IndicatorAttainmentVo;
import com.yu.sam.domain.dto.ObeAttainmentVo;

/**
 * OBE 达成度计算器纯逻辑单测（N1）。
 *
 * @author N1
 */
class ObeAttainmentCalculatorTest
{
    private final ObeAttainmentCalculator calc = new ObeAttainmentCalculator();

    private SamObeGraduationRequirement gr(long id, double target)
    {
        SamObeGraduationRequirement g = new SamObeGraduationRequirement();
        g.setGrId(id);
        g.setGrCode("GR-0" + id);
        g.setGrName("毕业要求" + id);
        g.setTargetValue(target);
        g.setStatus("0");
        g.setDelFlag("0");
        return g;
    }

    private SamObeIndicatorPoint ip(long id, long grId, String code, double weight, Double target)
    {
        SamObeIndicatorPoint p = new SamObeIndicatorPoint();
        p.setIpId(id);
        p.setGrId(grId);
        p.setIpCode(code);
        p.setIpName("指标点" + code);
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

    @Test
    @DisplayName("weightedAttainment：加权平均并保留4位")
    void weightedAttainment_basic()
    {
        double[] scores = {0.8, 0.6};
        double[] weights = {2.0, 1.0};
        // (0.8*2 + 0.6*1)/3 = 0.7333
        assertEquals(0.7333, ObeAttainmentCalculator.weightedAttainment(scores, weights), 1e-9);
    }

    @Test
    @DisplayName("weightedAttainment：跳过非正权重并钳制越界得分")
    void weightedAttainment_skipsInvalidWeight()
    {
        double[] scores = {0.9, 1.6, 0.5};
        double[] weights = {2.0, 2.0, 0.0};
        // 0.5 权重为0跳过；1.6 钳制为1.0；(0.9*2 + 1.0*2)/4 = 0.95
        assertEquals(0.95, ObeAttainmentCalculator.weightedAttainment(scores, weights), 1e-9);
    }

    @Test
    @DisplayName("weightedAttainment：全零权重返回0")
    void weightedAttainment_allZeroWeights()
    {
        assertEquals(0.0, ObeAttainmentCalculator.weightedAttainment(new double[]{0.5}, new double[]{0.0}), 1e-9);
    }

    @Test
    @DisplayName("computeRequirement：指标点加权聚合毕业要求达成度")
    void computeRequirement_weightedByIndicator()
    {
        SamObeGraduationRequirement g = gr(1L, 0.7);
        List<SamObeIndicatorPoint> ips = new ArrayList<>();
        ips.add(ip(10L, 1L, "1.1", 1.0, 0.7));
        ips.add(ip(11L, 1L, "1.2", 2.0, 0.7));

        List<SamObeCourseSupport> supports = new ArrayList<>();
        supports.add(support(10L, 100L, 2.0));
        supports.add(support(10L, 101L, 1.0));
        supports.add(support(11L, 102L, 1.0));

        Map<Long, Double> scoreMap = new HashMap<>();
        scoreMap.put(100L, 0.8);
        scoreMap.put(101L, 0.6);
        scoreMap.put(102L, 0.5);

        ObeAttainmentVo vo = calc.computeRequirement(g, ips, supports, scoreMap, new HashMap<>(), "cohort");

        // IP1 = (0.8*2+0.6*1)/3 = 0.7333 → 达成
        IndicatorAttainmentVo ip1 = vo.getIndicators().get(0);
        assertEquals(0.7333, ip1.getAttainment(), 1e-9);
        assertEquals("1", ip1.getAchieved());
        // IP2 = 0.5 → 未达成
        IndicatorAttainmentVo ip2 = vo.getIndicators().get(1);
        assertEquals(0.5, ip2.getAttainment(), 1e-9);
        assertEquals("0", ip2.getAchieved());
        // GR = (0.7333*1 + 0.5*2)/3 = 0.5778 → 未达成
        assertEquals(0.5778, vo.getAttainment(), 1e-4);
        assertEquals("0", vo.getAchieved());
        assertEquals("cohort", vo.getScope());
    }

    @Test
    @DisplayName("computeRequirement：未修读支撑课程不计入")
    void computeRequirement_skipsUnscoredCourse()
    {
        SamObeGraduationRequirement g = gr(2L, 0.7);
        List<SamObeIndicatorPoint> ips = List.of(ip(20L, 2L, "2.1", 1.0, 0.7));
        List<SamObeCourseSupport> supports = new ArrayList<>();
        supports.add(support(20L, 200L, 1.0)); // 有成绩
        supports.add(support(20L, 201L, 1.0)); // 无成绩，跳过

        Map<Long, Double> scoreMap = new HashMap<>();
        scoreMap.put(200L, 0.9);

        ObeAttainmentVo vo = calc.computeRequirement(g, ips, supports, scoreMap, new HashMap<>(), "student");
        IndicatorAttainmentVo only = vo.getIndicators().get(0);
        assertEquals(1, only.getCourseCount());
        assertEquals(0.9, only.getAttainment(), 1e-9);
        assertEquals("1", only.getAchieved());
        assertEquals(0.9, vo.getAttainment(), 1e-9);
    }

    @Test
    @DisplayName("computeRequirement：停用指标点被忽略")
    void computeRequirement_ignoresInactiveIndicator()
    {
        SamObeGraduationRequirement g = gr(3L, 0.7);
        SamObeIndicatorPoint active = ip(30L, 3L, "3.1", 1.0, 0.7);
        SamObeIndicatorPoint disabled = ip(31L, 3L, "3.2", 5.0, 0.7);
        disabled.setStatus("1");
        List<SamObeIndicatorPoint> ips = new ArrayList<>();
        ips.add(active);
        ips.add(disabled);

        List<SamObeCourseSupport> supports = new ArrayList<>();
        supports.add(support(30L, 300L, 1.0));
        supports.add(support(31L, 301L, 1.0));
        Map<Long, Double> scoreMap = new HashMap<>();
        scoreMap.put(300L, 0.8);
        scoreMap.put(301L, 0.2);

        ObeAttainmentVo vo = calc.computeRequirement(g, ips, supports, scoreMap, new HashMap<>(), "cohort");
        assertEquals(1, vo.getIndicators().size());
        assertEquals(0.8, vo.getAttainment(), 1e-9);
    }

    @Test
    @DisplayName("computeRequirement：目标值按百分制填写时自动归一")
    void computeRequirement_percentTarget()
    {
        SamObeGraduationRequirement g = gr(4L, 70.0); // 70 分口径 → 0.7
        List<SamObeIndicatorPoint> ips = List.of(ip(40L, 4L, "4.1", 1.0, null)); // 指标点目标缺省→沿用GR
        List<SamObeCourseSupport> supports = List.of(support(40L, 400L, 1.0));
        Map<Long, Double> scoreMap = new HashMap<>();
        scoreMap.put(400L, 0.65);

        ObeAttainmentVo vo = calc.computeRequirement(g, ips, supports, scoreMap, new HashMap<>(), "cohort");
        assertEquals(0.7, vo.getTargetValue(), 1e-9);
        assertEquals(0.7, vo.getIndicators().get(0).getTargetValue(), 1e-9);
        assertEquals("0", vo.getAchieved()); // 0.65 < 0.7
    }

    @Test
    @DisplayName("无毕业要求：返回空明细，达成度0")
    void computeRequirement_noIndicator()
    {
        SamObeGraduationRequirement g = gr(5L, 0.7);
        ObeAttainmentVo vo = calc.computeRequirement(g, new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new HashMap<>(), "cohort");
        assertEquals(0.0, vo.getAttainment(), 1e-9);
        assertEquals("0", vo.getAchieved());
        assertEquals(0, vo.getIndicators().size());
    }
}
