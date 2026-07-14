package com.yu.oa.service;

import java.util.List;
import com.yu.oa.domain.OaMeetingRoom;

/**
 * 会议室Service接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface IOaMeetingRoomService 
{
    public OaMeetingRoom selectOaMeetingRoomByRoomId(Long roomId);
    public List<OaMeetingRoom> selectOaMeetingRoomList(OaMeetingRoom oaMeetingRoom);
    public int insertOaMeetingRoom(OaMeetingRoom oaMeetingRoom);
    public int updateOaMeetingRoom(OaMeetingRoom oaMeetingRoom);
    public int deleteOaMeetingRoomByRoomId(Long roomId);
    public int deleteOaMeetingRoomByRoomIds(Long[] roomIds);
}
