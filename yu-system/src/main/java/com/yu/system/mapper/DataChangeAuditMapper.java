package com.yu.system.mapper;

import java.util.List;

import com.yu.system.domain.DataChangeAudit;

/**
 * 字段级数据变更流水 数据层
 *
 * @author yu
 */
public interface DataChangeAuditMapper
{
    /**
     * 批量插入变更流水（同一次修改的多个字段差异一次落库）。
     *
     * @param list 流水列表
     * @return 结果
     */
    public int batchInsertDataChangeAudit(List<DataChangeAudit> list);

    /**
     * 查询变更流水列表（按条件）。
     *
     * @param dataChangeAudit 查询条件
     * @return 流水集合
     */
    public List<DataChangeAudit> selectDataChangeAuditList(DataChangeAudit dataChangeAudit);

    /**
     * 按实体类型 + 业务主键查询某条记录的全部字段变更历史（时间倒序）。
     *
     * @param entityType 实体类型
     * @param bizId      业务主键
     * @return 流水集合
     */
    public List<DataChangeAudit> selectByBiz(@org.apache.ibatis.annotations.Param("entityType") String entityType,
            @org.apache.ibatis.annotations.Param("bizId") String bizId);
}
