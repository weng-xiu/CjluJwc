package com.yu.sam.domain.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 毕业要求达成度视图（含指标点明细）。
 *
 * @author N1
 */
public class ObeAttainmentVo
{
    /** 毕业要求ID */
    private Long grId;

    /** 毕业要求编号 */
    private String grCode;

    /** 毕业要求名称 */
    private String grName;

    /** 达成度目标值 */
    private Double targetValue;

    /** 达成度（0~1，保留4位） */
    private Double attainment;

    /** 是否达成（0否 1是） */
    private String achieved;

    /** 学生ID（个体口径时有值） */
    private Long studentId;

    /** 学生姓名（个体口径时有值） */
    private String studentName;

    /** 统计口径：student 个体 / cohort 群体 */
    private String scope;

    /** 指标点达成度明细 */
    private List<IndicatorAttainmentVo> indicators = new ArrayList<>();

    public Long getGrId() { return grId; }
    public void setGrId(Long grId) { this.grId = grId; }

    public String getGrCode() { return grCode; }
    public void setGrCode(String grCode) { this.grCode = grCode; }

    public String getGrName() { return grName; }
    public void setGrName(String grName) { this.grName = grName; }

    public Double getTargetValue() { return targetValue; }
    public void setTargetValue(Double targetValue) { this.targetValue = targetValue; }

    public Double getAttainment() { return attainment; }
    public void setAttainment(Double attainment) { this.attainment = attainment; }

    public String getAchieved() { return achieved; }
    public void setAchieved(String achieved) { this.achieved = achieved; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public List<IndicatorAttainmentVo> getIndicators() { return indicators; }
    public void setIndicators(List<IndicatorAttainmentVo> indicators) { this.indicators = indicators; }
}
