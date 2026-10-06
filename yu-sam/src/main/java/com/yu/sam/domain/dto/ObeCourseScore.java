package com.yu.sam.domain.dto;

/**
 * 课程归一化得分载体（数据源查询结果，供达成度计算使用）。
 * score 为 0~1 的归一化得分（成绩/100）。
 *
 * @author N1
 */
public class ObeCourseScore
{
    /** 课程ID */
    private Long courseId;

    /** 归一化得分（0~1） */
    private Double score;

    public ObeCourseScore() {
    }

    public ObeCourseScore(Long courseId, Double score) {
        this.courseId = courseId;
        this.score = score;
    }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}
