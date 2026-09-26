package com.yu.portal.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 门户 AI 分析聚合数据层（Phase34 AI应用试点）
 *
 * 选课推荐与学业画像需要跨 TPM/AEM/SAM/BRM 取数，沿用项目内「只读聚合 + HashMap 直出」的分层口径，
 * 不改动各业务模块的领域模型；因项目未开启驼峰自动映射，SQL 一律显式 as 别名。
 * 所有统计均来自业务表实数，缺失的数据返回空集，由上层如实标注「无数据、不参与评分」。
 *
 * @author yu
 * @date 2026-09-26
 */
public interface PortalAiMapper
{
    /**
     * 学生基本档案（含专业、班级、院系名称）
     *
     * @param studentId 学生ID（门户口径等于 user_id）
     * @return 档案
     */
    public Map<String, Object> selectStudentProfile(@Param("studentId") Long studentId);

    /**
     * 学生所属专业已发布培养方案（多个时取方案年份最新）
     *
     * @param majorId 专业ID
     * @return 方案（planId/planName/totalCredits/planYear）
     */
    public Map<String, Object> selectPublishedPlan(@Param("majorId") Long majorId);

    /**
     * 按课程ID批量取课程库信息（学分、类别、建议学期、所属方案）
     *
     * @param courseIds 课程ID集合
     * @return 课程信息
     */
    public List<Map<String, Object>> selectCourseInfoByIds(@Param("courseIds") List<Long> courseIds);

    /**
     * 培养方案各学分模块的要求学分与课程门数
     *
     * @param planId 方案ID
     * @return 模块要求
     */
    public List<Map<String, Object>> selectPlanModuleRequire(@Param("planId") Long planId);

    /**
     * 学生已通过成绩按课程类别获得的学分
     *
     * @param studentId 学生ID
     * @return 类别获得学分
     */
    public List<Map<String, Object>> selectStudentEarnedByCategory(@Param("studentId") Long studentId);

    /**
     * 学生成绩总览（门数、均分、绩点、不及格门数、已获学分）
     *
     * @param studentId 学生ID
     * @return 总览
     */
    public Map<String, Object> selectStudentGradeSummary(@Param("studentId") Long studentId);

    /**
     * 同专业同年级学生的成绩基准
     *
     * @param majorId 专业ID
     * @param enrollmentYear 入学年份
     * @return 基准（人数、均分、均绩点）
     */
    public Map<String, Object> selectPeerGradeSummary(@Param("majorId") Long majorId,
            @Param("enrollmentYear") Integer enrollmentYear);

    /**
     * 学生最近成绩明细（含课程名、学期名）
     *
     * @param studentId 学生ID
     * @param limit 条数
     * @return 成绩明细
     */
    public List<Map<String, Object>> selectRecentGrades(@Param("studentId") Long studentId, @Param("limit") int limit);

    /**
     * 学生各类别课程的成绩表现
     *
     * @param studentId 学生ID
     * @return 类别表现
     */
    public List<Map<String, Object>> selectCategoryScoreStat(@Param("studentId") Long studentId);

    /**
     * 学生学业预警概况
     *
     * @param studentId 学生ID
     * @return 概况（总数、未解决、最高等级）
     */
    public Map<String, Object> selectStudentWarningSummary(@Param("studentId") Long studentId);

    /**
     * 学生学业预警明细
     *
     * @param studentId 学生ID
     * @return 明细
     */
    public List<Map<String, Object>> selectStudentWarnings(@Param("studentId") Long studentId);

    /**
     * 学生已修读学期数与最近学期
     *
     * @param studentId 学生ID
     * @return 进度概况
     */
    public Map<String, Object> selectStudentSemesterProgress(@Param("studentId") Long studentId);

    /**
     * 候选课程的评教得分（按课程聚合）
     *
     * @param courseIds 课程ID集合
     * @return 课程评教均分与人数
     */
    public List<Map<String, Object>> selectCourseEvalStat(@Param("courseIds") List<Long> courseIds);

    /**
     * 全校评教均分基准
     *
     * @return 均分
     */
    public Map<String, Object> selectEvalGlobalAvg();
}
