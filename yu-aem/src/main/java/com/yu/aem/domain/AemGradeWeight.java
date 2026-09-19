package com.yu.aem.domain;

import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotNull;

/**
 * 成绩权重配置对象 aem_grade_weight
 * A4：成绩总评权重可配置（按课程 > 按课程类别 > 全局默认 三级匹配）
 *
 * @author ruoyi
 * @date 2026-09-19
 */
public class AemGradeWeight extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 权重配置ID */
    private Long weightId;

    /** 课程ID（精确匹配，为空表示非课程级） */
    @Excel(name = "课程ID")
    private Long courseId;

    /** 课程类别（对应 tpm_course_library.course_category，为空表示非类别级） */
    @Excel(name = "课程类别")
    private String courseCategory;

    /** 平时成绩占比（百分数，如30表示30%） */
    @Excel(name = "平时占比")
    @NotNull(message = "平时占比不能为空")
    private Double regularRatio;

    /** 考试成绩占比（百分数，如70表示70%） */
    @Excel(name = "考试占比")
    @NotNull(message = "考试占比不能为空")
    private Double examRatio;

    /** 状态（0启用 1停用） */
    @Excel(name = "状态", readConverterExp = "0=启用,1=停用")
    private String status;

    /** 课程名称（非持久化，列表展示用） */
    @Excel(name = "课程名称")
    private String courseName;

    public Long getWeightId() { return weightId; }
    public void setWeightId(Long weightId) { this.weightId = weightId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getCourseCategory() { return courseCategory; }
    public void setCourseCategory(String courseCategory) { this.courseCategory = courseCategory; }
    public Double getRegularRatio() { return regularRatio; }
    public void setRegularRatio(Double regularRatio) { this.regularRatio = regularRatio; }
    public Double getExamRatio() { return examRatio; }
    public void setExamRatio(Double examRatio) { this.examRatio = examRatio; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    @Override
    public String toString() {
        return "AemGradeWeight{" +
                "weightId=" + weightId +
                ", courseId=" + courseId +
                ", courseCategory='" + courseCategory + '\'' +
                ", regularRatio=" + regularRatio +
                ", examRatio=" + examRatio +
                ", status='" + status + '\'' +
                '}';
    }
}
