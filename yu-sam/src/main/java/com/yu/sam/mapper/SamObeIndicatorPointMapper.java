package com.yu.sam.mapper;

import java.util.List;
import com.yu.sam.domain.SamObeIndicatorPoint;

/**
 * OBE指标点Mapper接口
 *
 * @author N1
 */
public interface SamObeIndicatorPointMapper
{
    public SamObeIndicatorPoint selectSamObeIndicatorPointByIpId(Long ipId);

    public List<SamObeIndicatorPoint> selectSamObeIndicatorPointList(SamObeIndicatorPoint query);

    public List<SamObeIndicatorPoint> selectByGrId(Long grId);

    public List<SamObeIndicatorPoint> selectByPlanId(Long planId);

    public int insertSamObeIndicatorPoint(SamObeIndicatorPoint record);

    public int updateSamObeIndicatorPoint(SamObeIndicatorPoint record);

    public int deleteSamObeIndicatorPointByIpIds(Long[] ipIds);

    public int deleteByGrIds(Long[] grIds);
}
