package com.yu.system.domain;

import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 学生信息对象 sys_student
 *
 * @author yu
 */
public class SysStudent extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 学生ID */
    private Long studentId;

    /** 关联用户ID */
    private Long userId;

    /** 学号 */
    @Excel(name = "学号")
    private String studentCode;

    /** 班级ID */
    private Long classId;

    /** 专业ID */
    private Long majorId;

    /** 院系ID */
    private Long deptId;

    /** 年级 */
    @Excel(name = "年级")
    private String grade;

    /** 入学年份 */
    @Excel(name = "入学年份")
    private Integer enrollmentYear;

    /** 学历层次(undergraduate/master/doctor) */
    @Excel(name = "学历层次")
    private String educationLevel;

    /** 学籍状态 */
    @Excel(name = "学籍状态")
    private String studentStatus;

    // ===== 非持久化字段，用于查询展示 =====
    /** 用户名 */
    private String userName;

    /** 真实姓名 */
    private String nickName;

    /** 班级名称 */
    private String className;

    /** 专业名称 */
    private String majorName;

    /** 院系名称 */
    private String deptName;

    public Long getStudentId()
    {
        return studentId;
    }

    public void setStudentId(Long studentId)
    {
        this.studentId = studentId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getStudentCode()
    {
        return studentCode;
    }

    public void setStudentCode(String studentCode)
    {
        this.studentCode = studentCode;
    }

    public Long getClassId()
    {
        return classId;
    }

    public void setClassId(Long classId)
    {
        this.classId = classId;
    }

    public Long getMajorId()
    {
        return majorId;
    }

    public void setMajorId(Long majorId)
    {
        this.majorId = majorId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getGrade()
    {
        return grade;
    }

    public void setGrade(String grade)
    {
        this.grade = grade;
    }

    public Integer getEnrollmentYear()
    {
        return enrollmentYear;
    }

    public void setEnrollmentYear(Integer enrollmentYear)
    {
        this.enrollmentYear = enrollmentYear;
    }

    public String getEducationLevel()
    {
        return educationLevel;
    }

    public void setEducationLevel(String educationLevel)
    {
        this.educationLevel = educationLevel;
    }

    public String getStudentStatus()
    {
        return studentStatus;
    }

    public void setStudentStatus(String studentStatus)
    {
        this.studentStatus = studentStatus;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public String getMajorName()
    {
        return majorName;
    }

    public void setMajorName(String majorName)
    {
        this.majorName = majorName;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }
}
