package com.yu.sam.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.yu.sam.domain.SamThesis;
import com.yu.sam.service.ISamThesisService;

/**
 * 毕业论文全过程接口层测试（Q1 第十五批：MockMvc standalone，覆盖 yu-sam S9 论文域控制器）。
 * 校验 CRUD 路由与 @Validated 必填校验（studentId @NotNull、planYear @NotBlank）、
 * add/edit 回填登录操作人、环节审核/材料补录/查重登记/成绩归档/抽检五类 ThesisAction
 * 通用动作体到服务层多参签名的字段取用与透传、stat 统计结构回传；
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamThesisControllerMockMvcTest {

    @Mock
    private ISamThesisService samThesisService;

    @InjectMocks
    private SamThesisController controller;

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
    @DisplayName("getInfo：路径变量绑定thesisId返回含环节留痕的档案详情")
    void getInfo_returnsDetail() throws Exception {
        SamThesis thesis = new SamThesis();
        thesis.setThesisId(6L);
        thesis.setCurrentStage("3");
        when(samThesisService.selectDetailWithProcess(eq(6L))).thenReturn(thesis);

        mockMvc.perform(get("/sam/thesis/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.currentStage").value("3"));
    }

    @Test
    @DisplayName("stat：查询条件透传并回传统计结构")
    void stat_returnsSummary() throws Exception {
        Map<String, Object> stat = new HashMap<>();
        stat.put("total", 20);
        stat.put("archived", 8);
        when(samThesisService.statSummary(any(SamThesis.class))).thenReturn(stat);

        mockMvc.perform(get("/sam/thesis/stat").param("planYear", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(20))
                .andExpect(jsonPath("$.data.archived").value(8));

        ArgumentCaptor<SamThesis> captor = ArgumentCaptor.forClass(SamThesis.class);
        verify(samThesisService).statSummary(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("2026", captor.getValue().getPlanYear());
    }

    @Test
    @DisplayName("add：合法请求体通过校验并回填登录用户为createBy")
    void add_validBodyInjectsCreateBy() throws Exception {
        when(samThesisService.insertSamThesis(any(SamThesis.class))).thenReturn(1);

        String body = "{\"studentId\":11,\"planYear\":\"2026\",\"topicName\":\"智能排课研究\"}";
        mockMvc.perform(post("/sam/thesis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamThesis> captor = ArgumentCaptor.forClass(SamThesis.class);
        verify(samThesisService).insertSamThesis(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("tester", captor.getValue().getCreateBy());
    }

    @Test
    @DisplayName("add：缺失必填studentId/planYear被拦截返回400，不落库")
    void add_missingRequiredRejected() throws Exception {
        mockMvc.perform(post("/sam/thesis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planYear\":\"2026\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/sam/thesis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":11}"))
                .andExpect(status().isBadRequest());

        verify(samThesisService, never()).insertSamThesis(any());
    }

    @Test
    @DisplayName("edit：合法请求体回填updateBy，影响行数0时toAjax降级500")
    void edit_injectsUpdateAndDegrades() throws Exception {
        when(samThesisService.updateSamThesis(any(SamThesis.class))).thenReturn(0);

        String body = "{\"thesisId\":6,\"studentId\":11,\"planYear\":\"2026\"}";
        mockMvc.perform(put("/sam/thesis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        ArgumentCaptor<SamThesis> captor = ArgumentCaptor.forClass(SamThesis.class);
        verify(samThesisService).updateSamThesis(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(samThesisService.deleteSamThesisByThesisIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/sam/thesis/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(samThesisService).deleteSamThesisByThesisIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(samThesisService.selectSamThesisList(any(SamThesis.class))).thenReturn(List.of());

        mockMvc.perform(get("/sam/thesis/list").param("studentName", "赵"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SamThesis> captor = ArgumentCaptor.forClass(SamThesis.class);
        verify(samThesisService).selectSamThesisList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("赵", captor.getValue().getStudentName());
    }

    @Test
    @DisplayName("stage/audit：ThesisAction取用stage/pass/score并透传审核签名，pass缺省按false")
    void audit_stageActionPassthrough() throws Exception {
        when(samThesisService.auditStage(any(), any(), anyBoolean(), any(), any(), any())).thenReturn(1);

        String body = "{\"thesisId\":6,\"stage\":\"2\",\"pass\":true,\"score\":88.5,\"opinion\":\"同意\"}";
        mockMvc.perform(post("/sam/thesis/stage/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samThesisService).auditStage(eq(6L), eq("2"), eq(true), eq(88.5), eq("同意"), eq("tester"));
    }

    @Test
    @DisplayName("stage/submit：材料补录取用title/content/attachment透传服务层")
    void submit_materialPassthrough() throws Exception {
        when(samThesisService.submitStage(any(), any(), any(), any(), any(), any())).thenReturn(1);

        String body = "{\"thesisId\":6,\"stage\":\"3\",\"title\":\"中期报告\",\"content\":\"进展良好\",\"attachment\":\"/profile/mid.pdf\"}";
        mockMvc.perform(post("/sam/thesis/stage/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samThesisService).submitStage(eq(6L), eq("3"), eq("中期报告"), eq("进展良好"), eq("/profile/mid.pdf"), eq("tester"));
    }

    @Test
    @DisplayName("check：查重登记score映射checkRate，影响行数0时降级500")
    void check_recordRateContract() throws Exception {
        when(samThesisService.recordCheck(any(), any(), any(), any(), any())).thenReturn(0);

        String body = "{\"thesisId\":6,\"score\":12.3,\"attachment\":\"/profile/chk.pdf\",\"opinion\":\"重复率合规\"}";
        mockMvc.perform(post("/sam/thesis/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(samThesisService).recordCheck(eq(6L), eq(12.3), eq("/profile/chk.pdf"), eq("重复率合规"), eq("tester"));
    }

    @Test
    @DisplayName("archive：成绩归档取用totalScore/defenseScore并透传")
    void archive_gradePassthrough() throws Exception {
        when(samThesisService.archiveGrade(any(), any(), any(), any(), any())).thenReturn(1);

        String body = "{\"thesisId\":6,\"totalScore\":91.0,\"defenseScore\":89.5,\"opinion\":\"优秀\"}";
        mockMvc.perform(post("/sam/thesis/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samThesisService).archiveGrade(eq(6L), eq(91.0), eq(89.5), eq("优秀"), eq("tester"));
    }

    @Test
    @DisplayName("sample：抽检状态维护绑定PUT并透传sampleStatus")
    void sample_markStatus() throws Exception {
        when(samThesisService.markSample(anyLong(), anyString(), anyString(), anyString())).thenReturn(1);

        String body = "{\"thesisId\":6,\"sampleStatus\":\"1\",\"opinion\":\"已送检\"}";
        mockMvc.perform(put("/sam/thesis/sample")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samThesisService).markSample(eq(6L), eq("1"), eq("已送检"), eq("tester"));
    }
}
