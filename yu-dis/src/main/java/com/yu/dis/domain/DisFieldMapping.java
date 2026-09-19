package com.yu.dis.domain;

import com.yu.common.core.domain.BaseEntity;

/**
 * 数据同步字段映射配置对象 dis_field_mapping
 *
 * 描述外部接口响应中某字段与本地业务表列的映射关系，
 * 供 D1 同步链路的"解析—落库"环节使用。
 *
 * @author ruoyi
 */
public class DisFieldMapping extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 映射ID */
    private Long mappingId;

    /** 接口ID（关联 dis_interface_config） */
    private Long interfaceId;

    /** 目标业务表名 */
    private String targetTable;

    /** 源字段名（响应JSON对象中的键，支持 a.b.c 嵌套路径） */
    private String sourceField;

    /** 目标列名 */
    private String targetColumn;

    /** 是否主键/唯一键（0否 1是），用于 upsert 判定 */
    private String keyFlag;

    /** 排序 */
    private Integer sortOrder;

    /** 状态（0正常 1停用） */
    private String status;

    public Long getMappingId() { return mappingId; }
    public void setMappingId(Long mappingId) { this.mappingId = mappingId; }

    public Long getInterfaceId() { return interfaceId; }
    public void setInterfaceId(Long interfaceId) { this.interfaceId = interfaceId; }

    public String getTargetTable() { return targetTable; }
    public void setTargetTable(String targetTable) { this.targetTable = targetTable; }

    public String getSourceField() { return sourceField; }
    public void setSourceField(String sourceField) { this.sourceField = sourceField; }

    public String getTargetColumn() { return targetColumn; }
    public void setTargetColumn(String targetColumn) { this.targetColumn = targetColumn; }

    public String getKeyFlag() { return keyFlag; }
    public void setKeyFlag(String keyFlag) { this.keyFlag = keyFlag; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
