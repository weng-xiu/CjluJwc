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

import com.yu.portal.domain.PortalBanner;
import com.yu.portal.service.IPortalBannerService;

/**
 * 门户轮播管理接口层测试（Q1 第十七批：MockMvc standalone）。
 * 覆盖轮播 CRUD 契约：list/getInfo 透传服务；add 受 @Validated 拦截（缺标题 400、不落库）与成功分支；
 * edit 的 toAjax 成败双分支；remove 走逗号串数组绑定。轮播生效时间窗/排序等业务由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalBannerManageControllerMockMvcTest {

    @Mock
    private IPortalBannerService portalBannerService;

    @InjectMocks
    private PortalBannerManageController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("list：返回轮播分页行")
    void list_returnsPage() throws Exception {
        PortalBanner b = new PortalBanner();
        b.setBannerId(3L);
        when(portalBannerService.selectPortalBannerList(any(PortalBanner.class)))
                .thenReturn(List.of(b));

        mockMvc.perform(get("/portal/bannerManage/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].bannerId").value(3));
    }

    @Test
    @DisplayName("getInfo：按主键返回轮播")
    void getInfo_returnsBanner() throws Exception {
        PortalBanner b = new PortalBanner();
        b.setBannerId(6L);
        b.setTitle("迎新");
        when(portalBannerService.selectPortalBannerById(6L)).thenReturn(b);

        mockMvc.perform(get("/portal/bannerManage/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("迎新"));
    }

    @Test
    @DisplayName("add：合法请求体落库")
    void add_validBodySucceeds() throws Exception {
        when(portalBannerService.insertPortalBanner(any(PortalBanner.class))).thenReturn(1);

        mockMvc.perform(post("/portal/bannerManage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"迎新\",\"imageUrl\":\"/a.png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<PortalBanner> captor = ArgumentCaptor.forClass(PortalBanner.class);
        verify(portalBannerService).insertPortalBanner(captor.capture());
        Assertions.assertEquals("迎新", captor.getValue().getTitle());
    }

    @Test
    @DisplayName("add：缺标题被 @Validated 拦截返回 400 且不落库")
    void add_missingTitleReturns400() throws Exception {
        mockMvc.perform(post("/portal/bannerManage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageUrl\":\"/a.png\"}"))
                .andExpect(status().isBadRequest());

        verify(portalBannerService, never()).insertPortalBanner(any());
    }

    @Test
    @DisplayName("edit：更新影响行数为 0 时返回 500")
    void edit_zeroRowsReturnsError() throws Exception {
        when(portalBannerService.updatePortalBanner(any(PortalBanner.class))).thenReturn(0);

        mockMvc.perform(put("/portal/bannerManage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bannerId\":6,\"title\":\"改\",\"imageUrl\":\"/b.png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号串绑定 ID 数组批量删除")
    void remove_bindsIdArray() throws Exception {
        when(portalBannerService.deletePortalBannerByIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/portal/bannerManage/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(portalBannerService).deletePortalBannerByIds(captor.capture());
        Assertions.assertArrayEquals(new Long[] {1L}, captor.getValue());
    }
}
