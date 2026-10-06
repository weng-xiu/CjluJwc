package com.yu.sam.mapper;

import java.util.List;
import com.yu.sam.domain.SamObeGraduationRequirement;

/**
 * OBE毕业要求Mapper接口
 *
 * @author N1
 */
public interface SamObeGraduationRequirementMapper
{
    public SamObeGraduationRequirement selectSamObeGraduationRequirementByGrId(Long grId);

    public List<SamObeGraduationRequirement> selectSamObeGraduationRequirementList(SamObeGraduationRequirement query);

    public List<SamObeGraduationRequirement> selectByPlanId(Long planId);

    public int insertSamObeGraduationRequirement(SamObeGraduationRequirement record);

    public int updateSamObeGraduationRequirement(SamObeGraduationRequirement record);

    public int deleteSamObeGraduationRequirementByGrIds(Long[] grIds);
}
