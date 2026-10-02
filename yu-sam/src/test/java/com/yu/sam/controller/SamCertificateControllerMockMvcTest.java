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
import java.util.HashMap;
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

import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.sam.domain.SamCertificate;
import com.yu.sam.service.ISamCertificateService;

/**
 * 证书管理接口层测试（Q1 第十五批：MockMvc standalone，覆盖 yu-sam S7 证书域控制器）。
 * 校验 CRUD 路由与 @Validated 必填校验（studentId @NotNull、certType @NotBlank）、
 * S7a 编号预览 @RequestParam 必填契约、批量生成透传登录操作人并包装统计信息、
 * 发放登记 issue 的存在性前置校验与字段回填；
 * Excel 导出依赖真实 POI 输出流与登录上下文，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamCertificateControllerMockMvcTest {

    @Mock
    private ISamCertificateService samCertificateService;

    @InjectMocks
    private SamCertificateController controller;

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
    @DisplayName("getInfo：路径变量绑定certId并返回证书记录")
    void getInfo_returnsCertificate() throws Exception {
        SamCertificate cert = new SamCertificate();
        cert.setCertId(9L);
        cert.setCertNumber("BX2026001");
        when(samCertificateService.selectSamCertificateByCertId(eq(9L))).thenReturn(cert);

        mockMvc.perform(get("/sam/certificate/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.certNumber").value("BX2026001"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(samCertificateService.insertSamCertificate(any(SamCertificate.class))).thenReturn(1);

        String body = "{\"studentId\":11,\"certType\":\"1\",\"certNumber\":\"BX2026001\"}";
        mockMvc.perform(post("/sam/certificate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamCertificate> captor = ArgumentCaptor.forClass(SamCertificate.class);
        verify(samCertificateService).insertSamCertificate(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(11L), captor.getValue().getStudentId());
    }

    @Test
    @DisplayName("add：缺失必填studentId被@NotNull拦截返回400，不落库")
    void add_missingStudentIdRejected() throws Exception {
        String body = "{\"certType\":\"1\"}";
        mockMvc.perform(post("/sam/certificate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(samCertificateService, never()).insertSamCertificate(any());
    }

    @Test
    @DisplayName("add：缺失必填certType被@NotBlank拦截返回400，不落库")
    void add_missingCertTypeRejected() throws Exception {
        String body = "{\"studentId\":11}";
        mockMvc.perform(post("/sam/certificate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(samCertificateService, never()).insertSamCertificate(any());
    }

    @Test
    @DisplayName("edit：合法请求体透传更新，影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(samCertificateService.updateSamCertificate(any(SamCertificate.class))).thenReturn(0);

        String body = "{\"certId\":9,\"studentId\":11,\"certType\":\"1\"}";
        mockMvc.perform(put("/sam/certificate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(samCertificateService.deleteSamCertificateByCertIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/sam/certificate/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(samCertificateService).deleteSamCertificateByCertIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(samCertificateService.selectSamCertificateList(any(SamCertificate.class))).thenReturn(List.of());

        mockMvc.perform(get("/sam/certificate/list").param("studentName", "赵"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SamCertificate> captor = ArgumentCaptor.forClass(SamCertificate.class);
        verify(samCertificateService).selectSamCertificateList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("赵", captor.getValue().getStudentName());
    }

    @Test
    @DisplayName("previewNumber：certType必填缺失返回400，预览编号字符串按success(String)重载落入msg")
    void previewNumber_contract() throws Exception {
        mockMvc.perform(get("/sam/certificate/previewNumber"))
                .andExpect(status().isBadRequest());

        when(samCertificateService.previewCertNumber(eq("1"), eq(2026))).thenReturn("BX2026002");
        mockMvc.perform(get("/sam/certificate/previewNumber")
                        .param("certType", "1").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("BX2026002"));
    }

    @Test
    @DisplayName("batchGenerate：透传登录操作人并将统计拼入提示语与data")
    void batchGenerate_passesOperatorAndStat() throws Exception {
        Map<String, Object> stat = new HashMap<>();
        stat.put("candidates", 5);
        stat.put("generated", 3);
        when(samCertificateService.batchGenerateCertificates(eq("1"), eq("2026"), eq("tester"))).thenReturn(stat);

        mockMvc.perform(post("/sam/certificate/batchGenerate")
                        .param("certType", "1").param("gradYear", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("已生成 3 份证书（候选 5 人）"))
                .andExpect(jsonPath("$.data.generated").value(3));
    }

    @Test
    @DisplayName("issue：证书不存在返回错误短路，存在时回填发放字段更新")
    void issue_contract() throws Exception {
        when(samCertificateService.selectSamCertificateByCertId(eq(404L))).thenReturn(null);
        mockMvc.perform(put("/sam/certificate/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"certId\":404}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
        verify(samCertificateService, never()).updateSamCertificate(any());

        SamCertificate cert = new SamCertificate();
        cert.setCertId(9L);
        when(samCertificateService.selectSamCertificateByCertId(eq(9L))).thenReturn(cert);
        when(samCertificateService.updateSamCertificate(any(SamCertificate.class))).thenReturn(1);
        mockMvc.perform(put("/sam/certificate/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"certId\":9,\"receiver\":\"赵六\",\"issueDate\":\"2026-10-02\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamCertificate> captor = ArgumentCaptor.forClass(SamCertificate.class);
        verify(samCertificateService).updateSamCertificate(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("1", captor.getValue().getIsIssued());
        org.junit.jupiter.api.Assertions.assertEquals("赵六", captor.getValue().getReceiver());
    }
}
