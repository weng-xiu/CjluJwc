package com.yu.portal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

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

import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.common.exception.ServiceException;
import com.yu.portal.controller.PortalThesisController.ThesisAction;
import com.yu.sam.domain.SamDegreeReview;
import com.yu.sam.domain.SamStudent;
import com.yu.sam.domain.SamThesis;
import com.yu.sam.domain.SamThesisTopic;
import com.yu.sam.service.ISamDegreeReviewService;
import com.yu.sam.service.ISamStudentService;
import com.yu.sam.service.ISamThesisService;
import com.yu.sam.service.ISamThesisTopicService;

/**
 * 毕业论文门户接口层测试（Q1 第四批：MockMvc standalone）。
 * 校验毕业论文师生双侧的归属/权限契约：topics 强制 status="1"；
 * my 按本人学籍查询（无档案返回 null 语义）；choose/submit 以本人 studentId 口径、
 * 越权操作他人论文抛 ServiceException；advisorList 非管理者强制 advisor=登录名；
 * detail 三层归属校验（不存在/无权/放行）；audit/check/archive 仅指导教师或教务；
 * degreePreview 以本人 studentId 试算。环节流转与查重/归档逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalThesisControllerMockMvcTest {

    @Mock
    private ISamThesisService samThesisService;

    @Mock
    private ISamThesisTopicService samThesisTopicService;

    @Mock
    private ISamStudentService samStudentService;

    @Mock
    private ISamDegreeReviewService samDegreeReviewService;

    @InjectMocks
    private PortalThesisController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        loginAs(1L); // 默认管理员：getUsername()=tester，isThesisManager=true
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(long userId) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setUserName("tester");
        LoginUser loginUser = new LoginUser(userId, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    private SamStudent student(long studentId) {
        SamStudent s = new SamStudent();
        s.setStudentId(studentId);
        return s;
    }

    private SamThesis thesis(long thesisId, long studentId, String advisor) {
        SamThesis t = new SamThesis();
        t.setThesisId(thesisId);
        t.setStudentId(studentId);
        t.setAdvisor(advisor);
        return t;
    }

    @Test
    @DisplayName("topics：查询条件强制 status='1'（仅可选题）")
    void topics_forcesAvailableStatus() throws Exception {
        when(samThesisTopicService.selectSamThesisTopicList(any(SamThesisTopic.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/thesis/topics").param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());

        ArgumentCaptor<SamThesisTopic> captor = ArgumentCaptor.forClass(SamThesisTopic.class);
        verify(samThesisTopicService).selectSamThesisTopicList(captor.capture());
        Assertions.assertEquals("1", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("my：本人尚无论文档案时返回 null 语义（不查详情）")
    void my_emptyReturnsNull() throws Exception {
        when(samStudentService.selectSamStudentByUserId(1L)).thenReturn(student(10L));
        when(samThesisService.selectSamThesisList(any(SamThesis.class))).thenReturn(List.of());

        mockMvc.perform(get("/portal/thesis/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samThesisService, never()).selectDetailWithProcess(anyLong());
    }

    @Test
    @DisplayName("my：有档案时按本人 studentId 过滤并返回环节留痕详情")
    void my_returnsDetailOfOwnThesis() throws Exception {
        when(samStudentService.selectSamStudentByUserId(1L)).thenReturn(student(10L));
        when(samThesisService.selectSamThesisList(any(SamThesis.class)))
                .thenReturn(List.of(thesis(5L, 10L, "tester")));
        when(samThesisService.selectDetailWithProcess(5L)).thenReturn(thesis(5L, 10L, "tester"));

        mockMvc.perform(get("/portal/thesis/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamThesis> captor = ArgumentCaptor.forClass(SamThesis.class);
        verify(samThesisService).selectSamThesisList(captor.capture());
        Assertions.assertEquals(Long.valueOf(10L), captor.getValue().getStudentId());
        verify(samThesisService).selectDetailWithProcess(5L);
    }

    @Test
    @DisplayName("choose：以本人 studentId 与登录名选题")
    void choose_bindsStudentAndOperator() throws Exception {
        when(samStudentService.selectSamStudentByUserId(1L)).thenReturn(student(10L));
        when(samThesisService.chooseTopic(10L, 7L, "tester")).thenReturn(thesis(1L, 10L, "tester"));

        mockMvc.perform(post("/portal/thesis/choose")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"topicId\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samThesisService).chooseTopic(10L, 7L, "tester");
    }

    @Test
    @DisplayName("choose：登录账号未关联学籍时抛 ServiceException")
    void choose_noStudentThrows() {
        // 未 stub 学籍 → 两次查询均返回 null
        ThesisAction action = new ThesisAction();
        action.setTopicId(7L);
        Assertions.assertThrows(ServiceException.class, () -> controller.choose(action));
        verify(samThesisService, never()).chooseTopic(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("submit：本人论文提交环节材料成功")
    void submit_ownedThesisSuccess() throws Exception {
        when(samStudentService.selectSamStudentByUserId(1L)).thenReturn(student(10L));
        when(samThesisService.selectSamThesisByThesisId(1L)).thenReturn(thesis(1L, 10L, "tester"));
        when(samThesisService.submitStage(1L, "开题", "t", "c", "a", "tester")).thenReturn(1);

        mockMvc.perform(post("/portal/thesis/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"thesisId\":1,\"stage\":\"开题\",\"title\":\"t\",\"content\":\"c\",\"attachment\":\"a\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samThesisService).submitStage(1L, "开题", "t", "c", "a", "tester");
    }

    @Test
    @DisplayName("submit：操作他人论文抛 ServiceException（防越权）")
    void submit_otherThesisRejected() {
        when(samStudentService.selectSamStudentByUserId(1L)).thenReturn(student(10L));
        when(samThesisService.selectSamThesisByThesisId(1L)).thenReturn(thesis(1L, 99L, "someone"));

        ThesisAction action = new ThesisAction();
        action.setThesisId(1L);
        action.setStage("开题");
        Assertions.assertThrows(ServiceException.class, () -> controller.submit(action));
        verify(samThesisService, never()).submitStage(anyLong(), anyString(), any(), any(), any(), anyString());
    }

    @Test
    @DisplayName("advisorList：管理员保留传入 advisor 查询条件")
    void advisorList_adminKeepsAdvisor() throws Exception {
        when(samThesisService.selectSamThesisList(any(SamThesis.class))).thenReturn(List.of());

        mockMvc.perform(get("/portal/thesis/advisorList").param("advisor", "zzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SamThesis> captor = ArgumentCaptor.forClass(SamThesis.class);
        verify(samThesisService).selectSamThesisList(captor.capture());
        Assertions.assertEquals("zzz", captor.getValue().getAdvisor());
    }

    @Test
    @DisplayName("advisorList：非管理者强制 advisor=登录名（仅见名下学生）")
    void advisorList_nonManagerForcesSelf() throws Exception {
        loginAs(100L);
        when(samThesisService.selectSamThesisList(any(SamThesis.class))).thenReturn(List.of());

        mockMvc.perform(get("/portal/thesis/advisorList").param("advisor", "zzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamThesis> captor = ArgumentCaptor.forClass(SamThesis.class);
        verify(samThesisService).selectSamThesisList(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getAdvisor());
    }

    @Test
    @DisplayName("detail：论文不存在返回错误码 500")
    void detail_notFound() throws Exception {
        when(samThesisService.selectSamThesisByThesisId(1L)).thenReturn(null);

        mockMvc.perform(get("/portal/thesis/detail/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("论文档案不存在"));
    }

    @Test
    @DisplayName("detail：非本人/非指导教师/非教务拒绝查看")
    void detail_forbiddenForOther() throws Exception {
        loginAs(100L);
        when(samThesisService.selectSamThesisByThesisId(1L)).thenReturn(thesis(1L, 99L, "someoneElse"));
        // 学籍两次查询均 null → isMyThesis 捕获异常返回 false

        mockMvc.perform(get("/portal/thesis/detail/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("无权查看该论文档案"));
    }

    @Test
    @DisplayName("detail：教务管理员可查看详情")
    void detail_adminSuccess() throws Exception {
        when(samThesisService.selectSamThesisByThesisId(1L)).thenReturn(thesis(1L, 99L, "someoneElse"));
        when(samThesisService.selectDetailWithProcess(1L)).thenReturn(thesis(1L, 99L, "someoneElse"));

        mockMvc.perform(get("/portal/thesis/detail/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("audit：指导教师/教务审核环节，pass 布尔与分数透传")
    void audit_success() throws Exception {
        when(samThesisService.selectSamThesisByThesisId(1L)).thenReturn(thesis(1L, 10L, "tester"));
        when(samThesisService.auditStage(1L, "中期", true, 85.0, "ok", "tester")).thenReturn(1);

        mockMvc.perform(post("/portal/thesis/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"thesisId\":1,\"stage\":\"中期\",\"pass\":true,\"score\":85.0,\"opinion\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samThesisService).auditStage(1L, "中期", true, 85.0, "ok", "tester");
    }

    @Test
    @DisplayName("audit：非指导教师且非教务审核抛 ServiceException")
    void audit_notAuditableThrows() {
        loginAs(100L);
        when(samThesisService.selectSamThesisByThesisId(1L)).thenReturn(thesis(1L, 10L, "someoneElse"));

        ThesisAction action = new ThesisAction();
        action.setThesisId(1L);
        action.setStage("中期");
        Assertions.assertThrows(ServiceException.class, () -> controller.audit(action));
    }

    @Test
    @DisplayName("check：查重结果登记透传本人登录名")
    void check_success() throws Exception {
        when(samThesisService.selectSamThesisByThesisId(1L)).thenReturn(thesis(1L, 10L, "tester"));
        when(samThesisService.recordCheck(1L, 12.5, "att", "op", "tester")).thenReturn(1);

        mockMvc.perform(post("/portal/thesis/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"thesisId\":1,\"score\":12.5,\"attachment\":\"att\",\"opinion\":\"op\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samThesisService).recordCheck(1L, 12.5, "att", "op", "tester");
    }

    @Test
    @DisplayName("archive：成绩归档透传总分/答辩分")
    void archive_success() throws Exception {
        when(samThesisService.selectSamThesisByThesisId(1L)).thenReturn(thesis(1L, 10L, "tester"));
        when(samThesisService.archiveGrade(1L, 90.0, 88.0, "op", "tester")).thenReturn(1);

        mockMvc.perform(post("/portal/thesis/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"thesisId\":1,\"totalScore\":90.0,\"defenseScore\":88.0,\"opinion\":\"op\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samThesisService).archiveGrade(1L, 90.0, 88.0, "op", "tester");
    }

    @Test
    @DisplayName("degreePreview：以本人 studentId 试算学位资格")
    void degreePreview_bindsStudent() throws Exception {
        when(samStudentService.selectSamStudentByUserId(1L)).thenReturn(student(10L));
        when(samDegreeReviewService.simulateReview(10L)).thenReturn(new SamDegreeReview());

        mockMvc.perform(get("/portal/thesis/degreePreview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samDegreeReviewService).simulateReview(10L);
    }
}
