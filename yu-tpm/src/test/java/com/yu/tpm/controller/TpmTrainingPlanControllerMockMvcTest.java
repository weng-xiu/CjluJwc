package com.yu.tpm.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
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
import com.yu.tpm.domain.TpmCourseLibrary;
import com.yu.tpm.domain.TpmCreditStructure;
import com.yu.tpm.domain.TpmTrainingPlan;
import com.yu.tpm.service.ITpmTrainingPlanService;

/**
 * 培养方案接口层测试（Q1 第四批：MockMvc standalone）。
 * 校验 HTTP 路由、@Validated 必填校验（planName）、发布/废止/复制生命周期端点参数契约、
 * T3 主子表一次保存的 JSON 结构拆解与 createBy/updateBy 回填规则；
 * 方案版本复制、发布校验等业务逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TpmTrainingPlanControllerMockMvcTest {

    @Mock
    private ITpmTrainingPlanService tpmTrainingPlanService;

    @InjectMocks
    private TpmTrainingPlanController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        // saveWithChildren 依赖 SecurityUtils.getUsername()，standalone 模式手动注入登录用户
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
    @DisplayName("getInfo：路径变量绑定planId并返回方案数据体")
    void getInfo_returnsPlan() throws Exception {
        TpmTrainingPlan plan = new TpmTrainingPlan();
        plan.setPlanId(1L);
        plan.setPlanName("2026级计算机培养方案");
        when(tpmTrainingPlanService.selectTpmTrainingPlanByPlanId(eq(1L))).thenReturn(plan);

        mockMvc.perform(get("/tpm/plan/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.planId").value(1))
                .andExpect(jsonPath("$.data.planName").value("2026级计算机培养方案"));
    }

    @Test
    @DisplayName("add：缺失必填planName被@NotBlank拦截返回400，不落库")
    void add_missingPlanNameRejected() throws Exception {
        String body = "{\"majorId\":1,\"planYear\":2026}";
        mockMvc.perform(post("/tpm/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(tpmTrainingPlanService, never()).insertTpmTrainingPlan(any());
    }

    @Test
    @DisplayName("publish：发布端点绑定planId并返回成功")
    void publish_bindsPlanId() throws Exception {
        when(tpmTrainingPlanService.publishTrainingPlan(eq(4L))).thenReturn(1);

        mockMvc.perform(put("/tpm/plan/publish/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tpmTrainingPlanService).publishTrainingPlan(eq(4L));
    }

    @Test
    @DisplayName("deprecate：废止端点走独立服务方法，不误调发布")
    void deprecate_usesOwnServiceMethod() throws Exception {
        when(tpmTrainingPlanService.deprecateTrainingPlan(eq(4L))).thenReturn(1);

        mockMvc.perform(put("/tpm/plan/deprecate/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tpmTrainingPlanService).deprecateTrainingPlan(eq(4L));
        verify(tpmTrainingPlanService, never()).publishTrainingPlan(any());
    }

    @Test
    @DisplayName("copy：复制成功时顶层附带新planId供前端跳转编辑")
    void copy_returnsNewPlanId() throws Exception {
        when(tpmTrainingPlanService.copyTrainingPlan(eq(6L))).thenReturn(77L);

        mockMvc.perform(post("/tpm/plan/copy/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.planId").value(77));
    }

    @Test
    @DisplayName("saveWithChildren：主子表JSON拆解透传，新子行回填createBy、已有子行回填updateBy")
    void saveWithChildren_splitsPayloadAndFillsAuditor() throws Exception {
        when(tpmTrainingPlanService.savePlanWithChildren(any(), anyList(), anyList())).thenReturn(1);

        String body = "{\"plan\":{\"planName\":\"方案A\"},"
                + "\"courseList\":[{\"courseId\":null,\"courseName\":\"新曲线\"},{\"courseId\":9,\"courseName\":\"旧课改\"}],"
                + "\"creditList\":[]}";
        mockMvc.perform(post("/tpm/plan/saveWithChildren")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmTrainingPlan> planCap = ArgumentCaptor.forClass(TpmTrainingPlan.class);
        ArgumentCaptor<List<TpmCourseLibrary>> courseCap = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<TpmCreditStructure>> creditCap = ArgumentCaptor.forClass(List.class);
        verify(tpmTrainingPlanService).savePlanWithChildren(planCap.capture(), courseCap.capture(), creditCap.capture());

        // planId 为空 → createBy=登录人；子表按主键有无区分新增/修改
        org.junit.jupiter.api.Assertions.assertEquals("tester", planCap.getValue().getCreateBy());
        List<TpmCourseLibrary> courses = courseCap.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(2, courses.size());
        org.junit.jupiter.api.Assertions.assertEquals("tester", courses.get(0).getCreateBy());
        org.junit.jupiter.api.Assertions.assertEquals("tester", courses.get(1).getUpdateBy());
        org.junit.jupiter.api.Assertions.assertTrue(creditCap.getValue().isEmpty());
    }

    @Test
    @DisplayName("saveWithChildren：已有planId时回填updateBy而非createBy")
    void saveWithChildren_existingPlanFillsUpdateBy() throws Exception {
        when(tpmTrainingPlanService.savePlanWithChildren(any(), any(), any())).thenReturn(1);

        String body = "{\"plan\":{\"planId\":5,\"planName\":\"方案A\"},\"courseList\":[],\"creditList\":[]}";
        mockMvc.perform(post("/tpm/plan/saveWithChildren")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<TpmTrainingPlan> captor = ArgumentCaptor.forClass(TpmTrainingPlan.class);
        verify(tpmTrainingPlanService).savePlanWithChildren(captor.capture(), any(), any());
        org.junit.jupiter.api.Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
        org.junit.jupiter.api.Assertions.assertNull(captor.getValue().getCreateBy());
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(tpmTrainingPlanService.selectTpmTrainingPlanList(any(TpmTrainingPlan.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/tpm/plan/list").param("planName", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<TpmTrainingPlan> captor = ArgumentCaptor.forClass(TpmTrainingPlan.class);
        verify(tpmTrainingPlanService).selectTpmTrainingPlanList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("2026", captor.getValue().getPlanName());
    }
}
