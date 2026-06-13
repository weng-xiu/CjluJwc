package com.yu.sam.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 学生学籍对象 sam_student
 * 
 * @author ruoyi
 * @date 2026-05-13
 */
public class SamStudent extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 学生ID */
    private Long studentId;

    /** 学号 */
    @Excel(name = "学号")
    private String studentNo;

    /** 姓名 */
    @Excel(name = "姓名")
    private String studentName;

    /** 性别（0男 1女） */
    @Excel(name = "性别", readConverterExp = "0=男,1=女")
    private String gender;

    /** 出生日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "出生日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date birthDate;

    /** 身份证号 */
    @Excel(name = "身份证号")
    private String idCard;

    /** 专业ID（关联brm_major） */
    @Excel(name = "专业ID")
    private Long majorId;

    /** 关联系统用户ID（关联sys_user） */
    @Excel(name = "用户ID")
    private Long userId;

    /** 院系ID（关联brm_department） */
    @Excel(name = "院系ID")
    private Long deptId;

    /** 班级ID（关联brm_class） */
    @Excel(name = "班级ID")
    private Long classId;

    /** 入学年份 */
    @Excel(name = "入学年份")
    private String enrollmentYear;

    /** 学历层次 */
    @Excel(name = "学历层次")
    private String educationLevel;

    /** 学籍状态（0在读 1休学 2退学 3毕业 4转出 5保留学籍） */
    @Excel(name = "学籍状态", readConverterExp = "0=在读,1=休学,2=退学,3=毕业,4=转出,5=保留学籍")
    private String studentStatus;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    @NotBlank(message = "学号不能为空")
    @Size(min = 0, max = 50, message = "学号长度不能超过50个字符")
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }

    @NotBlank(message = "姓名不能为空")
    @Size(min = 0, max = 100, message = "姓名长度不能超过100个字符")
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Date getBirthDate() { return birthDate; }
    public void setBirthDate(Date birthDate) { this.birthDate = birthDate; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

    @NotNull(message = "专业ID不能为空")
    public Long getMajorId() { return majorId; }
    public void setMajorId(Long majorId) { this.majorId = majorId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    @NotNull(message = "院系ID不能为空")
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    @NotNull(message = "班级ID不能为空")
    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }

    public String getEnrollmentYear() { return enrollmentYear; }
    public void setEnrollmentYear(String enrollmentYear) { this.enrollmentYear = enrollmentYear; }

    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }

    public String getStudentStatus() { return studentStatus; }
    public void setStudentStatus(String studentStatus) { this.studentStatus = studentStatus; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("studentId", getStudentId())
            .append("studentNo", getStudentNo())
            .append("studentName", getStudentName())
            .append("gender", getGender())
            .append("birthDate", getBirthDate())
            .append("idCard", getIdCard())
            .append("majorId", getMajorId())
            .append("userId", getUserId())
            .append("deptId", getDeptId())
            .append("classId", getClassId())
            .append("enrollmentYear", getEnrollmentYear())
            .append("educationLevel", getEducationLevel())
            .append("studentStatus", getStudentStatus())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
