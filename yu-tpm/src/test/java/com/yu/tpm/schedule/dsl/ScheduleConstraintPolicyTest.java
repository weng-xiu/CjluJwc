package com.yu.tpm.schedule.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * N4 约束策略软约束评分单元验证：默认策略与既有贪心常量口径一致。
 *
 * @author N4
 */
class ScheduleConstraintPolicyTest
{
    @Test
    @DisplayName("实践课命中实验室=labMatch(100) 且同校区(+50)")
    void labMatchWithCampus()
    {
        ScheduleConstraintPolicy p = ScheduleConstraintPolicy.defaults();
        int s = p.scoreRoom(true, true, true, false, 100, 100);
        assertEquals(100 + 50, s);
    }

    @Test
    @DisplayName("理论课误占实验室=regularInLab(-10) 且同楼宇(+30)")
    void regularInLabSameBuilding()
    {
        ScheduleConstraintPolicy p = ScheduleConstraintPolicy.defaults();
        int s = p.scoreRoom(false, true, false, true, 100, 100);
        assertEquals(-10 + 30, s);
    }

    @Test
    @DisplayName("容量富余按 divisor 线性扣分")
    void capacityFitPenalty()
    {
        ScheduleConstraintPolicy p = ScheduleConstraintPolicy.defaults();
        // needsLab=false,普通教室: +5；富余容量 60 → -(60/10) = -6
        int s = p.scoreRoom(false, false, false, false, 110, 50);
        assertEquals(5 - 6, s);
    }

    @Test
    @DisplayName("divisor<=0 时安全回落为 1，避免除零")
    void divisorGuard()
    {
        ScheduleConstraintPolicy p = ScheduleConstraintPolicy.defaults().setCapacityFitDivisor(0);
        int s = p.scoreRoom(false, false, false, false, 60, 50);
        assertEquals(5 - (10 / 1), s);
    }
}
