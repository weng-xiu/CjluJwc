package com.yu.tpm.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.yu.brm.mapper.BrmClassroomMapper;
import com.yu.tpm.domain.dto.AutoScheduleItem;
import com.yu.tpm.domain.dto.SchedulableClassroom;
import com.yu.tpm.domain.dto.ScheduleCandidate;
import com.yu.tpm.mapper.TpmCourseOfferingMapper;
import com.yu.tpm.mapper.TpmScheduleMapper;

/**
 * T1 时间片自动排课贪心算法单元测试（Q1 核心算法回归防护）。
 *
 * <p>覆盖：无候选/无教室的短路分支、理论课成功编排（dryRun 不落库）、容量不足失败归因、
 * 非 dryRun 落库调用。算法约束：硬约束（教室容量、教师/教室时段不冲突）+ 软约束（类型/校区/楼宇/容量贴合评分）。</p>
 */
@ExtendWith(MockitoExtension.class)
class ScheduleAutoTimetableTest {

    @Mock private TpmScheduleMapper scheduleMapper;
    @Mock private BrmClassroomMapper classroomMapper;
    @Mock private TpmCourseOfferingMapper courseOfferingMapper;

    @InjectMocks
    private ScheduleOptimizationServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "defaultCapacity", 30);
        ReflectionTestUtils.setField(service, "cfgDefaultWeeklyHours", 2);
    }

    private ScheduleCandidate candidate(Long offeringId, Integer maxStudents, Integer totalHours,
                                        Integer practiceHours, Long teacherId) {
        ScheduleCandidate c = new ScheduleCandidate();
        c.setOfferingId(offeringId);
        c.setCourseName("课程" + offeringId);
        c.setMaxStudents(maxStudents);
        c.setTotalHours(totalHours);
        c.setPracticeHours(practiceHours);
        c.setTeacherId(teacherId);
        c.setCampusId(1L);
        return c;
    }

    private SchedulableClassroom room(Long id, Integer capacity, String typeName) {
        SchedulableClassroom r = new SchedulableClassroom();
        r.setClassroomId(id);
        r.setClassroomName("教室" + id);
        r.setCapacity(capacity);
        r.setTypeName(typeName);
        r.setBuildingId(1L);
        r.setCampusId(1L);
        return r;
    }

    @Test
    @DisplayName("无待排开课：直接返回零统计")
    void noCandidates() {
        when(courseOfferingMapper.selectOfferingsToSchedule(1L)).thenReturn(new ArrayList<>());
        Map<String, Object> r = service.autoScheduleTimetable(1L, true, 5, 8, 2, 16);
        assertEquals(0, r.get("totalCandidates"));
        assertTrue(((String) r.get("message")).contains("没有需要自动排课"));
    }

    @Test
    @DisplayName("有候选但无可用教室：全部失败并归因")
    void noRooms_allFailed() {
        when(courseOfferingMapper.selectOfferingsToSchedule(1L))
                .thenReturn(new ArrayList<>(Arrays.asList(candidate(10L, 40, 32, 0, 5L))));
        when(scheduleMapper.selectSchedulableClassrooms()).thenReturn(new ArrayList<>());
        Map<String, Object> r = service.autoScheduleTimetable(1L, true, 5, 8, 2, 16);
        assertEquals(1, r.get("failedOfferings"));
        assertEquals(0, r.get("scheduledOfferings"));
    }

    @Test
    @DisplayName("理论课成功编排：dryRun 预览不落库，会话数=周课时/连堂节数")
    void theoryCourse_previewOnly() {
        when(courseOfferingMapper.selectOfferingsToSchedule(1L))
                .thenReturn(new ArrayList<>(Arrays.asList(candidate(10L, 40, 32, 0, 5L))));
        when(scheduleMapper.selectSchedulableClassrooms())
                .thenReturn(new ArrayList<>(Arrays.asList(room(1L, 50, "普通教室"))));
        when(scheduleMapper.selectSchedulesBySemester(1L)).thenReturn(new ArrayList<>());

        // totalHours 32 / weeks 16 = 周2课时；periodsPerSession 2 => 1 次会话/周
        Map<String, Object> r = service.autoScheduleTimetable(1L, true, 5, 8, 2, 16);

        assertEquals(1, r.get("scheduledOfferings"));
        assertEquals(1, r.get("totalSessions"));
        List<AutoScheduleItem> items = (List<AutoScheduleItem>) r.get("items");
        assertEquals(1L, items.get(0).getClassroomId());
        verify(scheduleMapper, times(0)).insertTpmSchedule(any());
    }

    @Test
    @DisplayName("容量不足：无满足 reqCap 的教室则失败")
    void capacityInsufficient_fails() {
        when(courseOfferingMapper.selectOfferingsToSchedule(1L))
                .thenReturn(new ArrayList<>(Arrays.asList(candidate(10L, 100, 32, 0, 5L))));
        when(scheduleMapper.selectSchedulableClassrooms())
                .thenReturn(new ArrayList<>(Arrays.asList(room(1L, 50, "普通教室")))); // 50 < 100
        when(scheduleMapper.selectSchedulesBySemester(1L)).thenReturn(new ArrayList<>());

        Map<String, Object> r = service.autoScheduleTimetable(1L, true, 5, 8, 2, 16);

        assertEquals(0, r.get("scheduledOfferings"));
        assertEquals(1, r.get("failedOfferings"));
    }

    @Test
    @DisplayName("非 dryRun：成功编排后逐条落库")
    void applyMode_persists() {
        when(courseOfferingMapper.selectOfferingsToSchedule(1L))
                .thenReturn(new ArrayList<>(Arrays.asList(candidate(10L, 40, 32, 0, 5L))));
        when(scheduleMapper.selectSchedulableClassrooms())
                .thenReturn(new ArrayList<>(Arrays.asList(room(1L, 50, "普通教室"))));
        when(scheduleMapper.selectSchedulesBySemester(1L)).thenReturn(new ArrayList<>());
        when(scheduleMapper.insertTpmSchedule(any())).thenReturn(1);

        Map<String, Object> r = service.autoScheduleTimetable(1L, false, 5, 8, 2, 16);

        assertEquals(1, r.get("inserted"));
        verify(scheduleMapper, times(1)).insertTpmSchedule(any());
    }

    @Test
    @DisplayName("同一教师两次会话不排在相同时段（教师硬约束）")
    void teacherConstraint_noOverlap() {
        // 周4课时 / 连堂2节 = 2 次会话，同一教师
        when(courseOfferingMapper.selectOfferingsToSchedule(1L))
                .thenReturn(new ArrayList<>(Arrays.asList(candidate(10L, 40, 64, 0, 5L))));
        when(scheduleMapper.selectSchedulableClassrooms())
                .thenReturn(new ArrayList<>(Arrays.asList(room(1L, 50, "普通教室"), room(2L, 50, "普通教室"))));
        when(scheduleMapper.selectSchedulesBySemester(1L)).thenReturn(new ArrayList<>());

        Map<String, Object> r = service.autoScheduleTimetable(1L, true, 5, 8, 2, 16);

        List<AutoScheduleItem> items = (List<AutoScheduleItem>) r.get("items");
        assertEquals(2, items.size());
        // 两次会话的(星期,起始节)不应完全相同
        boolean distinct = !(items.get(0).getWeekDay().equals(items.get(1).getWeekDay())
                && items.get(0).getStartPeriod().equals(items.get(1).getStartPeriod()));
        assertTrue(distinct, "同教师两次会话应落在不同时间片");
    }
}
