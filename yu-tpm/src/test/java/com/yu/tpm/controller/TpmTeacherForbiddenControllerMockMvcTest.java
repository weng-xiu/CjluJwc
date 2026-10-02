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

import com.yu.tpm.domain.TpmTeacherForbidden;
import com.yu.tpm.service.ITpmTeacherForbiddenService;

/**
 * 教师禁排时间片接口层测试（Q1 第十三批：MockMvc standalone，覆盖 F2-1 新增控制器）。
 * 校验 HTTP 路由、@Validated 必填校验（teacherId/weekDay/startPeriod/endPeriod）、
 * 增改删生命周期端点参数契约、list 条件绑定与表格结构；
 * 禁排时间片如何进入排课硬约束由 ScheduleOptimizationServiceImpl 算法层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TpmTeacherForbiddenControllerMockMvcTest {

    @Mock
    private ITpmTeacherForbiddenService tpmTeacherForbiddenService;

    @InjectMocks
    private TpmTeacherForbiddenController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定forbiddenId并返回禁排数据体")
    void getInfo_returnsForbidden() throws Exception {
        TpmTeacherForbidden forbidden = new TpmTeacherForbidden();
        forbidden.setForbiddenId(31L);
        forbidden.setTeacherId(7L);
        forbidden.setTeacherName("王老师");
        when(tpmTeacherForbiddenService.selectTpmTeacherForbiddenByForbiddenId(eq(31L))).thenReturn(forbidden);

        mockMvc.perform(get("/tpm/teacherForbidden/31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.forbiddenId").value(31))
                .andExpect(jsonPath("$.data.teacherName").value("王老师"));
    }

    @Test
    @DisplayName("add：合法时间片通过@Validated并落库，星期/节次透传")
    void add_validBodyPersists() throws Exception {
        when(tpmTeacherForbiddenService.insertTpmTeacherForbidden(any(TpmTeacherForbidden.class))).thenReturn(1);

        String body = "{\"teacherId\":7,\"weekDay\":3,\"startPeriod\":1,\"endPeriod\":2}";
        mockMvc.perform(post("/tpm/teacherForbidden")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmTeacherForbidden> captor = ArgumentCaptor.forClass(TpmTeacherForbidden.class);
        verify(tpmTeacherForbiddenService).insertTpmTeacherForbidden(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(7L), captor.getValue().getTeacherId());
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(3), captor.getValue().getWeekDay());
    }

    @Test
    @DisplayName("add：缺失必填teacherId被@NotNull拦截返回400，不落库")
    void add_missingTeacherIdRejected() throws Exception {
        String body = "{\"weekDay\":3,\"startPeriod\":1,\"endPeriod\":2}";
        mockMvc.perform(post("/tpm/teacherForbidden")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(tpmTeacherForbiddenService, never()).insertTpmTeacherForbidden(any());
    }

    @Test
    @DisplayName("add：缺失必填weekDay被@NotNull拦截返回400，不落库")
    void add_missingWeekDayRejected() throws Exception {
        String body = "{\"teacherId\":7,\"startPeriod\":1,\"endPeriod\":2}";
        mockMvc.perform(post("/tpm/teacherForbidden")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(tpmTeacherForbiddenService, never()).insertTpmTeacherForbidden(any());
    }

    @Test
    @DisplayName("edit：合法请求体走更新服务，节次区间透传")
    void edit_validBodyUpdates() throws Exception {
        when(tpmTeacherForbiddenService.updateTpmTeacherForbidden(any(TpmTeacherForbidden.class))).thenReturn(1);

        String body = "{\"forbiddenId\":31,\"teacherId\":7,\"weekDay\":4,\"startPeriod\":5,\"endPeriod\":6}";
        mockMvc.perform(put("/tpm/teacherForbidden")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmTeacherForbidden> captor = ArgumentCaptor.forClass(TpmTeacherForbidden.class);
        verify(tpmTeacherForbiddenService).updateTpmTeacherForbidden(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(6), captor.getValue().getEndPeriod());
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(tpmTeacherForbiddenService.deleteTpmTeacherForbiddenByForbiddenIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/tpm/teacherForbidden/31,32"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(tpmTeacherForbiddenService).deleteTpmTeacherForbiddenByForbiddenIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("remove：影响行数0时toAjax降级为error(500)")
    void remove_zeroRowsDegrades() throws Exception {
        when(tpmTeacherForbiddenService.deleteTpmTeacherForbiddenByForbiddenIds(any(Long[].class))).thenReturn(0);

        mockMvc.perform(delete("/tpm/teacherForbidden/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(tpmTeacherForbiddenService.selectTpmTeacherForbiddenList(any(TpmTeacherForbidden.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/tpm/teacherForbidden/list").param("teacherName", "王"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<TpmTeacherForbidden> captor = ArgumentCaptor.forClass(TpmTeacherForbidden.class);
        verify(tpmTeacherForbiddenService).selectTpmTeacherForbiddenList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("王", captor.getValue().getTeacherName());
    }
}
