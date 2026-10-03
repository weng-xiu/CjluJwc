package com.yu.tpm.controller;

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

import java.util.List;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.tpm.domain.TpmScheduleAdjustment;
import com.yu.tpm.service.ITpmScheduleAdjustmentService;

/**
 * 调停课申请接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验 /tpm/adjust 路由、@Validated 必填（scheduleId/adjustType/reason）、逗号数组批量删除，
 * 以及 approve/reject 审批端点：从请求体取 approveComment 透传服务层（void 方法），回执为 success()。
 * 调停课回写排课等业务逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TpmScheduleAdjustmentControllerMockMvcTest {

    @Mock
    private ITpmScheduleAdjustmentService tpmScheduleAdjustmentService;

    @InjectMocks
    private TpmScheduleAdjustmentController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定adjustId并返回数据体")
    void getInfo_returnsAdjustment() throws Exception {
        TpmScheduleAdjustment a = new TpmScheduleAdjustment();
        a.setAdjustId(5L);
        a.setScheduleId(9L);
        when(tpmScheduleAdjustmentService.selectTpmScheduleAdjustmentByAdjustId(eq(5L))).thenReturn(a);

        mockMvc.perform(get("/tpm/adjust/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.adjustId").value(5))
                .andExpect(jsonPath("$.data.scheduleId").value(9));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(tpmScheduleAdjustmentService.insertTpmScheduleAdjustment(any(TpmScheduleAdjustment.class))).thenReturn(1);

        String body = "{\"scheduleId\":9,\"adjustType\":\"STOP\",\"reason\":\"公假停课\"}";
        mockMvc.perform(post("/tpm/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmScheduleAdjustment> captor = ArgumentCaptor.forClass(TpmScheduleAdjustment.class);
        verify(tpmScheduleAdjustmentService).insertTpmScheduleAdjustment(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(9L), captor.getValue().getScheduleId());
    }

    @Test
    @DisplayName("add：reason空白被@NotBlank拦截返回400，不落库")
    void add_blankReasonRejected() throws Exception {
        String body = "{\"scheduleId\":9,\"adjustType\":\"STOP\",\"reason\":\"\"}";
        mockMvc.perform(post("/tpm/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(tpmScheduleAdjustmentService, never()).insertTpmScheduleAdjustment(any());
    }

    @Test
    @DisplayName("approve：从请求体取approveComment透传服务层并返回success")
    void approve_passesComment() throws Exception {
        mockMvc.perform(put("/tpm/adjust/approve/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approveComment\":\"同意调课\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tpmScheduleAdjustmentService).approve(eq(5L), eq("同意调课"));
    }

    @Test
    @DisplayName("reject：驳回走reject服务方法并透传审批意见")
    void reject_passesComment() throws Exception {
        mockMvc.perform(put("/tpm/adjust/reject/6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approveComment\":\"理由不足\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tpmScheduleAdjustmentService).reject(eq(6L), eq("理由不足"));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(tpmScheduleAdjustmentService.deleteTpmScheduleAdjustmentByAdjustIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/tpm/adjust/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(tpmScheduleAdjustmentService).deleteTpmScheduleAdjustmentByAdjustIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(tpmScheduleAdjustmentService.selectTpmScheduleAdjustmentList(any(TpmScheduleAdjustment.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/tpm/adjust/list").param("scheduleId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<TpmScheduleAdjustment> captor = ArgumentCaptor.forClass(TpmScheduleAdjustment.class);
        verify(tpmScheduleAdjustmentService).selectTpmScheduleAdjustmentList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(9L), captor.getValue().getScheduleId());
    }
}
