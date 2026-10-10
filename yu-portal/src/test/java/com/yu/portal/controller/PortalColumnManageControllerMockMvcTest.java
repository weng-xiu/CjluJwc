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

import com.yu.portal.domain.PortalColumn;
import com.yu.portal.service.IPortalColumnService;

/**
 * 门户栏目管理接口层测试（Q1 第十七批：MockMvc standalone）。
 * 覆盖栏目 CRUD 契约：list/getInfo 透传服务；add 受 @Validated 拦截（缺名称/编码 400、不落库）
 * 与成功分支；edit 的 toAjax 成败双分支；remove 走逗号串数组绑定。栏目层级/可见性等业务由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalColumnManageControllerMockMvcTest {

    @Mock
    private IPortalColumnService portalColumnService;

    @InjectMocks
    private PortalColumnManageController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("list：返回栏目分页行")
    void list_returnsPage() throws Exception {
        PortalColumn c = new PortalColumn();
        c.setColumnId(2L);
        when(portalColumnService.selectPortalColumnList(any(PortalColumn.class)))
                .thenReturn(List.of(c));

        mockMvc.perform(get("/portal/columnManage/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].columnId").value(2));
    }

    @Test
    @DisplayName("getInfo：按主键返回栏目")
    void getInfo_returnsColumn() throws Exception {
        PortalColumn c = new PortalColumn();
        c.setColumnId(4L);
        c.setColumnCode("news");
        when(portalColumnService.selectPortalColumnById(4L)).thenReturn(c);

        mockMvc.perform(get("/portal/columnManage/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.columnCode").value("news"));
    }

    @Test
    @DisplayName("add：合法请求体落库")
    void add_validBodySucceeds() throws Exception {
        when(portalColumnService.insertPortalColumn(any(PortalColumn.class))).thenReturn(1);

        mockMvc.perform(post("/portal/columnManage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columnName\":\"教务新闻\",\"columnCode\":\"news\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<PortalColumn> captor = ArgumentCaptor.forClass(PortalColumn.class);
        verify(portalColumnService).insertPortalColumn(captor.capture());
        Assertions.assertEquals("news", captor.getValue().getColumnCode());
    }

    @Test
    @DisplayName("add：缺栏目编码被 @Validated 拦截返回 400 且不落库")
    void add_missingCodeReturns400() throws Exception {
        mockMvc.perform(post("/portal/columnManage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columnName\":\"仅名称\"}"))
                .andExpect(status().isBadRequest());

        verify(portalColumnService, never()).insertPortalColumn(any());
    }

    @Test
    @DisplayName("edit：更新影响行数为 0 时返回 500")
    void edit_zeroRowsReturnsError() throws Exception {
        when(portalColumnService.updatePortalColumn(any(PortalColumn.class))).thenReturn(0);

        mockMvc.perform(put("/portal/columnManage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columnId\":4,\"columnName\":\"改\",\"columnCode\":\"c\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号串绑定 ID 数组批量删除")
    void remove_bindsIdArray() throws Exception {
        when(portalColumnService.deletePortalColumnByIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/portal/columnManage/7,8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(portalColumnService).deletePortalColumnByIds(captor.capture());
        Assertions.assertArrayEquals(new Long[] {7L, 8L}, captor.getValue());
    }
}
