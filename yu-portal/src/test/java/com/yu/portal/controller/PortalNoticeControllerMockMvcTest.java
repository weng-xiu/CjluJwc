package com.yu.portal.controller;

import static org.mockito.ArgumentMatchers.any;
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

import org.junit.jupiter.api.Assertions;
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

import com.yu.portal.domain.PortalNotice;
import com.yu.portal.service.IPortalNoticeService;

/**
 * 教务通知后台管理接口层测试（Q1 第十七批：MockMvc standalone）。
 * 覆盖通知 CRUD 契约：list/getInfo 透传服务；add 受 @Validated 拦截（缺必填 400、不落库）与成功分支；
 * edit 的 toAjax 成败双分支；remove 走逗号串数组绑定。导出端点依赖 ExcelUtil 输出流不在接口层覆盖，
 * 发布/置顶状态等业务由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalNoticeControllerMockMvcTest {

    @Mock
    private IPortalNoticeService portalNoticeService;

    @InjectMocks
    private PortalNoticeController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private String validNoticeBody() {
        return "{\"noticeTitle\":\"调课通知\",\"noticeType\":\"1\",\"publishStatus\":\"0\","
                + "\"targetRole\":\"1\",\"isTop\":\"0\"}";
    }

    @Test
    @DisplayName("list：返回通知分页行")
    void list_returnsPage() throws Exception {
        PortalNotice n = new PortalNotice();
        n.setNoticeId(2L);
        when(portalNoticeService.selectPortalNoticeList(any(PortalNotice.class)))
                .thenReturn(List.of(n));

        mockMvc.perform(get("/portal/noticeManage/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].noticeId").value(2));
    }

    @Test
    @DisplayName("getInfo：按主键返回通知")
    void getInfo_returnsNotice() throws Exception {
        PortalNotice n = new PortalNotice();
        n.setNoticeId(5L);
        n.setNoticeTitle("调课");
        when(portalNoticeService.selectPortalNoticeByNoticeId(5L)).thenReturn(n);

        mockMvc.perform(get("/portal/noticeManage/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.noticeTitle").value("调课"));
    }

    @Test
    @DisplayName("add：合法请求体落库")
    void add_validBodySucceeds() throws Exception {
        when(portalNoticeService.insertPortalNotice(any(PortalNotice.class))).thenReturn(1);

        mockMvc.perform(post("/portal/noticeManage")
                        .contentType(MediaType.APPLICATION_JSON).content(validNoticeBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<PortalNotice> captor = ArgumentCaptor.forClass(PortalNotice.class);
        verify(portalNoticeService).insertPortalNotice(captor.capture());
        Assertions.assertEquals("调课通知", captor.getValue().getNoticeTitle());
    }

    @Test
    @DisplayName("add：缺通知标题被 @Validated 拦截返回 400 且不落库")
    void add_missingTitleReturns400() throws Exception {
        mockMvc.perform(post("/portal/noticeManage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noticeType\":\"1\",\"publishStatus\":\"0\",\"targetRole\":\"1\",\"isTop\":\"0\"}"))
                .andExpect(status().isBadRequest());

        verify(portalNoticeService, never()).insertPortalNotice(any());
    }

    @Test
    @DisplayName("edit：更新影响行数为 0 时返回 500")
    void edit_zeroRowsReturnsError() throws Exception {
        when(portalNoticeService.updatePortalNotice(any(PortalNotice.class))).thenReturn(0);

        mockMvc.perform(put("/portal/noticeManage")
                        .contentType(MediaType.APPLICATION_JSON).content(validNoticeBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号串绑定 ID 数组批量删除")
    void remove_bindsIdArray() throws Exception {
        when(portalNoticeService.deletePortalNoticeByNoticeIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/portal/noticeManage/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(portalNoticeService).deletePortalNoticeByNoticeIds(captor.capture());
        Assertions.assertArrayEquals(new Long[] {1L, 2L}, captor.getValue());
    }
}
