package com.yu.sam.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

/**
 * N6 学生数字档案全息画像只读聚合Mapper（跨模块只读，JOIN sam/aem/tpm/brm）。
 * 仅做查询聚合，不写库。
 *
 * @author N6
 */
public interface SamStudentProfileMapper
{
    /** 学籍基础信息（含院系/专业/班级名称）。 */
    Map<String, Object> selectStudentBase(@Param("studentId") Long studentId);

    /** 学业汇总：修读门数/已获学分/应修学分/GPA/加权均分/不及格门数。 */
    Map<String, Object> selectAcademicSummary(@Param("studentId") Long studentId);

    /** 所属培养方案信息（按专业+入学年级匹配最新已发布方案）。 */
    Map<String, Object> selectPlanInfo(@Param("studentId") Long studentId);

    /** 培养方案学分结构分项。 */
    List<Map<String, Object>> selectCreditSections(@Param("planId") Long planId);

    /** 学业预警明细。 */
    List<Map<String, Object>> selectWarnings(@Param("studentId") Long studentId);

    /** 毕业/学位审核结论。 */
    List<Map<String, Object>> selectReviewRecords(@Param("studentId") Long studentId);

    /** 毕业论文（设计）档案。 */
    Map<String, Object> selectThesis(@Param("studentId") Long studentId);

    /** 近期成绩明细。 */
    List<Map<String, Object>> selectRecentGrades(@Param("studentId") Long studentId, @Param("limit") int limit);

    /** 按课程属性分类的已获学分（用于学分结构达成对比）。 */
    List<Map<String, Object>> selectEarnedByCategory(@Param("studentId") Long studentId);
}
