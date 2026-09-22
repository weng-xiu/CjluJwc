package com.yu.aem.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.yu.aem.domain.AemGradeStatistics;

/**
 * 成绩统计分析Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface AemGradeStatisticsMapper 
{
    public AemGradeStatistics selectAemGradeStatisticsByStatId(Long statId);
    public List<AemGradeStatistics> selectAemGradeStatisticsList(AemGradeStatistics aemGradeStatistics);
    public int insertAemGradeStatistics(AemGradeStatistics aemGradeStatistics);
    public int updateAemGradeStatistics(AemGradeStatistics aemGradeStatistics);
    public int deleteAemGradeStatisticsByStatId(Long statId);
    public int deleteAemGradeStatisticsByStatIds(Long[] statIds);

    /** 按课程和学期查询统计记录 */
    public AemGradeStatistics selectByCourseAndSemester(@Param("courseId") Long courseId, @Param("semesterId") Long semesterId);

    /**
     * SQL聚合查询某课程某学期的成绩统计（避免全量加载到内存）。
     * @return Map 含 totalStudents/maxScore/minScore/avgScore/passCount/excellentCount/failCount
     */
    public Map<String, Object> aggregateGradeByCourse(@Param("courseId") Long courseId, @Param("semesterId") Long semesterId);

    /**
     * 按学期聚合所有课程的成绩统计，返回列表供批量生成报表使用。
     */
    public List<Map<String, Object>> aggregateGradeBySemester(@Param("semesterId") Long semesterId);

    /**
     * 分数段分布（<60, 60-69, 70-79, 80-89, 90-100），用于图表。
     */
    public Map<String, Object> scoreDistribution(@Param("courseId") Long courseId, @Param("semesterId") Long semesterId);

    /**
     * 学期总览：课程数、学生数、平均绩点、通过率等汇总指标。
     */
    public Map<String, Object> semesterOverview(@Param("semesterId") Long semesterId);

    /**
     * 课程成绩排名（按总分降序），支持分页由PageHelper控制。
     */
    public List<Map<String, Object>> courseRanking(@Param("courseId") Long courseId, @Param("semesterId") Long semesterId);

    /** A7：班级维度成绩统计（courseId 可选） */
    public List<Map<String, Object>> statByClass(@Param("semesterId") Long semesterId, @Param("courseId") Long courseId);

    /** A7：教师维度成绩统计（经开课计划归口，近似口径） */
    public List<Map<String, Object>> statByTeacher(@Param("semesterId") Long semesterId);

    /** A7：专业维度成绩统计 */
    public List<Map<String, Object>> statByMajor(@Param("semesterId") Long semesterId);

    /** A7：按学期历史趋势（courseId 可选） */
    public List<Map<String, Object>> gradeTrend(@Param("courseId") Long courseId);
}
