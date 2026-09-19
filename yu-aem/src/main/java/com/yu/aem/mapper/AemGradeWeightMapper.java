package com.yu.aem.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.yu.aem.domain.AemGradeWeight;

/**
 * 成绩权重配置Mapper接口
 *
 * @author ruoyi
 * @date 2026-09-19
 */
public interface AemGradeWeightMapper
{
    public AemGradeWeight selectAemGradeWeightByWeightId(Long weightId);

    public List<AemGradeWeight> selectAemGradeWeightList(AemGradeWeight aemGradeWeight);

    public int insertAemGradeWeight(AemGradeWeight aemGradeWeight);

    public int updateAemGradeWeight(AemGradeWeight aemGradeWeight);

    public int deleteAemGradeWeightByWeightId(Long weightId);

    public int deleteAemGradeWeightByWeightIds(Long[] weightIds);

    /**
     * A4：按课程解析生效权重（匹配优先级：课程级 > 类别级 > 全局默认）
     *
     * @param courseId 课程ID
     * @return regularRatio/examRatio，无配置时为 null
     */
    public Map<String, Object> selectEffectiveWeightByCourseId(@Param("courseId") Long courseId);
}
