package com.yu.sam.mapper;

import java.util.List;
import com.yu.sam.domain.SamObeCourseSupport;

/**
 * OBE课程支撑矩阵Mapper接口
 *
 * @author N1
 */
public interface SamObeCourseSupportMapper
{
    public SamObeCourseSupport selectSamObeCourseSupportBySupportId(Long supportId);

    public List<SamObeCourseSupport> selectSamObeCourseSupportList(SamObeCourseSupport query);

    public List<SamObeCourseSupport> selectByIpId(Long ipId);

    public List<SamObeCourseSupport> selectByPlanId(Long planId);

    public int insertSamObeCourseSupport(SamObeCourseSupport record);

    public int updateSamObeCourseSupport(SamObeCourseSupport record);

    public int deleteSamObeCourseSupportBySupportIds(Long[] supportIds);

    public int deleteByIpIds(Long[] ipIds);
}
