package com.yu.tpm.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yu.brm.mapper.BrmClassroomMapper;
import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.domain.dto.ScheduleConflict;
import com.yu.tpm.domain.dto.StudentScheduleSlot;
import com.yu.tpm.mapper.TpmScheduleMapper;

/**
 * 排课冲突检测单元测试（T2 修正）。
 * 验证：按学生名单精确判定班级冲突、消除跨开课误报、同开课多条排课不判冲突、教室冲突按星期几分桶。
 */
@ExtendWith(MockitoExtension.class)
class ScheduleOptimizationServiceImplTest {

    @Mock
    private TpmScheduleMapper scheduleMapper;

    @Mock
    private BrmClassroomMapper classroomMapper;

    @InjectMocks
    private ScheduleOptimizationServiceImpl service;

    private StudentScheduleSlot slot(Long student, Long offering, Long sched, int day, int sp, int ep, int sw, int ew, String course) {
        StudentScheduleSlot s = new StudentScheduleSlot();
        s.setStudentId(student);
        s.setOfferingId(offering);
        s.setScheduleId(sched);
        s.setWeekDay(day);
        s.setStartPeriod(sp);
        s.setEndPeriod(ep);
        s.setStartWeek(sw);
        s.setEndWeek(ew);
        s.setCourseName(course);
        return s;
    }

    private TpmSchedule schedule(Long id, Long offering, Long classroom, int day, int sp, int ep, int sw, int ew) {
        TpmSchedule s = new TpmSchedule();
        s.setScheduleId(id);
        s.setOfferingId(offering);
        s.setClassroomId(classroom);
        s.setWeekDay(day);
        s.setStartPeriod(sp);
        s.setEndPeriod(ep);
        s.setStartWeek(sw);
        s.setEndWeek(ew);
        return s;
    }

    private long countByType(List<ScheduleConflict> conflicts, String type) {
        return conflicts.stream().filter(c -> type.equals(c.getConflictType())).count();
    }

    @Test
    @DisplayName("T2：不同学生所选时间重叠的两门开课不再误报为班级冲突")
    void differentStudentsOverlap_noClassConflict() {
        when(scheduleMapper.selectSchedulesBySemester(eq(1L))).thenReturn(new ArrayList<>());
        // 学生1 选开课10(周一1-2节)，学生2 选开课20(周一1-2节)，时间重叠但无共享学生
        when(scheduleMapper.selectStudentScheduleSlotsBySemester(eq(1L))).thenReturn(Arrays.asList(
                slot(101L, 10L, 1L, 1, 1, 2, 1, 16, "A"),
                slot(102L, 20L, 2L, 1, 1, 2, 1, 16, "B")
        ));

        List<ScheduleConflict> conflicts = service.detectConflicts(1L);

        assertEquals(0, countByType(conflicts, "CLASS_CONFLICT"), "无共享学生不应报班级冲突（修正原误报）");
    }

    @Test
    @DisplayName("T2：同一学生跨开课时间重叠必报班级冲突且按排课对去重")
    void sameStudentOverlap_reportsOnce() {
        when(scheduleMapper.selectSchedulesBySemester(eq(1L))).thenReturn(new ArrayList<>());
        // 学生101 与 学生102 同时选了开课10 与 开课20（同一排课对），应仅报告一次
        when(scheduleMapper.selectStudentScheduleSlotsBySemester(eq(1L))).thenReturn(Arrays.asList(
                slot(101L, 10L, 1L, 1, 1, 2, 1, 16, "A"),
                slot(101L, 20L, 2L, 1, 1, 2, 1, 16, "B"),
                slot(102L, 10L, 1L, 1, 1, 2, 1, 16, "A"),
                slot(102L, 20L, 2L, 1, 1, 2, 1, 16, "B")
        ));

        List<ScheduleConflict> conflicts = service.detectConflicts(1L);

        assertEquals(1, countByType(conflicts, "CLASS_CONFLICT"), "同一排课对去重后仅一条");
        ScheduleConflict c = conflicts.get(0);
        assertTrue(c.getMessage().contains("学生"), "消息应指出涉事学生");
    }

    @Test
    @DisplayName("T2：同一开课的多条排课时间重叠不算班级冲突")
    void sameOfferingMultiSchedule_noConflict() {
        when(scheduleMapper.selectSchedulesBySemester(eq(1L))).thenReturn(new ArrayList<>());
        // 学生101 的开课10 有两条排课(周一/周二各一次)——不应相互判为班级冲突
        when(scheduleMapper.selectStudentScheduleSlotsBySemester(eq(1L))).thenReturn(Arrays.asList(
                slot(101L, 10L, 1L, 1, 1, 2, 1, 16, "A"),
                slot(101L, 10L, 2L, 3, 1, 2, 1, 16, "A")
        ));

        List<ScheduleConflict> conflicts = service.detectConflicts(1L);

        assertEquals(0, countByType(conflicts, "CLASS_CONFLICT"));
    }

    @Test
    @DisplayName("T2：教室冲突按星期几分桶——同教室同星期重叠必报，不同星期不报")
    void classroomConflict_bucketsByWeekDay() {
        // 同教室(5) 周一 1-2 与 2-3 节重叠 → 冲突
        List<TpmSchedule> sameDay = Arrays.asList(
                schedule(1L, 10L, 5L, 1, 1, 2, 1, 16),
                schedule(2L, 20L, 5L, 1, 2, 3, 1, 16)
        );
        when(scheduleMapper.selectSchedulesBySemester(eq(1L))).thenReturn(sameDay);
        when(scheduleMapper.selectStudentScheduleSlotsBySemester(eq(1L))).thenReturn(new ArrayList<>());

        List<ScheduleConflict> conflicts = service.detectConflicts(1L);
        assertEquals(1, countByType(conflicts, "CLASSROOM_CONFLICT"), "同教室同星期节次重叠应报教室冲突");

        // 不同星期则不冲突
        List<TpmSchedule> diffDay = Arrays.asList(
                schedule(1L, 10L, 5L, 1, 1, 2, 1, 16),
                schedule(2L, 20L, 5L, 2, 1, 2, 1, 16)
        );
        when(scheduleMapper.selectSchedulesBySemester(eq(2L))).thenReturn(diffDay);
        List<ScheduleConflict> conflicts2 = service.detectConflicts(2L);
        assertEquals(0, countByType(conflicts2, "CLASSROOM_CONFLICT"), "不同星期不应报教室冲突");
    }
}
