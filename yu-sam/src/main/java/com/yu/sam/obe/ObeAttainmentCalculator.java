package com.yu.sam.obe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.yu.sam.domain.SamObeCourseSupport;
import com.yu.sam.domain.SamObeGraduationRequirement;
import com.yu.sam.domain.SamObeIndicatorPoint;
import com.yu.sam.domain.dto.IndicatorAttainmentVo;
import com.yu.sam.domain.dto.IndicatorAttainmentVo.SupportCourseVo;
import com.yu.sam.domain.dto.ObeAttainmentVo;

/**
 * OBE 达成度计算器（纯逻辑，无 DB 依赖，便于单测）。
 *
 * <p>计算口径：
 * <ul>
 *   <li>课程归一化得分 = 成绩/100（已在数据源归一，取值 [0,1]，越界钳制）；</li>
 *   <li>指标点达成度 = Σ(支撑课程归一化得分 × 支撑权重) / Σ(支撑权重)，仅计入有成绩的支撑课程；</li>
 *   <li>毕业要求达成度 = Σ(指标点达成度 × 指标点权重) / Σ(指标点权重)；</li>
 *   <li>达成判定：达成度 ≥ 目标值 记为达成("1")，否则未达成("0")。</li>
 * </ul>
 *
 * @author N1
 */
public class ObeAttainmentCalculator
{
    /** 目标值缺省（未配置时按 0.70 判定）。 */
    public static final double DEFAULT_TARGET = 0.70;

    /** 权重缺省（未配置或非法时按 1）。 */
    public static final double DEFAULT_WEIGHT = 1.0;

    /**
     * 加权达成度：Σ(value×weight)/Σ(weight)，跳过非正权重；无有效项返回 0，结果钳制 [0,1] 并保留4位。
     */
    public static double weightedAttainment(double[] values, double[] weights)
    {
        if (values == null || weights == null)
        {
            return 0.0;
        }
        double sum = 0.0;
        double wsum = 0.0;
        int n = Math.min(values.length, weights.length);
        for (int i = 0; i < n; i++)
        {
            double w = weights[i];
            if (w <= 0 || Double.isNaN(w))
            {
                continue;
            }
            double v = clamp01(values[i]);
            sum += v * w;
            wsum += w;
        }
        if (wsum <= 0)
        {
            return 0.0;
        }
        return round4(clamp01(sum / wsum));
    }

    /**
     * 计算单个毕业要求的达成度。
     *
     * @param gr            毕业要求（含目标值）
     * @param ips           该毕业要求下的指标点
     * @param supports      课程支撑关系（可含其它指标点的，内部按 ipId 过滤）
     * @param scoreByCourse 课程ID → 归一化得分（个体成绩或群体均分）
     * @param courseNameById 课程ID → 课程名称（明细展示，可为 null）
     * @param scope         统计口径：student / cohort
     * @return 毕业要求达成度视图
     */
    public ObeAttainmentVo computeRequirement(SamObeGraduationRequirement gr,
                                              List<SamObeIndicatorPoint> ips,
                                              List<SamObeCourseSupport> supports,
                                              Map<Long, Double> scoreByCourse,
                                              Map<Long, String> courseNameById,
                                              String scope)
    {
        ObeAttainmentVo vo = new ObeAttainmentVo();
        vo.setGrId(gr.getGrId());
        vo.setGrCode(gr.getGrCode());
        vo.setGrName(gr.getGrName());
        double grTarget = normalizeTarget(gr.getTargetValue());
        vo.setTargetValue(grTarget);
        vo.setScope(scope);

        double grTarget2 = grTarget;
        List<IndicatorAttainmentVo> indicatorVos = new ArrayList<>();
        double[] ipAttainments = new double[0];
        double[] ipWeights = new double[0];
        List<Double> attainList = new ArrayList<>();
        List<Double> weightList = new ArrayList<>();

        if (ips != null && !ips.isEmpty())
        {
            for (SamObeIndicatorPoint ip : ips)
            {
                if (!isActive(ip.getDelFlag(), ip.getStatus()))
                {
                    continue;
                }
                IndicatorAttainmentVo iv = computeIndicator(ip, supports, scoreByCourse, courseNameById, grTarget2);
                indicatorVos.add(iv);
                attainList.add(iv.getAttainment());
                weightList.add(iv.getWeight());
            }
        }

        ipAttainments = attainList.stream().mapToDouble(Double::doubleValue).toArray();
        ipWeights = weightList.stream().mapToDouble(Double::doubleValue).toArray();
        double grAttainment = weightedAttainment(ipAttainments, ipWeights);
        vo.setIndicators(indicatorVos);
        vo.setAttainment(grAttainment);
        vo.setAchieved(grAttainment >= grTarget ? "1" : "0");
        return vo;
    }

