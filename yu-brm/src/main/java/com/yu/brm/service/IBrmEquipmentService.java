package com.yu.brm.service;

import java.util.List;
import com.yu.brm.domain.BrmEquipment;

/**
 * 多媒体设备Service接口
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public interface IBrmEquipmentService 
{
    public BrmEquipment selectBrmEquipmentByEquipId(Long equipId);
    public List<BrmEquipment> selectBrmEquipmentList(BrmEquipment brmEquipment);
    public int insertBrmEquipment(BrmEquipment brmEquipment);
    public int updateBrmEquipment(BrmEquipment brmEquipment);
    public int deleteBrmEquipmentByEquipId(Long equipId);
    public int deleteBrmEquipmentByEquipIds(Long[] equipIds);
}
