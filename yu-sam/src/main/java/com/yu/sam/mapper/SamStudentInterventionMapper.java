package com.yu.sam.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.yu.sam.domain.SamStudentIntervention;

/**
 * 学生干预闭环记录Mapper（F4 预警-帮扶-效果回收）。
 *
 * @author F4
 */
public interface SamStudentInterventionMapper
{
    /** 干预记录列表（联查学号/姓名/学期名称）。 */
    List<SamStudentIntervention> selectInterventionList(SamStudentIntervention query);

    /** 按ID查询干预记录。 */
    SamStudentIntervention selectInterventionById(Long interventionId);

    /** 新增干预记录。 */
    int insertIntervention(SamStudentIntervention record);

    /** 修改干预记录。 */
    int updateIntervention(SamStudentIntervention record);

    /** 批量软删干预记录。 */
    int deleteInterventionByIds(Long[] interventionIds);

    /**
     * 按"学生+学期"口径现算学业指标（GPA/及格率/修读门数），
     * 供干预前后效果对比使用。
     */
    Map<String, Object> selectSemesterMetrics(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId);
}
