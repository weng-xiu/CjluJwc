package com.yu.tpm.controller;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.tpm.domain.dto.AvailableClassroom;
import com.yu.tpm.domain.dto.ScheduleConflict;
import com.yu.tpm.service.IScheduleOptimizationService;

/**
 * 排课优化接口层测试（Q1 第三批：MockMvc standalone）。
 * 校验 HTTP 路由、参数绑定（@RequestParam 数值类型）、JSON 响应契约与 dryRun 标志正确透传，
 * 业务算法本身由 ScheduleAutoTimetableTest 等纯单测覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduleOptimizationControllerMockMvcTest {

    @Mock
    private IScheduleOptimizationService scheduleOptimizationService;

    @InjectMocks
    private ScheduleOptimizationController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private ScheduleConflict conflict(String type, String message) {
        ScheduleConflict c = new ScheduleConflict();
        c.setConflictType(type);
        c.setMessage(message);
        return c;
    }

    private AvailableClassroom classroom(Long id, String name, int capacity) {
        AvailableClassroom room = new AvailableClassroom();
        room.setClassroomId(id);
        room.setClassroomName(name);
        room.setCapacity(capacity);
        return room;
    }

    @Test
    @DisplayName("detectConflicts：返回200与冲突明细列表")
    void detectConflicts_returnsConflictList() throws Exception {
        when(scheduleOptimizationService.detectConflicts(eq(1L)))
                .thenReturn(List.of(conflict("CLASSROOM", "教室冲突")));

        mockMvc.perform(get("/tpm/scheduleOpt/detectConflicts").param("semesterId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", contains(hasItem("conflictType", "CLASSROOM"))));
    }

    @Test
    @DisplayName("canAssign：参数绑定为Integer并透传，返回布尔判定")
    void canAssign_bindsParamsAndReturnsBoolean() throws Exception {
        when(scheduleOptimizationService.canAssignClassroom(
                eq(5L), eq(1), eq(1), eq(2), eq(1), eq(16))).thenReturn(true);

        mockMvc.perform(get("/tpm/scheduleOpt/canAssign")
                        .param("classroomId", "5")
                        .param("weekDay", "1")
                        .param("startPeriod", "1")
                        .param("endPeriod", "2")
                        .param("startWeek", "1")
                        .param("endWeek", "16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("availableClassrooms：返回可用教室列表")
    void availableClassrooms_returnsRoomList() throws Exception {
        when(scheduleOptimizationService.findAvailableClassrooms(
                eq(60), eq(2), eq(3), eq(4), eq(1), eq(15)))
                .thenReturn(List.of(classroom(9L, "一教101", 80)));

        mockMvc.perform(get("/tpm/scheduleOpt/availableClassrooms")
                        .param("minCapacity", "60")
                        .param("weekDay", "2")
                        .param("startPeriod", "3")
                        .param("endPeriod", "4")
                        .param("startWeek", "1")
                        .param("endWeek", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].classroomId").value(9))
                .andExpect(jsonPath("$.data[0].classroomName").value("一教101"));
    }

    @Test
    @DisplayName("autoSchedulePreview：dryRun=true 预览不落库，可选参数缺省传null")
    void autoSchedulePreview_passesDryRunTrue() throws Exception {
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("scheduledOfferings", 3);
        when(scheduleOptimizationService.autoScheduleTimetable(
                eq(1L), eq(true), isNull(), isNull(), isNull(), isNull())).thenReturn(preview);

        mockMvc.perform(post("/tpm/scheduleOpt/autoSchedulePreview").param("semesterId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scheduledOfferings").value(3));

        verify(scheduleOptimizationService).autoScheduleTimetable(
                eq(1L), eq(true), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("autoScheduleApply：dryRun=false 落库，自定义参数完整透传")
    void autoScheduleApply_passesDryRunFalseWithParams() throws Exception {
        Map<String, Object> applied = new LinkedHashMap<>();
        applied.put("scheduledOfferings", 5);
        when(scheduleOptimizationService.autoScheduleTimetable(
                eq(1L), eq(false), eq(3), eq(4), eq(2), eq(16))).thenReturn(applied);

        mockMvc.perform(post("/tpm/scheduleOpt/autoScheduleApply")
                        .param("semesterId", "1")
                        .param("daysPerWeek", "3")
                        .param("periodsPerDay", "4")
                        .param("periodsPerSession", "2")
                        .param("totalWeeks", "16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scheduledOfferings").value(5));

        verify(scheduleOptimizationService).autoScheduleTimetable(
                eq(1L), eq(false), eq(3), eq(4), eq(2), eq(16));
    }

    @Test
    @DisplayName("checkSlotConflict：无冲突返回空数组")
    void checkSlotConflict_emptyWhenNoConflict() throws Exception {
        when(scheduleOptimizationService.checkTargetSlotConflicts(eq(7L), eq(3), eq(5), eq(6)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/tpm/scheduleOpt/checkSlotConflict")
                        .param("scheduleId", "7")
                        .param("weekDay", "3")
                        .param("startPeriod", "5")
                        .param("endPeriod", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("dragAdjust：force默认false，缺省时以false调用服务")
    void dragAdjust_defaultForceFalse() throws Exception {
        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("success", true);
        when(scheduleOptimizationService.applyDragAdjust(eq(7L), eq(3), eq(5), eq(6), eq(false)))
                .thenReturn(ok);

        mockMvc.perform(post("/tpm/scheduleOpt/dragAdjust")
                        .param("scheduleId", "7")
                        .param("weekDay", "3")
                        .param("startPeriod", "5")
                        .param("endPeriod", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));

        verify(scheduleOptimizationService).applyDragAdjust(eq(7L), eq(3), eq(5), eq(6), eq(false));
    }

    @Test
    @DisplayName("dragAdjust：force=true 时忽略冲突强制移动")
    void dragAdjust_forceTrue() throws Exception {
        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("success", true);
        when(scheduleOptimizationService.applyDragAdjust(eq(7L), eq(3), eq(5), eq(6), eq(true)))
                .thenReturn(ok);

        mockMvc.perform(post("/tpm/scheduleOpt/dragAdjust")
                        .param("scheduleId", "7")
                        .param("weekDay", "3")
                        .param("startPeriod", "5")
                        .param("endPeriod", "6")
                        .param("force", "true"))
                .andExpect(status().isOk());

        verify(scheduleOptimizationService).applyDragAdjust(eq(7L), eq(3), eq(5), eq(6), eq(true));
        verify(scheduleOptimizationService, never()).applyDragAdjust(any(), any(), any(), any(), eq(false));
    }

    /** 辅助断言：列表项包含指定键值（避免手写冗长 jsonPath）。 */
    private static org.hamcrest.Matcher<? extends Map<? extends String, ?>> hasItem(String key, Object value) {
        return org.hamcrest.Matchers.hasEntry(org.hamcrest.Matchers.is(key), org.hamcrest.Matchers.is(value));
    }
}
