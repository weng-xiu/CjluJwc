package com.yu.oa.controller;

import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.oa.domain.OaSchedule;
import com.yu.oa.service.IOaScheduleService;

/**
 * OA 日程安排接口层测试（Q1 第五批：MockMvc standalone）。
 * 校验日程标准 CRUD 的 HTTP 路由、@PathVariable/@RequestBody 绑定与 toAjax 成败双分支；
 * 日程数据本身无归属强制逻辑，业务规则由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OaScheduleControllerMockMvcTest {

    @Mock
    private IOaScheduleService oaScheduleService;

    @InjectMocks
    private OaScheduleController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("list：日程分页返回")
    void list_returnsTable() throws Exception {
        when(oaScheduleService.selectOaScheduleList(any(OaSchedule.class))).thenReturn(List.of());

        mockMvc.perform(get("/oa/schedule/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @DisplayName("getInfo：按 scheduleId 查询详情")
    void getInfo_returnsData() throws Exception {
        when(oaScheduleService.selectOaScheduleByScheduleId(2L)).thenReturn(new OaSchedule());

        mockMvc.perform(get("/oa/schedule/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    @DisplayName("add：新增日程成功")
    void add_success() throws Exception {
        when(oaScheduleService.insertOaSchedule(any(OaSchedule.class))).thenReturn(1);

        mockMvc.perform(post("/oa/schedule")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(oaScheduleService).insertOaSchedule(any(OaSchedule.class));
    }

    @Test
    @DisplayName("edit：更新日程成功")
    void edit_success() throws Exception {
        when(oaScheduleService.updateOaSchedule(any(OaSchedule.class))).thenReturn(1);

        mockMvc.perform(put("/oa/schedule")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("edit：影响行数为 0 返回 500")
    void edit_zeroRowsReturnsError() throws Exception {
        when(oaScheduleService.updateOaSchedule(any(OaSchedule.class))).thenReturn(0);

        mockMvc.perform(put("/oa/schedule")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：批量删除日程")
    void remove_success() throws Exception {
        when(oaScheduleService.deleteOaScheduleByScheduleIds(any())).thenReturn(2);

        mockMvc.perform(delete("/oa/schedule/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
