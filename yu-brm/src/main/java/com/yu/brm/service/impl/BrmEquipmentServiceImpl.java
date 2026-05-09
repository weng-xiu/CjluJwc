package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.brm.mapper.BrmEquipmentMapper;
import com.yu.brm.domain.BrmEquipment;
import com.yu.brm.service.IBrmEquipmentService;

/**
 * 多媒体设备Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
@Service
public class BrmEquipmentServiceImpl implements IBrmEquipmentService 
{
    @Autowired
    private BrmEquipmentMapper brmEquipmentMapper;

    @Override
    public BrmEquipment selectBrmEquipmentByEquipId(Long equipId)
    {
        return brmEquipmentMapper.selectBrmEquipmentByEquipId(equipId);
    }

    @Override
    public List<BrmEquipment> selectBrmEquipmentList(BrmEquipment brmEquipment)
    {
        return brmEquipmentMapper.selectBrmEquipmentList(brmEquipment);
    }

    @Override
    public int insertBrmEquipment(BrmEquipment brmEquipment)
    {
        brmEquipment.setCreateTime(DateUtils.getNowDate());
        return brmEquipmentMapper.insertBrmEquipment(brmEquipment);
    }

    @Override
    public int updateBrmEquipment(BrmEquipment brmEquipment)
    {
        brmEquipment.setUpdateTime(DateUtils.getNowDate());
        return brmEquipmentMapper.updateBrmEquipment(brmEquipment);
    }

    @Override
    public int deleteBrmEquipmentByEquipId(Long equipId)
    {
        return brmEquipmentMapper.deleteBrmEquipmentByEquipId(equipId);
    }

    @Override
    public int deleteBrmEquipmentByEquipIds(Long[] equipIds)
    {
        return brmEquipmentMapper.deleteBrmEquipmentByEquipIds(equipIds);
    }
}
