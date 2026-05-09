package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.brm.mapper.BrmClassroomTypeMapper;
import com.yu.brm.domain.BrmClassroomType;
import com.yu.brm.service.IBrmClassroomTypeService;

/**
 * 教室类型Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
@Service
public class BrmClassroomTypeServiceImpl implements IBrmClassroomTypeService 
{
    @Autowired
    private BrmClassroomTypeMapper brmClassroomTypeMapper;

    @Override
    public BrmClassroomType selectBrmClassroomTypeByTypeId(Long typeId)
    {
        return brmClassroomTypeMapper.selectBrmClassroomTypeByTypeId(typeId);
    }

    @Override
    public List<BrmClassroomType> selectBrmClassroomTypeList(BrmClassroomType brmClassroomType)
    {
        return brmClassroomTypeMapper.selectBrmClassroomTypeList(brmClassroomType);
    }

    @Override
    public int insertBrmClassroomType(BrmClassroomType brmClassroomType)
    {
        brmClassroomType.setCreateTime(DateUtils.getNowDate());
        return brmClassroomTypeMapper.insertBrmClassroomType(brmClassroomType);
    }

    @Override
    public int updateBrmClassroomType(BrmClassroomType brmClassroomType)
    {
        brmClassroomType.setUpdateTime(DateUtils.getNowDate());
        return brmClassroomTypeMapper.updateBrmClassroomType(brmClassroomType);
    }

    @Override
    public int deleteBrmClassroomTypeByTypeId(Long typeId)
    {
        return brmClassroomTypeMapper.deleteBrmClassroomTypeByTypeId(typeId);
    }

    @Override
    public int deleteBrmClassroomTypeByTypeIds(Long[] typeIds)
    {
        return brmClassroomTypeMapper.deleteBrmClassroomTypeByTypeIds(typeIds);
    }
}
