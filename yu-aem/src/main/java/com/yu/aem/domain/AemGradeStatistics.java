package com.yu.aem.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 成绩统计分析对象 aem_grade_statistics
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public class AemGradeStatistics extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 统计ID */
    private Long statId;

    /** 课程ID */
    @Excel(name = "课程ID")
    private Long courseId;

    /** 学期ID */
    @Excel(name = "学期ID")
    private Long semesterId;

    /** 班级ID */
    @Excel(name = "班级ID")
    private Long classId;

    /** 总人数 */
    @Excel(name = "总人数")
    private Integer totalStudents;

    /** 最高分 */
    @Excel(name = "最高分")
    private Double maxScore;

    /** 最低分 */
    @Excel(name = "最低分")
    private Double minScore;

    /** 平均分 */
    @Excel(name = "平均分")
    private Double avgScore;

    /** 通过人数 */
    @Excel(name = "通过人数")
    private Integer passCount;

    /** 不及格人数 */
    @Excel(name = "不及格人数")
    private Integer failCount;

    /** 通过率（%） */
    @Excel(name = "通过率（%）")
    private Double passRate;

    /** 优秀人数 */
    @Excel(name = "优秀人数")
    private Integer excellentCount;

    /** 优秀率（%） */
    @Excel(name = "优秀率（%）")
    private Double excellentRate;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getStatId() { return statId; }
    public void setStatId(Long statId) { this.statId = statId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }

    public Integer getTotalStudents() { return totalStudents; }
    public void setTotalStudents(Integer totalStudents) { this.totalStudents = totalStudents; }

    public Double getMaxScore() { return maxScore; }
    public void setMaxScore(Double maxScore) { this.maxScore = maxScore; }

    public Double getMinScore() { return minScore; }
    public void setMinScore(Double minScore) { this.minScore = minScore; }

    public Double getAvgScore() { return avgScore; }
    public void setAvgScore(Double avgScore) { this.avgScore = avgScore; }

    public Integer getPassCount() { return passCount; }
    public void setPassCount(Integer passCount) { this.passCount = passCount; }

    public Integer getFailCount() { return failCount; }
    public void setFailCount(Integer failCount) { this.failCount = failCount; }

    public Double getPassRate() { return passRate; }
    public void setPassRate(Double passRate) { this.passRate = passRate; }

    public Integer getExcellentCount() { return excellentCount; }
    public void setExcellentCount(Integer excellentCount) { this.excellentCount = excellentCount; }

    public Double getExcellentRate() { return excellentRate; }
    public void setExcellentRate(Double excellentRate) { this.excellentRate = excellentRate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("statId", getStatId())
            .append("courseId", getCourseId())
            .append("semesterId", getSemesterId())
            .append("classId", getClassId())
            .append("totalStudents", getTotalStudents())
            .append("maxScore", getMaxScore())
            .append("minScore", getMinScore())
            .append("avgScore", getAvgScore())
            .append("passCount", getPassCount())
            .append("failCount", getFailCount())
            .append("passRate", getPassRate())
            .append("excellentCount", getExcellentCount())
            .append("excellentRate", getExcellentRate())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
