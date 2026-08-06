package com.yu.aem.service;

import java.util.List;
import com.yu.aem.domain.AemGradeStatistics;

/**
 * 成绩统计分析Service接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface IAemGradeStatisticsService 
{
    public AemGradeStatistics selectAemGradeStatisticsByStatId(Long statId);
    public List<AemGradeStatistics> selectAemGradeStatisticsList(AemGradeStatistics aemGradeStatistics);
    public int insertAemGradeStatistics(AemGradeStatistics aemGradeStatistics);
    public int deleteAemGradeStatisticsByStatIds(Long[] statIds);
    public int deleteAemGradeStatisticsByStatId(Long statId);

    /**
     * 按课程聚合成绩统计
     * 计算最高分、最低分、平均分、通过率、优秀率
     *
     * @param courseId    课程ID
     * @param semesterId  学期ID
     * @return 统计结果
     */
    public AemGradeStatistics aggregateByCourse(Long courseId, Long semesterId);
}
