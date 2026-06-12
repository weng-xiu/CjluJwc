package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional
    public int insertBrmClassroom(BrmClassroom brmClassroom)
    {
        // 数据范围校验：容量必须为正整数
        if (brmClassroom.getCapacity() != null && brmClassroom.getCapacity() <= 0)
        {
            throw new ServiceException("教室容量必须为正整数");
        }
        brmClassroom.setCreateTime(DateUtils.getNowDate());
        return brmClassroomMapper.insertBrmClassroom(brmClassroom);
    }

    @Override
    @Transactional
    public int updateBrmClassroom(BrmClassroom brmClassroom)
    {
        // 数据范围校验：容量必须为正整数
        if (brmClassroom.getCapacity() != null && brmClassroom.getCapacity() <= 0)
        {
            throw new ServiceException("教室容量必须为正整数");
        }
        brmClassroom.setUpdateTime(DateUtils.getNowDate());
        return brmClassroomMapper.updateBrmClassroom(brmClassroom);
    }

    @Override
    @Transactional
    public int deleteBrmClassroomByClassroomId(Long classroomId)
    {
        return brmClassroomMapper.deleteBrmClassroomByClassroomId(classroomId);
    }

    @Override
    @Transactional
    public int deleteBrmClassroomByClassroomIds(Long[] classroomIds)
    {
        return brmClassroomMapper.deleteBrmClassroomByClassroomIds(classroomIds);
    }
}
