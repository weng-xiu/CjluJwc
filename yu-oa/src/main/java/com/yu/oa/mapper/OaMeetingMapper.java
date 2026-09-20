package com.yu.oa.mapper;

import java.util.List;
import java.util.Map;
import com.yu.oa.domain.OaMeeting;

/**
 * 会议管理Mapper接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface OaMeetingMapper 
{
    public OaMeeting selectOaMeetingByMeetingId(Long meetingId);
    public List<OaMeeting> selectOaMeetingList(OaMeeting oaMeeting);
    public int insertOaMeeting(OaMeeting oaMeeting);
    public int updateOaMeeting(OaMeeting oaMeeting);
    public int deleteOaMeetingByMeetingId(Long meetingId);
    public int deleteOaMeetingByMeetingIds(Long[] meetingIds);
    public int countMeetingConflict(Map<String, Object> params);

    /** O2：查询会议室在给定时间区间内的占用安排（排除已取消） */
    public List<OaMeeting> selectRoomOccupancy(Map<String, Object> params);
}
