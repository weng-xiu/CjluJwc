package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.brm.mapper.BrmBuildingMapper;
import com.yu.brm.mapper.BrmClassroomMapper;
import com.yu.brm.domain.BrmBuilding;
import com.yu.brm.domain.BrmClassroom;
import com.yu.brm.service.IBrmBuildingService;

/**
 * 教学楼Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
@Service
public class BrmBuildingServiceImpl implements IBrmBuildingService 
{
    @Autowired
    private BrmBuildingMapper brmBuildingMapper;

    @Autowired
    private BrmClassroomMapper brmClassroomMapper;

    @Override
    public BrmBuilding selectBrmBuildingByBuildingId(Long buildingId)
    {
        return brmBuildingMapper.selectBrmBuildingByBuildingId(buildingId);
    }

    @Override
    public List<BrmBuilding> selectBrmBuildingList(BrmBuilding brmBuilding)
    {
        return brmBuildingMapper.selectBrmBuildingList(brmBuilding);
    }

    @Override
    @Transactional
    public int insertBrmBuilding(BrmBuilding brmBuilding)
    {
        brmBuilding.setCreateTime(DateUtils.getNowDate());
        return brmBuildingMapper.insertBrmBuilding(brmBuilding);
    }

    @Override
    @Transactional
    public int updateBrmBuilding(BrmBuilding brmBuilding)
    {
        brmBuilding.setUpdateTime(DateUtils.getNowDate());
        return brmBuildingMapper.updateBrmBuilding(brmBuilding);
    }

    @Override
    @Transactional
    public int deleteBrmBuildingByBuildingId(Long buildingId)
    {
        BrmClassroom query = new BrmClassroom();
        query.setBuildingId(buildingId);
        List<BrmClassroom> classrooms = brmClassroomMapper.selectBrmClassroomList(query);
        if (classrooms != null && !classrooms.isEmpty())
        {
            throw new ServiceException("该教学楼下存在教室，不允许删除");
        }
        return brmBuildingMapper.deleteBrmBuildingByBuildingId(buildingId);
    }

    @Override
    @Transactional
    public int deleteBrmBuildingByBuildingIds(Long[] buildingIds)
    {
        for (Long buildingId : buildingIds)
        {
            BrmClassroom query = new BrmClassroom();
            query.setBuildingId(buildingId);
            List<BrmClassroom> classrooms = brmClassroomMapper.selectBrmClassroomList(query);
            if (classrooms != null && !classrooms.isEmpty())
            {
                throw new ServiceException("该教学楼下存在教室，不允许删除");
            }
        }
        return brmBuildingMapper.deleteBrmBuildingByBuildingIds(buildingIds);
    }
}
