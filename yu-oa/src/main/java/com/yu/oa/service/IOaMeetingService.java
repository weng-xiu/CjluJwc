package com.yu.oa.service;

import java.util.Date;
import java.util.List;
import com.yu.oa.domain.OaMeeting;
import com.yu.oa.domain.OaMeetingMinutes;
import com.yu.oa.domain.OaMeetingParticipant;

/**
 * 会议管理Service接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface IOaMeetingService 
{
    public OaMeeting selectOaMeetingByMeetingId(Long meetingId);
    public List<OaMeeting> selectOaMeetingList(OaMeeting oaMeeting);
    public int insertOaMeeting(OaMeeting oaMeeting);
    public int updateOaMeeting(OaMeeting oaMeeting);
    public int deleteOaMeetingByMeetingId(Long meetingId);
    public int deleteOaMeetingByMeetingIds(Long[] meetingIds);

    /**
     * 检测会议室时间冲突
     */
    public boolean checkMeetingConflict(OaMeeting oaMeeting);

    /**
     * O2：查询某会议室在给定时间区间内的占用安排（排除已取消），用于占用日历视图。
     */
    public List<OaMeeting> roomOccupancy(Long roomId, Date beginTime, Date endTime);

    /**
     * 保存会议纪要
     */
    public int saveMinutes(OaMeetingMinutes minutes);

    /**
     * 更新参会状态
     */
    public int updateParticipantStatus(OaMeetingParticipant participant);

    /**
     * 会议签到
     */
    public int signMeeting(Long participantId);
}
