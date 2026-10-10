package com.yu.portal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.portal.domain.PortalNotice;
import com.yu.portal.service.IPortalNoticeService;

/**
 * 教务通知前台接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验前台仅可见已发布内容：list 强制覆盖 publishStatus=1（外部参数不可越权看草稿），
 * detail 透传查询返回列表。通知内容渲染/权限由服务层与 Security 覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalNoticeFrontControllerMockMvcTest {

    @Mock
    private IPortalNoticeService portalNoticeService;

    @InjectMocks
    private PortalNoticeFrontController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("list：强制只查已发布（publishStatus=1），忽略外部传入状态")
    void list_forcesPublishedStatus() throws Exception {
        when(portalNoticeService.selectPortalNoticeList(any(PortalNotice.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/notice/list").param("publishStatus", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<PortalNotice> captor = ArgumentCaptor.forClass(PortalNotice.class);
        verify(portalNoticeService).selectPortalNoticeList(captor.capture());
        Assertions.assertEquals("1", captor.getValue().getPublishStatus());
    }

    @Test
    @DisplayName("detail：透传查询条件返回列表并置于 data")
    void detail_returnsListInData() throws Exception {
        PortalNotice n = new PortalNotice();
        n.setNoticeId(1L);
        when(portalNoticeService.selectPortalNoticeList(any(PortalNotice.class)))
                .thenReturn(List.of(n));

        mockMvc.perform(get("/portal/notice/detail").param("noticeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].noticeId").value(1));
    }
}
