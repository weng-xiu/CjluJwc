package com.yu.sam.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 预警数据辅助查询Mapper（跨表查询aem/tpm相关数据）
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface SamWarningDataMapper 
{
    /**
     * 计算学生某学期GPA（直接查aem_grade_record表）
     * 采用中国标准算法：绩点=(分数/10)-5，60分以下为0
     * 
     * @param studentId 学生ID
     * @param semesterId 学期ID
     * @return GPA值
     */
    Double selectStudentGpa(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId);

    /**
     * 统计学生已获得学分（已通过课程的学分总和）
     * 
     * @param studentId 学生ID
     * @param semesterId 学期ID
     * @return 已获得学分
     */
    Double selectStudentEarnedCredits(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId);

    /**
     * 查询学生应修总学分（该学期所有课程的学分总和）
     * 
     * @param studentId 学生ID
     * @param semesterId 学期ID
     * @return 应修总学分
     */
    Double selectStudentRequiredCredits(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId);

    /**
     * 查询所有学生ID（分页）
     * 
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 学生ID列表
     */
    List<Long> selectAllStudentIds(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 查询学生总数
     * 
     * @return 学生总数
     */
    int selectStudentCount();

    /**
     * 查询学生不及格课程数（作为出勤预警的参考指标）
     * 多门课程不及格可能暗示出勤问题
     * 
     * @param studentId 学生ID
     * @param semesterId 学期ID（为null时统计全部学期）
     * @return 不及格课程数
     */
    Integer selectStudentFailCourseCount(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId);

    /**
     * 按课程分类统计学生未通过门数。
     *
     * @param studentId  学生ID
     * @param semesterId 学期ID（可为null）
     * @param category   课程分类（如"通识"，对应 tpm_course_library.course_category；为空时统计全部）
     * @return 未通过门数
     */
    Integer countFailByCourseCategory(@Param("studentId") Long studentId,
                                     @Param("semesterId") Long semesterId,
                                     @Param("category") String category);

    /**
     * 按课程名称关键字统计学生未通过门数（用于英语、体育等课程）。
     *
     * @param studentId  学生ID
     * @param semesterId 学期ID（可为null）
     * @param keyword    课程名模糊匹配关键字
     * @return 未通过门数
     */
    Integer countFailByCourseNameKeyword(@Param("studentId") Long studentId,
                                         @Param("semesterId") Long semesterId,
                                         @Param("keyword") String keyword);

    /**
     * 统计学生学位课程（必修课）不及格门数。
     *
     * @param studentId  学生ID
     * @param semesterId 学期ID（可为null）
     * @return 必修课未通过门数
     */
    Integer countDegreeCourseFail(@Param("studentId") Long studentId,
                                  @Param("semesterId") Long semesterId);

    /**
     * 查询学生所属培养方案ID（按专业 + 入学年级匹配已发布方案）。
     *
     * @param studentId 学生ID
     * @return 培养方案ID，无匹配时返回 null
     */
    Long selectPlanIdByStudent(@Param("studentId") Long studentId);

    /**
     * 查询培养方案规定应修总学分（优先取方案总学分，为空则汇总学分结构要求学分）。
     *
     * @param planId 培养方案ID
     * @return 应修总学分
     */
    Double selectPlanRequiredCredits(@Param("planId") Long planId);

    /**
     * 查询培养方案学分结构分项（通识/专业/选修/实践等）。
     *
     * @param planId 培养方案ID
     * @return 每个分项含 creditType/creditTypeName/requiredCredit
     */
    List<java.util.Map<String, Object>> selectPlanCreditSections(@Param("planId") Long planId);

    /**
     * 按课程属性分类统计学生已获得学分（course_category 匹配）。
     *
     * @param studentId  学生ID
     * @param semesterId 学期ID（可为null）
     * @param category   课程属性分类
     * @return 已获学分
     */
    Double sumEarnedCreditByCourseCategory(@Param("studentId") Long studentId,
                                           @Param("semesterId") Long semesterId,
                                           @Param("category") String category);

    /**
     * 按课程属性分类统计学生已修读课程门数（用于判断该分类是否已完成数据治理）。
     *
     * @param studentId  学生ID
     * @param semesterId 学期ID（可为null）
     * @param category   课程属性分类
     * @return 修读门数
     */
    Integer countCoursesByCourseCategory(@Param("studentId") Long studentId,
                                         @Param("semesterId") Long semesterId,
                                         @Param("category") String category);

    /**
     * 按课程名称关键字统计学生已修读课程门数（用于属性缺失时的降级判定）。
     *
     * @param studentId  学生ID
     * @param semesterId 学期ID（可为null）
     * @param keyword    课程名模糊匹配关键字
     * @return 修读门数
     */
    Integer countCoursesByNameKeyword(@Param("studentId") Long studentId,
                                      @Param("semesterId") Long semesterId,
                                      @Param("keyword") String keyword);
}
