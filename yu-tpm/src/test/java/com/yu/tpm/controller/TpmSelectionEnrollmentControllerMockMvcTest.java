package com.yu.tpm.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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

import com.yu.common.core.domain.AjaxResult;
import com.yu.tpm.domain.TpmSelectionEnrollment;
import com.yu.tpm.service.ITpmSelectionEnrollmentService;

/**
 * 选课名单接口层测试（Q1 第三批：MockMvc standalone）。
 * 校验 HTTP 路由、@Validated 请求体校验、PathVariable/RequestParam 绑定与抽签 seed 透传契约；
 * 抽签/递补/冲突判定等业务算法由 TpmSelectionEnrollmentServiceImplTest 纯单测覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TpmSelectionEnrollmentControllerMockMvcTest {

    @Mock
    private ITpmSelectionEnrollmentService tpmSelectionEnrollmentService;

    @InjectMocks
    private TpmSelectionEnrollmentController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：PathVariable绑定并返回200与数据体")
    void getInfo_returnsEnrollment() throws Exception {
        TpmSelectionEnrollment e = new TpmSelectionEnrollment();
        e.setEnrollId(1L);
        e.setStudentId(10L);
        e.setCourseOfferingId(20L);
        when(tpmSelectionEnrollmentService.selectTpmSelectionEnrollmentByEnrollId(eq(1L))).thenReturn(e);

        mockMvc.perform(get("/tpm/enroll/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.enrollId").value(1))
                .andExpect(jsonPath("$.data.studentId").value(10));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated校验并落库")
    void add_validBodyPersists() throws Exception {
        when(tpmSelectionEnrollmentService.insertTpmSelectionEnrollment(
                org.mockito.ArgumentMatchers.any(TpmSelectionEnrollment.class))).thenReturn(1);

        String body = "{\"roundId\":1,\"studentId\":10,\"courseOfferingId\":20}";
        mockMvc.perform(post("/tpm/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmSelectionEnrollment> captor = ArgumentCaptor.forClass(TpmSelectionEnrollment.class);
        verify(tpmSelectionEnrollmentService).insertTpmSelectionEnrollment(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(10L), captor.getValue().getStudentId());
    }

    @Test
    @DisplayName("add：缺失必填studentId被@NotNull拦截返回400，不落库")
    void add_missingRequiredFieldRejected() throws Exception {
        String body = "{\"roundId\":1,\"courseOfferingId\":20}";
        mockMvc.perform(post("/tpm/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(tpmSelectionEnrollmentService, never())
                .insertTpmSelectionEnrollment(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("validate：冲突检测端点透传三元组并返回警告列表")
    void validateSelection_passesParams() throws Exception {
        when(tpmSelectionEnrollmentService.checkSelectionConflicts(eq(10L), eq(20L), eq(1L)))
                .thenReturn(Collections.emptyList());

        String body = "{\"roundId\":1,\"studentId\":10,\"courseOfferingId\":20}";
        mockMvc.perform(post("/tpm/enroll/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("lottery：传入seed时原样透传用于可复现抽签")
    void lottery_withSeed() throws Exception {
        when(tpmSelectionEnrollmentService.runLottery(eq(3L), eq(99L)))
                .thenReturn(Map.of("passed", 5));

        mockMvc.perform(post("/tpm/enroll/lottery/3").param("seed", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passed").value(5));

        verify(tpmSelectionEnrollmentService).runLottery(eq(3L), eq(99L));
    }

    @Test
    @DisplayName("lottery：不传seed时以null调用（服务侧自动生成并记录）")
    void lottery_withoutSeed() throws Exception {
        when(tpmSelectionEnrollmentService.runLottery(eq(3L), eq(null)))
                .thenReturn(new LinkedHashMap<>());

        mockMvc.perform(post("/tpm/enroll/lottery/3"))
                .andExpect(status().isOk());

        verify(tpmSelectionEnrollmentService).runLottery(eq(3L), eq(null));
    }

    @Test
    @DisplayName("drop：退课端点直接返回服务层AjaxResult契约")
    void drop_returnsServiceResult() throws Exception {
        when(tpmSelectionEnrollmentService.dropCourse(eq(7L)))
                .thenReturn(AjaxResult.success("退课成功"));

        mockMvc.perform(post("/tpm/enroll/drop/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("退课成功"));
    }
}
