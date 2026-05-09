package com.yu.brm.service;

import java.util.List;
import com.yu.brm.domain.BrmEquipmentMaintenance;

/**
 * 设备维护记录Service接口
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public interface IBrmEquipmentMaintenanceService 
{
    public BrmEquipmentMaintenance selectBrmEquipmentMaintenanceByMaintenanceId(Long maintenanceId);
    public List<BrmEquipmentMaintenance> selectBrmEquipmentMaintenanceList(BrmEquipmentMaintenance brmEquipmentMaintenance);
    public int insertBrmEquipmentMaintenance(BrmEquipmentMaintenance brmEquipmentMaintenance);
    public int updateBrmEquipmentMaintenance(BrmEquipmentMaintenance brmEquipmentMaintenance);
    public int deleteBrmEquipmentMaintenanceByMaintenanceId(Long maintenanceId);
    public int deleteBrmEquipmentMaintenanceByMaintenanceIds(Long[] maintenanceIds);
}
