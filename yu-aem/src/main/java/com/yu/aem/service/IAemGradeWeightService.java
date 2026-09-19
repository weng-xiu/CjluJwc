package com.yu.aem.service;

import java.util.List;
import com.yu.aem.domain.AemGradeWeight;

/**
 * 成绩权重配置Service接口（A4）
 *
 * @author ruoyi
 * @date 2026-09-19
 */
public interface IAemGradeWeightService
{
    public AemGradeWeight selectAemGradeWeightByWeightId(Long weightId);

    public List<AemGradeWeight> selectAemGradeWeightList(AemGradeWeight aemGradeWeight);

    public int insertAemGradeWeight(AemGradeWeight aemGradeWeight);

    public int updateAemGradeWeight(AemGradeWeight aemGradeWeight);

    public int deleteAemGradeWeightByWeightIds(Long[] weightIds);

    /**
     * A4：解析课程生效权重（课程级 > 类别级 > 全局默认，均无配置时返回系统默认 30/70）
     *
     * @param courseId 课程ID
     * @return 长度为2的数组：[平时占比小数, 考试占比小数]，如 [0.3, 0.7]
     */
    public double[] resolveRatios(Long courseId);
}
