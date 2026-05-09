package com.yu.brm.mapper;

import java.util.List;
import com.yu.brm.domain.BrmCampus;

public interface BrmCampusMapper 
{
    public BrmCampus selectBrmCampusByCampusId(Long campusId);
    public List<BrmCampus> selectBrmCampusList(BrmCampus brmCampus);
    public int insertBrmCampus(BrmCampus brmCampus);
    public int updateBrmCampus(BrmCampus brmCampus);
    public int deleteBrmCampusByCampusId(Long campusId);
    public int deleteBrmCampusByCampusIds(Long[] campusIds);
}
