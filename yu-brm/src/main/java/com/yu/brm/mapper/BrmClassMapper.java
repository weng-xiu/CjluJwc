package com.yu.brm.mapper;

import java.util.List;
import com.yu.brm.domain.BrmClass;

public interface BrmClassMapper 
{
    public BrmClass selectBrmClassByClassId(Long classId);
    public List<BrmClass> selectBrmClassList(BrmClass brmClass);
    public int insertBrmClass(BrmClass brmClass);
    public int updateBrmClass(BrmClass brmClass);
    public int deleteBrmClassByClassId(Long classId);
    public int deleteBrmClassByClassIds(Long[] classIds);
}
