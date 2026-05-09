package com.yu.brm.service;

import java.util.List;
import com.yu.brm.domain.BrmCampus;

/**
 * 校区Service接口
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public interface IBrmCampusService 
{
    public BrmCampus selectBrmCampusByCampusId(Long campusId);
    public List<BrmCampus> selectBrmCampusList(BrmCampus brmCampus);
    public int insertBrmCampus(BrmCampus brmCampus);
    public int updateBrmCampus(BrmCampus brmCampus);
    public int deleteBrmCampusByCampusId(Long campusId);
    public int deleteBrmCampusByCampusIds(Long[] campusIds);
}
