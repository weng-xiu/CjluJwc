package com.yu.tpm.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.brm.service.IBrmClassroomBorrowService;
import com.yu.tpm.service.IBrmBorrowFlowService;

/**
 * 教室借用审批流程接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验 /brm/borrowFlow 流程编排端点：submit/trace 只读委托流程服务、
 * deptApprove/aaApprove/cancel 从请求体解析 approved 布尔与意见透传（void 方法回执 success()）、
 * checkConflict 必填 classroomId+borrowDate（yyyy-MM-dd）返回冲突列表落data数组、occupancy 教室占用日历。
 * Flowable 两级审批与冲突算法由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrmBorrowFlowControllerMockMvcTest {

    @Mock
    private IBrmBorrowFlowService brmBorrowFlowService;

    @Mock
    private IBrmClassroomBorrowService brmClassroomBorrowService;

    @InjectMocks
    private BrmBorrowFlowController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("submit：提交申请委托流程服务并返回成功")
    void submit_delegatesFlowService() throws Exception {
        mockMvc.perform(post("/brm/borrowFlow/submit/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(brmBorrowFlowService).submit(eq(1L));
    }

    @Test
    @DisplayName("deptApprove：请求体approved布尔与意见透传，回执success")
    void deptApprove_parsesBodyAndDelegates() throws Exception {
        mockMvc.perform(put("/brm/borrowFlow/deptApprove/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true,\"opinion\":\"院系同意\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(brmBorrowFlowService).deptApprove(eq(2L), eq(true), eq("院系同意"));
    }

    @Test
    @DisplayName("aaApprove：终审驳回分支approved=false透传")
    void aaApprove_rejectBranch() throws Exception {
        mockMvc.perform(put("/brm/borrowFlow/aaApprove/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":false,\"opinion\":\"时间冲突\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(brmBorrowFlowService).aaApprove(eq(3L), eq(false), eq("时间冲突"));
    }

    @Test
    @DisplayName("cancel：撤销委托流程服务")
    void cancel_delegates() throws Exception {
        mockMvc.perform(put("/brm/borrowFlow/cancel/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(brmBorrowFlowService).cancel(eq(4L));
    }

    @Test
    @DisplayName("checkConflict：必填参数齐备，冲突列表落data数组")
    void checkConflict_returnsList() throws Exception {
        when(brmClassroomBorrowService.checkConflict(any(), any(), any(), any(), any()))
                .thenReturn(List.of("13:00-14:00 已被占用"));

        mockMvc.perform(get("/brm/borrowFlow/checkConflict")
                        .param("classroomId", "7").param("borrowDate", "2026-05-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0]").value("13:00-14:00 已被占用"));
    }

    @Test
    @DisplayName("checkConflict：缺失必填borrowDate返回400")
    void checkConflict_missingDateRejected() throws Exception {
        mockMvc.perform(get("/brm/borrowFlow/checkConflict").param("classroomId", "7"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("occupancy：必填classroomId，占用日历Map落data")
    void occupancy_returnsMap() throws Exception {
        when(brmClassroomBorrowService.classroomOccupancy(any(), any(), any()))
                .thenReturn(Map.of("occupied", 3));

        mockMvc.perform(get("/brm/borrowFlow/occupancy").param("classroomId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.occupied").value(3));
    }

    @Test
    @DisplayName("trace：全流程追溯Map落data")
    void trace_returnsMap() throws Exception {
        when(brmBorrowFlowService.trace(eq(9L))).thenReturn(Map.of("nodes", 2));

        mockMvc.perform(get("/brm/borrowFlow/trace/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nodes").value(2));

        verify(brmBorrowFlowService).trace(eq(9L));
    }
}
