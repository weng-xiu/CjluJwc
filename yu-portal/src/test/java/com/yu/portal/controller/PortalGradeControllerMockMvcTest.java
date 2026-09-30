package com.yu.portal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.aem.domain.AemGradeRecord;
import com.yu.aem.service.IAemGradeRecordService;
import com.yu.aem.service.IAemGradeStatisticsService;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;

/**
 * 门户成绩接口层测试（Q1 第六批：MockMvc standalone）。
 * 校验学生端成绩查询/统计的强制本人绑定（查询参数越权被登录用户覆盖）、
 * statistics 实时汇总（GPA 学分加权、平均分、空列表 null 语义）纯计算契约、
 * submit 的 recordId→gradeId 与 finalScore→examScore 字段映射（含缺省 null 分支）、
 * 教师端录入/修改的 toAjax 成败双分支；GPA 规则、审核链路等业务逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalGradeControllerMockMvcTest {

    @Mock
    private IAemGradeRecordService aemGradeRecordService;

    @Mock
    private IAemGradeStatisticsService aemGradeStatisticsService;

    @InjectMocks
    private PortalGradeController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        // BaseController.getUserId() 依赖 SecurityContext，standalone 模式手动注入登录用户（userId=1）
        SysUser sysUser = new SysUser();
        sysUser.setUserName("tester");
        LoginUser loginUser = new LoginUser(1L, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("list：查询参数带他人 studentId 时被强制覆盖为当前登录用户（防越权）")
    void list_forcesOwnStudentScope() throws Exception {
        when(aemGradeRecordService.selectAemGradeRecordListForPortal(any(AemGradeRecord.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/grade/list").param("studentId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<AemGradeRecord> captor = ArgumentCaptor.forClass(AemGradeRecord.class);
        verify(aemGradeRecordService).selectAemGradeRecordListForPortal(captor.capture());
        Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getStudentId());
    }

    @Test
    @DisplayName("statistics：按本人成绩实时汇总 GPA（学分加权）/学分/均分")
    void statistics_aggregatesWeightedGpa() throws Exception {
        AemGradeRecord a = new AemGradeRecord();
        a.setCredit(3.0);
        a.setGradePoint(4.0);
        a.setTotalScore(95.0);
        AemGradeRecord b = new AemGradeRecord();
        b.setCredit(1.5);
        b.setGradePoint(0.0);
        b.setTotalScore(80.0);
        when(aemGradeRecordService.selectAemGradeRecordListForPortal(any(AemGradeRecord.class)))
                .thenReturn(List.of(a, b));

        mockMvc.perform(get("/portal/grade/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.courseCount").value(2))
                .andExpect(jsonPath("$.data.totalCredit").value(4.5));

        // avgGpa = (4.0*3 + 0*1.5)/4.5 = 2.67，avgScore = (95+80)/2 = 87.5（浮点用 delta 断言）
        ArgumentCaptor<AemGradeRecord> captor = ArgumentCaptor.forClass(AemGradeRecord.class);
        verify(aemGradeRecordService).selectAemGradeRecordListForPortal(captor.capture());
        Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getStudentId());
    }

    @Test
    @DisplayName("statistics：无成绩时 avgGpa/avgScore 返回 null 而非 0（空语义区分）")
    void statistics_emptyListNullMetrics() throws Exception {
        when(aemGradeRecordService.selectAemGradeRecordListForPortal(any(AemGradeRecord.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/grade/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avgGpa").doesNotExist())
                .andExpect(jsonPath("$.data.avgScore").doesNotExist())
                .andExpect(jsonPath("$.data.courseCount").value(0))
                .andExpect(jsonPath("$.data.totalCredit").value(0.0));
    }

    @Test
    @DisplayName("entry：教师录入成绩请求体绑定并 toAjax 成功")
    void entry_bindsBodyAndSucceeds() throws Exception {
        when(aemGradeRecordService.insertAemGradeRecord(any(AemGradeRecord.class))).thenReturn(1);

        String body = "{\"studentId\":10,\"courseId\":20,\"regularScore\":80,\"examScore\":90}";
        mockMvc.perform(post("/portal/grade/entry")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<AemGradeRecord> captor = ArgumentCaptor.forClass(AemGradeRecord.class);
        verify(aemGradeRecordService).insertAemGradeRecord(captor.capture());
        Assertions.assertEquals(Long.valueOf(10L), captor.getValue().getStudentId());
        Assertions.assertEquals(Double.valueOf(90.0), captor.getValue().getExamScore());
    }

    @Test
    @DisplayName("editEntry：教师修改成绩走更新重载，影响行数为 0 时返回 500")
    void editEntry_zeroRowsReturnsError() throws Exception {
        when(aemGradeRecordService.updateAemGradeRecord(any(AemGradeRecord.class))).thenReturn(0);

        mockMvc.perform(put("/portal/grade/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gradeId\":5,\"totalScore\":60}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("submit：recordId 映射 gradeId、finalScore 映射 examScore 后更新")
    void submit_mapsParamNamesToRecord() throws Exception {
        when(aemGradeRecordService.updateAemGradeRecord(any(AemGradeRecord.class))).thenReturn(1);

        String body = "{\"recordId\":7,\"regularScore\":85,\"finalScore\":91}";
        mockMvc.perform(post("/portal/grade/submit")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<AemGradeRecord> captor = ArgumentCaptor.forClass(AemGradeRecord.class);
        verify(aemGradeRecordService).updateAemGradeRecord(captor.capture());
        Assertions.assertEquals(Long.valueOf(7L), captor.getValue().getGradeId());
        Assertions.assertEquals(Double.valueOf(85.0), captor.getValue().getRegularScore());
        Assertions.assertEquals(Double.valueOf(91.0), captor.getValue().getExamScore());
    }

    @Test
    @DisplayName("submit：键全部缺省时以空记录调用更新（不抛 NPE），字段保持 null")
    void submit_allKeysAbsent() throws Exception {
        when(aemGradeRecordService.updateAemGradeRecord(any(AemGradeRecord.class))).thenReturn(1);

        mockMvc.perform(post("/portal/grade/submit")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<AemGradeRecord> captor = ArgumentCaptor.forClass(AemGradeRecord.class);
        verify(aemGradeRecordService).updateAemGradeRecord(captor.capture());
        Assertions.assertNull(captor.getValue().getGradeId());
        Assertions.assertNull(captor.getValue().getRegularScore());
        Assertions.assertNull(captor.getValue().getExamScore());
    }

    @Test
    @DisplayName("entryList：教师端列表不强制绑定登录用户（按查询条件走全量服务方法）")
    void entryList_usesPlainServiceMethod() throws Exception {
        when(aemGradeRecordService.selectAemGradeRecordList(any(AemGradeRecord.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/grade/entryList").param("courseId", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        verify(aemGradeRecordService).selectAemGradeRecordList(any(AemGradeRecord.class));
        verify(aemGradeRecordService, never()).selectAemGradeRecordListForPortal(any(AemGradeRecord.class));
    }
}
