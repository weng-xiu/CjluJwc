package com.yu.oa.mapper;

import java.util.List;
import com.yu.oa.domain.OaMeetingMinutes;

/**
 * 会议纪要Mapper接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface OaMeetingMinutesMapper 
{
    public OaMeetingMinutes selectOaMeetingMinutesByMinutesId(Long minutesId);
    public OaMeetingMinutes selectOaMeetingMinutesByMeetingId(Long meetingId);
    public List<OaMeetingMinutes> selectOaMeetingMinutesList(OaMeetingMinutes oaMeetingMinutes);
    public int insertOaMeetingMinutes(OaMeetingMinutes oaMeetingMinutes);
    public int updateOaMeetingMinutes(OaMeetingMinutes oaMeetingMinutes);
    public int deleteOaMeetingMinutesByMeetingId(Long meetingId);
    public int deleteOaMeetingMinutesByMinutesIds(Long[] minutesIds);
}
