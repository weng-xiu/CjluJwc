package com.yu.brm.service;

import java.util.List;
import com.yu.brm.domain.BrmClass;

/**
 * 班级Service接口
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public interface IBrmClassService 
{
    public BrmClass selectBrmClassByClassId(Long classId);
    public List<BrmClass> selectBrmClassList(BrmClass brmClass);
    public int insertBrmClass(BrmClass brmClass);
    public int updateBrmClass(BrmClass brmClass);
    public int deleteBrmClassByClassId(Long classId);
    public int deleteBrmClassByClassIds(Long[] classIds);
}
