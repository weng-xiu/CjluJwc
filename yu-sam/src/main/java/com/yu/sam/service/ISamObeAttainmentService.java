package com.yu.sam.service;

import java.util.List;
import java.util.Map;

import com.yu.sam.domain.SamObeCourseSupport;
import com.yu.sam.domain.SamObeGraduationRequirement;
import com.yu.sam.domain.SamObeIndicatorPoint;
import com.yu.sam.domain.dto.ObeAttainmentVo;

/**
 * N1 OBE 毕业要求达成度服务：配置维护 + 达成度计算。
 *
 * @author N1
 */
public interface ISamObeAttainmentService
{
    // ---------- 毕业要求 ----------
    List<SamObeGraduationRequirement> selectRequirementList(SamObeGraduationRequirement query);

    SamObeGraduationRequirement selectRequirementByGrId(Long grId);

    int insertRequirement(SamObeGraduationRequirement record);

    int updateRequirement(SamObeGraduationRequirement record);

    int deleteRequirementByGrIds(Long[] grIds);

    // ---------- 指标点 ----------
    List<SamObeIndicatorPoint> selectIndicatorList(SamObeIndicatorPoint query);

    SamObeIndicatorPoint selectIndicatorByIpId(Long ipId);

    int insertIndicator(SamObeIndicatorPoint record);

    int updateIndicator(SamObeIndicatorPoint record);

    int deleteIndicatorByIpIds(Long[] ipIds);

    // ---------- 课程支撑矩阵 ----------
    List<SamObeCourseSupport> selectSupportList(SamObeCourseSupport query);

    SamObeCourseSupport selectSupportBySupportId(Long supportId);

    int insertSupport(SamObeCourseSupport record);

    int updateSupport(SamObeCourseSupport record);

    int deleteSupportBySupportIds(Long[] supportIds);

    // ---------- 达成度计算 ----------
    /** 个体口径：某学生在某培养方案下各毕业要求达成度。 */
    List<ObeAttainmentVo> calculateByStudent(Long planId, Long studentId);

    /** 群体口径：某培养方案下各毕业要求达成度（基于课程成绩均值）。semesterId 可空。 */
    List<ObeAttainmentVo> calculateCohort(Long planId, Long semesterId);

    /** 达成度看板：群体口径 + 汇总统计。 */
    Map<String, Object> dashboard(Long planId, Long semesterId);
}
