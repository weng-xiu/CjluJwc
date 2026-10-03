package com.yu.tpm.controller;

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

import com.yu.tpm.domain.TpmSelectionRule;
import com.yu.tpm.service.ITpmSelectionRuleService;

/**
 * 选课规则接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验 /tpm/rule 路由、@Validated 必填（roundId/ruleName/ruleType）、逗号数组批量删除契约。
 * 规则表达式解析与命中逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TpmSelectionRuleControllerMockMvcTest {

    @Mock
    private ITpmSelectionRuleService tpmSelectionRuleService;

    @InjectMocks
    private TpmSelectionRuleController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定ruleId并返回规则数据体")
    void getInfo_returnsRule() throws Exception {
        TpmSelectionRule r = new TpmSelectionRule();
        r.setRuleId(3L);
        r.setRoundId(1L);
        when(tpmSelectionRuleService.selectTpmSelectionRuleByRuleId(eq(3L))).thenReturn(r);

        mockMvc.perform(get("/tpm/rule/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ruleId").value(3))
                .andExpect(jsonPath("$.data.roundId").value(1));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(tpmSelectionRuleService.insertTpmSelectionRule(any(TpmSelectionRule.class))).thenReturn(1);

        String body = "{\"roundId\":1,\"ruleName\":\"限选本专业\",\"ruleType\":\"LIMIT\"}";
        mockMvc.perform(post("/tpm/rule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmSelectionRule> captor = ArgumentCaptor.forClass(TpmSelectionRule.class);
        verify(tpmSelectionRuleService).insertTpmSelectionRule(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getRoundId());
    }

    @Test
    @DisplayName("add：缺失必填roundId被@NotNull拦截返回400，不落库")
    void add_missingRoundIdRejected() throws Exception {
        String body = "{\"ruleName\":\"限选本专业\",\"ruleType\":\"LIMIT\"}";
        mockMvc.perform(post("/tpm/rule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(tpmSelectionRuleService, never()).insertTpmSelectionRule(any());
    }

    @Test
    @DisplayName("add：ruleType空白被@NotBlank拦截返回400，不落库")
    void add_blankRuleTypeRejected() throws Exception {
        String body = "{\"roundId\":1,\"ruleName\":\"限选本专业\",\"ruleType\":\"\"}";
        mockMvc.perform(post("/tpm/rule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(tpmSelectionRuleService, never()).insertTpmSelectionRule(any());
    }

    @Test
    @DisplayName("edit：更新走服务方法并透传域对象")
    void edit_passthrough() throws Exception {
        when(tpmSelectionRuleService.updateTpmSelectionRule(any(TpmSelectionRule.class))).thenReturn(1);

        mockMvc.perform(put("/tpm/rule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruleId\":3,\"roundId\":1,\"ruleName\":\"改后\",\"ruleType\":\"LIMIT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tpmSelectionRuleService).updateTpmSelectionRule(any(TpmSelectionRule.class));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(tpmSelectionRuleService.deleteTpmSelectionRuleByRuleIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/tpm/rule/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(tpmSelectionRuleService).deleteTpmSelectionRuleByRuleIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(tpmSelectionRuleService.selectTpmSelectionRuleList(any(TpmSelectionRule.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/tpm/rule/list").param("roundId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<TpmSelectionRule> captor = ArgumentCaptor.forClass(TpmSelectionRule.class);
        verify(tpmSelectionRuleService).selectTpmSelectionRuleList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getRoundId());
    }
}
