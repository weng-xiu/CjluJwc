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

import com.yu.aem.domain.AemGradeReview;
import com.yu.aem.service.IAemGradeReviewService;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;

/**
 * 成绩复核审批接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验 /aem/gradeReview 路由、@Validated 必填（gradeId/studentId/courseId/reviewReason）、
 * 多级审批端点：approve 必填 boolean approved、submit/cancel 走流程服务、
 * cancel 透传登录用户名（SecurityContext）、trace 未进入流程返回错误码500。
 * 流程编排与回写重算逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AemGradeReviewControllerMockMvcTest {

    @Mock
    private IAemGradeReviewService aemGradeReviewService;

    @InjectMocks
    private AemGradeReviewController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
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
    @DisplayName("getInfo：路径变量绑定reviewId并返回数据体")
    void getInfo_returnsReview() throws Exception {
        AemGradeReview r = new AemGradeReview();
        r.setReviewId(2L);
        r.setGradeId(9L);
        when(aemGradeReviewService.selectAemGradeReviewByReviewId(eq(2L))).thenReturn(r);

        mockMvc.perform(get("/aem/gradeReview/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.reviewId").value(2))
                .andExpect(jsonPath("$.data.gradeId").value(9));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(aemGradeReviewService.insertAemGradeReview(any(AemGradeReview.class))).thenReturn(1);

        String body = "{\"gradeId\":9,\"studentId\":3,\"courseId\":7,\"reviewReason\":\"分数登记有误\"}";
        mockMvc.perform(post("/aem/gradeReview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<AemGradeReview> captor = ArgumentCaptor.forClass(AemGradeReview.class);
        verify(aemGradeReviewService).insertAemGradeReview(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(9L), captor.getValue().getGradeId());
    }

    @Test
    @DisplayName("add：reviewReason空白被@NotBlank拦截返回400，不落库")
    void add_blankReasonRejected() throws Exception {
        String body = "{\"gradeId\":9,\"studentId\":3,\"courseId\":7,\"reviewReason\":\"\"}";
        mockMvc.perform(post("/aem/gradeReview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(aemGradeReviewService, never()).insertAemGradeReview(any());
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(aemGradeReviewService.deleteAemGradeReviewByReviewIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/aem/gradeReview/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(aemGradeReviewService).deleteAemGradeReviewByReviewIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("approve：透传reviewId/approved/opinion到流程审批服务")
    void approve_passthroughParams() throws Exception {
        when(aemGradeReviewService.approveReviewByFlow(eq(5L), eq(true), eq("同意"))).thenReturn(1);

        mockMvc.perform(post("/aem/gradeReview/approve/5")
                        .param("approved", "true").param("opinion", "同意"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(aemGradeReviewService).approveReviewByFlow(eq(5L), eq(true), eq("同意"));
    }

    @Test
    @DisplayName("approve：缺失必填approved参数返回400")
    void approve_missingApprovedRejected() throws Exception {
        mockMvc.perform(post("/aem/gradeReview/approve/5"))
                .andExpect(status().isBadRequest());

        verify(aemGradeReviewService, never()).approveReviewByFlow(any(), eq(true), any());
    }

    @Test
    @DisplayName("submit：提交流程走submitForApproval并绑定reviewId")
    void submit_bindsReviewId() throws Exception {
        when(aemGradeReviewService.submitForApproval(eq(6L))).thenReturn(1);

        mockMvc.perform(post("/aem/gradeReview/submit/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(aemGradeReviewService).submitForApproval(eq(6L));
    }

    @Test
    @DisplayName("cancel：撤销透传reviewId与当前登录用户名")
    void cancel_passesOperator() throws Exception {
        when(aemGradeReviewService.cancelByApplicant(eq(7L), eq("tester"))).thenReturn(1);

        mockMvc.perform(post("/aem/gradeReview/cancel/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(aemGradeReviewService).cancelByApplicant(eq(7L), eq("tester"));
    }

    @Test
    @DisplayName("trace：有流程明细时Map落data")
    void trace_returnsDetail() throws Exception {
        when(aemGradeReviewService.traceReview(eq(8L))).thenReturn(Map.of("nodes", 2));

        mockMvc.perform(get("/aem/gradeReview/trace/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nodes").value(2));
    }

    @Test
    @DisplayName("trace：未进入流程返回null时降级为错误码500")
    void trace_nullReturnsError() throws Exception {
        when(aemGradeReviewService.traceReview(eq(9L))).thenReturn(null);

        mockMvc.perform(get("/aem/gradeReview/trace/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(aemGradeReviewService.selectAemGradeReviewList(any(AemGradeReview.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/aem/gradeReview/list").param("gradeId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<AemGradeReview> captor = ArgumentCaptor.forClass(AemGradeReview.class);
        verify(aemGradeReviewService).selectAemGradeReviewList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(9L), captor.getValue().getGradeId());
    }
}
