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
import com.yu.sam.domain.SamDegreeConfig;
import com.yu.sam.service.ISamDegreeConfigService;

/**
 * 学位授予条件配置接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-sam S3 学位配置控制器）。
 * 校验路由 /sam/degreeConfig、@Validated 必填校验（configName @NotBlank）、
 * add/edit 回填登录操作人、effective 解析当前生效配置、setDefault 按 configId 设默认、
 * 逗号数组批量删除、list 查询绑定与表格结构。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamDegreeConfigControllerMockMvcTest {

    @Mock
    private ISamDegreeConfigService samDegreeConfigService;

    @InjectMocks
    private SamDegreeConfigController controller;

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
    @DisplayName("getInfo：路径变量绑定configId并返回配置")
    void getInfo_returnsConfig() throws Exception {
        SamDegreeConfig config = new SamDegreeConfig();
        config.setConfigId(4L);
        config.setConfigName("2026学位口径");
        when(samDegreeConfigService.selectSamDegreeConfigByConfigId(eq(4L))).thenReturn(config);

        mockMvc.perform(get("/sam/degreeConfig/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.configName").value("2026学位口径"));
    }

    @Test
    @DisplayName("effective：返回当前生效配置")
    void effective_returnsResolvedConfig() throws Exception {
        SamDegreeConfig config = new SamDegreeConfig();
        config.setConfigId(4L);
        when(samDegreeConfigService.resolveEffective()).thenReturn(config);

        mockMvc.perform(get("/sam/degreeConfig/effective"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.configId").value(4));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并回填登录操作人")
    void add_validBodyPersists() throws Exception {
        when(samDegreeConfigService.insertSamDegreeConfig(any(SamDegreeConfig.class))).thenReturn(1);

        mockMvc.perform(post("/sam/degreeConfig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configName\":\"2026学位口径\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamDegreeConfig> captor = ArgumentCaptor.forClass(SamDegreeConfig.class);
        verify(samDegreeConfigService).insertSamDegreeConfig(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("tester", captor.getValue().getCreateBy());
    }

    @Test
    @DisplayName("add：缺失必填configName被@NotBlank拦截返回400，不落库")
    void add_missingConfigNameRejected() throws Exception {
        mockMvc.perform(post("/sam/degreeConfig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(samDegreeConfigService, never()).insertSamDegreeConfig(any());
    }

    @Test
    @DisplayName("edit：合法请求体透传并回填更新操作人")
    void edit_validBodyPersists() throws Exception {
        when(samDegreeConfigService.updateSamDegreeConfig(any(SamDegreeConfig.class))).thenReturn(1);

        mockMvc.perform(put("/sam/degreeConfig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configId\":4,\"configName\":\"修订口径\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamDegreeConfig> captor = ArgumentCaptor.forClass(SamDegreeConfig.class);
        verify(samDegreeConfigService).updateSamDegreeConfig(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
    }

    @Test
    @DisplayName("setDefault：按configId设默认，影响行数0时toAjax降级500")
    void setDefault_contract() throws Exception {
        when(samDegreeConfigService.setDefault(eq(4L))).thenReturn(1);
        mockMvc.perform(put("/sam/degreeConfig/default/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        when(samDegreeConfigService.setDefault(eq(9L))).thenReturn(0);
        mockMvc.perform(put("/sam/degreeConfig/default/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(samDegreeConfigService.deleteSamDegreeConfigByConfigIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/sam/degreeConfig/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(samDegreeConfigService).deleteSamDegreeConfigByConfigIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(samDegreeConfigService.selectSamDegreeConfigList(any(SamDegreeConfig.class))).thenReturn(List.of());

        mockMvc.perform(get("/sam/degreeConfig/list").param("configName", "学位"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SamDegreeConfig> captor = ArgumentCaptor.forClass(SamDegreeConfig.class);
        verify(samDegreeConfigService).selectSamDegreeConfigList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("学位", captor.getValue().getConfigName());
    }
}
