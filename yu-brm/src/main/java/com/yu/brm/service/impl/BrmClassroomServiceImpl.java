package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.brm.mapper.BrmClassroomMapper;
import com.yu.brm.domain.BrmClassroom;
import com.yu.brm.service.IBrmClassroomService;

/**
 * 教室Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
@Service
public class BrmClassroomServiceImpl implements IBrmClassroomService 
{
    @Autowired
    private BrmClassroomMapper brmClassroomMapper;

    @Override
    public BrmClassroom selectBrmClassroomByClassroomId(Long classroomId)
    {
        return brmClassroomMapper.selectBrmClassroomByClassroomId(classroomId);
    }

    @Override
    public List<BrmClassroom> selectBrmClassroomList(BrmClassroom brmClassroom)
    {
        return brmClassroomMapper.selectBrmClassroomList(brmClassroom);
    }

    @Override
    public int insertBrmClassroom(BrmClassroom brmClassroom)
    {
        brmClassroom.setCreateTime(DateUtils.getNowDate());
        return brmClassroomMapper.insertBrmClassroom(brmClassroom);
    }

    @Override
    public int updateBrmClassroom(BrmClassroom brmClassroom)
    {
        brmClassroom.setUpdateTime(DateUtils.getNowDate());
        return brmClassroomMapper.updateBrmClassroom(brmClassroom);
    }

    @Override
    public int deleteBrmClassroomByClassroomId(Long classroomId)
    {
        return brmClassroomMapper.deleteBrmClassroomByClassroomId(classroomId);
    }

    @Override
    public int deleteBrmClassroomByClassroomIds(Long[] classroomIds)
    {
        return brmClassroomMapper.deleteBrmClassroomByClassroomIds(classroomIds);
    }
}
