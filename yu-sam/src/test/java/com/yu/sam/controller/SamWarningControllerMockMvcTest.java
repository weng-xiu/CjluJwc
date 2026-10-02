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
import com.yu.sam.domain.SamWarning;
import com.yu.sam.service.ISamWarningService;
import com.yu.sam.service.impl.AcademicWarningEngine;

/**
 * 学籍预警接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-sam S6 预警控制器）。
 * 控制器同时注入预警服务与预警引擎，二者均 @Mock。校验路由 /sam/warning、
 * @Validated 必填校验（studentId @NotNull、warningType @NotBlank）、
 * myWarnings 缺省以当前登录用户ID兜底、statistics 按类型/级别/是否解除聚合计数
 * （登录用户ID=1为管理员故不附加本人过滤）、generateBatch 触发引擎批量生成并透传 semesterId
 * （@RequestParam 必填缺失返回400）、逗号数组批量删除、list 查询绑定与表格结构。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamWarningControllerMockMvcTest {

    @Mock
    private ISamWarningService samWarningService;

    @Mock
    private AcademicWarningEngine academicWarningEngine;

    @InjectMocks
    private SamWarningController controller;

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
    @DisplayName("getInfo：路径变量绑定warningId并返回预警记录")
    void getInfo_returnsWarning() throws Exception {
        SamWarning warning = new SamWarning();
        warning.setWarningId(6L);
        when(samWarningService.selectSamWarningByWarningId(eq(6L))).thenReturn(warning);

        mockMvc.perform(get("/sam/warning/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.warningId").value(6));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(samWarningService.insertSamWarning(any(SamWarning.class))).thenReturn(1);

        mockMvc.perform(post("/sam/warning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":11,\"warningType\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("add：缺失必填warningType被@NotBlank拦截返回400，不落库")
    void add_missingWarningTypeRejected() throws Exception {
        mockMvc.perform(post("/sam/warning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":11}"))
                .andExpect(status().isBadRequest());

        verify(samWarningService, never()).insertSamWarning(any());
    }

    @Test
    @DisplayName("add：缺失必填studentId被@NotNull拦截返回400，不落库")
    void add_missingStudentIdRejected() throws Exception {
        mockMvc.perform(post("/sam/warning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warningType\":\"0\"}"))
                .andExpect(status().isBadRequest());

        verify(samWarningService, never()).insertSamWarning(any());
    }

    @Test
    @DisplayName("edit：影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(samWarningService.updateSamWarning(any(SamWarning.class))).thenReturn(0);

        mockMvc.perform(put("/sam/warning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warningId\":6,\"studentId\":11,\"warningType\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("myWarnings：缺省studentId以当前登录用户ID兜底")
    void myWarnings_defaultsToCurrentUser() throws Exception {
        when(samWarningService.selectSamWarningList(any(SamWarning.class))).thenReturn(List.of());

        mockMvc.perform(get("/sam/warning/myWarnings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SamWarning> captor = ArgumentCaptor.forClass(SamWarning.class);
        verify(samWarningService).selectSamWarningList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getStudentId());
    }

    @Test
    @DisplayName("myWarnings：显式studentId直接用于查询")
    void myWarnings_usesGivenStudentId() throws Exception {
        when(samWarningService.selectSamWarningList(any(SamWarning.class))).thenReturn(List.of());

        mockMvc.perform(get("/sam/warning/myWarnings").param("studentId", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamWarning> captor = ArgumentCaptor.forClass(SamWarning.class);
        verify(samWarningService).selectSamWarningList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(11L), captor.getValue().getStudentId());
    }

    @Test
    @DisplayName("statistics：按类型/级别/未解除聚合计数")
    void statistics_aggregatesCounts() throws Exception {
        SamWarning w1 = new SamWarning();
        w1.setWarningType("0");
        w1.setWarningLevel("2");
        w1.setIsResolved("0");
        SamWarning w2 = new SamWarning();
        w2.setWarningType("1");
        w2.setWarningLevel("0");
        w2.setIsResolved("1");
        when(samWarningService.selectSamWarningList(any(SamWarning.class))).thenReturn(List.of(w1, w2));

        mockMvc.perform(get("/sam/warning/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.gpaCount").value(1))
                .andExpect(jsonPath("$.data.creditCount").value(1))
                .andExpect(jsonPath("$.data.highRiskCount").value(1))
                .andExpect(jsonPath("$.data.unresolvedCount").value(1));
    }

    @Test
    @DisplayName("generateBatch：缺失必填semesterId返回400")
    void generateBatch_missingSemesterIdRejected() throws Exception {
        mockMvc.perform(post("/sam/warning/generateBatch"))
                .andExpect(status().isBadRequest());

        verify(academicWarningEngine, never()).generateWarningsBatch(any());
    }

    @Test
    @DisplayName("generateBatch：透传semesterId并返回引擎生成结果Map")
    void generateBatch_returnsEngineResult() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("generated", 7);
        when(academicWarningEngine.generateWarningsBatch(eq(3L))).thenReturn(result);

        mockMvc.perform(post("/sam/warning/generateBatch").param("semesterId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.generated").value(7));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(samWarningService.deleteSamWarningByWarningIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/sam/warning/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(samWarningService).deleteSamWarningByWarningIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(samWarningService.selectSamWarningList(any(SamWarning.class))).thenReturn(List.of());

        mockMvc.perform(get("/sam/warning/list").param("studentId", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SamWarning> captor = ArgumentCaptor.forClass(SamWarning.class);
        verify(samWarningService).selectSamWarningList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(11L), captor.getValue().getStudentId());
    }
}
