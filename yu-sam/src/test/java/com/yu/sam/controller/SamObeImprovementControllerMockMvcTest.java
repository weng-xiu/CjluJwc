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

import java.util.ArrayList;
import java.util.Collections;

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
import com.yu.sam.domain.SamObeImprovement;
import com.yu.sam.service.ISamObeImprovementService;

/**
 * OBE 持续改进台账接口层测试（F3 整改闭环，MockMvc standalone）。
 * 校验路由 /sam/obe/improvement、@Validated 必填校验（planId @NotNull、measure @NotNull）、
 * add/edit 回填登录操作人、逗号数组批量删除、list 查询绑定与表格结构。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 *
 * @author N1
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamObeImprovementControllerMockMvcTest
{
    @Mock
    private ISamObeImprovementService improvementService;

    @InjectMocks
    private SamObeImprovementController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
        sysUser.setUserId(1L);
        sysUser.setUserName("tester");
        LoginUser loginUser = new LoginUser(1L, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("list：planId/status 查询绑定并返回表格结构")
    void list_bindsQuery() throws Exception
    {
        SamObeImprovement row = new SamObeImprovement();
        row.setImpId(1L);
        row.setMeasure("增加过程性考核");
        row.setStatus("0");
        ArrayList<SamObeImprovement> rows = new ArrayList<>();
        rows.add(row);
        when(improvementService.selectImprovementList(any(SamObeImprovement.class)))
                .thenReturn(rows);

        mockMvc.perform(get("/sam/obe/improvement/list")
                        .param("planId", "3").param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0].measure").value("增加过程性考核"));

        ArgumentCaptor<SamObeImprovement> captor = ArgumentCaptor.forClass(SamObeImprovement.class);
        verify(improvementService).selectImprovementList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(3L, captor.getValue().getPlanId());
        org.junit.jupiter.api.Assertions.assertEquals("0", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("getInfo：路径变量绑定impId并返回台账")
    void getInfo_returnsImprovement() throws Exception
    {
        SamObeImprovement imp = new SamObeImprovement();
        imp.setImpId(9L);
        imp.setGrName("工程知识");
        when(improvementService.selectImprovementByImpId(eq(9L))).thenReturn(imp);

        mockMvc.perform(get("/sam/obe/improvement/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grName").value("工程知识"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并回填createBy")
    void add_validBodyInjectsCreateBy() throws Exception
    {
        when(improvementService.insertImprovement(any(SamObeImprovement.class))).thenReturn(1);

        String body = "{\"planId\":3,\"grId\":1,\"measure\":\"增加课程设计环节\",\"owner\":\"张老师\"}";
        mockMvc.perform(post("/sam/obe/improvement")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamObeImprovement> captor = ArgumentCaptor.forClass(SamObeImprovement.class);
        verify(improvementService).insertImprovement(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("tester", captor.getValue().getCreateBy());
        org.junit.jupiter.api.Assertions.assertEquals(3L, captor.getValue().getPlanId());
    }

    @Test
    @DisplayName("add：缺失必填measure被@NotNull拦截返回400，不落库")
    void add_missingMeasureRejected() throws Exception
    {
        mockMvc.perform(post("/sam/obe/improvement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":3,\"grId\":1}"))
                .andExpect(status().isBadRequest());

        verify(improvementService, never()).insertImprovement(any());
    }

    @Test
    @DisplayName("add：缺失必填planId被@NotNull拦截返回400，不落库")
    void add_missingPlanIdRejected() throws Exception
    {
        mockMvc.perform(post("/sam/obe/improvement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"measure\":\"改进考核方式\"}"))
                .andExpect(status().isBadRequest());

        verify(improvementService, never()).insertImprovement(any());
    }

    @Test
    @DisplayName("edit：状态推进与效果回填并注入updateBy")
    void edit_updatesStatusAndEffect() throws Exception
    {
        when(improvementService.updateImprovement(any(SamObeImprovement.class))).thenReturn(1);

        String body = "{\"impId\":9,\"planId\":3,\"measure\":\"增加课程设计环节\",\"status\":\"2\",\"effectAttainment\":0.72,\"effectNote\":\"复评达标\"}";
        mockMvc.perform(put("/sam/obe/improvement")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamObeImprovement> captor = ArgumentCaptor.forClass(SamObeImprovement.class);
        verify(improvementService).updateImprovement(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
        org.junit.jupiter.api.Assertions.assertEquals("2", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("remove：逗号数组批量删除")
    void remove_batchIds() throws Exception
    {
        when(improvementService.deleteImprovementByImpIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/sam/obe/improvement/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(improvementService).deleteImprovementByImpIds(eq(new Long[]{1L, 2L}));
    }
}
