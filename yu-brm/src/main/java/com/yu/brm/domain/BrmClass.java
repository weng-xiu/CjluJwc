package com.yu.brm.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 班级对象 brm_class
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public class BrmClass extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 班级ID */
    private Long classId;

    /** 班级编码 */
    @Excel(name = "班级编码")
    private String classCode;

    /** 班级名称 */
    @Excel(name = "班级名称")
    private String className;

    /** 所属专业ID */
    @Excel(name = "所属专业ID")
    private Long majorId;

    /** 年级 */
    @Excel(name = "年级")
    private String grade;

    /** 学生人数 */
    @Excel(name = "学生人数")
    private Integer studentCount;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }

    @NotBlank(message = "班级编码不能为空")
    @Size(min = 0, max = 50, message = "班级编码长度不能超过50个字符")
    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }

    @NotBlank(message = "班级名称不能为空")
    @Size(min = 0, max = 100, message = "班级名称长度不能超过100个字符")
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public Long getMajorId() { return majorId; }
    public void setMajorId(Long majorId) { this.majorId = majorId; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public Integer getStudentCount() { return studentCount; }
    public void setStudentCount(Integer studentCount) { this.studentCount = studentCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("classId", getClassId())
            .append("classCode", getClassCode())
            .append("className", getClassName())
            .append("majorId", getMajorId())
            .append("grade", getGrade())
            .append("studentCount", getStudentCount())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
