package com.yu.sam.controller;

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
import com.yu.sam.domain.SamGraduationProcedure;
import com.yu.sam.service.ISamGraduationProcedureService;

/**
 * 离校手续接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-sam S7c 离校手续控制器）。
 * 校验路由 /sam/graduationProcedure、CRUD 的 @Validated 必填校验（studentId @NotNull）、
 * init 幂等初始化将新增条数拼入提示语并回填 data、items 环节明细列表、
 * toggle 按 (procedureId,stepId,done,operator) 调用（done 做 Boolean.TRUE 归一）、
 * autoCheck 自动判定计数、statOverview 统计 Map，init/toggle/autoCheck 均透传登录操作人。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamGraduationProcedureControllerMockMvcTest {

    @Mock
    private ISamGraduationProcedureService samGraduationProcedureService;

    @InjectMocks
    private SamGraduationProcedureController controller;

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
    @DisplayName("getInfo：路径变量绑定procedureId并返回手续记录")
    void getInfo_returnsProcedure() throws Exception {
        SamGraduationProcedure procedure = new SamGraduationProcedure();
        procedure.setProcedureId(5L);
        when(samGraduationProcedureService.selectSamGraduationProcedureByProcedureId(eq(5L))).thenReturn(procedure);

        mockMvc.perform(get("/sam/graduationProcedure/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.procedureId").value(5));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(samGraduationProcedureService.insertSamGraduationProcedure(any(SamGraduationProcedure.class))).thenReturn(1);

        mockMvc.perform(post("/sam/graduationProcedure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":11}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("add：缺失必填studentId被@NotNull拦截返回400，不落库")
    void add_missingStudentIdRejected() throws Exception {
        mockMvc.perform(post("/sam/graduationProcedure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(samGraduationProcedureService, never()).insertSamGraduationProcedure(any());
    }

    @Test
    @DisplayName("init：透传登录操作人，将新增条数拼入msg并回填data")
    void init_returnsCountInMsgAndData() throws Exception {
        when(samGraduationProcedureService.initProcedures(eq(11L), eq("tester"))).thenReturn(3);

        mockMvc.perform(post("/sam/graduationProcedure/init").param("studentId", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("初始化完成，新增 3 条离校手续记录"))
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    @DisplayName("items：按procedureId返回环节明细列表")
    void items_returnsList() throws Exception {
        when(samGraduationProcedureService.listItems(eq(5L))).thenReturn(List.of());

        mockMvc.perform(get("/sam/graduationProcedure/items/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("toggle：done=true归一并透传operator，影响行数0时降级500")
    void toggle_contract() throws Exception {
        when(samGraduationProcedureService.toggleItem(eq(5L), eq(2L), eq(true), eq("tester"))).thenReturn(1);
        mockMvc.perform(put("/sam/graduationProcedure/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"procedureId\":5,\"stepId\":2,\"done\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        when(samGraduationProcedureService.toggleItem(eq(5L), eq(2L), eq(false), eq("tester"))).thenReturn(0);
        mockMvc.perform(put("/sam/graduationProcedure/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"procedureId\":5,\"stepId\":2,\"done\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("autoCheck：透传operator，将更新人数拼入msg并回填data")
    void autoCheck_returnsCount() throws Exception {
        when(samGraduationProcedureService.autoCheck(eq(11L), eq("tester"))).thenReturn(2);

        mockMvc.perform(post("/sam/graduationProcedure/autoCheck").param("studentId", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("自动判定完成，更新 2 名学生离校状态"))
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    @DisplayName("statOverview：返回离校办理总览统计Map")
    void statOverview_returnsMap() throws Exception {
        Map<String, Object> stat = new HashMap<>();
        stat.put("total", 20);
        when(samGraduationProcedureService.statOverview()).thenReturn(stat);

        mockMvc.perform(get("/sam/graduationProcedure/stat/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(20));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(samGraduationProcedureService.deleteSamGraduationProcedureByProcedureIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/sam/graduationProcedure/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(samGraduationProcedureService).deleteSamGraduationProcedureByProcedureIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(samGraduationProcedureService.selectSamGraduationProcedureList(any(SamGraduationProcedure.class))).thenReturn(List.of());

        mockMvc.perform(get("/sam/graduationProcedure/list").param("studentId", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SamGraduationProcedure> captor = ArgumentCaptor.forClass(SamGraduationProcedure.class);
        verify(samGraduationProcedureService).selectSamGraduationProcedureList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(11L), captor.getValue().getStudentId());
    }
}
