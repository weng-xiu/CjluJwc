package com.yu.brm.mapper;

import java.util.List;
import com.yu.brm.domain.BrmClassroomType;

public interface BrmClassroomTypeMapper 
{
    public BrmClassroomType selectBrmClassroomTypeByTypeId(Long typeId);
    public List<BrmClassroomType> selectBrmClassroomTypeList(BrmClassroomType brmClassroomType);
    public int insertBrmClassroomType(BrmClassroomType brmClassroomType);
    public int updateBrmClassroomType(BrmClassroomType brmClassroomType);
    public int deleteBrmClassroomTypeByTypeId(Long typeId);
    public int deleteBrmClassroomTypeByTypeIds(Long[] typeIds);
}
