package com.yu.tpm.domain.dto;

/**
 * 教师禁排时间片（F2-1）：自动排课时作为硬约束屏蔽的 (星期×节次窗口)。
 * 由 TpmScheduleMapper.selectForbiddenSlotsBySemester 查询装配。
 *
 * @author ruoyi
 */
public class ForbiddenSlot
{
    /** 教师ID */
    private Long teacherId;

    /** 星期几（1-7） */
    private Integer weekDay;

    /** 开始节次 */
    private Integer startPeriod;

    /** 结束节次 */
    private Integer endPeriod;

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public Integer getWeekDay() { return weekDay; }
    public void setWeekDay(Integer weekDay) { this.weekDay = weekDay; }

    public Integer getStartPeriod() { return startPeriod; }
    public void setStartPeriod(Integer startPeriod) { this.startPeriod = startPeriod; }

    public Integer getEndPeriod() { return endPeriod; }
    public void setEndPeriod(Integer endPeriod) { this.endPeriod = endPeriod; }
}
