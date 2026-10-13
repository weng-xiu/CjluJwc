package com.yu.web.controller.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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

import com.yu.system.domain.SysAiEvalCase;
import com.yu.system.domain.SysAiEvalResult;
import com.yu.system.domain.SysAiEvalRun;
import com.yu.system.service.ISysAiEvalService;
import com.yu.system.service.ISysConfigService;

/**
 * 智能体发布前评测接口层测试（K4 AI 治理 ③ · MockMvc standalone）。
 * 只读钉住：用例 list 表格结构、case/getRun 路径变量绑定、runResults 明细落 data、批次消歧契约；
 * 发布门禁的写留痕链路依赖登录上下文（getUsername）与真实问答，属服务层覆盖范围，此处不触发。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysAiEvalControllerMockMvcTest {

    @Mock
    private ISysAiEvalService evalService;

    @Mock
    private ISysConfigService configService;

    @InjectMocks
    private SysAiEvalController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private SysAiEvalCase evalCase(long id) {
        SysAiEvalCase c = new SysAiEvalCase();
        c.setCaseId(id);
        c.setQuestion("补考申请流程？");
        return c;
    }

    private SysAiEvalRun run(long id, String result) {
        SysAiEvalRun r = new SysAiEvalRun();
        r.setRunId(id);
        r.setTriggerType(SysAiEvalRun.TRIGGER_PUBLISH);
        r.setTotalCases(3);
        r.setPassedCases(3);
        r.setPassRate(100d);
        r.setPassThreshold(80d);
        r.setResult(result);
        return r;
    }

    @Test
    @DisplayName("case/list：返回表格结构")
    void caseList_returnsTable() throws Exception {
        when(evalService.selectEvalCaseList(any(SysAiEvalCase.class))).thenReturn(List.of(evalCase(1L)));

        mockMvc.perform(get("/system/aiEval/case/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows.length()").value(1))
                .andExpect(jsonPath("$.rows[0].caseId").value(1));
    }

    @Test
    @DisplayName("case/{id}：路径变量绑定 caseId")
    void getCase_bindsCaseId() throws Exception {
        when(evalService.selectEvalCaseById(9L)).thenReturn(evalCase(9L));

        mockMvc.perform(get("/system/aiEval/case/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.caseId").value(9));
    }

    @Test
    @DisplayName("run/list：批次表格返回")
    void runList_returnsTable() throws Exception {
        when(evalService.selectEvalRunList(any(SysAiEvalRun.class))).thenReturn(List.of(run(5L, SysAiEvalRun.RESULT_PASS)));

        mockMvc.perform(get("/system/aiEval/run/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].runId").value(5))
                .andExpect(jsonPath("$.rows[0].result").value("0"));
    }

    @Test
    @DisplayName("run/{id}/results：逐条明细落 data")
    void runResults_returnsData() throws Exception {
        SysAiEvalResult r = new SysAiEvalResult();
        r.setResultId(1L);
        r.setRunId(5L);
        r.setPassed("0");
        when(evalService.selectResultsByRunId(5L)).thenReturn(List.of(r));

        mockMvc.perform(get("/system/aiEval/run/5/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].passed").value("0"));
    }
}
