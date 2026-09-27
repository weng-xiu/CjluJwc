package com.yu.tpm.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.yu.common.utils.schedule.TimeSlotUtils;

/**
 * 时间段冲突检测工具类单元测试（Q1 核心算法回归防护）。
 *
 * <p>TimeSlotUtils 是排课冲突检测、选课时间冲突判定、自动排课占用网格共同依赖的底层算法，
 * 之前无任何单测。本用例覆盖其三类判定的边界：星期、节次交叉、周次交叉的真值表与相邻不重叠边界。</p>
 */
class TimeSlotUtilsTest {

    @Nested
    @DisplayName("hasTimeConflict：星期/节次/周次三重条件")
    class HasTimeConflict {

        @Test
        @DisplayName("星期不同则必不冲突，即使节次周次完全重叠")
        void differentWeekDay_neverConflict() {
            assertFalse(TimeSlotUtils.hasTimeConflict(1, 1, 2, 1, 16, 2, 1, 2, 1, 16));
        }

        @Test
        @DisplayName("同星期、节次与周次均重叠则冲突")
        void sameDayOverlapAll_conflict() {
            assertTrue(TimeSlotUtils.hasTimeConflict(1, 1, 2, 1, 16, 1, 2, 3, 1, 16));
        }

        @Test
        @DisplayName("同星期、节次重叠但周次不相交则不冲突（单双周场景）")
        void periodOverlapButWeekDisjoint_noConflict() {
            assertFalse(TimeSlotUtils.hasTimeConflict(1, 1, 2, 1, 8, 1, 1, 2, 9, 16));
        }

        @Test
        @DisplayName("同星期、周次重叠但节次首尾相接（2-3 与 3-4 共享第3节）算冲突")
        void periodTouching_boundaryIsOverlap() {
            // start1<=end2 && start2<=end1 => 2<=4 && 3<=3 成立，节次区间为闭区间
            assertTrue(TimeSlotUtils.hasTimeConflict(3, 1, 2, 1, 16, 3, 2, 3, 1, 16));
        }

        @Test
        @DisplayName("同星期、节次完全不相交（1-2 与 3-4）不冲突")
        void periodDisjoint_noConflict() {
            assertFalse(TimeSlotUtils.hasTimeConflict(3, 1, 2, 1, 16, 3, 3, 4, 1, 16));
        }
    }

    @Nested
    @DisplayName("hasPeriodsOverlap / hasWeeksOverlap：闭区间相交")
    class IntervalOverlap {

        @Test
        @DisplayName("区间相交判定：包含端点相接")
        void closedIntervalTouch() {
            assertTrue(TimeSlotUtils.hasPeriodsOverlap(1, 3, 3, 5));
            assertTrue(TimeSlotUtils.hasWeeksOverlap(1, 8, 8, 16));
        }

        @Test
        @DisplayName("区间相离判定：end < start 无交集")
        void closedIntervalDisjoint() {
            assertFalse(TimeSlotUtils.hasPeriodsOverlap(1, 2, 3, 4));
            assertFalse(TimeSlotUtils.hasWeeksOverlap(1, 8, 9, 16));
        }

        @Test
        @DisplayName("完全包含：小区间落入大区间内必相交")
        void containment() {
            assertTrue(TimeSlotUtils.hasPeriodsOverlap(3, 4, 1, 8));
            assertTrue(TimeSlotUtils.hasWeeksOverlap(5, 6, 1, 16));
        }
    }
}
