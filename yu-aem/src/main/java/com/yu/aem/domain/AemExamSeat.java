package com.yu.aem.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 考场座位编排对象 aem_exam_seat
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public class AemExamSeat extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 座位ID */
    private Long seatId;

    /** 考试ID */
    @Excel(name = "考试ID")
    private Long examId;

    /** 教室ID */
    @Excel(name = "教室ID")
    private Long classroomId;

    /** 学生ID */
    @Excel(name = "学生ID")
    private Long studentId;

    /** 座位号 */
    @Excel(name = "座位号")
    private Integer seatNumber;

    /** 行号 */
    @Excel(name = "行号")
    private Integer rowNumber;

    /** 列号 */
    @Excel(name = "列号")
    private Integer colNumber;

    /** 状态（0正常 1缺考） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=缺考")
    private String status;

    public Long getSeatId() { return seatId; }
    public void setSeatId(Long seatId) { this.seatId = seatId; }

    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }

    public Long getClassroomId() { return classroomId; }
    public void setClassroomId(Long classroomId) { this.classroomId = classroomId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Integer getSeatNumber() { return seatNumber; }
    public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }

    public Integer getRowNumber() { return rowNumber; }
    public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }

    public Integer getColNumber() { return colNumber; }
    public void setColNumber(Integer colNumber) { this.colNumber = colNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("seatId", getSeatId())
            .append("examId", getExamId())
            .append("classroomId", getClassroomId())
            .append("studentId", getStudentId())
            .append("seatNumber", getSeatNumber())
            .append("rowNumber", getRowNumber())
            .append("colNumber", getColNumber())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
