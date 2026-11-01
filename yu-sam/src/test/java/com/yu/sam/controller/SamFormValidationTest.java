package com.yu.sam.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.sam.service.ISamThesisService;

/**
 * S-6：请求体表单类的必填约束契约。
 *
 * <p>这几个表单此前都是「裸 @RequestBody」：缺定位 ID 时不会报错，Service 拿着 null 主键
 * 去 update，命中 0 行却返回成功（前端显示已保存），或把 null 当 false 反向撤销办理状态。
 * 本测试锁定两件事：约束本身存在；控制器改回裸 @RequestBody 时用例会红。</p>
 */
class SamFormValidationTest
{
    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    /** 控制器会取 getUsername() 填审计字段，需要登录上下文；缺了会报「获取用户信息异常」而不是 400 */
    @BeforeEach
    void setUpSecurityContext()
    {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(1L);
        sysUser.setUserName("tester");
        LoginUser loginUser = new LoginUser(1L, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDownSecurityContext()
    {
        SecurityContextHolder.clearContext();
    }

    private static boolean hit(Set<? extends ConstraintViolation<?>> violations, String field)
    {
        return violations.stream().anyMatch((v) -> field.equals(v.getPropertyPath().toString()));
    }

    @Test
    @DisplayName("SamThesisController.ThesisAction：缺 thesisId 触发约束，带齐则通过")
    void thesisActionRequiresThesisId()
    {
        SamThesisController.ThesisAction empty = new SamThesisController.ThesisAction();
        assertTrue(hit(validator.validate(empty), "thesisId"), "缺 thesisId 必须被拦");

        SamThesisController.ThesisAction full = new SamThesisController.ThesisAction();
        full.setThesisId(1L);
        assertTrue(validator.validate(full).isEmpty(), "带齐字段不应误报");
    }

    @Test
    @DisplayName("SamCertReissueApplyController.AuditForm：缺 applyId 触发约束")
    void auditFormRequiresApplyId()
    {
        SamCertReissueApplyController.AuditForm form = new SamCertReissueApplyController.AuditForm();
        assertTrue(hit(validator.validate(form), "applyId"));

        form.setApplyId(9L);
        assertTrue(validator.validate(form).isEmpty());
    }

    @Test
    @DisplayName("SamCertificateController.IssueForm：缺 certId 触发约束")
    void issueFormRequiresCertId()
    {
        SamCertificateController.IssueForm form = new SamCertificateController.IssueForm();
        assertTrue(hit(validator.validate(form), "certId"));

        form.setCertId(3L);
        assertTrue(validator.validate(form).isEmpty());
    }

    @Test
    @DisplayName("SamGraduationProcedureController.ToggleForm：procedureId 与 done 都必填")
    void toggleFormRequiresIdAndDone()
    {
        SamGraduationProcedureController.ToggleForm form = new SamGraduationProcedureController.ToggleForm();
        Set<ConstraintViolation<SamGraduationProcedureController.ToggleForm>> violations = validator.validate(form);
        assertTrue(hit(violations, "procedureId"), "缺定位 ID 必须被拦");
        assertTrue(hit(violations, "done"), "done 缺省会被当成 false，必须被拦");

        form.setProcedureId(5L);
        form.setDone(Boolean.FALSE);
        assertTrue(validator.validate(form).isEmpty(), "显式传 false 是合法语义，不应误报");
    }

    @Test
    @DisplayName("SamRegistrationController.RegisterForm：空 ID 数组与空状态都要拦")
    void registerFormRequiresIdsAndStatus()
    {
        SamRegistrationController.RegisterForm form = new SamRegistrationController.RegisterForm();
        Set<ConstraintViolation<SamRegistrationController.RegisterForm>> violations = validator.validate(form);
        assertTrue(hit(violations, "registrationIds"));
        assertTrue(hit(violations, "status"));

        form.setRegistrationIds(new Long[] { 1L });
        form.setStatus("1");
        assertTrue(validator.validate(form).isEmpty());
    }

    @Test
    @DisplayName("控制器链路：/sam/thesis/stage/audit 缺 thesisId 返回 400 且不落 Service")
    void thesisAuditRejectsMissingThesisId() throws Exception
    {
        ISamThesisService service = org.mockito.Mockito.mock(ISamThesisService.class);
        when(service.markSample(anyLong(), any(), any(), any())).thenReturn(1);
        when(service.auditStage(anyLong(), any(), any(Boolean.class), any(), any(), any())).thenReturn(1);
        SamThesisController controller = new SamThesisController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "samThesisService", service);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(post("/sam/thesis/stage/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"open\",\"pass\":true}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).auditStage(anyLong(), any(), any(Boolean.class), any(), any(), any());

        // 同一条链路带齐 thesisId 时照常进入 Service（证明 400 来自约束而不是路由问题）
        mockMvc.perform(put("/sam/thesis/sample")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"thesisId\":7,\"sampleStatus\":\"1\"}"))
                .andExpect(status().isOk());
    }
}
