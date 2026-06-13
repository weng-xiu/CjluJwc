package com.yu.system.domain;

import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 教师账号关联对象 sys_teacher_link
 *
 * @author yu
 */
public class SysTeacherLink extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 关联ID */
    private Long linkId;

    /** 用户ID */
    private Long userId;

    /** 教师ID(brm_teacher) */
    private Long teacherId;

    /** 教师工号 */
    @Excel(name = "教师工号")
    private String teacherCode;

    // ===== 非持久化字段 =====
    /** 用户名 */
    private String userName;

    /** 教师姓名 */
    private String teacherName;

    /** 职称 */
    private String title;

    /** 院系名称 */
    private String deptName;

    public Long getLinkId()
    {
        return linkId;
    }

    public void setLinkId(Long linkId)
    {
        this.linkId = linkId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getTeacherId()
    {
        return teacherId;
    }

    public void setTeacherId(Long teacherId)
    {
        this.teacherId = teacherId;
    }

    public String getTeacherCode()
    {
        return teacherCode;
    }

    public void setTeacherCode(String teacherCode)
    {
        this.teacherCode = teacherCode;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getTeacherName()
    {
        return teacherName;
    }

    public void setTeacherName(String teacherName)
    {
        this.teacherName = teacherName;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
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
