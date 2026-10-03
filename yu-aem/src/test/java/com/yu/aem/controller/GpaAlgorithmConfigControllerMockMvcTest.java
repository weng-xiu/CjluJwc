package com.yu.aem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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

import com.yu.aem.domain.AemGpaAlgorithmConfig;
import com.yu.aem.domain.AemGpaScoreMapping;
import com.yu.aem.service.IAemGradeRecordService;
import com.yu.aem.service.IGpaAlgorithmConfigService;

/**
 * GPA算法配置接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验 /aem/gpaConfig 路由：@Validated 必填（algorithmCode/algorithmName）、
 * setDefault 走 toAjax、mappings 字面量段与 {configId} 消歧（GET 查询落data数组/POST 保存绑定List）、
 * recalculate 解析请求体半参并委托成绩服务批量重算（回执落msg）。
 * 算法映射保存与重算逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GpaAlgorithmConfigControllerMockMvcTest {

    @Mock
    private IGpaAlgorithmConfigService gpaAlgorithmConfigService;

    @Mock
    private IAemGradeRecordService aemGradeRecordService;

    @InjectMocks
    private GpaAlgorithmConfigController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定configId并返回配置数据体")
    void getInfo_returnsConfig() throws Exception {
        AemGpaAlgorithmConfig c = new AemGpaAlgorithmConfig();
        c.setConfigId(2L);
        c.setAlgorithmCode("GPA_STD");
        when(gpaAlgorithmConfigService.selectAlgorithmConfigById(eq(2L))).thenReturn(c);

        mockMvc.perform(get("/aem/gpaConfig/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.configId").value(2))
                .andExpect(jsonPath("$.data.algorithmCode").value("GPA_STD"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(gpaAlgorithmConfigService.insertAlgorithmConfig(any(AemGpaAlgorithmConfig.class))).thenReturn(1);

        mockMvc.perform(post("/aem/gpaConfig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"algorithmCode\":\"GPA_4_0\",\"algorithmName\":\"四点制\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<AemGpaAlgorithmConfig> captor = ArgumentCaptor.forClass(AemGpaAlgorithmConfig.class);
        verify(gpaAlgorithmConfigService).insertAlgorithmConfig(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("GPA_4_0", captor.getValue().getAlgorithmCode());
    }

    @Test
    @DisplayName("add：algorithmCode空白被@NotBlank拦截返回400，不落库")
    void add_blankCodeRejected() throws Exception {
        mockMvc.perform(post("/aem/gpaConfig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"algorithmCode\":\"\",\"algorithmName\":\"四点制\"}"))
                .andExpect(status().isBadRequest());

        verify(gpaAlgorithmConfigService, never()).insertAlgorithmConfig(any());
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(gpaAlgorithmConfigService.deleteAlgorithmConfigByIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/aem/gpaConfig/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(gpaAlgorithmConfigService).deleteAlgorithmConfigByIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("setDefault：路径configId走toAjax")
    void setDefault_bindsConfigId() throws Exception {
        when(gpaAlgorithmConfigService.setDefaultAlgorithm(eq(3L))).thenReturn(1);

        mockMvc.perform(post("/aem/gpaConfig/setDefault/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(gpaAlgorithmConfigService).setDefaultAlgorithm(eq(3L));
    }

    @Test
    @DisplayName("getMappings：字面量段消歧，List落data数组")
    void getMappings_returnsList() throws Exception {
        when(gpaAlgorithmConfigService.selectScoreMappings(eq(3L))).thenReturn(List.of(new AemGpaScoreMapping()));

        mockMvc.perform(get("/aem/gpaConfig/mappings/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());

        verify(gpaAlgorithmConfigService).selectScoreMappings(eq(3L));
    }

    @Test
    @DisplayName("saveMappings：请求体List绑定并透传configId")
    void saveMappings_bindsListBody() throws Exception {
        when(gpaAlgorithmConfigService.saveScoreMappings(eq(3L), anyList())).thenReturn(1);

        mockMvc.perform(post("/aem/gpaConfig/mappings/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"gradePoint\":4.0,\"minScore\":90}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<List<AemGpaScoreMapping>> captor = ArgumentCaptor.forClass(List.class);
        verify(gpaAlgorithmConfigService).saveScoreMappings(eq(3L), captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(1, captor.getValue().size());
    }

    @Test
    @DisplayName("recalculate：解析半参委托成绩服务批量重算，回执落msg")
    void recalculate_parsesParamsAndDelegates() throws Exception {
        mockMvc.perform(post("/aem/gpaConfig/recalculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"semesterId\":5,\"algorithmCode\":\"GPA_STD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("重算完成"));

        verify(aemGradeRecordService).batchRecalculateGpa(eq(5L), eq("GPA_STD"));
    }

    @Test
    @DisplayName("recalculate：请求体缺参时以null透传")
    void recalculate_nullParams() throws Exception {
        mockMvc.perform(post("/aem/gpaConfig/recalculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("重算完成"));

        verify(aemGradeRecordService).batchRecalculateGpa(eq(null), eq(null));
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(gpaAlgorithmConfigService.selectAlgorithmConfigList(any(AemGpaAlgorithmConfig.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/aem/gpaConfig/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        verify(gpaAlgorithmConfigService).selectAlgorithmConfigList(any(AemGpaAlgorithmConfig.class));
    }
}
