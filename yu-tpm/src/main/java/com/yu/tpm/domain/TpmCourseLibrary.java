package com.yu.tpm.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 课程库对象 tpm_course_library
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public class TpmCourseLibrary extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 课程ID */
    private Long courseId;

    /** 课程编码 */
    @Excel(name = "课程编码")
    private String courseCode;

    /** 课程名称 */
    @Excel(name = "课程名称")
    private String courseName;

    /** 英文名称 */
    @Excel(name = "英文名称")
    private String courseNameEn;

    /** 学分 */
    @Excel(name = "学分")
    private Double credit;

    /** 理论学时 */
    @Excel(name = "理论学时")
    private Integer theoryHours;

    /** 实践学时 */
    @Excel(name = "实践学时")
    private Integer practiceHours;

    /** 总学时 */
    @Excel(name = "总学时")
    private Integer totalHours;

    /** 课程类型 */
    @Excel(name = "课程类型")
    private String courseType;

    /** 课程类别 */
    @Excel(name = "课程类别")
    private String courseCategory;

    /** 考核方式 */
    @Excel(name = "考核方式")
    private String assessmentMethod;

    /** 建议修读学期 */
    @Excel(name = "建议修读学期")
    private Integer semesterOrder;

    /** 所属方案ID */
    @Excel(name = "所属方案ID")
    private Long planId;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 所属培养方案名称（非持久化） */
    @Excel(name = "所属方案")
    private String planName;

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    @NotBlank(message = "课程编码不能为空")
    @Size(min = 0, max = 50, message = "课程编码长度不能超过50个字符")
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    @NotBlank(message = "课程名称不能为空")
    @Size(min = 0, max = 200, message = "课程名称长度不能超过200个字符")
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseNameEn() { return courseNameEn; }
    public void setCourseNameEn(String courseNameEn) { this.courseNameEn = courseNameEn; }

    public Double getCredit() { return credit; }
    public void setCredit(Double credit) { this.credit = credit; }

    public Integer getTheoryHours() { return theoryHours; }
    public void setTheoryHours(Integer theoryHours) { this.theoryHours = theoryHours; }

    public Integer getPracticeHours() { return practiceHours; }
    public void setPracticeHours(Integer practiceHours) { this.practiceHours = practiceHours; }

    public Integer getTotalHours() { return totalHours; }
    public void setTotalHours(Integer totalHours) { this.totalHours = totalHours; }

    public String getCourseType() { return courseType; }
    public void setCourseType(String courseType) { this.courseType = courseType; }

    public String getCourseCategory() { return courseCategory; }
    public void setCourseCategory(String courseCategory) { this.courseCategory = courseCategory; }

    public String getAssessmentMethod() { return assessmentMethod; }
    public void setAssessmentMethod(String assessmentMethod) { this.assessmentMethod = assessmentMethod; }

    public Integer getSemesterOrder() { return semesterOrder; }
    public void setSemesterOrder(Integer semesterOrder) { this.semesterOrder = semesterOrder; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("courseId", getCourseId())
            .append("courseCode", getCourseCode())
            .append("courseName", getCourseName())
            .append("courseNameEn", getCourseNameEn())
            .append("credit", getCredit())
            .append("theoryHours", getTheoryHours())
            .append("practiceHours", getPracticeHours())
            .append("totalHours", getTotalHours())
            .append("courseType", getCourseType())
            .append("courseCategory", getCourseCategory())
            .append("assessmentMethod", getAssessmentMethod())
            .append("semesterOrder", getSemesterOrder())
            .append("planId", getPlanId())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("planName", getPlanName())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
