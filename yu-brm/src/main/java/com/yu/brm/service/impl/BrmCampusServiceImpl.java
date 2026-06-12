package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.brm.mapper.BrmCampusMapper;
import com.yu.brm.mapper.BrmBuildingMapper;
import com.yu.brm.domain.BrmCampus;
import com.yu.brm.domain.BrmBuilding;
import com.yu.brm.service.IBrmCampusService;

/**
 * 校区Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
@Service
public class BrmCampusServiceImpl implements IBrmCampusService 
{
    @Autowired
    private BrmCampusMapper brmCampusMapper;

    @Autowired
    private BrmBuildingMapper brmBuildingMapper;

    @Override
    public BrmCampus selectBrmCampusByCampusId(Long campusId)
    {
        return brmCampusMapper.selectBrmCampusByCampusId(campusId);
    }

    @Override
    public List<BrmCampus> selectBrmCampusList(BrmCampus brmCampus)
    {
        return brmCampusMapper.selectBrmCampusList(brmCampus);
    }

    @Override
    @Transactional
    public int insertBrmCampus(BrmCampus brmCampus)
    {
        // 唯一性校验：校区名称不能重复
        BrmCampus query = new BrmCampus();
        query.setCampusName(brmCampus.getCampusName());
        List<BrmCampus> existing = brmCampusMapper.selectBrmCampusList(query);
        if (existing != null && !existing.isEmpty())
        {
            throw new ServiceException("校区名称'" + brmCampus.getCampusName() + "'已存在");
        }
        brmCampus.setCreateTime(DateUtils.getNowDate());
        return brmCampusMapper.insertBrmCampus(brmCampus);
    }

    @Override
    @Transactional
    public int updateBrmCampus(BrmCampus brmCampus)
    {
        // 唯一性校验：校区名称不能重复（排除自身）
        BrmCampus query = new BrmCampus();
        query.setCampusName(brmCampus.getCampusName());
        List<BrmCampus> existing = brmCampusMapper.selectBrmCampusList(query);
        if (existing != null && !existing.isEmpty())
        {
            for (BrmCampus item : existing)
            {
                if (!item.getCampusId().equals(brmCampus.getCampusId()))
                {
                    throw new ServiceException("校区名称'" + brmCampus.getCampusName() + "'已存在");
                }
            }
        }
        brmCampus.setUpdateTime(DateUtils.getNowDate());
        return brmCampusMapper.updateBrmCampus(brmCampus);
    }

    @Override
    @Transactional
    public int deleteBrmCampusByCampusId(Long campusId)
    {
        BrmBuilding query = new BrmBuilding();
        query.setCampusId(campusId);
        List<BrmBuilding> buildings = brmBuildingMapper.selectBrmBuildingList(query);
        if (buildings != null && !buildings.isEmpty())
        {
            throw new ServiceException("该校区下存在教学楼，不允许删除");
        }
        return brmCampusMapper.deleteBrmCampusByCampusId(campusId);
    }

    @Override
    @Transactional
    public int deleteBrmCampusByCampusIds(Long[] campusIds)
    {
        for (Long campusId : campusIds)
        {
            BrmBuilding query = new BrmBuilding();
            query.setCampusId(campusId);
            List<BrmBuilding> buildings = brmBuildingMapper.selectBrmBuildingList(query);
            if (buildings != null && !buildings.isEmpty())
            {
                throw new ServiceException("该校区下存在教学楼，不允许删除");
            }
        }
        return brmCampusMapper.deleteBrmCampusByCampusIds(campusIds);
    }
}
