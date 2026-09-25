package com.yu.web.domain;

import com.yu.common.annotation.Excel;
import com.yu.common.annotation.Excel.ColumnType;

/**
 * 师生数据上报报表VO（P3，用于按教育部状态数据口径导出分院系师生汇总）。
 *
 * @author ruoyi
 * @date 2026-09-25
 */
public class SubjectReportVo
{
    /** 院系名称 */
    @Excel(name = "院系名称")
    private String deptName;

    /** 学籍总数 */
    @Excel(name = "学籍总数", cellType = ColumnType.NUMERIC)
    private Long totalStudent;

    /** 在读学生数 */
    @Excel(name = "在读学生数", cellType = ColumnType.NUMERIC)
    private Long enrolledStudent;

    /** 在职教师数 */
    @Excel(name = "在职教师数", cellType = ColumnType.NUMERIC)
    private Long teacherCount;

    /** 开课门次 */
    @Excel(name = "开课门次", cellType = ColumnType.NUMERIC)
    private Long offeringCount;

    /** 平均及格率(%) */
    @Excel(name = "平均及格率(%)")
    private String avgPassRate;

    public String getDeptName()
    {
        return deptName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public Long getTotalStudent()
    {
        return totalStudent;
    }

    public void setTotalStudent(Long totalStudent)
    {
        this.totalStudent = totalStudent;
    }

    public Long getEnrolledStudent()
    {
        return enrolledStudent;
    }

    public void setEnrolledStudent(Long enrolledStudent)
    {
        this.enrolledStudent = enrolledStudent;
    }

    public Long getTeacherCount()
    {
        return teacherCount;
    }

    public void setTeacherCount(Long teacherCount)
    {
        this.teacherCount = teacherCount;
    }

    public Long getOfferingCount()
    {
        return offeringCount;
    }

    public void setOfferingCount(Long offeringCount)
    {
        this.offeringCount = offeringCount;
    }

    public String getAvgPassRate()
    {
        return avgPassRate;
    }

    public void setAvgPassRate(String avgPassRate)
    {
        this.avgPassRate = avgPassRate;
    }
}
