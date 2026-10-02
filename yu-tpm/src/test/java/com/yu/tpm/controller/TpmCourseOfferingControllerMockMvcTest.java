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

import java.util.LinkedHashMap;
import java.util.List;
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

import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.domain.dto.BatchOfferingRequest;
import com.yu.tpm.service.ITpmCourseOfferingService;

/**
 * 开课计划接口层测试（Q1 第十三批：MockMvc standalone）。
 * 校验 HTTP 路由、@Validated 必填校验（semesterId/courseId）、T4 批量生成请求体透传与摘要落 data、
 * 确认/取消开课生命周期端点的路径变量绑定与 toAjax 成败双分支、逗号数组批量删除契约；
 * 批量生成的容量分配、教师预分配、幂等跳过等业务逻辑由服务层与 OfferingPlanGeneratorTest 覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TpmCourseOfferingControllerMockMvcTest {

    @Mock
    private ITpmCourseOfferingService tpmCourseOfferingService;

    @InjectMocks
    private TpmCourseOfferingController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定offeringId并返回开课数据体")
    void getInfo_returnsOffering() throws Exception {
        TpmCourseOffering offering = new TpmCourseOffering();
        offering.setOfferingId(3L);
        offering.setCourseName("高等数学");
        when(tpmCourseOfferingService.selectTpmCourseOfferingByOfferingId(eq(3L))).thenReturn(offering);

        mockMvc.perform(get("/tpm/offering/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.offeringId").value(3))
                .andExpect(jsonPath("$.data.courseName").value("高等数学"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(tpmCourseOfferingService.insertTpmCourseOffering(any(TpmCourseOffering.class))).thenReturn(1);

        String body = "{\"semesterId\":1,\"courseId\":2,\"maxStudents\":60}";
        mockMvc.perform(post("/tpm/offering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmCourseOffering> captor = ArgumentCaptor.forClass(TpmCourseOffering.class);
        verify(tpmCourseOfferingService).insertTpmCourseOffering(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(2L), captor.getValue().getCourseId());
    }

    @Test
    @DisplayName("add：缺失必填courseId被@NotNull拦截返回400，不落库")
    void add_missingCourseIdRejected() throws Exception {
        String body = "{\"semesterId\":1,\"maxStudents\":60}";
        mockMvc.perform(post("/tpm/offering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(tpmCourseOfferingService, never()).insertTpmCourseOffering(any());
    }

    @Test
    @DisplayName("add：缺失必填semesterId被@NotNull拦截返回400，不落库")
    void add_missingSemesterIdRejected() throws Exception {
        String body = "{\"courseId\":2}";
        mockMvc.perform(post("/tpm/offering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(tpmCourseOfferingService, never()).insertTpmCourseOffering(any());
    }

    @Test
    @DisplayName("batchGenerate：T4请求体透传，生成摘要Map落data")
    void batchGenerate_passthroughRequestAndSummary() throws Exception {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("generated", 5);
        summary.put("skippedExisting", 2);
        summary.put("capacity", 30);
        when(tpmCourseOfferingService.batchGenerateOfferings(any(BatchOfferingRequest.class))).thenReturn(summary);

        String body = "{\"planId\":8,\"semesterId\":1,\"classCount\":2}";
        mockMvc.perform(post("/tpm/offering/batchGenerate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generated").value(5))
                .andExpect(jsonPath("$.data.skippedExisting").value(2));

        ArgumentCaptor<BatchOfferingRequest> captor = ArgumentCaptor.forClass(BatchOfferingRequest.class);
        verify(tpmCourseOfferingService).batchGenerateOfferings(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(8L), captor.getValue().getPlanId());
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(2), captor.getValue().getClassCount());
    }

    @Test
    @DisplayName("confirm：确认开课路径变量绑定，成功影响行数→200")
    void confirm_bindsIdSuccess() throws Exception {
        when(tpmCourseOfferingService.confirmOffering(eq(4L))).thenReturn(1);

        mockMvc.perform(put("/tpm/offering/confirm/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tpmCourseOfferingService).confirmOffering(eq(4L));
    }

    @Test
    @DisplayName("cancel：取消开课影响行数0时toAjax降级为500 error")
    void cancel_zeroRowsDegrades() throws Exception {
        when(tpmCourseOfferingService.cancelOffering(eq(4L))).thenReturn(0);

        mockMvc.perform(put("/tpm/offering/cancel/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(tpmCourseOfferingService).cancelOffering(eq(4L));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(tpmCourseOfferingService.deleteTpmCourseOfferingByOfferingIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/tpm/offering/1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(tpmCourseOfferingService).deleteTpmCourseOfferingByOfferingIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(3, captor.getValue().length);
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1L), captor.getValue()[0]);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(tpmCourseOfferingService.selectTpmCourseOfferingList(any(TpmCourseOffering.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/tpm/offering/list").param("courseName", "数学"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<TpmCourseOffering> captor = ArgumentCaptor.forClass(TpmCourseOffering.class);
        verify(tpmCourseOfferingService).selectTpmCourseOfferingList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("数学", captor.getValue().getCourseName());
    }
}
