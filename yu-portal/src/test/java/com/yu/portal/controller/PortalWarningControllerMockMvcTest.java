package com.yu.portal.controller;

import static org.mockito.ArgumentMatchers.any;
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

import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.sam.domain.SamWarning;
import com.yu.sam.service.ISamWarningService;

/**
 * 门户学业预警接口层测试（Q1 第六批：MockMvc standalone）。
 * 校验预警列表强制绑定登录学生（外部 studentId 参数不进入查询对象）、
 * 可选 semesterId 有/无两分支透传，以及 statistics 的按类型（GPA/学分/出勤/综合）、
 * 按级别（一般/严重/高危）聚合与"未解除"口径（isResolved=0 或 null 均计未解除）；
 * 预警生成与阈值判定业务逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalWarningControllerMockMvcTest {

    @Mock
    private ISamWarningService samWarningService;

    @InjectMocks
    private PortalWarningController controller;

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

    private SamWarning warning(String type, String level, String resolved) {
        SamWarning w = new SamWarning();
        w.setWarningType(type);
        w.setWarningLevel(level);
        w.setIsResolved(resolved);
        return w;
    }

    @Test
    @DisplayName("list：查询对象强制绑定登录学生并透传显式 semesterId")
    void list_forcesLoggedInStudentScope() throws Exception {
        SamWarning w = warning("0", "1", "0");
        w.setWarningId(5L);
        when(samWarningService.selectSamWarningList(any(SamWarning.class)))
                .thenReturn(List.of(w));

        mockMvc.perform(get("/portal/warning/list").param("semesterId", "20261"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].warningId").value(5))
                .andExpect(jsonPath("$.rows[0].warningType").value("0"));

        ArgumentCaptor<SamWarning> captor = ArgumentCaptor.forClass(SamWarning.class);
        verify(samWarningService).selectSamWarningList(captor.capture());
        Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getStudentId());
        Assertions.assertEquals(Long.valueOf(20261L), captor.getValue().getSemesterId());
    }

    @Test
    @DisplayName("list：semesterId 缺省时以 null 查询全部学期")
    void list_semesterAbsentPassesNull() throws Exception {
        when(samWarningService.selectSamWarningList(any(SamWarning.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/warning/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows").isEmpty());

        ArgumentCaptor<SamWarning> captor = ArgumentCaptor.forClass(SamWarning.class);
        verify(samWarningService).selectSamWarningList(captor.capture());
        Assertions.assertNull(captor.getValue().getSemesterId());
    }

    @Test
    @DisplayName("statistics：按类型/级别聚合计数，查询以登录学生+semesterId 透传")
    void statistics_aggregatesByTypeAndLevel() throws Exception {
        List<SamWarning> list = List.of(
                warning("0", "0", "1"), // GPA 一般 已解除
                warning("0", "1", "0"), // GPA 严重 未解除
                warning("1", "1", null), // 学分 严重 未解除（null 视同未解除）
                warning("2", "2", "0"), // 出勤 高危 未解除
                warning("3", "0", "1")); // 综合 一般 已解除
        when(samWarningService.selectSamWarningList(any(SamWarning.class))).thenReturn(list);

        mockMvc.perform(get("/portal/warning/statistics").param("semesterId", "20261"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.unresolvedCount").value(3))
                .andExpect(jsonPath("$.data.seriousCount").value(2));

        verify(samWarningService).selectSamWarningList(any(SamWarning.class));
    }

    @Test
    @DisplayName("statistics：类型四分类计数正确（gpa/credit/attendance/comprehensive）")
    void statistics_typeBreakdown() throws Exception {
        List<SamWarning> list = List.of(
                warning("0", "0", "0"),
                warning("1", "1", "1"),
                warning("2", "2", "1"),
                warning("3", "0", "1"));
        when(samWarningService.selectSamWarningList(any(SamWarning.class))).thenReturn(list);

        mockMvc.perform(get("/portal/warning/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gpaCount").value(1))
                .andExpect(jsonPath("$.data.creditCount").value(1))
                .andExpect(jsonPath("$.data.attendanceCount").value(1))
                .andExpect(jsonPath("$.data.comprehensiveCount").value(1))
                .andExpect(jsonPath("$.data.highRiskCount").value(1));
    }

    @Test
    @DisplayName("statistics：无预警记录时各计数为 0（空态契约而非报错）")
    void statistics_emptyListAllZero() throws Exception {
        when(samWarningService.selectSamWarningList(any(SamWarning.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/warning/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.unresolvedCount").value(0))
                .andExpect(jsonPath("$.data.normalCount").value(0));

        ArgumentCaptor<SamWarning> captor = ArgumentCaptor.forClass(SamWarning.class);
        verify(samWarningService).selectSamWarningList(captor.capture());
        Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getStudentId());
        Assertions.assertNull(captor.getValue().getSemesterId());
    }
}
