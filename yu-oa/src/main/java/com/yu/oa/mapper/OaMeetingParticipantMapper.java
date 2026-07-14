package com.yu.oa.mapper;

import java.util.List;
import com.yu.oa.domain.OaMeetingParticipant;

/**
 * 参会人员Mapper接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface OaMeetingParticipantMapper 
{
    public OaMeetingParticipant selectOaMeetingParticipantByParticipantId(Long participantId);
    public List<OaMeetingParticipant> selectOaMeetingParticipantByMeetingId(Long meetingId);
    public List<OaMeetingParticipant> selectOaMeetingParticipantList(OaMeetingParticipant oaMeetingParticipant);
    public int insertOaMeetingParticipant(OaMeetingParticipant oaMeetingParticipant);
    public int updateOaMeetingParticipant(OaMeetingParticipant oaMeetingParticipant);
    public int deleteOaMeetingParticipantByMeetingId(Long meetingId);
    public int deleteOaMeetingParticipantByParticipantIds(Long[] participantIds);
}
