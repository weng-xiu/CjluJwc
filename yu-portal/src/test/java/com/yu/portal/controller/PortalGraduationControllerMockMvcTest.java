package com.yu.portal.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.yu.sam.service.ISamGraduationReviewService;

/**
 * 学生毕业预审门户接口层测试（Q1 第四批：MockMvc standalone）。
 * 校验 S4 只读自助预审契约：preReview 以 getUserId() 作为学生ID口径调用服务
 * （用户无法越权查询他人），结果原样返回 $.data。达成计算逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalGraduationControllerMockMvcTest {

    @Mock
    private ISamGraduationReviewService samGraduationReviewService;

    @InjectMocks
    private PortalGraduationController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
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
    @DisplayName("preReview：以学生ID口径调用预审并将分项达成结果置于 data")
    void preReview_bindsLoginUserAndReturnsData() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("qualified", false);
        result.put("gapCount", 3);
        when(samGraduationReviewService.preReview(1L)).thenReturn(result);

        mockMvc.perform(get("/portal/graduation/preReview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.qualified").value(false))
                .andExpect(jsonPath("$.data.gapCount").value(3));

        verify(samGraduationReviewService).preReview(1L);
    }
}
