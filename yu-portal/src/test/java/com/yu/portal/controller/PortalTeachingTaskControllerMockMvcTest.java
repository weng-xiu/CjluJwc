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
import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.service.ITpmCourseOfferingService;

/**
 * 门户教学任务查询接口层测试（Q1 第四批：MockMvc standalone）。
 * 核心校验 P6 防越权契约：非管理员强制绑定登录教师 teacherId，管理员保留查询条件；
 * 分页返回结构（$.rows/$.code）契约。任务数据装配由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalTeachingTaskControllerMockMvcTest {

    @Mock
    private ITpmCourseOfferingService tpmCourseOfferingService;

    @InjectMocks
    private PortalTeachingTaskController controller;

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

    private void loginAs(long userId) {
        SysUser sysUser = new SysUser();
        sysUser.setUserName("tester");
        LoginUser loginUser = new LoginUser(userId, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @Test
    @DisplayName("list：非管理员强制覆盖 teacherId 为登录用户（防越权看到他人任务）")
    void list_forcesTeacherScopeForNonAdmin() throws Exception {
        loginAs(100L);
        when(tpmCourseOfferingService.selectTpmCourseOfferingList(any(TpmCourseOffering.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/teachingTask/list").param("teacherId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<TpmCourseOffering> captor = ArgumentCaptor.forClass(TpmCourseOffering.class);
        verify(tpmCourseOfferingService).selectTpmCourseOfferingList(captor.capture());
        Assertions.assertEquals(Long.valueOf(100L), captor.getValue().getTeacherId());
    }

    @Test
    @DisplayName("list：管理员不覆盖查询条件（可查看全部教师任务）")
    void list_adminKeepsQueryParam() throws Exception {
        // setUp 已登录 userId=1（admin）
        when(tpmCourseOfferingService.selectTpmCourseOfferingList(any(TpmCourseOffering.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/teachingTask/list").param("teacherId", "888"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmCourseOffering> captor = ArgumentCaptor.forClass(TpmCourseOffering.class);
        verify(tpmCourseOfferingService).selectTpmCourseOfferingList(captor.capture());
        Assertions.assertEquals(Long.valueOf(888L), captor.getValue().getTeacherId());
    }

    @Test
    @DisplayName("list：未传 teacherId 时非管理员仍绑定本人，返回 rows 数组")
    void list_nonAdminNoParamBindsSelf() throws Exception {
        loginAs(200L);
        TpmCourseOffering row = new TpmCourseOffering();
        row.setTeacherId(200L);
        when(tpmCourseOfferingService.selectTpmCourseOfferingList(any(TpmCourseOffering.class)))
                .thenReturn(List.of(row));

        mockMvc.perform(get("/portal/teachingTask/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray())
                .andExpect(jsonPath("$.total").value(1));

        ArgumentCaptor<TpmCourseOffering> captor = ArgumentCaptor.forClass(TpmCourseOffering.class);
        verify(tpmCourseOfferingService).selectTpmCourseOfferingList(captor.capture());
        Assertions.assertEquals(Long.valueOf(200L), captor.getValue().getTeacherId());
    }
}
