package com.yu.common.utils.schedule;

/**
 * 时间段冲突检测工具类
 *
 * @author ruoyi
 */
public class TimeSlotUtils
{
    /** 这个类不能实例化 */
    private TimeSlotUtils()
    {
    }

    /**
     * 检测两个排课时间段是否冲突
     * 冲突条件：同一星期几 且 节次有交叉 且 周次有交叉
     *
     * @param weekDay1      时间段1的星期几（1-7）
     * @param startPeriod1  时间段1的开始节次
     * @param endPeriod1    时间段1的结束节次
     * @param startWeek1    时间段1的起始周
     * @param endWeek1      时间段1的结束周
     * @param weekDay2      时间段2的星期几（1-7）
     * @param startPeriod2  时间段2的开始节次
     * @param endPeriod2    时间段2的结束节次
     * @param startWeek2    时间段2的起始周
     * @param endWeek2      时间段2的结束周
     * @return true表示冲突，false表示不冲突
     */
    public static boolean hasTimeConflict(int weekDay1, int startPeriod1, int endPeriod1, int startWeek1, int endWeek1,
                                          int weekDay2, int startPeriod2, int endPeriod2, int startWeek2, int endWeek2)
    {
        // 星期几不同则不冲突
        if (weekDay1 != weekDay2)
        {
            return false;
        }
        // 节次无交叉则不冲突
        if (!hasPeriodsOverlap(startPeriod1, endPeriod1, startPeriod2, endPeriod2))
        {
            return false;
        }
        // 周次无交叉则不冲突
        if (!hasWeeksOverlap(startWeek1, endWeek1, startWeek2, endWeek2))
        {
            return false;
        }
        return true;
    }

    /**
     * 检测节次是否有交叉
     *
     * @param start1 节次1的开始
     * @param end1   节次1的结束
     * @param start2 节次2的开始
     * @param end2   节次2的结束
     * @return true表示有交叉，false表示无交叉
     */
    public static boolean hasPeriodsOverlap(int start1, int end1, int start2, int end2)
    {
        return start1 <= end2 && start2 <= end1;
    }

    /**
     * 检测周次是否有交叉
     *
     * @param startWeek1 周次1的起始周
     * @param endWeek1   周次1的结束周
     * @param startWeek2 周次2的起始周
     * @param endWeek2   周次2的结束周
     * @return true表示有交叉，false表示无交叉
     */
    public static boolean hasWeeksOverlap(int startWeek1, int endWeek1, int startWeek2, int endWeek2)
    {
        return startWeek1 <= endWeek2 && startWeek2 <= endWeek1;
    }
}
