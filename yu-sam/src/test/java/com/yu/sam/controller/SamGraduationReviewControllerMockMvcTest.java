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

import com.yu.sam.domain.SamGraduationReview;
import com.yu.sam.service.ISamGraduationReviewService;

/**
 * 毕业审核接口层测试（Q1 第十五批：MockMvc standalone，覆盖 yu-sam S1 毕业审核域）。
 * 校验 CRUD 路由与 @Validated 必填校验（studentId @NotNull）、S1 单人自动审核路径变量绑定、
 * 批量自动审核 JSON 数组体反序列化为 List&lt;Long&gt; 并回传统计；
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamGraduationReviewControllerMockMvcTest {

    @Mock
    private ISamGraduationReviewService samGraduationReviewService;

    @InjectMocks
    private SamGraduationReviewController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定reviewId并返回审核记录")
    void getInfo_returnsReview() throws Exception {
        SamGraduationReview review = new SamGraduationReview();
        review.setReviewId(5L);
        review.setReviewStatus("1");
        when(samGraduationReviewService.selectSamGraduationReviewByReviewId(eq(5L))).thenReturn(review);

        mockMvc.perform(get("/sam/graduationReview/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.reviewId").value(5))
                .andExpect(jsonPath("$.data.reviewStatus").value("1"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(samGraduationReviewService.insertSamGraduationReview(any(SamGraduationReview.class))).thenReturn(1);

        String body = "{\"studentId\":11,\"totalCreditsEarned\":150.5,\"requiredCredits\":160}";
        mockMvc.perform(post("/sam/graduationReview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamGraduationReview> captor = ArgumentCaptor.forClass(SamGraduationReview.class);
        verify(samGraduationReviewService).insertSamGraduationReview(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(11L), captor.getValue().getStudentId());
    }

    @Test
    @DisplayName("add：缺失必填studentId被@NotNull拦截返回400，不落库")
    void add_missingStudentIdRejected() throws Exception {
        String body = "{\"requiredCredits\":160}";
        mockMvc.perform(post("/sam/graduationReview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(samGraduationReviewService, never()).insertSamGraduationReview(any());
    }

    @Test
    @DisplayName("edit：合法请求体透传更新，影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(samGraduationReviewService.updateSamGraduationReview(any(SamGraduationReview.class))).thenReturn(0);

        String body = "{\"reviewId\":5,\"studentId\":11}";
        mockMvc.perform(put("/sam/graduationReview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(samGraduationReviewService.deleteSamGraduationReviewByReviewIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/sam/graduationReview/1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(samGraduationReviewService).deleteSamGraduationReviewByReviewIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(3, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(samGraduationReviewService.selectSamGraduationReviewList(any(SamGraduationReview.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/sam/graduationReview/list").param("reviewStatus", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SamGraduationReview> captor = ArgumentCaptor.forClass(SamGraduationReview.class);
        verify(samGraduationReviewService).selectSamGraduationReviewList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("1", captor.getValue().getReviewStatus());
    }

    @Test
    @DisplayName("autoReview：路径变量绑定studentId并返回审核结果体")
    void autoReview_bindsStudentId() throws Exception {
        SamGraduationReview result = new SamGraduationReview();
        result.setReviewId(8L);
        result.setStudentId(11L);
        when(samGraduationReviewService.autoReview(eq(11L))).thenReturn(result);

        mockMvc.perform(post("/sam/graduationReview/autoReview/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.studentId").value(11));
    }

    @Test
    @DisplayName("batchReview：JSON数组体反序列化为studentIds并回传批量统计")
    void batchReview_bindsIdArray() throws Exception {
        Map<String, Object> stat = new HashMap<>();
        stat.put("total", 2);
        stat.put("passed", 1);
        when(samGraduationReviewService.batchAutoReview(any(List.class))).thenReturn(stat);

        mockMvc.perform(post("/sam/graduationReview/batchReview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[11,12]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(samGraduationReviewService).batchAutoReview(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(List.of(11L, 12L), captor.getValue());
    }
}
