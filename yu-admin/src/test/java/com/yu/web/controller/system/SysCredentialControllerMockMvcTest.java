package com.yu.web.controller.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.system.domain.SysPrintRecord;
import com.yu.system.service.ISysPrintService;

/**
 * 电子凭证接口层测试（Q1 第十二批：MockMvc standalone）。
 * 校验 list/getInfo 绑定、render 三参 @RequestParam 绑定（可选 semesterId 缺省为 null）、
 * issue 固定以 channel="0" 委派服务、batchIssue 返回结果 Map 落 data、print 走 renderByRecord、
 * revoke 依据影响行数 toAjax（1 成功 / 0 降级 500）；
 * 同时钉住 {@code /render}、{@code /print/{recordId}} 等字面量段优先于 {@code /{recordId}} 的消歧契约。
 * 编号/验证码生成、模板渲染等真实逻辑由 ISysPrintService 覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysCredentialControllerMockMvcTest {

    @Mock
    private ISysPrintService sysPrintService;

    @InjectMocks
    private SysCredentialController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private SysPrintRecord record(long id) {
        SysPrintRecord r = new SysPrintRecord();
        r.setRecordId(id);
        return r;
    }

    @Test
    @DisplayName("list：返回凭证发放记录表格结构")
    void list_returnsTable() throws Exception {
        when(sysPrintService.selectRecordList(any(SysPrintRecord.class))).thenReturn(List.of(record(1L)));

        mockMvc.perform(get("/system/credential/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows.length()").value(1))
                .andExpect(jsonPath("$.rows[0].recordId").value(1));
    }

    @Test
    @DisplayName("getInfo：路径变量绑定 recordId")
    void getInfo_bindsRecordId() throws Exception {
        when(sysPrintService.selectRecordById(eq(5L))).thenReturn(record(5L));

        mockMvc.perform(get("/system/credential/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordId").value(5));
    }

    @Test
    @DisplayName("render：bizType/bizId 绑定、可选 semesterId 缺省为 null，HTML 落 data")
    void render_bindsParamsHtmlToData() throws Exception {
        when(sysPrintService.render(eq("GRADE"), eq(100L), isNull())).thenReturn("<HTML>R</HTML>");

        mockMvc.perform(get("/system/credential/render").param("bizType", "GRADE").param("bizId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("<HTML>R</HTML>"));

        verify(sysPrintService).render(eq("GRADE"), eq(100L), isNull());
    }

    @Test
    @DisplayName("issue：固定以 channel=\"0\" 委派服务并透传 semesterId")
    void issue_hardcodesChannelZero() throws Exception {
        when(sysPrintService.issue(any(), any(), any(), any())).thenReturn("<HTML>ISSUED</HTML>");

        mockMvc.perform(post("/system/credential/issue")
                        .param("bizType", "EXAM_TICKET").param("bizId", "200").param("semesterId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("<HTML>ISSUED</HTML>"));

        verify(sysPrintService).issue(eq("EXAM_TICKET"), eq(200L), eq(3L), eq("0"));
    }

    @Test
    @DisplayName("batchIssue：结果 Map 落 data")
    void batchIssue_returnsResultMap() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("success", 2);
        result.put("failed", 0);
        when(sysPrintService.batchIssue(eq("GRADE"), eq(5L))).thenReturn(result);

        mockMvc.perform(post("/system/credential/batchIssue").param("bizType", "GRADE").param("scopeId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(2))
                .andExpect(jsonPath("$.data.failed").value(0));
    }

    @Test
    @DisplayName("print：按记录 ID 走 renderByRecord")
    void print_renderByRecord() throws Exception {
        when(sysPrintService.renderByRecord(eq(7L))).thenReturn("<HTML>P</HTML>");

        mockMvc.perform(get("/system/credential/print/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("<HTML>P</HTML>"));
    }

    @Test
    @DisplayName("revoke：影响行数 1 返回操作成功")
    void revoke_ok() throws Exception {
        when(sysPrintService.revokeRecord(eq(9L))).thenReturn(1);

        mockMvc.perform(put("/system/credential/revoke/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"));
    }

    @Test
    @DisplayName("revoke：影响行数 0 经 toAjax 降级 500")
    void revoke_zeroRowsReturnsError() throws Exception {
        when(sysPrintService.revokeRecord(eq(9L))).thenReturn(0);

        mockMvc.perform(put("/system/credential/revoke/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("操作失败"));
    }
}
