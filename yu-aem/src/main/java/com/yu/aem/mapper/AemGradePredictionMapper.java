package com.yu.aem.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 成绩与学业预测Mapper接口（F2-3 智能算法深化）。
 *
 * <p>只读聚合查询，不写任何业务表；基于 aem_grade_record 历史成绩派生
 * 学生逐学期绩点序列与课程难度画像，供预测算法与选课推荐反哺使用。</p>
 *
 * @author ruoyi
 */
public interface AemGradePredictionMapper
{
    /**
     * 学生逐学期成绩序列（按学期升序）：每学期平均成绩、平均绩点、已获学分、修读门数、不及格门数。
     * 用于线性趋势拟合与风险预测。studentId 必填。
     */
    public List<Map<String, Object>> selectStudentSemesterSeries(@Param("studentId") Long studentId);

    /**
     * 学生基本信息（学号、姓名、班级、专业、年级），供预测报告头展示。
     */
    public Map<String, Object> selectStudentBase(@Param("studentId") Long studentId);

    /**
     * 课程难度画像（semesterId 可选，不传为全量口径）：每门课程的修读人次、
     * 平均分、平均绩点、通过率、优秀率、不及格人次。难度指数在 Service 层可解释计算。
     */
    public List<Map<String, Object>> selectCourseDifficulty(@Param("semesterId") Long semesterId);

    /**
     * 班级风险看板基础数据（指定学期内有成绩的学生聚合）：学生、班级、
     * 本学期平均分、平均绩点、不及格门数。用于批量风险分级与排序。
     */
    public List<Map<String, Object>> selectStudentRiskBase(@Param("semesterId") Long semesterId,
                                                           @Param("classId") Long classId);

    /**
     * 单学生全学期累计概览：平均绩点、不及格门数、已获学分、修读学期数。供无历史序列时兜底分级。
     */
    public Map<String, Object> selectStudentOverall(@Param("studentId") Long studentId);
}
