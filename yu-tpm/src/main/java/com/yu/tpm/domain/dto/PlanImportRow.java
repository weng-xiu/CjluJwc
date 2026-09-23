package com.yu.tpm.domain.dto;

import com.yu.common.annotation.Excel;

/**
 * 培养方案导入行（P7）：一行 = 一个方案 + 其课程编码清单
 * 业务唯一键：专业编码 + 方案年份 + 学历层次
 *
 * @author ruoyi
 * @date 2026-09-22
 */
public class PlanImportRow
{
    /** 方案名称 */
    @Excel(name = "方案名称")
    private String planName;

    /** 专业编码（brm_major.major_code） */
    @Excel(name = "专业编码")
    private String majorCode;

    /** 学历层次（可填字典标签如“本科”，也可填编码） */
    @Excel(name = "学历层次")
    private String educationLevel;

    /** 方案年份 */
    @Excel(name = "方案年份")
    private String planYear;

    /** 总学分 */
    @Excel(name = "总学分")
    private Double totalCredits;

    /** 版本号（可空，默认 V1） */
    @Excel(name = "版本号")
    private String version;

    /** 课程编码清单，多个以逗号/分号/顿号/空白分隔 */
    @Excel(name = "课程编码清单", width = 80)
    private String courseCodes;

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public String getMajorCode() { return majorCode; }
    public void setMajorCode(String majorCode) { this.majorCode = majorCode; }

    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }

    public String getPlanYear() { return planYear; }
    public void setPlanYear(String planYear) { this.planYear = planYear; }

    public Double getTotalCredits() { return totalCredits; }
    public void setTotalCredits(Double totalCredits) { this.totalCredits = totalCredits; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getCourseCodes() { return courseCodes; }
    public void setCourseCodes(String courseCodes) { this.courseCodes = courseCodes; }
}
