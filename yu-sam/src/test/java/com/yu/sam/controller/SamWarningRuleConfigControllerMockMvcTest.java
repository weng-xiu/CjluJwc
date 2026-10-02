package com.yu.sam.controller;

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

import com.yu.sam.domain.SamWarningRuleConfig;
import com.yu.sam.service.ISamWarningRuleConfigService;

/**
 * 预警规则配置接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-sam S6 预警规则控制器）。
 * 标准 CRUD，控制器未使用登录上下文，无需注入 SecurityContext。校验路由 /sam/warningRule、
 * @Validated 必填校验（ruleCode/ruleName @NotBlank）、服务方法采用 ById/ByIds 命名、
 * 逗号数组批量删除、list 查询绑定与表格结构。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamWarningRuleConfigControllerMockMvcTest {

    @Mock
    private ISamWarningRuleConfigService samWarningRuleConfigService;

    @InjectMocks
    private SamWarningRuleConfigController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定ruleId并返回规则配置")
    void getInfo_returnsRule() throws Exception {
        SamWarningRuleConfig rule = new SamWarningRuleConfig();
        rule.setRuleId(3L);
        rule.setRuleName("GPA红线");
        when(samWarningRuleConfigService.selectSamWarningRuleConfigById(eq(3L))).thenReturn(rule);

        mockMvc.perform(get("/sam/warningRule/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ruleName").value("GPA红线"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(samWarningRuleConfigService.insertSamWarningRuleConfig(any(SamWarningRuleConfig.class))).thenReturn(1);

        mockMvc.perform(post("/sam/warningRule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruleCode\":\"R_GPA\",\"ruleName\":\"GPA红线\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samWarningRuleConfigService).insertSamWarningRuleConfig(any(SamWarningRuleConfig.class));
    }

    @Test
    @DisplayName("add：缺失必填ruleName被@NotBlank拦截返回400，不落库")
    void add_missingRuleNameRejected() throws Exception {
        mockMvc.perform(post("/sam/warningRule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruleCode\":\"R_GPA\"}"))
                .andExpect(status().isBadRequest());

        verify(samWarningRuleConfigService, never()).insertSamWarningRuleConfig(any());
    }

    @Test
    @DisplayName("add：缺失必填ruleCode被@NotBlank拦截返回400，不落库")
    void add_missingRuleCodeRejected() throws Exception {
        mockMvc.perform(post("/sam/warningRule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruleName\":\"GPA红线\"}"))
                .andExpect(status().isBadRequest());

        verify(samWarningRuleConfigService, never()).insertSamWarningRuleConfig(any());
    }

    @Test
    @DisplayName("edit：影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(samWarningRuleConfigService.updateSamWarningRuleConfig(any(SamWarningRuleConfig.class))).thenReturn(0);

        mockMvc.perform(put("/sam/warningRule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruleId\":3,\"ruleCode\":\"R_GPA\",\"ruleName\":\"GPA红线A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(samWarningRuleConfigService.deleteSamWarningRuleConfigByIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/sam/warningRule/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(samWarningRuleConfigService).deleteSamWarningRuleConfigByIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(samWarningRuleConfigService.selectSamWarningRuleConfigList(any(SamWarningRuleConfig.class))).thenReturn(List.of());

        mockMvc.perform(get("/sam/warningRule/list").param("ruleName", "GPA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SamWarningRuleConfig> captor = ArgumentCaptor.forClass(SamWarningRuleConfig.class);
        verify(samWarningRuleConfigService).selectSamWarningRuleConfigList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("GPA", captor.getValue().getRuleName());
    }
}
