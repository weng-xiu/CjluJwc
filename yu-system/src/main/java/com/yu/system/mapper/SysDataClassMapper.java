package com.yu.system.mapper;

import java.util.List;

import com.yu.system.domain.SysDataClass;

/**
 * 数据分类分级台账 数据层
 *
 * @author yu
 * @date 2026-10-07
 */
public interface SysDataClassMapper
{
    /**
     * 查询台账列表
     */
    public List<SysDataClass> selectDataClassList(SysDataClass dataClass);

    /**
     * 按主键查询
     */
    public SysDataClass selectDataClassById(Long classId);

    /**
     * 按表名+字段名查询（唯一性校验）
     */
    public SysDataClass selectDataClassByColumn(SysDataClass dataClass);

    /**
     * 新增
     */
    public int insertDataClass(SysDataClass dataClass);

    /**
     * 修改
     */
    public int updateDataClass(SysDataClass dataClass);

    /**
     * 删除（单条）
     */
    public int deleteDataClassById(Long classId);

    /**
     * 批量删除
     */
    public int deleteDataClassByIds(Long[] classIds);
}
