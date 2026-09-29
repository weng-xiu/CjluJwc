package com.yu.aem.controller;

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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
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
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;

/**
 * 成绩记录接口层测试（Q1 第四批：MockMvc standalone）。
 * 校验 HTTP 路由、PathVariable 数组绑定（批量删除/提交）、@RequestParam 默认值（audit approved）、
 * A5 提交-审核-解锁链路的 operator 透传（SecurityContext 注入 LoginUser）与录入窗口契约；
 * GPA 计算、锁定规则等业务逻辑由服务层单测覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AemGradeRecordControllerMockMvcTest {

    @Mock
    private IAemGradeRecordService aemGradeRecordService;

    @InjectMocks
    private AemGradeRecordController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        // BaseController.getUsername() 依赖 SecurityContext，standalone 模式手动注入登录用户
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
    @DisplayName("getInfo：路径变量绑定gradeId并返回成绩数据体")
    void getInfo_returnsGradeRecord() throws Exception {
        AemGradeRecord r = new AemGradeRecord();
        r.setGradeId(1L);
        r.setStudentId(10L);
        r.setTotalScore(85.0);
        when(aemGradeRecordService.selectAemGradeRecordByGradeId(eq(1L))).thenReturn(r);

        mockMvc.perform(get("/aem/gradeRecord/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.gradeId").value(1))
                .andExpect(jsonPath("$.data.studentId").value(10))
                .andExpect(jsonPath("$.data.totalScore").value(85.0));
    }

    @Test
    @DisplayName("getDetail：走明细查询重载（含复核子表），与getInfo区分路由")
    void getDetail_usesDetailServiceMethod() throws Exception {
        AemGradeRecord r = new AemGradeRecord();
        r.setGradeId(2L);
        r.setReviews(List.of());
        when(aemGradeRecordService.selectAemGradeRecordDetail(eq(2L))).thenReturn(r);

        mockMvc.perform(get("/aem/gradeRecord/detail/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gradeId").value(2))
                .andExpect(jsonPath("$.data.reviews").isArray());

        verify(aemGradeRecordService).selectAemGradeRecordDetail(eq(2L));
        verify(aemGradeRecordService, never()).selectAemGradeRecordByGradeId(any());
    }

    @Test
    @DisplayName("add：JSON请求体绑定成绩字段并透传服务层落库")
    void add_bindsBodyAndPersists() throws Exception {
        when(aemGradeRecordService.insertAemGradeRecord(any(AemGradeRecord.class))).thenReturn(1);

        String body = "{\"studentId\":10,\"courseId\":20,\"semesterId\":3,\"regularScore\":80,\"examScore\":90}";
        mockMvc.perform(post("/aem/gradeRecord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<AemGradeRecord> captor = ArgumentCaptor.forClass(AemGradeRecord.class);
        verify(aemGradeRecordService).insertAemGradeRecord(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(10L), captor.getValue().getStudentId());
        org.junit.jupiter.api.Assertions.assertEquals(Double.valueOf(90.0), captor.getValue().getExamScore());
    }

    @Test
    @DisplayName("remove：逗号分隔路径变量绑定为Long数组批量删除")
    void remove_bindsIdArray() throws Exception {
        when(aemGradeRecordService.deleteAemGradeRecordByGradeIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/aem/gradeRecord/1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(aemGradeRecordService).deleteAemGradeRecordByGradeIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new Long[] {1L, 2L, 3L}, captor.getValue());
    }

    @Test
    @DisplayName("submit：A5提交透传gradeIds数组与当前登录用户名")
    void submit_passesIdsAndOperator() throws Exception {
        when(aemGradeRecordService.submitGrade(any(Long[].class), eq("tester"))).thenReturn(1);

        mockMvc.perform(put("/aem/gradeRecord/submit/7,8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> ids = ArgumentCaptor.forClass(Long[].class);
        verify(aemGradeRecordService).submitGrade(ids.capture(), eq("tester"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new Long[] {7L, 8L}, ids.getValue());
    }

    @Test
    @DisplayName("audit：不传approved时默认true（审核锁定），操作人取登录用户")
    void audit_approvedDefaultsTrue() throws Exception {
        when(aemGradeRecordService.auditGrade(any(Long[].class), eq(true), eq("tester"))).thenReturn(1);

        mockMvc.perform(put("/aem/gradeRecord/audit/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(aemGradeRecordService).auditGrade(eq(new Long[] {9L}), eq(true), eq("tester"));
    }

    @Test
    @DisplayName("audit：approved=false 驳回分支原样透传")
    void audit_rejectBranch() throws Exception {
        when(aemGradeRecordService.auditGrade(any(Long[].class), eq(false), eq("tester"))).thenReturn(1);

        mockMvc.perform(put("/aem/gradeRecord/audit/9").param("approved", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(aemGradeRecordService).auditGrade(eq(new Long[] {9L}), eq(false), eq("tester"));
    }

    @Test
    @DisplayName("unlock：管理解锁端点绑定路径数组并返回成功")
    void unlock_bindsPathArray() throws Exception {
        when(aemGradeRecordService.unlockGrade(any(Long[].class), eq("tester"))).thenReturn(1);

        mockMvc.perform(put("/aem/gradeRecord/unlock/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(aemGradeRecordService).unlockGrade(eq(new Long[] {5L}), eq("tester"));
    }

    @Test
    @DisplayName("entryWindow：A5录入窗口状态返回服务层Map契约")
    void entryWindow_returnsServiceMap() throws Exception {
        when(aemGradeRecordService.getEntryWindowStatus())
                .thenReturn(Map.of("open", true, "semesterId", 5));

        mockMvc.perform(get("/aem/gradeRecord/entryWindow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.open").value(true))
                .andExpect(jsonPath("$.data.semesterId").value(5));
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        AemGradeRecord cond = new AemGradeRecord();
        cond.setStudentId(10L);
        when(aemGradeRecordService.selectAemGradeRecordList(any(AemGradeRecord.class)))
                .thenReturn(List.of(cond));

        mockMvc.perform(get("/aem/gradeRecord/list").param("studentId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray())
                .andExpect(jsonPath("$.rows[0].studentId").value(10));

        ArgumentCaptor<AemGradeRecord> captor = ArgumentCaptor.forClass(AemGradeRecord.class);
        verify(aemGradeRecordService).selectAemGradeRecordList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(10L), captor.getValue().getStudentId());
    }
}
