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
import com.yu.sam.domain.SamProcedureStep;
import com.yu.sam.service.ISamProcedureStepService;

/**
 * 离校环节配置接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-sam S7c 环节配置控制器）。
 * 校验路由 /sam/procedureStep、@Validated 必填校验（stepKey/stepName @NotBlank）、
 * all 全量不分页列表、add/edit 回填登录操作人、逗号数组批量删除、list 查询绑定与表格结构。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamProcedureStepControllerMockMvcTest {

    @Mock
    private ISamProcedureStepService samProcedureStepService;

    @InjectMocks
    private SamProcedureStepController controller;

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
    @DisplayName("getInfo：路径变量绑定stepId并返回环节配置")
    void getInfo_returnsStep() throws Exception {
        SamProcedureStep step = new SamProcedureStep();
        step.setStepId(2L);
        step.setStepName("图书馆离校");
        when(samProcedureStepService.selectSamProcedureStepByStepId(eq(2L))).thenReturn(step);

        mockMvc.perform(get("/sam/procedureStep/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.stepName").value("图书馆离校"));
    }

    @Test
    @DisplayName("all：全量环节不分页返回列表")
    void all_returnsFullList() throws Exception {
        when(samProcedureStepService.selectSamProcedureStepList(any(SamProcedureStep.class))).thenReturn(List.of());

        mockMvc.perform(get("/sam/procedureStep/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并回填登录操作人")
    void add_validBodyPersists() throws Exception {
        when(samProcedureStepService.insertSamProcedureStep(any(SamProcedureStep.class))).thenReturn(1);

        mockMvc.perform(post("/sam/procedureStep")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stepKey\":\"library\",\"stepName\":\"图书馆离校\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamProcedureStep> captor = ArgumentCaptor.forClass(SamProcedureStep.class);
        verify(samProcedureStepService).insertSamProcedureStep(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("tester", captor.getValue().getCreateBy());
    }

    @Test
    @DisplayName("add：缺失必填stepName被@NotBlank拦截返回400，不落库")
    void add_missingStepNameRejected() throws Exception {
        mockMvc.perform(post("/sam/procedureStep")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stepKey\":\"library\"}"))
                .andExpect(status().isBadRequest());

        verify(samProcedureStepService, never()).insertSamProcedureStep(any());
    }

    @Test
    @DisplayName("add：缺失必填stepKey被@NotBlank拦截返回400，不落库")
    void add_missingStepKeyRejected() throws Exception {
        mockMvc.perform(post("/sam/procedureStep")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stepName\":\"图书馆离校\"}"))
                .andExpect(status().isBadRequest());

        verify(samProcedureStepService, never()).insertSamProcedureStep(any());
    }

    @Test
    @DisplayName("edit：影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(samProcedureStepService.updateSamProcedureStep(any(SamProcedureStep.class))).thenReturn(0);

        mockMvc.perform(put("/sam/procedureStep")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stepId\":2,\"stepKey\":\"library\",\"stepName\":\"图书馆离校A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(samProcedureStepService.deleteSamProcedureStepByStepIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/sam/procedureStep/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(samProcedureStepService).deleteSamProcedureStepByStepIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(samProcedureStepService.selectSamProcedureStepList(any(SamProcedureStep.class))).thenReturn(List.of());

        mockMvc.perform(get("/sam/procedureStep/list").param("stepName", "图书馆"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SamProcedureStep> captor = ArgumentCaptor.forClass(SamProcedureStep.class);
        verify(samProcedureStepService).selectSamProcedureStepList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("图书馆", captor.getValue().getStepName());
    }
}
