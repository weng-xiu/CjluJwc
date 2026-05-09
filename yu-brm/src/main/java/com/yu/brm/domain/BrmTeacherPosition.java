package com.yu.brm.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 教师任职信息对象 brm_teacher_position
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public class BrmTeacherPosition extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 任职ID */
    private Long posId;

    /** 教师ID */
    private Long teacherId;

    /** 任职院系ID */
    @Excel(name = "任职院系ID")
    private Long deptId;

    /** 任职岗位 */
    @Excel(name = "任职岗位")
    private String positionTitle;

    /** 任职开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "任职开始日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startDate;

    /** 任职结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "任职结束日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endDate;

    /** 是否现任 */
    @Excel(name = "是否现任", readConverterExp = "0=是,1=否")
    private String isCurrent;

    public Long getPosId() { return posId; }
    public void setPosId(Long posId) { this.posId = posId; }

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public String getIsCurrent() { return isCurrent; }
    public void setIsCurrent(String isCurrent) { this.isCurrent = isCurrent; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("posId", getPosId())
            .append("teacherId", getTeacherId())
            .append("deptId", getDeptId())
            .append("positionTitle", getPositionTitle())
            .append("startDate", getStartDate())
            .append("endDate", getEndDate())
            .append("isCurrent", getIsCurrent())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
