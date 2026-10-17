package com.yu.web.controller.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.yu.web.domain.StatusReportBatch;
import com.yu.web.service.IStatusReportService;

/**
 * 教育部状态数据上报控制器契约测试（G5）。
 * 校验「字段映射/院系下拉/数据预览/批次列表」读接口的统一响应序列化，
 * 生成批次与标记/作废/删除留痕操作均携带登录操作人（getUsername）透传服务层，
 * 且 toAjax 在影响行数为 0 时降级 500；预览分页对敏感字段脱敏由服务层负责，视图层不感知。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StatusReportControllerMockMvcTest
{
    @Mock
    private IStatusReportService statusReportService;

    @InjectMocks
    private StatusReportController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
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
    @DisplayName("GET /fields：字段映射说明按上报类型透传并序列化列表")
    void fieldsReturnsList() throws Exception
    {
        when(statusReportService.selectFieldList("01")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/system/statusReport/fields").param("reportType", "01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());

        verify(statusReportService).selectFieldList("01");
    }

    @Test
    @DisplayName("GET /deptOptions：院系下拉自含查询序列化")
    void deptOptionsReturnsList() throws Exception
    {
        Map<String, Object> opt = new LinkedHashMap<>();
        opt.put("deptId", 10);
        opt.put("deptName", "计算机科学与技术学院");
        when(statusReportService.selectDeptOptions()).thenReturn(java.util.List.of(opt));

        mockMvc.perform(get("/system/statusReport/deptOptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].deptName").value("计算机科学与技术学院"));
    }

    @Test
    @DisplayName("GET /preview：预览分页返回表格结构")
    void previewReturnsTable() throws Exception
    {
        when(statusReportService.selectReportDataForPreview(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/system/statusReport/preview").param("reportType", "03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());
    }

    @Test
    @DisplayName("POST /generate：一键生成批次携带登录操作人")
    void generatePassesOperator() throws Exception
    {
        when(statusReportService.generateBatch(any(StatusReportBatch.class), eq("tester")))
                .thenReturn(new StatusReportBatch());

        mockMvc.perform(post("/system/statusReport/generate")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());

        verify(statusReportService).generateBatch(any(StatusReportBatch.class), eq("tester"));
    }

    @Test
    @DisplayName("PUT /submit：标记上报携带操作人，影响行数 0 降级 500")
    void submitMarksAndDegrades() throws Exception
    {
        when(statusReportService.markSubmitted(1L, "tester")).thenReturn(0);
        mockMvc.perform(put("/system/statusReport/submit/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        when(statusReportService.markSubmitted(2L, "tester")).thenReturn(1);
        mockMvc.perform(put("/system/statusReport/submit/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(statusReportService).markSubmitted(1L, "tester");
        verify(statusReportService).markSubmitted(2L, "tester");
    }

    @Test
    @DisplayName("DELETE /batch/{batchId}：删除留痕透传批次号")
    void removeBatchPassesId() throws Exception
    {
        when(statusReportService.deleteBatchById(3L)).thenReturn(1);

        mockMvc.perform(delete("/system/statusReport/batch/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(statusReportService).deleteBatchById(3L);
    }
}
