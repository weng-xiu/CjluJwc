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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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

import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.sam.domain.SamStudentIntervention;
import com.yu.sam.service.ISamStudentInterventionService;

/**
 * 学生干预闭环接口层测试（F4，MockMvc standalone）。
 * 校验路由 /sam/intervention、@Validated 必填（studentId @NotNull、interventionType/measure @NotBlank）、
 * add/edit 回填登录操作人、逗号数组批量删除、list 查询绑定、效果预览参数绑定与差值返回。
 * 台账 Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 *
 * @author F4
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamStudentInterventionControllerMockMvcTest
{
    @Mock
    private ISamStudentInterventionService interventionService;

    @InjectMocks
    private SamStudentInterventionController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
        sysUser.setUserId(1L);
        sysUser.setUserName("tester");
        LoginUser loginUser = new LoginUser(1L, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("list：studentId/status 查询绑定并返回表格结构")
    void list_bindsQuery() throws Exception
    {
        SamStudentIntervention row = new SamStudentIntervention();
        row.setInterventionId(1L);
        row.setStudentId(1001L);
        row.setMeasure("每周一次学业辅导");
        row.setStatus("0");
        ArrayList<SamStudentIntervention> rows = new ArrayList<>();
        rows.add(row);
        when(interventionService.selectInterventionList(any(SamStudentIntervention.class))).thenReturn(rows);

        mockMvc.perform(get("/sam/intervention/list").param("studentId", "1001").param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0].measure").value("每周一次学业辅导"));

        ArgumentCaptor<SamStudentIntervention> captor = ArgumentCaptor.forClass(SamStudentIntervention.class);
        verify(interventionService).selectInterventionList(captor.capture());
        Assertions.assertEquals(1001L, captor.getValue().getStudentId());
        Assertions.assertEquals("0", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("getInfo：路径变量绑定interventionId并返回干预记录")
    void getInfo_returnsRecord() throws Exception
    {
        SamStudentIntervention r = new SamStudentIntervention();
        r.setInterventionId(9L);
        r.setHelperName("张老师");
        when(interventionService.selectInterventionById(eq(9L))).thenReturn(r);

        mockMvc.perform(get("/sam/intervention/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.helperName").value("张老师"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并回填createBy")
    void add_validBodyInjectsCreateBy() throws Exception
    {
        when(interventionService.insertIntervention(any(SamStudentIntervention.class))).thenReturn(1);

        String body = "{\"studentId\":1001,\"interventionType\":\"0\",\"measure\":\"每周一次学业辅导\",\"helperName\":\"张老师\"}";
        mockMvc.perform(post("/sam/intervention")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamStudentIntervention> captor = ArgumentCaptor.forClass(SamStudentIntervention.class);
        verify(interventionService).insertIntervention(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getCreateBy());
        Assertions.assertEquals(1001L, captor.getValue().getStudentId());
    }

    @Test
    @DisplayName("add：缺失必填measure被@NotBlank拦截返回400，不落库")
    void add_missingMeasureRejected() throws Exception
    {
        mockMvc.perform(post("/sam/intervention")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":1001,\"interventionType\":\"0\"}"))
                .andExpect(status().isBadRequest());

        verify(interventionService, never()).insertIntervention(any());
    }

    @Test
    @DisplayName("add：缺失必填studentId被@NotNull拦截返回400，不落库")
    void add_missingStudentIdRejected() throws Exception
    {
        mockMvc.perform(post("/sam/intervention")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interventionType\":\"0\",\"measure\":\"学业辅导\"}"))
                .andExpect(status().isBadRequest());

        verify(interventionService, never()).insertIntervention(any());
    }

    @Test
    @DisplayName("edit：更新状态并注入updateBy")
    void edit_injectsUpdateBy() throws Exception
    {
        when(interventionService.updateIntervention(any(SamStudentIntervention.class))).thenReturn(1);

        String body = "{\"interventionId\":9,\"studentId\":1001,\"interventionType\":\"0\",\"measure\":\"学业辅导\",\"status\":\"1\"}";
        mockMvc.perform(put("/sam/intervention")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamStudentIntervention> captor = ArgumentCaptor.forClass(SamStudentIntervention.class);
        verify(interventionService).updateIntervention(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
        Assertions.assertEquals("1", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("remove：逗号数组批量删除")
    void remove_batchIds() throws Exception
    {
        when(interventionService.deleteInterventionByIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/sam/intervention/11,12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(interventionService).deleteInterventionByIds(eq(new Long[]{11L, 12L}));
    }

    @Test
    @DisplayName("preview：基线/效果学期参数绑定并返回差值")
    void preview_returnsDeltas() throws Exception
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("available", true);
        data.put("gpaDelta", 0.35);
        data.put("passRateDelta", 12.5);
        when(interventionService.previewEffect(eq(1001L), eq(1L), eq(2L))).thenReturn(data);

        mockMvc.perform(get("/sam/intervention/preview")
                        .param("studentId", "1001")
                        .param("baselineSemesterId", "1")
                        .param("effectSemesterId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gpaDelta").value(0.35))
                .andExpect(jsonPath("$.data.passRateDelta").value(12.5));

        verify(interventionService).previewEffect(eq(1001L), eq(1L), eq(2L));
    }
}
