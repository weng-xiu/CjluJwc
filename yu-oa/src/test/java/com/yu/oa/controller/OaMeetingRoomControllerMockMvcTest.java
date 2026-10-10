package com.yu.oa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
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

import com.yu.oa.domain.OaMeeting;
import com.yu.oa.domain.OaMeetingRoom;
import com.yu.oa.service.IOaMeetingRoomService;
import com.yu.oa.service.IOaMeetingService;

/**
 * OA 会议室接口层测试（Q1 第五批：MockMvc standalone）。
 * 重点覆盖 O2 占用日历端点：合法 yyyy-MM-dd 解析为 [00:00, 次日00:00) 窗口查询、
 * 缺省日期取当天、非法格式短路返回错误（不调用服务）；以及会议室标准 CRUD。
 * 占用区间计算与冲突查询逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OaMeetingRoomControllerMockMvcTest {

    @Mock
    private IOaMeetingRoomService oaMeetingRoomService;

    @Mock
    private IOaMeetingService oaMeetingService;

    @InjectMocks
    private OaMeetingRoomController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("occupancy：合法日期解析为当天时间窗查询占用")
    void occupancy_validDateQueriesWindow() throws Exception {
        when(oaMeetingService.roomOccupancy(eq(5L), any(Date.class), any(Date.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/oa/meetingRoom/occupancy/5").param("date", "2026-10-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());

        verify(oaMeetingService).roomOccupancy(eq(5L), any(Date.class), any(Date.class));
    }

    @Test
    @DisplayName("occupancy：缺省日期时按当天查询（仍调用服务）")
    void occupancy_defaultToday() throws Exception {
        when(oaMeetingService.roomOccupancy(eq(6L), any(Date.class), any(Date.class)))
                .thenReturn(List.of(new OaMeeting()));

        mockMvc.perform(get("/oa/meetingRoom/occupancy/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());

        verify(oaMeetingService).roomOccupancy(eq(6L), any(Date.class), any(Date.class));
    }

    @Test
    @DisplayName("occupancy：非法日期格式短路返回错误，不查询服务")
    void occupancy_invalidDateRejected() throws Exception {
        mockMvc.perform(get("/oa/meetingRoom/occupancy/7").param("date", "not-a-date"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("日期格式不正确，应为 yyyy-MM-dd"));

        verify(oaMeetingService, never()).roomOccupancy(any(), any(), any());
    }

    @Test
    @DisplayName("list：会议室分页返回")
    void list_returnsTable() throws Exception {
        when(oaMeetingRoomService.selectOaMeetingRoomList(any(OaMeetingRoom.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/oa/meetingRoom/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());
    }

    @Test
    @DisplayName("getInfo：按 roomId 查询详情")
    void getInfo_returnsData() throws Exception {
        when(oaMeetingRoomService.selectOaMeetingRoomByRoomId(3L)).thenReturn(new OaMeetingRoom());

        mockMvc.perform(get("/oa/meetingRoom/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    @DisplayName("add：新增会议室成功")
    void add_success() throws Exception {
        when(oaMeetingRoomService.insertOaMeetingRoom(any(OaMeetingRoom.class))).thenReturn(1);

        mockMvc.perform(post("/oa/meetingRoom")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
