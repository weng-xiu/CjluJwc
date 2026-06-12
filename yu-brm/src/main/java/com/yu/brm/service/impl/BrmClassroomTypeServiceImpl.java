package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.brm.mapper.BrmClassroomTypeMapper;
import com.yu.brm.mapper.BrmClassroomMapper;
import com.yu.brm.domain.BrmClassroomType;
import com.yu.brm.domain.BrmClassroom;
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

    @Autowired
    private BrmClassroomMapper brmClassroomMapper;

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
    @Transactional
    public int insertBrmClassroomType(BrmClassroomType brmClassroomType)
    {
        // 唯一性校验：类型名称不能重复
        BrmClassroomType query = new BrmClassroomType();
        query.setTypeName(brmClassroomType.getTypeName());
        List<BrmClassroomType> existing = brmClassroomTypeMapper.selectBrmClassroomTypeList(query);
        if (existing != null && !existing.isEmpty())
        {
            throw new ServiceException("类型名称'" + brmClassroomType.getTypeName() + "'已存在");
        }
        brmClassroomType.setCreateTime(DateUtils.getNowDate());
        return brmClassroomTypeMapper.insertBrmClassroomType(brmClassroomType);
    }

    @Override
    @Transactional
    public int updateBrmClassroomType(BrmClassroomType brmClassroomType)
    {
        // 唯一性校验：类型名称不能重复（排除自身）
        BrmClassroomType query = new BrmClassroomType();
        query.setTypeName(brmClassroomType.getTypeName());
        List<BrmClassroomType> existing = brmClassroomTypeMapper.selectBrmClassroomTypeList(query);
        if (existing != null && !existing.isEmpty())
        {
            for (BrmClassroomType item : existing)
            {
                if (!item.getTypeId().equals(brmClassroomType.getTypeId()))
                {
                    throw new ServiceException("类型名称'" + brmClassroomType.getTypeName() + "'已存在");
                }
            }
        }
        brmClassroomType.setUpdateTime(DateUtils.getNowDate());
        return brmClassroomTypeMapper.updateBrmClassroomType(brmClassroomType);
    }

    @Override
    @Transactional
    public int deleteBrmClassroomTypeByTypeId(Long typeId)
    {
        BrmClassroom query = new BrmClassroom();
        query.setTypeId(typeId);
        List<BrmClassroom> classrooms = brmClassroomMapper.selectBrmClassroomList(query);
        if (classrooms != null && !classrooms.isEmpty())
        {
            throw new ServiceException("该教室类型下存在教室，不允许删除");
        }
        return brmClassroomTypeMapper.deleteBrmClassroomTypeByTypeId(typeId);
    }

    @Override
    @Transactional
    public int deleteBrmClassroomTypeByTypeIds(Long[] typeIds)
    {
        for (Long typeId : typeIds)
        {
            BrmClassroom query = new BrmClassroom();
            query.setTypeId(typeId);
            List<BrmClassroom> classrooms = brmClassroomMapper.selectBrmClassroomList(query);
            if (classrooms != null && !classrooms.isEmpty())
            {
                throw new ServiceException("该教室类型下存在教室，不允许删除");
            }
        }
        return brmClassroomTypeMapper.deleteBrmClassroomTypeByTypeIds(typeIds);
    }
}
