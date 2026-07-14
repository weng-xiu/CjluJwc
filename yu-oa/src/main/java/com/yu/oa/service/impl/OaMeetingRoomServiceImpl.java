package com.yu.oa.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.common.utils.DateUtils;
import com.yu.oa.domain.OaMeetingRoom;
import com.yu.oa.mapper.OaMeetingRoomMapper;
import com.yu.oa.service.IOaMeetingRoomService;

/**
 * 会议室Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@Service
public class OaMeetingRoomServiceImpl implements IOaMeetingRoomService
{
    @Autowired
    private OaMeetingRoomMapper oaMeetingRoomMapper;

    @Override
    public OaMeetingRoom selectOaMeetingRoomByRoomId(Long roomId)
    {
        return oaMeetingRoomMapper.selectOaMeetingRoomByRoomId(roomId);
    }

    @Override
    public List<OaMeetingRoom> selectOaMeetingRoomList(OaMeetingRoom oaMeetingRoom)
    {
        return oaMeetingRoomMapper.selectOaMeetingRoomList(oaMeetingRoom);
    }

    @Override
    @Transactional
    public int insertOaMeetingRoom(OaMeetingRoom oaMeetingRoom)
    {
        oaMeetingRoom.setCreateTime(DateUtils.getNowDate());
        return oaMeetingRoomMapper.insertOaMeetingRoom(oaMeetingRoom);
    }

    @Override
    @Transactional
    public int updateOaMeetingRoom(OaMeetingRoom oaMeetingRoom)
    {
        oaMeetingRoom.setUpdateTime(DateUtils.getNowDate());
        return oaMeetingRoomMapper.updateOaMeetingRoom(oaMeetingRoom);
    }

    @Override
    @Transactional
    public int deleteOaMeetingRoomByRoomId(Long roomId)
    {
        return oaMeetingRoomMapper.deleteOaMeetingRoomByRoomId(roomId);
    }

    @Override
    @Transactional
    public int deleteOaMeetingRoomByRoomIds(Long[] roomIds)
    {
        return oaMeetingRoomMapper.deleteOaMeetingRoomByRoomIds(roomIds);
    }
}
