package com.yu.oa.mapper;

import java.util.List;
import com.yu.oa.domain.OaSchedule;

/**
 * 日程安排Mapper接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface OaScheduleMapper 
{
    public OaSchedule selectOaScheduleByScheduleId(Long scheduleId);
    public List<OaSchedule> selectOaScheduleList(OaSchedule oaSchedule);
    public int insertOaSchedule(OaSchedule oaSchedule);
    public int updateOaSchedule(OaSchedule oaSchedule);
    public int deleteOaScheduleByScheduleId(Long scheduleId);
    public int deleteOaScheduleByScheduleIds(Long[] scheduleIds);
}
