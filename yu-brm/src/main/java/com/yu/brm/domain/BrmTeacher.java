package com.yu.brm.domain;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 教师对象 brm_teacher
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public class BrmTeacher extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 教师ID */
    private Long teacherId;

    /** 教师工号 */
    @Excel(name = "教师工号")
    private String teacherCode;

    /** 教师姓名 */
    @Excel(name = "教师姓名")
    private String teacherName;

    /** 所属院系ID */
    @Excel(name = "所属院系ID")
    private Long deptId;

    /** 性别 */
    @Excel(name = "性别", readConverterExp = "0=男,1=女,2=未知")
    private String gender;

    /** 联系电话 */
    @Excel(name = "联系电话")
    private String phone;

    /** 邮箱 */
    @Excel(name = "邮箱")
    private String email;

    /** 职称 */
    @Excel(name = "职称")
    private String title;

    /** 学历 */
    @Excel(name = "学历")
    private String education;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 任职信息 */
    private List<BrmTeacherPosition> positionList;

    /** 授课资格信息 */
    private List<BrmTeacherQualification> qualificationList;

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    @NotBlank(message = "教师工号不能为空")
    @Size(min = 0, max = 50, message = "教师工号长度不能超过50个字符")
    public String getTeacherCode() { return teacherCode; }
    public void setTeacherCode(String teacherCode) { this.teacherCode = teacherCode; }

    @NotBlank(message = "教师姓名不能为空")
    @Size(min = 0, max = 50, message = "教师姓名长度不能超过50个字符")
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    @Size(min = 0, max = 20, message = "联系电话长度不能超过20个字符")
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    @Size(min = 0, max = 100, message = "邮箱长度不能超过100个字符")
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<BrmTeacherPosition> getPositionList() { return positionList; }
    public void setPositionList(List<BrmTeacherPosition> positionList) { this.positionList = positionList; }

    public List<BrmTeacherQualification> getQualificationList() { return qualificationList; }
    public void setQualificationList(List<BrmTeacherQualification> qualificationList) { this.qualificationList = qualificationList; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("teacherId", getTeacherId())
            .append("teacherCode", getTeacherCode())
            .append("teacherName", getTeacherName())
            .append("deptId", getDeptId())
            .append("gender", getGender())
            .append("phone", getPhone())
            .append("email", getEmail())
            .append("title", getTitle())
            .append("education", getEducation())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
