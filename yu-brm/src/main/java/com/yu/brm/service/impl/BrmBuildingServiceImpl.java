package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.brm.mapper.BrmBuildingMapper;
import com.yu.brm.domain.BrmBuilding;
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
    public int insertBrmBuilding(BrmBuilding brmBuilding)
    {
        brmBuilding.setCreateTime(DateUtils.getNowDate());
        return brmBuildingMapper.insertBrmBuilding(brmBuilding);
    }

    @Override
    public int updateBrmBuilding(BrmBuilding brmBuilding)
    {
        brmBuilding.setUpdateTime(DateUtils.getNowDate());
        return brmBuildingMapper.updateBrmBuilding(brmBuilding);
    }

    @Override
    public int deleteBrmBuildingByBuildingId(Long buildingId)
    {
        return brmBuildingMapper.deleteBrmBuildingByBuildingId(buildingId);
    }

    @Override
    public int deleteBrmBuildingByBuildingIds(Long[] buildingIds)
    {
        return brmBuildingMapper.deleteBrmBuildingByBuildingIds(buildingIds);
    }
}
