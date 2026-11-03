package com.yu.portal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.aem.domain.AemExamInvigilation;
import com.yu.aem.domain.AemExamPlan;
import com.yu.aem.service.IAemExamInvigilationService;
import com.yu.aem.service.IAemExamPlanService;
import com.yu.brm.domain.BrmClassroom;
import com.yu.brm.service.IBrmClassroomService;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;

/**
 * 门户考试与监考接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验学生端考试列表走门户专用查询（不套部门数据范围），教师端监考列表逐条回填考试名/教室名，
 * 且相同 examId/classroomId 经本地缓存去重仅查询一次、外键为空则不触发关联查询。
 * 考试冲突检测、监考排布等业务由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalExamControllerMockMvcTest {

    @Mock
    private IAemExamPlanService aemExamPlanService;

    @Mock
    private IAemExamInvigilationService aemExamInvigilationService;

    @Mock
    private IBrmClassroomService brmClassroomService;

    @InjectMocks
    private PortalExamController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        loginAs(1L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** standalone 模式手动注入登录用户（userId=1 为 admin） */
    private void loginAs(Long userId) {
        SysUser sysUser = new SysUser();
        sysUser.setUserName("tester");
        LoginUser loginUser = new LoginUser(userId, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @Test
    @DisplayName("list：学生端走门户专用考试查询")
    void list_usesPortalScopedQuery() throws Exception {
        when(aemExamPlanService.selectAemExamPlanListForPortal(any(AemExamPlan.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/exam/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        verify(aemExamPlanService).selectAemExamPlanListForPortal(any(AemExamPlan.class));
        verify(aemExamPlanService, never()).selectAemExamPlanList(any(AemExamPlan.class));
    }

    @Test
    @DisplayName("invigilationList：回填考试名/教室名，相同外键缓存去重仅查一次")
    void invigilationList_fillsNamesWithCacheDedup() throws Exception {
        AemExamInvigilation a = new AemExamInvigilation();
        a.setExamId(1L);
        a.setClassroomId(2L);
        AemExamInvigilation b = new AemExamInvigilation();
        b.setExamId(1L); // 与 a 相同考试
        b.setClassroomId(2L); // 与 a 相同教室
        when(aemExamInvigilationService.selectAemExamInvigilationList(any(AemExamInvigilation.class)))
                .thenReturn(List.of(a, b));
        AemExamPlan plan = new AemExamPlan();
        plan.setExamName("期末考");
        when(aemExamPlanService.selectAemExamPlanByExamId(1L)).thenReturn(plan);
        BrmClassroom room = new BrmClassroom();
        room.setClassroomName("A101");
        when(brmClassroomService.selectBrmClassroomByClassroomId(2L)).thenReturn(room);

        mockMvc.perform(get("/portal/exam/invigilationList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].examName").value("期末考"))
                .andExpect(jsonPath("$.rows[0].classroomName").value("A101"))
                .andExpect(jsonPath("$.rows[1].examName").value("期末考"));

        // 两条相同外键只触发一次关联查询（computeIfAbsent 缓存）
        verify(aemExamPlanService, times(1)).selectAemExamPlanByExamId(1L);
        verify(brmClassroomService, times(1)).selectBrmClassroomByClassroomId(2L);
    }

    @Test
    @DisplayName("invigilationList：考试/教室外键为空时不触发关联查询")
    void invigilationList_absentForeignKeysNoLookup() throws Exception {
        AemExamInvigilation a = new AemExamInvigilation();
        when(aemExamInvigilationService.selectAemExamInvigilationList(any(AemExamInvigilation.class)))
                .thenReturn(List.of(a));

        mockMvc.perform(get("/portal/exam/invigilationList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].examName").doesNotExist());

        verify(aemExamPlanService, never()).selectAemExamPlanByExamId(any());
        verify(brmClassroomService, never()).selectBrmClassroomByClassroomId(any());
    }

    @Test
    @DisplayName("invigilationList：关联记录缺失时回填 null 而非报错")
    void invigilationList_missingRelationNull() throws Exception {
        AemExamInvigilation a = new AemExamInvigilation();
        a.setExamId(9L);
        when(aemExamInvigilationService.selectAemExamInvigilationList(any(AemExamInvigilation.class)))
                .thenReturn(List.of(a));
        when(aemExamPlanService.selectAemExamPlanByExamId(9L)).thenReturn(null);

        mockMvc.perform(get("/portal/exam/invigilationList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].examName").doesNotExist());

        ArgumentCaptor<AemExamInvigilation> captor = ArgumentCaptor.forClass(AemExamInvigilation.class);
        verify(aemExamInvigilationService).selectAemExamInvigilationList(captor.capture());
        Assertions.assertNull(captor.getValue().getExamName());
    }

    @Test
    @DisplayName("invigilationList：S-5 非管理员强制绑定登录教师，不传参数也只看本人监考")
    void invigilationList_nonAdminForcesOwnTeacher() throws Exception {
        loginAs(600L);
        when(aemExamInvigilationService.selectAemExamInvigilationList(any(AemExamInvigilation.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/exam/invigilationList").param("teacherId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<AemExamInvigilation> captor = ArgumentCaptor.forClass(AemExamInvigilation.class);
        verify(aemExamInvigilationService).selectAemExamInvigilationList(captor.capture());
        Assertions.assertEquals(Long.valueOf(600L), captor.getValue().getTeacherId());
    }
}
