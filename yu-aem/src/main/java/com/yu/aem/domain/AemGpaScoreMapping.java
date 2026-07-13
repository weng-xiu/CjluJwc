package com.yu.aem.domain;

import java.io.Serializable;
import java.math.BigDecimal;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * GPA分数段映射对象 aem_gpa_score_mapping
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public class AemGpaScoreMapping implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 映射ID */
    private Long mappingId;

    /** 算法配置ID */
    private Long configId;

    /** 最低分（含） */
    private BigDecimal minScore;

    /** 最高分（含） */
    private BigDecimal maxScore;

    /** 绩点值 */
    private BigDecimal gradePoint;

    /** 等级 */
    private String gradeLevel;

    /** 排序 */
    private Integer sortOrder;

    public Long getMappingId() { return mappingId; }
    public void setMappingId(Long mappingId) { this.mappingId = mappingId; }

    public Long getConfigId() { return configId; }
    public void setConfigId(Long configId) { this.configId = configId; }

    public BigDecimal getMinScore() { return minScore; }
    public void setMinScore(BigDecimal minScore) { this.minScore = minScore; }

    public BigDecimal getMaxScore() { return maxScore; }
    public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }

    public BigDecimal getGradePoint() { return gradePoint; }
    public void setGradePoint(BigDecimal gradePoint) { this.gradePoint = gradePoint; }

    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("mappingId", getMappingId())
            .append("configId", getConfigId())
            .append("minScore", getMinScore())
            .append("maxScore", getMaxScore())
            .append("gradePoint", getGradePoint())
            .append("gradeLevel", getGradeLevel())
            .append("sortOrder", getSortOrder())
            .toString();
    }
}
