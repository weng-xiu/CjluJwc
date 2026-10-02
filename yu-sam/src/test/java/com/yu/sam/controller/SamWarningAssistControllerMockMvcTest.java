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
import java.util.List;

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
import com.yu.sam.domain.SamWarningAssist;
import com.yu.sam.domain.SamWarningAssistRecord;
import com.yu.sam.service.ISamWarningAssistService;

/**
 * 预警帮扶任务接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-sam S6 帮扶闭环控制器）。
 * 校验路由 /sam/warningAssist、dispatch 的 @Validated 必填校验（warningId @NotNull）并回填操作人、
 * claim 按 (assistId,userId,nickName) 认领、follow 回填操作人三要素（operatorId/operatorName/createBy）后登记、
 * finish 的 resolveWarning 参数缺省为 false、close 关闭并透传结办备注、
 * 逗号数组批量删除、list 查询绑定与表格结构。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamWarningAssistControllerMockMvcTest {

    @Mock
    private ISamWarningAssistService samWarningAssistService;

    @InjectMocks
    private SamWarningAssistController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
        sysUser.setUserId(1L);
        sysUser.setUserName("tester");
        sysUser.setNickName("测试员");
        LoginUser loginUser = new LoginUser(1L, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定assistId并返回帮扶任务")
    void getInfo_returnsAssist() throws Exception {
        SamWarningAssist assist = new SamWarningAssist();
        assist.setAssistId(9L);
        when(samWarningAssistService.selectSamWarningAssistByAssistId(eq(9L))).thenReturn(assist);

        mockMvc.perform(get("/sam/warningAssist/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.assistId").value(9));
    }

    @Test
    @DisplayName("dispatch：合法请求体通过@Validated并回填登录操作人")
    void dispatch_validBodyPersists() throws Exception {
        when(samWarningAssistService.dispatch(any(SamWarningAssist.class))).thenReturn(1);

        mockMvc.perform(post("/sam/warningAssist/dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warningId\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamWarningAssist> captor = ArgumentCaptor.forClass(SamWarningAssist.class);
        verify(samWarningAssistService).dispatch(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("tester", captor.getValue().getCreateBy());
    }

    @Test
    @DisplayName("dispatch：缺失必填warningId被@NotNull拦截返回400，不派发")
    void dispatch_missingWarningIdRejected() throws Exception {
        mockMvc.perform(post("/sam/warningAssist/dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(samWarningAssistService, never()).dispatch(any());
    }

    @Test
    @DisplayName("claim：按(assistId,userId,nickName)认领")
    void claim_passesUserIdAndNickName() throws Exception {
        when(samWarningAssistService.claim(eq(9L), eq(1L), eq("测试员"))).thenReturn(1);

        mockMvc.perform(put("/sam/warningAssist/claim/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samWarningAssistService).claim(eq(9L), eq(1L), eq("测试员"));
    }

    @Test
    @DisplayName("follow：回填操作人三要素后登记跟踪记录")
    void follow_setsOperatorAndPersists() throws Exception {
        when(samWarningAssistService.follow(eq(9L), any(SamWarningAssistRecord.class))).thenReturn(1);

        mockMvc.perform(post("/sam/warningAssist/follow/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"followContent\":\"首次谈话\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamWarningAssistRecord> captor = ArgumentCaptor.forClass(SamWarningAssistRecord.class);
        verify(samWarningAssistService).follow(eq(9L), captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getOperatorId());
        org.junit.jupiter.api.Assertions.assertEquals("测试员", captor.getValue().getOperatorName());
        org.junit.jupiter.api.Assertions.assertEquals("tester", captor.getValue().getCreateBy());
    }

    @Test
    @DisplayName("finish：resolveWarning缺省为false")
    void finish_defaultsResolveWarningFalse() throws Exception {
        when(samWarningAssistService.finish(eq(9L), any(), eq(false))).thenReturn(1);

        mockMvc.perform(put("/sam/warningAssist/finish/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samWarningAssistService).finish(eq(9L), any(), eq(false));
    }

    @Test
    @DisplayName("finish：显式resolveWarning=true同步解除预警，影响行数0时降级500")
    void finish_resolveWarningTrueDegrades() throws Exception {
        when(samWarningAssistService.finish(eq(9L), eq("已达标"), eq(true))).thenReturn(0);

        mockMvc.perform(put("/sam/warningAssist/finish/9")
                        .param("finishRemark", "已达标")
                        .param("resolveWarning", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("close：关闭并透传结办备注")
    void close_passesRemark() throws Exception {
        when(samWarningAssistService.closeAssist(eq(9L), eq("结办"))).thenReturn(1);

        mockMvc.perform(put("/sam/warningAssist/close/9").param("finishRemark", "结办"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(samWarningAssistService.deleteSamWarningAssistByAssistIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/sam/warningAssist/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(samWarningAssistService).deleteSamWarningAssistByAssistIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(samWarningAssistService.selectSamWarningAssistList(any(SamWarningAssist.class))).thenReturn(List.of());

        mockMvc.perform(get("/sam/warningAssist/list").param("warningId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SamWarningAssist> captor = ArgumentCaptor.forClass(SamWarningAssist.class);
        verify(samWarningAssistService).selectSamWarningAssistList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(5L), captor.getValue().getWarningId());
    }
}
