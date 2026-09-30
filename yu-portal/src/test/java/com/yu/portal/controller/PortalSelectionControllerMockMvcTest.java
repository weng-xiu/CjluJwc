package com.yu.portal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.domain.TpmSelectionEnrollment;
import com.yu.tpm.domain.TpmSelectionRound;
import com.yu.tpm.domain.dto.ConflictWarning;
import com.yu.tpm.service.ITpmCourseOfferingService;
import com.yu.tpm.service.ITpmSelectionEnrollmentService;
import com.yu.tpm.service.ITpmSelectionRoundService;

/**
 * 门户选课中心接口层测试（Q1 第六批：MockMvc standalone）。
 * 校验可选课程列表的"移动端只展示已确认开课"默认状态位与显式条件保留、
 * validate 的 (登录用户, 开课, 轮次) 三元组透传、enrollWithValidation 将服务层
 * AjaxResult（含自定义 msg/code）原样直返而非 toAjax 包装、enroll/result 的强制本人绑定、
 * drop 路径变量与成败双分支；抽签、容量扣减、冲突算法等业务逻辑由服务层单测覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalSelectionControllerMockMvcTest {

    @Mock
    private ITpmSelectionRoundService tpmSelectionRoundService;

    @Mock
    private ITpmSelectionEnrollmentService tpmSelectionEnrollmentService;

    @Mock
    private ITpmCourseOfferingService tpmCourseOfferingService;

    @InjectMocks
    private PortalSelectionController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        // BaseController.getUserId() 依赖 SecurityContext，standalone 模式手动注入登录用户（userId=1）
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

    @Test
    @DisplayName("courseList：未传状态时默认只查已确认开课（offeringStatus=1）")
    void courseList_defaultsToConfirmedStatus() throws Exception {
        when(tpmCourseOfferingService.selectTpmCourseOfferingListForPortal(any(TpmCourseOffering.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/selection/courseList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<TpmCourseOffering> captor = ArgumentCaptor.forClass(TpmCourseOffering.class);
        verify(tpmCourseOfferingService).selectTpmCourseOfferingListForPortal(captor.capture());
        Assertions.assertEquals("1", captor.getValue().getOfferingStatus());
    }

    @Test
    @DisplayName("courseList：显式传入 offeringStatus 时保留原条件不覆盖")
    void courseList_keepsExplicitStatus() throws Exception {
        when(tpmCourseOfferingService.selectTpmCourseOfferingListForPortal(any(TpmCourseOffering.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/selection/courseList").param("offeringStatus", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmCourseOffering> captor = ArgumentCaptor.forClass(TpmCourseOffering.class);
        verify(tpmCourseOfferingService).selectTpmCourseOfferingListForPortal(captor.capture());
        Assertions.assertEquals("0", captor.getValue().getOfferingStatus());
    }

    @Test
    @DisplayName("roundList：轮次查询条件绑定域对象并返回表格结构")
    void roundList_bindsQuery() throws Exception {
        TpmSelectionRound r = new TpmSelectionRound();
        r.setRoundId(3L);
        r.setRoundName("第一轮普选");
        when(tpmSelectionRoundService.selectTpmSelectionRoundList(any(TpmSelectionRound.class)))
                .thenReturn(List.of(r));

        mockMvc.perform(get("/portal/selection/roundList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].roundId").value(3))
                .andExpect(jsonPath("$.rows[0].roundName").value("第一轮普选"));
    }

    @Test
    @DisplayName("validate：三元组以登录用户+body 字段透传冲突检测，冲突列表落 data")
    void validate_passesTriplesAndReturnsConflicts() throws Exception {
        ConflictWarning warn = new ConflictWarning();
        when(tpmSelectionEnrollmentService.checkSelectionConflicts(eq(1L), eq(20L), eq(3L)))
                .thenReturn(List.of(warn));

        String body = "{\"courseOfferingId\":20,\"roundId\":3,\"studentId\":999}";
        mockMvc.perform(post("/portal/selection/validate")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());

        // body 里伪造的 studentId 被忽略，检测以登录用户为准
        verify(tpmSelectionEnrollmentService).checkSelectionConflicts(eq(1L), eq(20L), eq(3L));
    }

    @Test
    @DisplayName("enrollWithValidation：服务层失败结果（含 msg）原样直返，不经 toAjax 包装")
    void enrollWithValidation_forwardsServiceFailure() throws Exception {
        when(tpmSelectionEnrollmentService.enrollWithValidation(eq(1L), eq(20L), eq(3L)))
                .thenReturn(AjaxResult.error("该课程容量已满，请选择其他课程"));

        String body = "{\"courseOfferingId\":20,\"roundId\":3}";
        mockMvc.perform(post("/portal/selection/enrollWithValidation")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("该课程容量已满，请选择其他课程"));
    }

    @Test
    @DisplayName("enrollWithValidation：服务层带 data 成功结果直返")
    void enrollWithValidation_forwardsServiceSuccess() throws Exception {
        when(tpmSelectionEnrollmentService.enrollWithValidation(eq(1L), eq(20L), eq(3L)))
                .thenReturn(AjaxResult.success("选课成功", "WL-1"));

        String body = "{\"courseOfferingId\":20,\"roundId\":3}";
        mockMvc.perform(post("/portal/selection/enrollWithValidation")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("选课成功"))
                .andExpect(jsonPath("$.data").value("WL-1"));
    }

    @Test
    @DisplayName("enroll：选课记录强制绑定当前登录学生后落库")
    void enroll_forcesOwnStudentId() throws Exception {
        when(tpmSelectionEnrollmentService.insertTpmSelectionEnrollment(any(TpmSelectionEnrollment.class)))
                .thenReturn(1);

        String body = "{\"courseOfferingId\":20,\"roundId\":3,\"studentId\":999}";
        mockMvc.perform(post("/portal/selection/enroll")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmSelectionEnrollment> captor = ArgumentCaptor.forClass(TpmSelectionEnrollment.class);
        verify(tpmSelectionEnrollmentService).insertTpmSelectionEnrollment(captor.capture());
        Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getStudentId());
        Assertions.assertEquals(Long.valueOf(20L), captor.getValue().getCourseOfferingId());
    }

    @Test
    @DisplayName("drop：报名ID走路径变量，删除成功/失败映射 toAjax 双分支")
    void drop_pathVariableAndBothBranches() throws Exception {
        when(tpmSelectionEnrollmentService.deleteTpmSelectionEnrollmentByEnrollId(eq(7L))).thenReturn(1);
        mockMvc.perform(delete("/portal/selection/drop/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        when(tpmSelectionEnrollmentService.deleteTpmSelectionEnrollmentByEnrollId(eq(8L))).thenReturn(0);
        mockMvc.perform(delete("/portal/selection/drop/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(tpmSelectionEnrollmentService).deleteTpmSelectionEnrollmentByEnrollId(eq(7L));
        verify(tpmSelectionEnrollmentService).deleteTpmSelectionEnrollmentByEnrollId(eq(8L));
    }

    @Test
    @DisplayName("result：查询强制以登录用户新建查询对象（不接受任何外部条件泄漏）")
    void result_forcesOwnStudentQuery() throws Exception {
        TpmSelectionEnrollment e = new TpmSelectionEnrollment();
        e.setEnrollId(11L);
        e.setStudentId(1L);
        when(tpmSelectionEnrollmentService.selectTpmSelectionEnrollmentList(any(TpmSelectionEnrollment.class)))
                .thenReturn(List.of(e));

        mockMvc.perform(get("/portal/selection/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].enrollId").value(11));

        ArgumentCaptor<TpmSelectionEnrollment> captor = ArgumentCaptor.forClass(TpmSelectionEnrollment.class);
        verify(tpmSelectionEnrollmentService).selectTpmSelectionEnrollmentList(captor.capture());
        Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getStudentId());
        Assertions.assertNull(captor.getValue().getCourseOfferingId());
    }

    @Test
    @DisplayName("result：登录用户无选课记录时返回空 rows 而非报错")
    void result_emptyRows() throws Exception {
        when(tpmSelectionEnrollmentService.selectTpmSelectionEnrollmentList(any(TpmSelectionEnrollment.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/selection/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isEmpty());

        verify(tpmSelectionEnrollmentService, never()).insertTpmSelectionEnrollment(any(TpmSelectionEnrollment.class));
    }
}
