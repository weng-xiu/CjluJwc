package com.yu.brm.service;

import java.util.List;
import com.yu.brm.domain.BrmBuilding;

/**
 * 教学楼Service接口
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public interface IBrmBuildingService 
{
    public BrmBuilding selectBrmBuildingByBuildingId(Long buildingId);
    public List<BrmBuilding> selectBrmBuildingList(BrmBuilding brmBuilding);
    public int insertBrmBuilding(BrmBuilding brmBuilding);
    public int updateBrmBuilding(BrmBuilding brmBuilding);
    public int deleteBrmBuildingByBuildingId(Long buildingId);
    public int deleteBrmBuildingByBuildingIds(Long[] buildingIds);
}
