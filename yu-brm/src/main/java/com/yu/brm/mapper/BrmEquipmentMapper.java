package com.yu.brm.mapper;

import java.util.List;
import com.yu.brm.domain.BrmEquipment;

public interface BrmEquipmentMapper 
{
    public BrmEquipment selectBrmEquipmentByEquipId(Long equipId);
    public List<BrmEquipment> selectBrmEquipmentList(BrmEquipment brmEquipment);
    public int insertBrmEquipment(BrmEquipment brmEquipment);
    public int updateBrmEquipment(BrmEquipment brmEquipment);
    public int deleteBrmEquipmentByEquipId(Long equipId);
    public int deleteBrmEquipmentByEquipIds(Long[] equipIds);
}
