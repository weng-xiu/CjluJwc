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
}
