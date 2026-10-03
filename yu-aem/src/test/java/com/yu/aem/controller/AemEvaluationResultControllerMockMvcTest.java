package com.yu.aem.controller;

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

import com.yu.aem.domain.AemEvaluationResult;
import com.yu.aem.service.IAemEvaluationResultService;

/**
 * 评教结果接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验 /aem/evaluationResult 路由、@Validated 必填（questionnaireId/courseId/teacherId）、
 * 逗号数组批量删除契约；评教打分与统计口径由服务层与 AemEvaluationStat 覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AemEvaluationResultControllerMockMvcTest {

    @Mock
    private IAemEvaluationResultService aemEvaluationResultService;

    @InjectMocks
    private AemEvaluationResultController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定resultId并返回结果数据体")
    void getInfo_returnsResult() throws Exception {
        AemEvaluationResult r = new AemEvaluationResult();
        r.setResultId(11L);
        r.setTeacherId(3L);
        when(aemEvaluationResultService.selectAemEvaluationResultByResultId(eq(11L))).thenReturn(r);

        mockMvc.perform(get("/aem/evaluationResult/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.resultId").value(11))
                .andExpect(jsonPath("$.data.teacherId").value(3));
    }

    @Test
    @DisplayName("add：合法三元必填请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(aemEvaluationResultService.insertAemEvaluationResult(any(AemEvaluationResult.class))).thenReturn(1);

        String body = "{\"questionnaireId\":1,\"courseId\":2,\"teacherId\":3}";
        mockMvc.perform(post("/aem/evaluationResult")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<AemEvaluationResult> captor = ArgumentCaptor.forClass(AemEvaluationResult.class);
        verify(aemEvaluationResultService).insertAemEvaluationResult(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(2L), captor.getValue().getCourseId());
    }

    @Test
    @DisplayName("add：缺失必填teacherId被@NotNull拦截返回400，不落库")
    void add_missingTeacherIdRejected() throws Exception {
        String body = "{\"questionnaireId\":1,\"courseId\":2}";
        mockMvc.perform(post("/aem/evaluationResult")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(aemEvaluationResultService, never()).insertAemEvaluationResult(any());
    }

    @Test
    @DisplayName("edit：更新走服务方法并透传域对象")
    void edit_passthrough() throws Exception {
        when(aemEvaluationResultService.updateAemEvaluationResult(any(AemEvaluationResult.class))).thenReturn(1);

        mockMvc.perform(put("/aem/evaluationResult")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultId\":11,\"questionnaireId\":1,\"courseId\":2,\"teacherId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(aemEvaluationResultService).updateAemEvaluationResult(any(AemEvaluationResult.class));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(aemEvaluationResultService.deleteAemEvaluationResultByResultIds(any(Long[].class))).thenReturn(3);

        mockMvc.perform(delete("/aem/evaluationResult/1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(aemEvaluationResultService).deleteAemEvaluationResultByResultIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(3, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(aemEvaluationResultService.selectAemEvaluationResultList(any(AemEvaluationResult.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/aem/evaluationResult/list").param("courseId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<AemEvaluationResult> captor = ArgumentCaptor.forClass(AemEvaluationResult.class);
        verify(aemEvaluationResultService).selectAemEvaluationResultList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(2L), captor.getValue().getCourseId());
    }
}
