package com.yu.system.service;

import java.util.List;

import com.yu.common.utils.bean.FieldChange;
import com.yu.system.domain.DataChangeAudit;

/**
 * 字段级数据变更流水 服务层（K1 合规③）
 *
 * @author yu
 */
public interface IDataChangeAuditService
{
    /**
     * 记录一次业务修改产生的字段级变更流水。
     *
     * <p>自动补齐操作人、来源 IP、链路 traceId 与变更时间；{@code changes} 为空时不写库。
     * 应在与业务变更相同的事务内调用，保证「变更」与「留痕」原子性。</p>
     *
     * @param entityType      实体类型（如 aem_grade_record）
     * @param entityTypeLabel 实体中文名（如 成绩记录）
     * @param bizId           业务主键值
     * @param changes         {@link com.yu.common.utils.bean.FieldDiffUtils#diff} 产出的差异列表
     */
    public void recordDiff(String entityType, String entityTypeLabel, String bizId, List<FieldChange> changes);

    /**
     * 按条件查询变更流水列表。
     *
     * @param dataChangeAudit 查询条件
     * @return 流水集合
     */
    public List<DataChangeAudit> selectDataChangeAuditList(DataChangeAudit dataChangeAudit);

    /**
     * 查询某条业务记录的全部字段变更历史。
     *
     * @param entityType 实体类型
     * @param bizId      业务主键
     * @return 流水集合
     */
    public List<DataChangeAudit> selectByBiz(String entityType, String bizId);
}
