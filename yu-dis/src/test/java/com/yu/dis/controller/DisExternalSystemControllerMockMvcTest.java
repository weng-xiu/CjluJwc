package com.yu.dis.controller;

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

import com.yu.dis.domain.DisExternalSystem;
import com.yu.dis.service.IDisExternalSystemService;

/**
 * 外部系统配置接口层测试（Q1 第十四批：MockMvc standalone）。
 * 校验路由 /dis/system、@Validated 必填校验（systemName/systemCode/systemType 三重 @NotBlank）、
 * add/edit 域对象透传、逗号数组批量删除、list 查询条件绑定。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DisExternalSystemControllerMockMvcTest {

    @Mock
    private IDisExternalSystemService disExternalSystemService;

    @InjectMocks
    private DisExternalSystemController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定systemId并返回系统数据体")
    void getInfo_returnsSystem() throws Exception {
        DisExternalSystem system = new DisExternalSystem();
        system.setSystemId(2L);
        system.setSystemName("研究生系统");
        when(disExternalSystemService.selectDisExternalSystemBySystemId(eq(2L))).thenReturn(system);

        mockMvc.perform(get("/dis/system/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.systemId").value(2))
                .andExpect(jsonPath("$.data.systemName").value("研究生系统"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(disExternalSystemService.insertDisExternalSystem(any(DisExternalSystem.class))).thenReturn(1);

        String body = "{\"systemName\":\"财务系统\",\"systemCode\":\"FIN\",\"systemType\":\"FINANCE\"}";
        mockMvc.perform(post("/dis/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<DisExternalSystem> captor = ArgumentCaptor.forClass(DisExternalSystem.class);
        verify(disExternalSystemService).insertDisExternalSystem(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("FIN", captor.getValue().getSystemCode());
        org.junit.jupiter.api.Assertions.assertEquals("FINANCE", captor.getValue().getSystemType());
    }

    @Test
    @DisplayName("add：缺失必填systemCode被@NotBlank拦截返回400，不落库")
    void add_missingSystemCodeRejected() throws Exception {
        String body = "{\"systemName\":\"财务系统\",\"systemType\":\"FINANCE\"}";
        mockMvc.perform(post("/dis/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(disExternalSystemService, never()).insertDisExternalSystem(any());
    }

    @Test
    @DisplayName("add：缺失必填systemType被@NotBlank拦截返回400，不落库")
    void add_missingSystemTypeRejected() throws Exception {
        String body = "{\"systemName\":\"财务系统\",\"systemCode\":\"FIN\"}";
        mockMvc.perform(post("/dis/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(disExternalSystemService, never()).insertDisExternalSystem(any());
    }

    @Test
    @DisplayName("edit：合法请求体透传更新，影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(disExternalSystemService.updateDisExternalSystem(any(DisExternalSystem.class))).thenReturn(0);

        String body = "{\"systemId\":2,\"systemName\":\"财务系统A\",\"systemCode\":\"FIN\",\"systemType\":\"FINANCE\"}";
        mockMvc.perform(put("/dis/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(disExternalSystemService).updateDisExternalSystem(any(DisExternalSystem.class));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(disExternalSystemService.deleteDisExternalSystemBySystemIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/dis/system/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(disExternalSystemService).deleteDisExternalSystemBySystemIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(disExternalSystemService.selectDisExternalSystemList(any(DisExternalSystem.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/dis/system/list").param("systemName", "系统"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<DisExternalSystem> captor = ArgumentCaptor.forClass(DisExternalSystem.class);
        verify(disExternalSystemService).selectDisExternalSystemList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("系统", captor.getValue().getSystemName());
    }
}
