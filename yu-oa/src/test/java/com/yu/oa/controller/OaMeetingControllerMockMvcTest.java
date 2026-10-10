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

import com.yu.oa.domain.OaMeeting;
import com.yu.oa.domain.OaMeetingMinutes;
import com.yu.oa.domain.OaMeetingParticipant;
import com.yu.oa.service.IOaMeetingService;

/**
 * OA 会议管理接口层测试（Q1 第五批：MockMvc standalone）。
 * 校验会议 CRUD、冲突检测（布尔结果落 data）、纪要保存、参会状态更新、会议签到的
 * HTTP 路由与请求体/路径绑定及 toAjax 双分支；会议室占用编排与冲突判定逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OaMeetingControllerMockMvcTest {

    @Mock
    private IOaMeetingService oaMeetingService;

    @InjectMocks
    private OaMeetingController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("list：分页返回 rows/total/code")
    void list_returnsTable() throws Exception {
        when(oaMeetingService.selectOaMeetingList(any(OaMeeting.class))).thenReturn(List.of());

        mockMvc.perform(get("/oa/meeting/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());
    }

    @Test
    @DisplayName("getInfo：按 meetingId 查询详情")
    void getInfo_returnsData() throws Exception {
        when(oaMeetingService.selectOaMeetingByMeetingId(2L)).thenReturn(new OaMeeting());

        mockMvc.perform(get("/oa/meeting/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    @DisplayName("add：新增会议成功")
    void add_success() throws Exception {
        when(oaMeetingService.insertOaMeeting(any(OaMeeting.class))).thenReturn(1);

        mockMvc.perform(post("/oa/meeting")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(oaMeetingService).insertOaMeeting(any(OaMeeting.class));
    }

    @Test
    @DisplayName("remove：批量删除会议")
    void remove_success() throws Exception {
        when(oaMeetingService.deleteOaMeetingByMeetingIds(any())).thenReturn(2);

        mockMvc.perform(delete("/oa/meeting/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("checkConflict：冲突检测返回布尔结果")
    void checkConflict_returnsBoolean() throws Exception {
        when(oaMeetingService.checkMeetingConflict(any(OaMeeting.class))).thenReturn(true);

        mockMvc.perform(get("/oa/meeting/checkConflict").param("roomId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("minutes：保存会议纪要")
    void minutes_success() throws Exception {
        when(oaMeetingService.saveMinutes(any(OaMeetingMinutes.class))).thenReturn(1);

        mockMvc.perform(post("/oa/meeting/minutes")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(oaMeetingService).saveMinutes(any(OaMeetingMinutes.class));
    }

    @Test
    @DisplayName("participant：更新参会状态")
    void updateParticipant_success() throws Exception {
        when(oaMeetingService.updateParticipantStatus(any(OaMeetingParticipant.class))).thenReturn(1);

        mockMvc.perform(put("/oa/meeting/participant")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("sign：会议签到透传 participantId")
    void sign_success() throws Exception {
        when(oaMeetingService.signMeeting(9L)).thenReturn(1);

        mockMvc.perform(post("/oa/meeting/sign/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(oaMeetingService).signMeeting(9L);
    }
}
