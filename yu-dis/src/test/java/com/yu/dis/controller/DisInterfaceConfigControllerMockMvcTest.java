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

import com.yu.dis.domain.DisInterfaceConfig;
import com.yu.dis.service.IDisInterfaceConfigService;

/**
 * 接口配置接口层测试（Q1 第十五批：MockMvc standalone，覆盖 yu-dis 接口配置控制器）。
 * 校验 CRUD 路由与 @Validated 必填校验（systemId @NotNull、interfaceName/requestMethod @NotBlank）、
 * add/edit 域对象透传、逗号数组批量删除、list 查询条件绑定；
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DisInterfaceConfigControllerMockMvcTest {

    @Mock
    private IDisInterfaceConfigService disInterfaceConfigService;

    @InjectMocks
    private DisInterfaceConfigController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定interfaceId并返回接口记录")
    void getInfo_returnsInterface() throws Exception {
        DisInterfaceConfig config = new DisInterfaceConfig();
        config.setInterfaceId(6L);
        config.setInterfaceName("学籍查询");
        when(disInterfaceConfigService.selectDisInterfaceConfigByInterfaceId(eq(6L))).thenReturn(config);

        mockMvc.perform(get("/dis/interface/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.interfaceName").value("学籍查询"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(disInterfaceConfigService.insertDisInterfaceConfig(any(DisInterfaceConfig.class))).thenReturn(1);

        String body = "{\"systemId\":1,\"interfaceName\":\"学籍查询\",\"requestMethod\":\"GET\",\"requestPath\":\"/api/student\"}";
        mockMvc.perform(post("/dis/interface")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<DisInterfaceConfig> captor = ArgumentCaptor.forClass(DisInterfaceConfig.class);
        verify(disInterfaceConfigService).insertDisInterfaceConfig(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getSystemId());
        org.junit.jupiter.api.Assertions.assertEquals("GET", captor.getValue().getRequestMethod());
    }

    @Test
    @DisplayName("add：缺失必填systemId被@NotNull拦截返回400，不落库")
    void add_missingSystemIdRejected() throws Exception {
        String body = "{\"interfaceName\":\"学籍查询\",\"requestMethod\":\"GET\"}";
        mockMvc.perform(post("/dis/interface")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(disInterfaceConfigService, never()).insertDisInterfaceConfig(any());
    }

    @Test
    @DisplayName("add：缺失必填requestMethod被@NotBlank拦截返回400，不落库")
    void add_missingMethodRejected() throws Exception {
        String body = "{\"systemId\":1,\"interfaceName\":\"学籍查询\"}";
        mockMvc.perform(post("/dis/interface")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(disInterfaceConfigService, never()).insertDisInterfaceConfig(any());
    }

    @Test
    @DisplayName("add：缺失必填interfaceName被@NotBlank拦截返回400，不落库")
    void add_missingNameRejected() throws Exception {
        String body = "{\"systemId\":1,\"requestMethod\":\"POST\"}";
        mockMvc.perform(post("/dis/interface")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(disInterfaceConfigService, never()).insertDisInterfaceConfig(any());
    }

    @Test
    @DisplayName("edit：合法请求体透传更新，影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(disInterfaceConfigService.updateDisInterfaceConfig(any(DisInterfaceConfig.class))).thenReturn(0);

        String body = "{\"interfaceId\":6,\"systemId\":1,\"interfaceName\":\"学籍查询\",\"requestMethod\":\"POST\"}";
        mockMvc.perform(put("/dis/interface")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(disInterfaceConfigService).updateDisInterfaceConfig(any(DisInterfaceConfig.class));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(disInterfaceConfigService.deleteDisInterfaceConfigByInterfaceIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/dis/interface/1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(disInterfaceConfigService).deleteDisInterfaceConfigByInterfaceIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(3, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(disInterfaceConfigService.selectDisInterfaceConfigList(any(DisInterfaceConfig.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/dis/interface/list").param("interfaceName", "学籍"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<DisInterfaceConfig> captor = ArgumentCaptor.forClass(DisInterfaceConfig.class);
        verify(disInterfaceConfigService).selectDisInterfaceConfigList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("学籍", captor.getValue().getInterfaceName());
    }
}
