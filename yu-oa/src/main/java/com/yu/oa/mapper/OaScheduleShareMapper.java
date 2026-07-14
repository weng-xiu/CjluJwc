package com.yu.oa.mapper;

import java.util.List;
import com.yu.oa.domain.OaScheduleShare;

/**
 * 日程共享人员Mapper接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface OaScheduleShareMapper 
{
    public OaScheduleShare selectOaScheduleShareByShareId(Long shareId);
    public List<OaScheduleShare> selectOaScheduleShareByScheduleId(Long scheduleId);
    public int insertOaScheduleShare(OaScheduleShare oaScheduleShare);
    public int updateOaScheduleShare(OaScheduleShare oaScheduleShare);
    public int deleteOaScheduleShareByScheduleId(Long scheduleId);
    public int deleteOaScheduleShareByShareIds(Long[] shareIds);
}
