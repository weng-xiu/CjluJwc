package com.yu.oa.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.SecurityUtils;
import com.yu.oa.domain.OaMeeting;
import com.yu.oa.domain.OaMeetingMinutes;
import com.yu.oa.domain.OaMeetingParticipant;
import com.yu.oa.mapper.OaMeetingMapper;
import com.yu.oa.mapper.OaMeetingMinutesMapper;
import com.yu.oa.mapper.OaMeetingParticipantMapper;
import com.yu.oa.service.IOaMeetingService;

/**
 * 会议管理Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@Service
public class OaMeetingServiceImpl implements IOaMeetingService
{
    @Autowired
    private OaMeetingMapper oaMeetingMapper;

    @Autowired
    private OaMeetingParticipantMapper oaMeetingParticipantMapper;

    @Autowired
    private OaMeetingMinutesMapper oaMeetingMinutesMapper;

    @Override
    public OaMeeting selectOaMeetingByMeetingId(Long meetingId)
    {
        OaMeeting meeting = oaMeetingMapper.selectOaMeetingByMeetingId(meetingId);
        if (meeting != null)
        {
            meeting.setParticipantList(oaMeetingParticipantMapper.selectOaMeetingParticipantByMeetingId(meetingId));
            meeting.setMinutes(oaMeetingMinutesMapper.selectOaMeetingMinutesByMeetingId(meetingId));
        }
        return meeting;
    }

    @Override
    public List<OaMeeting> selectOaMeetingList(OaMeeting oaMeeting)
    {
        return oaMeetingMapper.selectOaMeetingList(oaMeeting);
    }

    @Override
    @Transactional
    public int insertOaMeeting(OaMeeting oaMeeting)
    {
        if (checkMeetingConflict(oaMeeting))
        {
            throw new RuntimeException("会议室时间冲突，请选择其他时间段");
        }
        oaMeeting.setCreateTime(DateUtils.getNowDate());
        oaMeeting.setOrganizerId(SecurityUtils.getLoginUser().getUserId());
        oaMeeting.setOrganizerName(SecurityUtils.getUsername());
        oaMeeting.setMeetingStatus("0");
        int rows = oaMeetingMapper.insertOaMeeting(oaMeeting);
        insertParticipant(oaMeeting);
        return rows;
    }

    @Override
    @Transactional
    public int updateOaMeeting(OaMeeting oaMeeting)
    {
        if (checkMeetingConflict(oaMeeting))
        {
            throw new RuntimeException("会议室时间冲突，请选择其他时间段");
        }
        oaMeeting.setUpdateTime(DateUtils.getNowDate());
        oaMeetingParticipantMapper.deleteOaMeetingParticipantByMeetingId(oaMeeting.getMeetingId());
        insertParticipant(oaMeeting);
        return oaMeetingMapper.updateOaMeeting(oaMeeting);
    }

    @Override
    @Transactional
    public int deleteOaMeetingByMeetingId(Long meetingId)
    {
        oaMeetingParticipantMapper.deleteOaMeetingParticipantByMeetingId(meetingId);
        oaMeetingMinutesMapper.deleteOaMeetingMinutesByMeetingId(meetingId);
        return oaMeetingMapper.deleteOaMeetingByMeetingId(meetingId);
    }

    @Override
    @Transactional
    public int deleteOaMeetingByMeetingIds(Long[] meetingIds)
    {
        for (Long meetingId : meetingIds)
        {
            oaMeetingParticipantMapper.deleteOaMeetingParticipantByMeetingId(meetingId);
            oaMeetingMinutesMapper.deleteOaMeetingMinutesByMeetingId(meetingId);
        }
        return oaMeetingMapper.deleteOaMeetingByMeetingIds(meetingIds);
    }

    @Override
    public boolean checkMeetingConflict(OaMeeting oaMeeting)
    {
        if (oaMeeting.getRoomId() == null || oaMeeting.getStartTime() == null || oaMeeting.getEndTime() == null)
        {
            return false;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("roomId", oaMeeting.getRoomId());
        params.put("startTime", oaMeeting.getStartTime());
        params.put("endTime", oaMeeting.getEndTime());
        params.put("meetingId", oaMeeting.getMeetingId());
        return oaMeetingMapper.countMeetingConflict(params) > 0;
    }

    @Override
    @Transactional
    public int saveMinutes(OaMeetingMinutes minutes)
    {
        minutes.setUpdateTime(DateUtils.getNowDate());
        OaMeetingMinutes exist = oaMeetingMinutesMapper.selectOaMeetingMinutesByMeetingId(minutes.getMeetingId());
        if (exist != null)
        {
            minutes.setMinutesId(exist.getMinutesId());
            return oaMeetingMinutesMapper.updateOaMeetingMinutes(minutes);
        }
        minutes.setCreateTime(DateUtils.getNowDate());
        minutes.setRecorderId(SecurityUtils.getLoginUser().getUserId());
        minutes.setRecorderName(SecurityUtils.getUsername());
        return oaMeetingMinutesMapper.insertOaMeetingMinutes(minutes);
    }

    @Override
    @Transactional
    public int updateParticipantStatus(OaMeetingParticipant participant)
    {
        return oaMeetingParticipantMapper.updateOaMeetingParticipant(participant);
    }

    @Override
    @Transactional
    public int signMeeting(Long participantId)
    {
        OaMeetingParticipant participant = new OaMeetingParticipant();
        participant.setParticipantId(participantId);
        participant.setSignStatus("1");
        participant.setSignTime(new Date());
        return oaMeetingParticipantMapper.updateOaMeetingParticipant(participant);
    }

    private void insertParticipant(OaMeeting oaMeeting)
    {
        List<OaMeetingParticipant> participantList = oaMeeting.getParticipantList();
        if (participantList == null || participantList.isEmpty())
        {
            return;
        }
        for (OaMeetingParticipant participant : participantList)
        {
            participant.setMeetingId(oaMeeting.getMeetingId());
            participant.setAttendStatus("0");
            participant.setSignStatus("0");
            participant.setCreateTime(DateUtils.getNowDate());
            oaMeetingParticipantMapper.insertOaMeetingParticipant(participant);
        }
    }
}
