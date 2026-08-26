package com.yu.aem.service;

import java.util.List;
import java.util.Map;
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
     */
    public AemGradeStatistics aggregateByCourse(Long courseId, Long semesterId);

    /**
     * 按学期批量聚合所有课程的成绩统计
     * @return 聚合的课程数
     */
    public int aggregateBySemester(Long semesterId);

    /**
     * 分数段分布（供图表）
     */
    public Map<String, Object> scoreDistribution(Long courseId, Long semesterId);

    /**
     * 学期成绩总览（课程数、人次、平均分、平均绩点、通过率）
     */
    public Map<String, Object> semesterOverview(Long semesterId);

    /**
     * 课程成绩排名列表
     */
    public List<Map<String, Object>> courseRanking(Long courseId, Long semesterId);
}
