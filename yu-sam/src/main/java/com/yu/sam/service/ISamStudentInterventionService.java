package com.yu.sam.service;

import java.util.List;
import java.util.Map;

import com.yu.sam.domain.SamStudentIntervention;

/**
 * 学生干预闭环记录服务（F4 预警-帮扶-效果回收）。
 *
 * @author F4
 */
public interface ISamStudentInterventionService
{
    /** 干预记录列表（含学期口径效果指标）。 */
    List<SamStudentIntervention> selectInterventionList(SamStudentIntervention query);

    /** 干预记录详情（含学期口径效果指标）。 */
    SamStudentIntervention selectInterventionById(Long interventionId);

    /** 新增干预记录。 */
    int insertIntervention(SamStudentIntervention record);

    /** 修改干预记录。 */
    int updateIntervention(SamStudentIntervention record);

    /** 批量删除干预记录（软删）。 */
    int deleteInterventionByIds(Long[] interventionIds);

    /**
     * 效果预览：按基线学期与效果学期现算 GPA/及格率及变化，供登记前试算。
     *
     * @param studentId 学生ID
     * @param baselineSemesterId 基线学期ID（可空）
     * @param effectSemesterId 效果学期ID（可空）
     * @return 双学期指标与差值
     */
    Map<String, Object> previewEffect(Long studentId, Long baselineSemesterId, Long effectSemesterId);
}
