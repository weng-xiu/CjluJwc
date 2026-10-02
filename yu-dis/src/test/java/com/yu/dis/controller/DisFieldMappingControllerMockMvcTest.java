package com.yu.dis.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.yu.dis.domain.DisFieldMapping;
import com.yu.dis.service.IDisFieldMappingService;

/**
 * 字段映射接口层测试（Q1 第十五批：MockMvc standalone，覆盖 yu-dis 字段映射控制器）。
 * 校验 CRUD 路由、逗号数组批量删除、list 查询条件绑定；
 * 注意本控制器 add/edit 未加 @Validated（与同域 DisInterfaceConfig 不同），
 * 缺失字段不会被 400 拦截——用例按现状契约固化该差异。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DisFieldMappingControllerMockMvcTest {

    @Mock
    private IDisFieldMappingService disFieldMappingService;

    @InjectMocks
    private DisFieldMappingController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定mappingId并返回映射记录")
    void getInfo_returnsMapping() throws Exception {
        DisFieldMapping mapping = new DisFieldMapping();
        mapping.setMappingId(2L);
        mapping.setSourceField("XM");
        mapping.setTargetColumn("student_name");
        when(disFieldMappingService.selectDisFieldMappingByMappingId(eq(2L))).thenReturn(mapping);

        mockMvc.perform(get("/dis/fieldMapping/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sourceField").value("XM"))
                .andExpect(jsonPath("$.data.targetColumn").value("student_name"));
    }

    @Test
    @DisplayName("add：合法请求体透传落库")
    void add_validBodyPersists() throws Exception {
        when(disFieldMappingService.insertDisFieldMapping(any(DisFieldMapping.class))).thenReturn(1);

        String body = "{\"interfaceId\":3,\"sourceField\":\"XH\",\"targetColumn\":\"student_no\",\"keyFlag\":\"1\"}";
        mockMvc.perform(post("/dis/fieldMapping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<DisFieldMapping> captor = ArgumentCaptor.forClass(DisFieldMapping.class);
        verify(disFieldMappingService).insertDisFieldMapping(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(3L), captor.getValue().getInterfaceId());
        org.junit.jupiter.api.Assertions.assertEquals("1", captor.getValue().getKeyFlag());
    }

    @Test
    @DisplayName("add：未加@Validated，缺失interfaceId仍按现状直接落库（固化契约差异）")
    void add_withoutValidationStillPersists() throws Exception {
        when(disFieldMappingService.insertDisFieldMapping(any(DisFieldMapping.class))).thenReturn(1);

        mockMvc.perform(post("/dis/fieldMapping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceField\":\"XH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(disFieldMappingService).insertDisFieldMapping(any(DisFieldMapping.class));
    }

    @Test
    @DisplayName("edit：合法请求体透传更新，影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(disFieldMappingService.updateDisFieldMapping(any(DisFieldMapping.class))).thenReturn(0);

        String body = "{\"mappingId\":2,\"interfaceId\":3,\"sourceField\":\"XM\",\"targetColumn\":\"student_name\"}";
        mockMvc.perform(put("/dis/fieldMapping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(disFieldMappingService.deleteDisFieldMappingByMappingIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/dis/fieldMapping/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(disFieldMappingService).deleteDisFieldMappingByMappingIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(disFieldMappingService.selectDisFieldMappingList(any(DisFieldMapping.class))).thenReturn(List.of());

        mockMvc.perform(get("/dis/fieldMapping/list").param("interfaceId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<DisFieldMapping> captor = ArgumentCaptor.forClass(DisFieldMapping.class);
        verify(disFieldMappingService).selectDisFieldMappingList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(3L), captor.getValue().getInterfaceId());
    }
}
