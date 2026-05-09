package com.yu.brm.service;

import java.util.List;
import com.yu.brm.domain.BrmClassroomType;

/**
 * 教室类型Service接口
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public interface IBrmClassroomTypeService 
{
    public BrmClassroomType selectBrmClassroomTypeByTypeId(Long typeId);
    public List<BrmClassroomType> selectBrmClassroomTypeList(BrmClassroomType brmClassroomType);
    public int insertBrmClassroomType(BrmClassroomType brmClassroomType);
    public int updateBrmClassroomType(BrmClassroomType brmClassroomType);
    public int deleteBrmClassroomTypeByTypeId(Long typeId);
    public int deleteBrmClassroomTypeByTypeIds(Long[] typeIds);
}
