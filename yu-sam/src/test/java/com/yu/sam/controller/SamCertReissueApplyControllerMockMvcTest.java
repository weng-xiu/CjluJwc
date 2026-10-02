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

import java.util.Collections;
import java.util.List;

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

import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.sam.domain.SamCertReissueApply;
import com.yu.sam.service.ISamCertReissueApplyService;

/**
 * 证书补办申请接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-sam S7b 补办闭环控制器）。
 * 校验路由 /sam/certReissue、submit 的 @Validated 必填校验（studentId/origCertId @NotNull）、
 * 提交透传登录操作人、受理/驳回按 applyId+opinion+operator 三元组调用服务、
 * certsOfStudent 的 @RequestParam 必填契约、逗号数组批量删除、list 查询绑定与表格结构。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamCertReissueApplyControllerMockMvcTest {

    @Mock
    private ISamCertReissueApplyService samCertReissueApplyService;

    @InjectMocks
    private SamCertReissueApplyController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
        sysUser.setUserId(1L);
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
    @DisplayName("getInfo：路径变量绑定applyId并返回补办申请记录")
    void getInfo_returnsApply() throws Exception {
        SamCertReissueApply apply = new SamCertReissueApply();
        apply.setApplyId(7L);
        when(samCertReissueApplyService.selectSamCertReissueApplyByApplyId(eq(7L))).thenReturn(apply);

        mockMvc.perform(get("/sam/certReissue/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.applyId").value(7));
    }

    @Test
    @DisplayName("certsOfStudent：缺失必填studentId返回400")
    void certsOfStudent_missingStudentIdRejected() throws Exception {
        mockMvc.perform(get("/sam/certReissue/certsOfStudent"))
                .andExpect(status().isBadRequest());

        verify(samCertReissueApplyService, never()).selectCertsForStudent(any());
    }

    @Test
    @DisplayName("certsOfStudent：按studentId返回可补办原证书列表")
    void certsOfStudent_returnsList() throws Exception {
        when(samCertReissueApplyService.selectCertsForStudent(eq(11L))).thenReturn(List.of());

        mockMvc.perform(get("/sam/certReissue/certsOfStudent").param("studentId", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("submit：合法请求体通过@Validated并回填登录操作人")
    void submit_validBodyPersists() throws Exception {
        when(samCertReissueApplyService.submitApply(any(SamCertReissueApply.class))).thenReturn(1);

        String body = "{\"studentId\":11,\"origCertId\":3}";
        mockMvc.perform(post("/sam/certReissue/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamCertReissueApply> captor = ArgumentCaptor.forClass(SamCertReissueApply.class);
        verify(samCertReissueApplyService).submitApply(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("tester", captor.getValue().getCreateBy());
    }

    @Test
    @DisplayName("submit：缺失必填origCertId被@NotNull拦截返回400，不落库")
    void submit_missingOrigCertIdRejected() throws Exception {
        String body = "{\"studentId\":11}";
        mockMvc.perform(post("/sam/certReissue/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(samCertReissueApplyService, never()).submitApply(any());
    }

    @Test
    @DisplayName("approve：按applyId+opinion+operator三元组受理，返回200")
    void approve_passesIdOpinionAndOperator() throws Exception {
        when(samCertReissueApplyService.approve(eq(7L), eq("同意补办"), eq("tester"))).thenReturn(1);

        mockMvc.perform(put("/sam/certReissue/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applyId\":7,\"auditOpinion\":\"同意补办\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samCertReissueApplyService).approve(eq(7L), eq("同意补办"), eq("tester"));
    }

    @Test
    @DisplayName("reject：影响行数0时toAjax降级500")
    void reject_zeroRowsDegrades() throws Exception {
        when(samCertReissueApplyService.reject(eq(7L), any(), eq("tester"))).thenReturn(0);

        mockMvc.perform(put("/sam/certReissue/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applyId\":7,\"auditOpinion\":\"材料不齐\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(samCertReissueApplyService.deleteSamCertReissueApplyByApplyIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/sam/certReissue/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(samCertReissueApplyService).deleteSamCertReissueApplyByApplyIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(samCertReissueApplyService.selectSamCertReissueApplyList(any(SamCertReissueApply.class))).thenReturn(List.of());

        mockMvc.perform(get("/sam/certReissue/list").param("studentId", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SamCertReissueApply> captor = ArgumentCaptor.forClass(SamCertReissueApply.class);
        verify(samCertReissueApplyService).selectSamCertReissueApplyList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(11L), captor.getValue().getStudentId());
    }
}
