package com.yu.portal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.aem.domain.AemEvaluationQuestion;
import com.yu.aem.domain.AemEvaluationQuestionnaire;
import com.yu.aem.domain.AemEvaluationResult;
import com.yu.aem.service.IAemEvaluationQuestionService;
import com.yu.aem.service.IAemEvaluationQuestionnaireService;
import com.yu.aem.service.IAemEvaluationResultService;
import com.yu.aem.service.IAemEvaluationStatService;
import com.yu.brm.domain.BrmTeacher;
import com.yu.brm.service.IBrmTeacherService;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;

/**
 * 门户评教接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验学生端问卷列表按登录学生标记 completed（关联查询强制 studentId=本人，空列表不查已完成）、
 * 提交评教的三重契约（缺问卷ID error、重复提交 error、首次提交回填本人 studentId 与评教时间），
 * status 依未完成问卷数给出 completed/message，questions 题目投影字段口径；
 * 教师端 teacherResults/myReport 依 selectBrmTeacherByUserId 命中与否返回空/汇总或 error，
 * courseComments 过滤空白评语。评教统计算法由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalEvaluationControllerMockMvcTest {

    @Mock
    private IAemEvaluationQuestionnaireService aemEvaluationQuestionnaireService;

    @Mock
    private IAemEvaluationQuestionService aemEvaluationQuestionService;

    @Mock
    private IAemEvaluationResultService aemEvaluationResultService;

    @Mock
    private IAemEvaluationStatService aemEvaluationStatService;

    @Mock
    private IBrmTeacherService brmTeacherService;

    @InjectMocks
    private PortalEvaluationController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
        sysUser.setUserName("tester");
        LoginUser loginUser = new LoginUser(1L, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private AemEvaluationQuestionnaire q(long id) {
        AemEvaluationQuestionnaire questionnaire = new AemEvaluationQuestionnaire();
        questionnaire.setQuestionnaireId(id);
        return questionnaire;
    }

    @Test
    @DisplayName("questionnaireList：按登录学生已完成记录标记 completed")
    void questionnaireList_marksCompleted() throws Exception {
        when(aemEvaluationQuestionnaireService.selectAemEvaluationQuestionnaireList(any(AemEvaluationQuestionnaire.class)))
                .thenReturn(new ArrayList<>(List.of(q(1L), q(2L))));
        AemEvaluationResult done = new AemEvaluationResult();
        done.setQuestionnaireId(1L);
        when(aemEvaluationResultService.selectAemEvaluationResultList(any(AemEvaluationResult.class)))
                .thenReturn(List.of(done));

        mockMvc.perform(get("/portal/evaluation/questionnaireList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].completed").value(true))
                .andExpect(jsonPath("$.rows[1].completed").value(false));

        ArgumentCaptor<AemEvaluationResult> captor = ArgumentCaptor.forClass(AemEvaluationResult.class);
        verify(aemEvaluationResultService).selectAemEvaluationResultList(captor.capture());
        Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getStudentId());
    }

    @Test
    @DisplayName("questionnaireList：无问卷时不查询已完成记录")
    void questionnaireList_emptySkipsResultQuery() throws Exception {
        when(aemEvaluationQuestionnaireService.selectAemEvaluationQuestionnaireList(any(AemEvaluationQuestionnaire.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/evaluation/questionnaireList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows").isEmpty());

        verify(aemEvaluationResultService, never()).selectAemEvaluationResultList(any());
    }

    @Test
    @DisplayName("submit：缺问卷ID返回 error 且不落库")
    void submit_missingQuestionnaireIdError() throws Exception {
        mockMvc.perform(post("/portal/evaluation/submit")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"totalScore\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(aemEvaluationResultService, never()).insertAemEvaluationResult(any());
    }

    @Test
    @DisplayName("submit：重复提交被防重校验拒绝")
    void submit_duplicateRejected() throws Exception {
        when(aemEvaluationResultService.selectAemEvaluationResultList(any(AemEvaluationResult.class)))
                .thenReturn(List.of(new AemEvaluationResult()));

        mockMvc.perform(post("/portal/evaluation/submit")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"questionnaireId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(aemEvaluationResultService, never()).insertAemEvaluationResult(any());
    }

    @Test
    @DisplayName("submit：首次提交回填本人 studentId 与评教时间后落库")
    void submit_freshBindsStudent() throws Exception {
        when(aemEvaluationResultService.selectAemEvaluationResultList(any(AemEvaluationResult.class)))
                .thenReturn(List.of());
        when(aemEvaluationResultService.insertAemEvaluationResult(any(AemEvaluationResult.class))).thenReturn(1);

        mockMvc.perform(post("/portal/evaluation/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionnaireId\":1,\"studentId\":999,\"totalScore\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<AemEvaluationResult> captor = ArgumentCaptor.forClass(AemEvaluationResult.class);
        verify(aemEvaluationResultService).insertAemEvaluationResult(captor.capture());
        Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getStudentId()); // 覆盖伪造 999
        Assertions.assertNotNull(captor.getValue().getEvalDate());
    }

    @Test
    @DisplayName("status：有未完成问卷时 completed=false 且提示份数")
    void status_hasPending() throws Exception {
        when(aemEvaluationQuestionnaireService.selectAemEvaluationQuestionnaireList(any(AemEvaluationQuestionnaire.class)))
                .thenReturn(List.of(q(1L)));

        mockMvc.perform(get("/portal/evaluation/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(false))
                .andExpect(jsonPath("$.data.message").value(org.hamcrest.Matchers.containsString("1 份")));
    }

    @Test
    @DisplayName("status：无待评问卷时 completed=true")
    void status_allDone() throws Exception {
        when(aemEvaluationQuestionnaireService.selectAemEvaluationQuestionnaireList(any(AemEvaluationQuestionnaire.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/evaluation/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(true));
    }

    @Test
    @DisplayName("questions：按问卷ID+启用状态查询并投影题目字段")
    void questions_projectsFields() throws Exception {
        AemEvaluationQuestion question = new AemEvaluationQuestion();
        question.setQuestionId(11L);
        question.setQuestionType("1");
        question.setQuestionContent("教学质量如何？");
        question.setMaxScore(5.0);
        question.setSortOrder(1);
        when(aemEvaluationQuestionService.selectAemEvaluationQuestionList(any(AemEvaluationQuestion.class)))
                .thenReturn(List.of(question));

        mockMvc.perform(get("/portal/evaluation/questions/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].questionTitle").value("教学质量如何？"))
                .andExpect(jsonPath("$.data[0].questionId").value(11));

        ArgumentCaptor<AemEvaluationQuestion> captor = ArgumentCaptor.forClass(AemEvaluationQuestion.class);
        verify(aemEvaluationQuestionService).selectAemEvaluationQuestionList(captor.capture());
        Assertions.assertEquals(Long.valueOf(3L), captor.getValue().getQuestionnaireId());
        Assertions.assertEquals("0", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("teacherResults：无教师档案返回空列表")
    void teacherResults_noTeacherEmpty() throws Exception {
        when(brmTeacherService.selectBrmTeacherByUserId(1L)).thenReturn(null);

        mockMvc.perform(get("/portal/evaluation/teacherResults"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("teacherResults：命中教师档案则按其 teacherId 汇总")
    void teacherResults_byTeacher() throws Exception {
        BrmTeacher teacher = new BrmTeacher();
        teacher.setTeacherId(55L);
        when(brmTeacherService.selectBrmTeacherByUserId(1L)).thenReturn(teacher);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("courseName", "高等数学");
        when(aemEvaluationStatService.teacherCourseBreakdown(55L)).thenReturn(List.of(row));

        mockMvc.perform(get("/portal/evaluation/teacherResults"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].courseName").value("高等数学"));

        verify(aemEvaluationStatService).teacherCourseBreakdown(55L);
    }

    @Test
    @DisplayName("myReport：无教师档案返回 error")
    void myReport_noTeacherError() throws Exception {
        when(brmTeacherService.selectBrmTeacherByUserId(1L)).thenReturn(null);

        mockMvc.perform(get("/portal/evaluation/myReport"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("courseComments：过滤空白评语仅保留有效评语")
    void courseComments_filtersBlank() throws Exception {
        AemEvaluationResult withComment = new AemEvaluationResult();
        withComment.setComment("讲课生动");
        AemEvaluationResult blank = new AemEvaluationResult();
        blank.setComment("   ");
        AemEvaluationResult nullComment = new AemEvaluationResult();
        when(aemEvaluationResultService.selectAemEvaluationResultList(any(AemEvaluationResult.class)))
                .thenReturn(List.of(withComment, blank, nullComment));

        mockMvc.perform(get("/portal/evaluation/comments/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0]").value("讲课生动"));
    }
}
