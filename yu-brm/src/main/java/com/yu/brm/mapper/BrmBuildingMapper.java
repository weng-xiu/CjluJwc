package com.yu.brm.mapper;

import java.util.List;
import com.yu.brm.domain.BrmBuilding;

public interface BrmBuildingMapper 
{
    public BrmBuilding selectBrmBuildingByBuildingId(Long buildingId);
    public List<BrmBuilding> selectBrmBuildingList(BrmBuilding brmBuilding);
    public int insertBrmBuilding(BrmBuilding brmBuilding);
    public int updateBrmBuilding(BrmBuilding brmBuilding);
    public int deleteBrmBuildingByBuildingId(Long buildingId);
    public int deleteBrmBuildingByBuildingIds(Long[] buildingIds);
}
