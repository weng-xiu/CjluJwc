package com.yu.system.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import com.yu.system.domain.SysDataClass;
import com.yu.system.mapper.SysDataClassMapper;
import com.yu.system.service.ISysDataClassService;

/**
 * 数据分类分级台账 服务实现
 *
 * @author yu
 * @date 2026-10-07
 */
@Service
public class SysDataClassServiceImpl implements ISysDataClassService
{
    @Autowired
    private SysDataClassMapper dataClassMapper;

    @Override
    public List<SysDataClass> selectDataClassList(SysDataClass dataClass)
    {
        return dataClassMapper.selectDataClassList(dataClass);
    }

    @Override
    public SysDataClass selectDataClassById(Long classId)
    {
        return dataClassMapper.selectDataClassById(classId);
    }

    /**
     * 校验同一「表名 + 字段名」是否唯一：命中既有登记且非自身则视为不唯一（返回 false）。
     */
    @Override
    public boolean checkColumnUnique(SysDataClass dataClass)
    {
        Long classId = StringUtils.isNull(dataClass.getClassId()) ? -1L : dataClass.getClassId();
        SysDataClass info = dataClassMapper.selectDataClassByColumn(dataClass);
        return !(StringUtils.isNotNull(info) && info.getClassId().longValue() != classId.longValue());
    }

    @Override
    public int insertDataClass(SysDataClass dataClass)
    {
        dataClass.setCreateTime(DateUtils.getNowDate());
        return dataClassMapper.insertDataClass(dataClass);
    }

    @Override
    public int updateDataClass(SysDataClass dataClass)
    {
        dataClass.setUpdateTime(DateUtils.getNowDate());
        return dataClassMapper.updateDataClass(dataClass);
    }

    @Override
    public int deleteDataClassByIds(Long[] classIds)
    {
        return dataClassMapper.deleteDataClassByIds(classIds);
    }
}
