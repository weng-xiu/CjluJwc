package com.yu.sam.domain.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 指标点达成度视图。
 *
 * @author N1
 */
public class IndicatorAttainmentVo
{
    /** 指标点ID */
    private Long ipId;

    /** 指标点编号 */
    private String ipCode;

    /** 指标点名称 */
    private String ipName;

    /** 在毕业要求中的权重 */
    private Double weight;

    /** 达成度目标值 */
    private Double targetValue;

    /** 达成度（0~1，保留4位） */
    private Double attainment;

    /** 是否达成（0否 1是） */
    private String achieved;

    /** 有效支撑课程数（有成绩的支撑课程数） */
    private int courseCount;

    /** 支撑课程明细 */
    private List<SupportCourseVo> courses = new ArrayList<>();

    public Long getIpId() { return ipId; }
    public void setIpId(Long ipId) { this.ipId = ipId; }

    public String getIpCode() { return ipCode; }
    public void setIpCode(String ipCode) { this.ipCode = ipCode; }

    public String getIpName() { return ipName; }
    public void setIpName(String ipName) { this.ipName = ipName; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public Double getTargetValue() { return targetValue; }
    public void setTargetValue(Double targetValue) { this.targetValue = targetValue; }

    public Double getAttainment() { return attainment; }
    public void setAttainment(Double attainment) { this.attainment = attainment; }

    public String getAchieved() { return achieved; }
    public void setAchieved(String achieved) { this.achieved = achieved; }

    public int getCourseCount() { return courseCount; }
    public void setCourseCount(int courseCount) { this.courseCount = courseCount; }

    public List<SupportCourseVo> getCourses() { return courses; }
    public void setCourses(List<SupportCourseVo> courses) { this.courses = courses; }

    /** 支撑课程明细 */
    public static class SupportCourseVo
    {
        private Long courseId;
        private String courseName;
        private Double supportWeight;
        private Double score;

        public SupportCourseVo() {
        }

        public SupportCourseVo(Long courseId, String courseName, Double supportWeight, Double score) {
            this.courseId = courseId;
            this.courseName = courseName;
            this.supportWeight = supportWeight;
            this.score = score;
        }

        public Long getCourseId() { return courseId; }
        public void setCourseId(Long courseId) { this.courseId = courseId; }

        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }

        public Double getSupportWeight() { return supportWeight; }
        public void setSupportWeight(Double supportWeight) { this.supportWeight = supportWeight; }

        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
    }
}
