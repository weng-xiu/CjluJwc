package com.yu.web.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 主题分析统计Mapper（P3，只读跨模块聚合）
 *
 * 覆盖三大主题：学生结构、成绩分析、师资分析，另含师生数据上报导出。
 * 全部来自真实业务表（sam_student / aem_grade_record / brm_teacher /
 * tpm_course_offering / brm_department），不做任何 mock。
 *
 * @author ruoyi
 * @date 2026-09-25
 */
public interface SubjectStatMapper
{
    /** 当前学期（覆盖今天的学期，无则取最新启用学期） */
    public Map<String, Object> selectCurrentSemester();

    // ========== 学生结构主题 ==========

    /** 学生总览：学籍总数 + 各学籍状态人数 */
    public Map<String, Object> selectStudentOverview();

    /** 在读学生按院系分布 */
    public List<Map<String, Object>> selectStudentByDept();

    /** 在读学生按入学年份分布 */
    public List<Map<String, Object>> selectStudentByEnrollmentYear();

    /** 在读学生按性别分布 */
    public List<Map<String, Object>> selectStudentByGender();

    /** 在读学生按培养层次分布 */
    public List<Map<String, Object>> selectStudentByEducationLevel();

    // ========== 成绩分析主题 ==========

    /** 成绩总览：记录数 + 平均分 + 平均绩点 + 及格率 + 优秀率 */
    public Map<String, Object> selectGradeOverview(@Param("semesterId") Long semesterId);

    /** 总评分数段分布（优/良/中/及格/不及格） */
    public List<Map<String, Object>> selectGradeScoreBand(@Param("semesterId") Long semesterId);

    /** 按院系的成绩聚合（记录数/平均分/及格率） */
    public List<Map<String, Object>> selectGradeByDept(@Param("semesterId") Long semesterId);

    /** 按学期的成绩趋势（平均分/及格率） */
    public List<Map<String, Object>> selectGradeTrend();

    // ========== 师资分析主题 ==========

    /** 教师总览：在职教师数 + 开课门次 + 班级数 */
    public Map<String, Object> selectTeacherOverview(@Param("semesterId") Long semesterId);

    /** 在职教师按院系分布 */
    public List<Map<String, Object>> selectTeacherByDept();

    /** 在职教师按职称分布 */
    public List<Map<String, Object>> selectTeacherByTitle();

    /** 在职教师按学历分布 */
    public List<Map<String, Object>> selectTeacherByEducation();

    /** 在职教师按性别分布 */
    public List<Map<String, Object>> selectTeacherByGender();

    /** 教师授课工作量 TOP N（开课门次/班级数/选课容量合计） */
    public List<Map<String, Object>> selectTeacherWorkloadTop(@Param("semesterId") Long semesterId, @Param("limit") int limit);

    // ========== 师生数据上报导出 ==========

    /** 分院系师生数据报表（学籍数/在读数/教师数/开课门次/平均及格率） */
    public List<Map<String, Object>> selectReportByDept(@Param("semesterId") Long semesterId);
}
