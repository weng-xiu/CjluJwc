package com.yu.sam.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
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
import com.yu.sam.domain.SamRegistration;
import com.yu.sam.service.ISamRegistrationService;

/**
 * 学期注册接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-sam S8 学期注册控制器）。
 * 本控制器无 add 端点，且 edit 未加 @Validated（仅回填登录操作人后透传），故无必填校验路径。
 * 校验路由 /sam/registration、currentSemester 返回 Long 学期ID落入 data、
 * init 幂等初始化将新增条数拼入 msg 并回填 data、batchRegister 按
 * (registrationIds,status,deferReason,operator) 调用并 toAjax、statOverview 统计 Map、
 * statByDept 院系统计列表，init/batchRegister/edit 均透传登录操作人。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamRegistrationControllerMockMvcTest {

    @Mock
    private ISamRegistrationService samRegistrationService;

    @InjectMocks
    private SamRegistrationController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
        sysUser.setUserId(1L);
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
    @DisplayName("getInfo：路径变量绑定registrationId并返回记录")
    void getInfo_returnsRegistration() throws Exception {
        SamRegistration reg = new SamRegistration();
        reg.setRegistrationId(8L);
        when(samRegistrationService.selectSamRegistrationByRegistrationId(eq(8L))).thenReturn(reg);

        mockMvc.perform(get("/sam/registration/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.registrationId").value(8));
    }

    @Test
    @DisplayName("currentSemester：返回当前学期ID（Long落入data）")
    void currentSemester_returnsLong() throws Exception {
        when(samRegistrationService.getCurrentSemesterId()).thenReturn(5L);

        mockMvc.perform(get("/sam/registration/currentSemester"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    @DisplayName("init：透传operator，将新增条数拼入msg并回填data")
    void init_returnsCountInMsgAndData() throws Exception {
        when(samRegistrationService.initRegistration(eq(3L), eq("tester"))).thenReturn(4);

        mockMvc.perform(post("/sam/registration/init").param("semesterId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("报到初始化完成，新增 4 条未注册记录"))
                .andExpect(jsonPath("$.data").value(4));
    }

    @Test
    @DisplayName("batchRegister：按四参调用服务，影响行数0时toAjax降级500")
    void batchRegister_contract() throws Exception {
        when(samRegistrationService.batchRegister(any(Long[].class), eq("1"), any(), eq("tester"))).thenReturn(2);
        mockMvc.perform(put("/sam/registration/batchRegister")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationIds\":[1,2],\"status\":\"1\",\"deferReason\":\"病假\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        when(samRegistrationService.batchRegister(any(Long[].class), eq("0"), any(), eq("tester"))).thenReturn(0);
        mockMvc.perform(put("/sam/registration/batchRegister")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationIds\":[3],\"status\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("edit：未加@Validated，仅回填updateBy后透传更新")
    void edit_notValidatedStillPersists() throws Exception {
        when(samRegistrationService.updateSamRegistration(any(SamRegistration.class))).thenReturn(1);

        mockMvc.perform(put("/sam/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationId\":8,\"status\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamRegistration> captor = ArgumentCaptor.forClass(SamRegistration.class);
        verify(samRegistrationService).updateSamRegistration(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
    }

    @Test
    @DisplayName("statOverview：返回注册总览统计Map")
    void statOverview_returnsMap() throws Exception {
        Map<String, Object> stat = new HashMap<>();
        stat.put("registered", 100);
        when(samRegistrationService.statOverview(eq(3L))).thenReturn(stat);

        mockMvc.perform(get("/sam/registration/stat/overview").param("semesterId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.registered").value(100));
    }

    @Test
    @DisplayName("statByDept：返回院系注册率列表")
    void statByDept_returnsList() throws Exception {
        when(samRegistrationService.statByDept(eq(3L))).thenReturn(List.of());

        mockMvc.perform(get("/sam/registration/stat/byDept").param("semesterId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(samRegistrationService.deleteSamRegistrationByRegistrationIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/sam/registration/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(samRegistrationService).deleteSamRegistrationByRegistrationIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(samRegistrationService.selectSamRegistrationList(any(SamRegistration.class))).thenReturn(List.of());

        mockMvc.perform(get("/sam/registration/list").param("semesterId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SamRegistration> captor = ArgumentCaptor.forClass(SamRegistration.class);
        verify(samRegistrationService).selectSamRegistrationList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(3L), captor.getValue().getSemesterId());
    }
}
