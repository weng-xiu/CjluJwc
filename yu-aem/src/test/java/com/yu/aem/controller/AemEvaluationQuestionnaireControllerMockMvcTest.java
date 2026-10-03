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

import com.yu.aem.domain.AemEvaluationQuestionnaire;
import com.yu.aem.service.IAemEvaluationQuestionnaireService;

/**
 * 评教问卷配置接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验 /aem/questionnaire 路由、@Validated 必填（title）、主子表联动明细查询（detail 走独立服务方法）、
 * 逗号数组批量删除契约；Excel 导入导出与题目级业务由服务层覆盖，此处不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AemEvaluationQuestionnaireControllerMockMvcTest {

    @Mock
    private IAemEvaluationQuestionnaireService aemEvaluationQuestionnaireService;

    @InjectMocks
    private AemEvaluationQuestionnaireController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定questionnaireId并返回问卷数据体")
    void getInfo_returnsQuestionnaire() throws Exception {
        AemEvaluationQuestionnaire q = new AemEvaluationQuestionnaire();
        q.setQuestionnaireId(5L);
        q.setTitle("教学评估问卷");
        when(aemEvaluationQuestionnaireService.selectAemEvaluationQuestionnaireByQuestionnaireId(eq(5L))).thenReturn(q);

        mockMvc.perform(get("/aem/questionnaire/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.questionnaireId").value(5))
                .andExpect(jsonPath("$.data.title").value("教学评估问卷"));
    }

    @Test
    @DisplayName("getDetail：走含题目子表的明细服务方法，与getInfo路由区分")
    void getDetail_usesDetailServiceMethod() throws Exception {
        AemEvaluationQuestionnaire q = new AemEvaluationQuestionnaire();
        q.setQuestionnaireId(6L);
        q.setQuestions(List.of());
        when(aemEvaluationQuestionnaireService.selectAemEvaluationQuestionnaireDetail(eq(6L))).thenReturn(q);

        mockMvc.perform(get("/aem/questionnaire/detail/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questionnaireId").value(6))
                .andExpect(jsonPath("$.data.questions").isArray());

        verify(aemEvaluationQuestionnaireService).selectAemEvaluationQuestionnaireDetail(eq(6L));
        verify(aemEvaluationQuestionnaireService, never()).selectAemEvaluationQuestionnaireByQuestionnaireId(any());
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(aemEvaluationQuestionnaireService.insertAemEvaluationQuestionnaire(any(AemEvaluationQuestionnaire.class))).thenReturn(1);

        String body = "{\"title\":\"期末评估\",\"semesterId\":1}";
        mockMvc.perform(post("/aem/questionnaire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<AemEvaluationQuestionnaire> captor = ArgumentCaptor.forClass(AemEvaluationQuestionnaire.class);
        verify(aemEvaluationQuestionnaireService).insertAemEvaluationQuestionnaire(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("期末评估", captor.getValue().getTitle());
    }

    @Test
    @DisplayName("add：缺失必填title被@NotBlank拦截返回400，不落库")
    void add_missingTitleRejected() throws Exception {
        String body = "{\"semesterId\":1}";
        mockMvc.perform(post("/aem/questionnaire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(aemEvaluationQuestionnaireService, never()).insertAemEvaluationQuestionnaire(any());
    }

    @Test
    @DisplayName("edit：更新走服务方法并透传域对象")
    void edit_passthrough() throws Exception {
        when(aemEvaluationQuestionnaireService.updateAemEvaluationQuestionnaire(any(AemEvaluationQuestionnaire.class))).thenReturn(1);

        String body = "{\"questionnaireId\":5,\"title\":\"更新标题\"}";
        mockMvc.perform(put("/aem/questionnaire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(aemEvaluationQuestionnaireService).updateAemEvaluationQuestionnaire(any(AemEvaluationQuestionnaire.class));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(aemEvaluationQuestionnaireService.deleteAemEvaluationQuestionnaireByQuestionnaireIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/aem/questionnaire/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(aemEvaluationQuestionnaireService).deleteAemEvaluationQuestionnaireByQuestionnaireIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new Long[] {1L, 2L}, captor.getValue());
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(aemEvaluationQuestionnaireService.selectAemEvaluationQuestionnaireList(any(AemEvaluationQuestionnaire.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/aem/questionnaire/list").param("title", "评估"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<AemEvaluationQuestionnaire> captor = ArgumentCaptor.forClass(AemEvaluationQuestionnaire.class);
        verify(aemEvaluationQuestionnaireService).selectAemEvaluationQuestionnaireList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("评估", captor.getValue().getTitle());
    }
}
