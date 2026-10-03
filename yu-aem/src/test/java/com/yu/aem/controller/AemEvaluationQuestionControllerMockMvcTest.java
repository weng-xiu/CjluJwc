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

import com.yu.aem.domain.AemEvaluationQuestion;
import com.yu.aem.service.IAemEvaluationQuestionService;

/**
 * 评教问题接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验 /aem/question 路由、@Validated 必填（questionnaireId/questionType/questionContent）、
 * 逗号数组批量删除契约；Excel 导入（importData 依赖真实 POI 输入流与登录上下文）不属纯接口层契约，故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AemEvaluationQuestionControllerMockMvcTest {

    @Mock
    private IAemEvaluationQuestionService aemEvaluationQuestionService;

    @InjectMocks
    private AemEvaluationQuestionController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定questionId并返回题目数据体")
    void getInfo_returnsQuestion() throws Exception {
        AemEvaluationQuestion q = new AemEvaluationQuestion();
        q.setQuestionId(9L);
        q.setQuestionContent("课程组织是否合理");
        when(aemEvaluationQuestionService.selectAemEvaluationQuestionByQuestionId(eq(9L))).thenReturn(q);

        mockMvc.perform(get("/aem/question/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.questionId").value(9))
                .andExpect(jsonPath("$.data.questionContent").value("课程组织是否合理"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(aemEvaluationQuestionService.insertAemEvaluationQuestion(any(AemEvaluationQuestion.class))).thenReturn(1);

        String body = "{\"questionnaireId\":1,\"questionType\":\"1\",\"questionContent\":\"教师备课是否充分\"}";
        mockMvc.perform(post("/aem/question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<AemEvaluationQuestion> captor = ArgumentCaptor.forClass(AemEvaluationQuestion.class);
        verify(aemEvaluationQuestionService).insertAemEvaluationQuestion(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getQuestionnaireId());
    }

    @Test
    @DisplayName("add：缺失必填questionnaireId被@NotNull拦截返回400，不落库")
    void add_missingQuestionnaireIdRejected() throws Exception {
        String body = "{\"questionType\":\"1\",\"questionContent\":\"x\"}";
        mockMvc.perform(post("/aem/question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(aemEvaluationQuestionService, never()).insertAemEvaluationQuestion(any());
    }

    @Test
    @DisplayName("add：questionContent为空白被@NotBlank拦截返回400，不落库")
    void add_blankContentRejected() throws Exception {
        String body = "{\"questionnaireId\":1,\"questionType\":\"1\",\"questionContent\":\"\"}";
        mockMvc.perform(post("/aem/question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(aemEvaluationQuestionService, never()).insertAemEvaluationQuestion(any());
    }

    @Test
    @DisplayName("edit：更新走服务方法并透传域对象")
    void edit_passthrough() throws Exception {
        when(aemEvaluationQuestionService.updateAemEvaluationQuestion(any(AemEvaluationQuestion.class))).thenReturn(1);

        mockMvc.perform(put("/aem/question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":9,\"questionnaireId\":1,\"questionType\":\"1\",\"questionContent\":\"改后\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(aemEvaluationQuestionService).updateAemEvaluationQuestion(any(AemEvaluationQuestion.class));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(aemEvaluationQuestionService.deleteAemEvaluationQuestionByQuestionIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/aem/question/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(aemEvaluationQuestionService).deleteAemEvaluationQuestionByQuestionIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(aemEvaluationQuestionService.selectAemEvaluationQuestionList(any(AemEvaluationQuestion.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/aem/question/list").param("questionnaireId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<AemEvaluationQuestion> captor = ArgumentCaptor.forClass(AemEvaluationQuestion.class);
        verify(aemEvaluationQuestionService).selectAemEvaluationQuestionList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getQuestionnaireId());
    }
}
