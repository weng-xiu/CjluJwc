package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.brm.mapper.BrmEquipmentMaintenanceMapper;
import com.yu.brm.domain.BrmEquipmentMaintenance;
import com.yu.brm.service.IBrmEquipmentMaintenanceService;

/**
 * 设备维护记录Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
@Service
public class BrmEquipmentMaintenanceServiceImpl implements IBrmEquipmentMaintenanceService 
{
    @Autowired
    private BrmEquipmentMaintenanceMapper brmEquipmentMaintenanceMapper;

    @Override
    public BrmEquipmentMaintenance selectBrmEquipmentMaintenanceByMaintenanceId(Long maintenanceId)
    {
        return brmEquipmentMaintenanceMapper.selectBrmEquipmentMaintenanceByMaintenanceId(maintenanceId);
    }

    @Override
    public List<BrmEquipmentMaintenance> selectBrmEquipmentMaintenanceList(BrmEquipmentMaintenance brmEquipmentMaintenance)
    {
        return brmEquipmentMaintenanceMapper.selectBrmEquipmentMaintenanceList(brmEquipmentMaintenance);
    }

    @Override
    public int insertBrmEquipmentMaintenance(BrmEquipmentMaintenance brmEquipmentMaintenance)
    {
        brmEquipmentMaintenance.setCreateTime(DateUtils.getNowDate());
        return brmEquipmentMaintenanceMapper.insertBrmEquipmentMaintenance(brmEquipmentMaintenance);
    }

    @Override
    public int updateBrmEquipmentMaintenance(BrmEquipmentMaintenance brmEquipmentMaintenance)
    {
        brmEquipmentMaintenance.setUpdateTime(DateUtils.getNowDate());
        return brmEquipmentMaintenanceMapper.updateBrmEquipmentMaintenance(brmEquipmentMaintenance);
    }

    @Override
    public int deleteBrmEquipmentMaintenanceByMaintenanceId(Long maintenanceId)
    {
        return brmEquipmentMaintenanceMapper.deleteBrmEquipmentMaintenanceByMaintenanceId(maintenanceId);
    }

    @Override
    public int deleteBrmEquipmentMaintenanceByMaintenanceIds(Long[] maintenanceIds)
    {
        return brmEquipmentMaintenanceMapper.deleteBrmEquipmentMaintenanceByMaintenanceIds(maintenanceIds);
    }
}
