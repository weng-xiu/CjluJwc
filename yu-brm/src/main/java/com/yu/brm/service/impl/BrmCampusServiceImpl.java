package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.brm.mapper.BrmCampusMapper;
import com.yu.brm.domain.BrmCampus;
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
    public int insertBrmCampus(BrmCampus brmCampus)
    {
        brmCampus.setCreateTime(DateUtils.getNowDate());
        return brmCampusMapper.insertBrmCampus(brmCampus);
    }

    @Override
    public int updateBrmCampus(BrmCampus brmCampus)
    {
        brmCampus.setUpdateTime(DateUtils.getNowDate());
        return brmCampusMapper.updateBrmCampus(brmCampus);
    }

    @Override
    public int deleteBrmCampusByCampusId(Long campusId)
    {
        return brmCampusMapper.deleteBrmCampusByCampusId(campusId);
    }

    @Override
    public int deleteBrmCampusByCampusIds(Long[] campusIds)
    {
        return brmCampusMapper.deleteBrmCampusByCampusIds(campusIds);
    }
}
