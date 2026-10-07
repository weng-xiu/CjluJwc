package com.yu.system.service;

import java.util.List;

import com.yu.system.domain.SysDataClass;

/**
 * 数据分类分级台账 服务层
 *
 * @author yu
 * @date 2026-10-07
 */
public interface ISysDataClassService
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
     * 校验同一表字段是否唯一（唯一返回 true）
     */
    public boolean checkColumnUnique(SysDataClass dataClass);

    /**
     * 新增台账
     */
    public int insertDataClass(SysDataClass dataClass);

    /**
     * 修改台账
     */
    public int updateDataClass(SysDataClass dataClass);

    /**
     * 批量删除
     */
    public int deleteDataClassByIds(Long[] classIds);
}
