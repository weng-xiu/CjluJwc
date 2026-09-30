package com.yu.portal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.service.ITpmScheduleService;

/**
 * 门户课表接口层测试（Q1 第六批：MockMvc standalone）。
 * 校验学生课表 myList 以登录用户（而非外部参数）作为查询主体、可选 semesterId 的
 * 有/无两分支绑定、list/teacherList 的域对象条件映射、detail 返回列表落 data 的契约；
 * 选课→开课→排课的联查 SQL 逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalScheduleControllerMockMvcTest {

    @Mock
    private ITpmScheduleService tpmScheduleService;

    @InjectMocks
    private PortalScheduleController controller;

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
    @DisplayName("myList：学生课表以登录用户为查询主体，显式 semesterId 原样透传")
    void myList_usesLoggedInStudentWithSemester() throws Exception {
        TpmSchedule s = new TpmSchedule();
        s.setScheduleId(100L);
        when(tpmScheduleService.selectStudentScheduleList(eq(1L), eq(20261L)))
                .thenReturn(List.of(s));

        mockMvc.perform(get("/portal/schedule/myList").param("semesterId", "20261"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].scheduleId").value(100));

        verify(tpmScheduleService).selectStudentScheduleList(eq(1L), eq(20261L));
    }

    @Test
    @DisplayName("myList：semesterId 缺省（required=false）时以 null 透传服务层查全部学期")
    void myList_semesterAbsentPassesNull() throws Exception {
        when(tpmScheduleService.selectStudentScheduleList(eq(1L), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/schedule/myList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isEmpty());

        verify(tpmScheduleService).selectStudentScheduleList(eq(1L), isNull());
    }

    @Test
    @DisplayName("list：查询参数绑定进 TpmSchedule 域对象并返回表格结构")
    void list_bindsDomainQuery() throws Exception {
        TpmSchedule s = new TpmSchedule();
        s.setScheduleId(101L);
        s.setOfferingId(20L);
        when(tpmScheduleService.selectTpmScheduleList(any(TpmSchedule.class)))
                .thenReturn(List.of(s));

        mockMvc.perform(get("/portal/schedule/list").param("offeringId", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].offeringId").value(20));

        ArgumentCaptor<TpmSchedule> captor = ArgumentCaptor.forClass(TpmSchedule.class);
        verify(tpmScheduleService).selectTpmScheduleList(captor.capture());
        Assertions.assertEquals(Long.valueOf(20L), captor.getValue().getOfferingId());
    }

    @Test
    @DisplayName("teacherList：教师端按域对象条件（如教室）查询课表")
    void teacherList_bindsTeacherFilter() throws Exception {
        when(tpmScheduleService.selectTpmScheduleList(any(TpmSchedule.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/schedule/teacherList").param("classroomName", "A101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<TpmSchedule> captor = ArgumentCaptor.forClass(TpmSchedule.class);
        verify(tpmScheduleService).selectTpmScheduleList(captor.capture());
        Assertions.assertEquals("A101", captor.getValue().getClassroomName());
    }

    @Test
    @DisplayName("detail：课表详情返回列表落 data（而非 rows）")
    void detail_putsListInData() throws Exception {
        TpmSchedule s = new TpmSchedule();
        s.setScheduleId(102L);
        when(tpmScheduleService.selectTpmScheduleList(any(TpmSchedule.class)))
                .thenReturn(List.of(s));

        mockMvc.perform(get("/portal/schedule/detail").param("scheduleId", "102"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].scheduleId").value(102));
    }
}
