package com.yu.tpm.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.service.ITpmScheduleService;

/**
 * 排课管理接口层测试（Q1 第十三批：MockMvc standalone）。
 * 校验 HTTP 路由、@Validated 必填校验（offeringId）、增改删生命周期端点参数契约、
 * list 条件绑定与表格结构；冲突检测/自动排课/拖拽调整等算法契约由
 * ScheduleOptimizationControllerMockMvcTest 与 ScheduleAutoTimetableTest 覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TpmScheduleControllerMockMvcTest {

    @Mock
    private ITpmScheduleService tpmScheduleService;

    @InjectMocks
    private TpmScheduleController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定scheduleId并返回排课数据体")
    void getInfo_returnsSchedule() throws Exception {
        TpmSchedule schedule = new TpmSchedule();
        schedule.setScheduleId(21L);
        schedule.setOfferingId(3L);
        schedule.setCourseName("高等数学");
        when(tpmScheduleService.selectTpmScheduleByScheduleId(eq(21L))).thenReturn(schedule);

        mockMvc.perform(get("/tpm/schedule/21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.scheduleId").value(21))
                .andExpect(jsonPath("$.data.courseName").value("高等数学"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(tpmScheduleService.insertTpmSchedule(any(TpmSchedule.class))).thenReturn(1);

        String body = "{\"offeringId\":3,\"classroomId\":5,\"weekDay\":1,\"startPeriod\":1,\"endPeriod\":2}";
        mockMvc.perform(post("/tpm/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmSchedule> captor = ArgumentCaptor.forClass(TpmSchedule.class);
        verify(tpmScheduleService).insertTpmSchedule(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(3L), captor.getValue().getOfferingId());
    }

    @Test
    @DisplayName("add：缺失必填offeringId被@NotNull拦截返回400，不落库")
    void add_missingOfferingIdRejected() throws Exception {
        String body = "{\"classroomId\":5,\"weekDay\":1}";
        mockMvc.perform(post("/tpm/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(tpmScheduleService, never()).insertTpmSchedule(any());
    }

    @Test
    @DisplayName("edit：合法请求体走更新服务，时段字段透传")
    void edit_validBodyUpdates() throws Exception {
        when(tpmScheduleService.updateTpmSchedule(any(TpmSchedule.class))).thenReturn(1);

        String body = "{\"scheduleId\":21,\"offeringId\":3,\"weekDay\":2,\"startPeriod\":3,\"endPeriod\":4}";
        mockMvc.perform(put("/tpm/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmSchedule> captor = ArgumentCaptor.forClass(TpmSchedule.class);
        verify(tpmScheduleService).updateTpmSchedule(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(2), captor.getValue().getWeekDay());
    }

    @Test
    @DisplayName("edit：offeringId为空被@NotNull拦截返回400，不更新")
    void edit_missingOfferingIdRejected() throws Exception {
        String body = "{\"scheduleId\":21,\"weekDay\":2}";
        mockMvc.perform(put("/tpm/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(tpmScheduleService, never()).updateTpmSchedule(any());
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(tpmScheduleService.deleteTpmScheduleByScheduleIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/tpm/schedule/21,22,23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(tpmScheduleService).deleteTpmScheduleByScheduleIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(3, captor.getValue().length);
    }

    @Test
    @DisplayName("remove：影响行数0时toAjax降级为error(500)")
    void remove_zeroRowsDegrades() throws Exception {
        when(tpmScheduleService.deleteTpmScheduleByScheduleIds(any(Long[].class))).thenReturn(0);

        mockMvc.perform(delete("/tpm/schedule/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(tpmScheduleService.selectTpmScheduleList(any(TpmSchedule.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/tpm/schedule/list").param("courseName", "数学"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<TpmSchedule> captor = ArgumentCaptor.forClass(TpmSchedule.class);
        verify(tpmScheduleService).selectTpmScheduleList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("数学", captor.getValue().getCourseName());
    }
}
