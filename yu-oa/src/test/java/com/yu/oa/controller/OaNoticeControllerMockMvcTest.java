package com.yu.oa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.yu.oa.domain.OaNotice;
import com.yu.oa.service.IOaNoticeService;

/**
 * OA 通知公告接口层测试（Q1 第五批：MockMvc standalone）。
 * 校验公告 CRUD 与发布/撤回/已读回执三类状态流转端点的 @PathVariable 绑定、
 * toAjax 成败双分支（影响行数 0 → code 500）；发布可见性与回执统计逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OaNoticeControllerMockMvcTest {

    @Mock
    private IOaNoticeService oaNoticeService;

    @InjectMocks
    private OaNoticeController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("list：公告分页返回")
    void list_returnsTable() throws Exception {
        when(oaNoticeService.selectOaNoticeList(any(OaNotice.class))).thenReturn(List.of());

        mockMvc.perform(get("/oa/notice/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());
    }

    @Test
    @DisplayName("getInfo：按 noticeId 查询详情")
    void getInfo_returnsData() throws Exception {
        OaNotice notice = new OaNotice();
        notice.setNoticeId(4L);
        when(oaNoticeService.selectOaNoticeByNoticeId(4L)).thenReturn(notice);

        mockMvc.perform(get("/oa/notice/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.noticeId").value(4));
    }

    @Test
    @DisplayName("add：新增公告成功")
    void add_success() throws Exception {
        when(oaNoticeService.insertOaNotice(any(OaNotice.class))).thenReturn(1);

        mockMvc.perform(post("/oa/notice")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("remove：批量删除公告")
    void remove_success() throws Exception {
        when(oaNoticeService.deleteOaNoticeByNoticeIds(any())).thenReturn(2);

        mockMvc.perform(delete("/oa/notice/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("publish：发布公告透传 noticeId")
    void publish_success() throws Exception {
        when(oaNoticeService.publishOaNotice(6L)).thenReturn(1);

        mockMvc.perform(post("/oa/notice/publish/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(oaNoticeService).publishOaNotice(6L);
    }

    @Test
    @DisplayName("revoke：撤回公告影响行数为 0 返回 500")
    void revoke_zeroRowsReturnsError() throws Exception {
        when(oaNoticeService.revokeOaNotice(7L)).thenReturn(0);

        mockMvc.perform(post("/oa/notice/revoke/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("read：已读回执透传 noticeId")
    void read_success() throws Exception {
        when(oaNoticeService.readOaNotice(8L)).thenReturn(1);

        mockMvc.perform(post("/oa/notice/read/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(oaNoticeService).readOaNotice(8L);
    }
}