    /** 计算单个指标点达成度。 */
    private IndicatorAttainmentVo computeIndicator(SamObeIndicatorPoint ip,
                                                   List<SamObeCourseSupport> supports,
                                                   Map<Long, Double> scoreByCourse,
                                                   Map<Long, String> courseNameById,
                                                   double grTarget)
    {
        IndicatorAttainmentVo iv = new IndicatorAttainmentVo();
        iv.setIpId(ip.getIpId());
        iv.setIpCode(ip.getIpCode());
        iv.setIpName(ip.getIpName());
        double ipWeight = normalizeWeight(ip.getWeight());
        iv.setWeight(ipWeight);
        double ipTarget = ip.getTargetValue() == null ? grTarget : normalizeTarget(ip.getTargetValue());
        iv.setTargetValue(ipTarget);

        List<Double> scoreArr = new ArrayList<>();
        List<Double> weightArr = new ArrayList<>();
        List<SupportCourseVo> courseVos = new ArrayList<>();
        if (supports != null)
        {
            for (SamObeCourseSupport s : supports)
            {
                if (!isActive(s.getDelFlag(), s.getStatus()))
                {
                    continue;
                }
                if (s.getIpId() == null || !s.getIpId().equals(ip.getIpId()))
                {
                    continue;
                }
                Double score = scoreByCourse == null ? null : scoreByCourse.get(s.getCourseId());
                if (score == null)
                {
                    // 未修读该支撑课程，不计入本次达成度评价
                    continue;
                }
                double sw = normalizeWeight(s.getSupportWeight());
                scoreArr.add(clamp01(score));
                weightArr.add(sw);
                SupportCourseVo cv = new SupportCourseVo();
                cv.setCourseId(s.getCourseId());
                cv.setCourseName(courseNameById == null ? null : courseNameById.get(s.getCourseId()));
                cv.setSupportWeight(sw);
                cv.setScore(round4(clamp01(score)));
                courseVos.add(cv);
            }
        }

        double[] sa = scoreArr.stream().mapToDouble(Double::doubleValue).toArray();
        double[] wa = weightArr.stream().mapToDouble(Double::doubleValue).toArray();
        double attainment = weightedAttainment(sa, wa);
        iv.setCourses(courseVos);
        iv.setCourseCount(courseVos.size());
        iv.setAttainment(attainment);
        iv.setAchieved(attainment >= ipTarget ? "1" : "0");
        return iv;
    }

    /** 钳制到 [0,1]。 */
    public static double clamp01(double v)
    {
        if (Double.isNaN(v) || v < 0) return 0.0;
        if (v > 1) return 1.0;
        return v;
    }

    /** 保留4位小数。 */
    public static double round4(double v)
    {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private static double normalizeTarget(Double t)
    {
        if (t == null) return DEFAULT_TARGET;
        double v = t > 1.0 ? t / 100.0 : t; // 兼容按百分制填写的目标值
        return clamp01(v);
    }

    private static double normalizeWeight(Double w)
    {
        if (w == null || w <= 0 || Double.isNaN(w)) return DEFAULT_WEIGHT;
        return w;
    }

    private static boolean isActive(String delFlag, String status)
    {
        if (delFlag != null && "2".equals(delFlag)) return false;
        return status == null || "0".equals(status);
    }
}
