package com.yu.sam.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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

import java.util.HashMap;
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

import com.yu.sam.domain.SamDegreeReview;
import com.yu.sam.service.ISamDegreeReviewService;

/**
 * 学位审核接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-sam S1 学位审核控制器）。
 * 校验路由 /sam/degreeReview、CRUD 的 @Validated 必填校验（studentId @NotNull）、
 * autoReview 单人自动审核返回审核记录、batchReview 批量自动审核返回统计 Map、
 * 逗号数组批量删除、list 查询绑定与表格结构。控制器未使用登录上下文，无需注入 SecurityContext。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamDegreeReviewControllerMockMvcTest {

    @Mock
    private ISamDegreeReviewService samDegreeReviewService;

    @InjectMocks
    private SamDegreeReviewController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定reviewId并返回审核记录")
    void getInfo_returnsReview() throws Exception {
        SamDegreeReview review = new SamDegreeReview();
        review.setReviewId(6L);
        when(samDegreeReviewService.selectSamDegreeReviewByReviewId(eq(6L))).thenReturn(review);

        mockMvc.perform(get("/sam/degreeReview/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.reviewId").value(6));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(samDegreeReviewService.insertSamDegreeReview(any(SamDegreeReview.class))).thenReturn(1);

        mockMvc.perform(post("/sam/degreeReview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":11}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samDegreeReviewService).insertSamDegreeReview(any(SamDegreeReview.class));
    }

    @Test
    @DisplayName("add：缺失必填studentId被@NotNull拦截返回400，不落库")
    void add_missingStudentIdRejected() throws Exception {
        mockMvc.perform(post("/sam/degreeReview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(samDegreeReviewService, never()).insertSamDegreeReview(any());
    }

    @Test
    @DisplayName("edit：影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(samDegreeReviewService.updateSamDegreeReview(any(SamDegreeReview.class))).thenReturn(0);

        mockMvc.perform(put("/sam/degreeReview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewId\":6,\"studentId\":11}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("autoReview：路径变量studentId单人审核并返回记录")
    void autoReview_returnsReview() throws Exception {
        SamDegreeReview review = new SamDegreeReview();
        review.setReviewId(6L);
        review.setStudentId(11L);
        when(samDegreeReviewService.autoReview(eq(11L))).thenReturn(review);

        mockMvc.perform(post("/sam/degreeReview/autoReview/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.studentId").value(11));
    }

    @Test
    @DisplayName("batchReview：请求体List<Long>透传并返回统计Map")
    void batchReview_returnsMap() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("total", 2);
        result.put("success", 2);
        when(samDegreeReviewService.batchAutoReview(anyList())).thenReturn(result);

        mockMvc.perform(post("/sam/degreeReview/batchReview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[11,12]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2));

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(samDegreeReviewService).batchAutoReview(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().size());
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(samDegreeReviewService.deleteSamDegreeReviewByReviewIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/sam/degreeReview/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(samDegreeReviewService).deleteSamDegreeReviewByReviewIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(samDegreeReviewService.selectSamDegreeReviewList(any(SamDegreeReview.class))).thenReturn(List.of());

        mockMvc.perform(get("/sam/degreeReview/list").param("studentId", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SamDegreeReview> captor = ArgumentCaptor.forClass(SamDegreeReview.class);
        verify(samDegreeReviewService).selectSamDegreeReviewList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(11L), captor.getValue().getStudentId());
    }
}
