package com.yu.brm.mapper;

import java.util.List;
import com.yu.brm.domain.BrmEquipmentMaintenance;

public interface BrmEquipmentMaintenanceMapper 
{
    public BrmEquipmentMaintenance selectBrmEquipmentMaintenanceByMaintenanceId(Long maintenanceId);
    public List<BrmEquipmentMaintenance> selectBrmEquipmentMaintenanceList(BrmEquipmentMaintenance brmEquipmentMaintenance);
    public int insertBrmEquipmentMaintenance(BrmEquipmentMaintenance brmEquipmentMaintenance);
    public int updateBrmEquipmentMaintenance(BrmEquipmentMaintenance brmEquipmentMaintenance);
    public int deleteBrmEquipmentMaintenanceByMaintenanceId(Long maintenanceId);
    public int deleteBrmEquipmentMaintenanceByMaintenanceIds(Long[] maintenanceIds);
}
