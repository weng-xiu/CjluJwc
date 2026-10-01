package com.yu.web.controller.system;

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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.system.domain.SysConfig;
import com.yu.system.service.ISysConfigService;

/**
 * 参数配置接口层测试（Q1 第十批：MockMvc standalone）。
 * 校验 @Validated 必填字段（configName/configKey/configValue）缺失的 400 拦截、参数键名唯一性冲突在 add/edit
 * 分别返回 500 且不落库、新增回填 createBy 与修改回填 updateBy（SecurityContext 登录用户）、
 * getConfigKey 的键名含点号仍被完整绑定（PathPatternParser 不做后缀截断），且因 BaseController.success(String)
 * 重载更精确，键值落在 msg 字段而非 data（既有契约，前端按 msg 取值）；
 * 另校验 remove 逗号路径变量绑定 Long[]、refreshCache 与 /{configIds} 的路径优先级消歧、
 * insertConfig 影响行数 0 时 toAjax 降级 500；
 * 参数缓存 Redis 重载、内置参数（configType=Y）禁删等业务逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysConfigControllerMockMvcTest {

    @Mock
    private ISysConfigService configService;

    @InjectMocks
    private SysConfigController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        // BaseController.getUsername() 依赖 SecurityContext，standalone 手动注入登录用户
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

    private SysConfig buildConfig(long id, String key, String value) {
        SysConfig c = new SysConfig();
        c.setConfigId(id);
        c.setConfigName("参数" + id);
        c.setConfigKey(key);
        c.setConfigValue(value);
        c.setConfigType("Y");
        return c;
    }

    @Test
    @DisplayName("list：查询条件绑定域对象并返回表格结构")
    void list_bindsQueryAndReturnsTable() throws Exception {
        when(configService.selectConfigList(any(SysConfig.class)))
                .thenReturn(List.of(buildConfig(1L, "sys.index.skinName", "skin-blue")));

        mockMvc.perform(get("/system/config/list").param("configName", "皮肤"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].configKey").value("sys.index.skinName"));

        ArgumentCaptor<SysConfig> captor = ArgumentCaptor.forClass(SysConfig.class);
        verify(configService).selectConfigList(captor.capture());
        Assertions.assertEquals("皮肤", captor.getValue().getConfigName());
    }

    @Test
    @DisplayName("getInfo：路径变量绑定 configId 并返回参数详情")
    void getInfo_bindsPathId() throws Exception {
        when(configService.selectConfigById(eq(4L)))
                .thenReturn(buildConfig(4L, "sys.user.initPassword", "123456"));

        mockMvc.perform(get("/system/config/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.configId").value(4))
                .andExpect(jsonPath("$.data.configValue").value("123456"));
    }

    @Test
    @DisplayName("getConfigKey：键名含点号完整绑定，键值按 success(String) 契约落在 msg")
    void getConfigKey_bindsDottedKeyAndReturnsInMsg() throws Exception {
        when(configService.selectConfigByKey(eq("sys.account.captchaEnabled"))).thenReturn("false");

        mockMvc.perform(get("/system/config/configKey/sys.account.captchaEnabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("false"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(configService).selectConfigByKey(eq("sys.account.captchaEnabled"));
    }

    @Test
    @DisplayName("add：configValue 缺失被 @Validated 拦截返回 400，唯一性校验与落库均不执行")
    void add_missingValueRejected() throws Exception {
        String body = "{\"configName\":\"主题色\",\"configKey\":\"sys.ui.theme\"}";
        mockMvc.perform(post("/system/config")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(configService, never()).checkConfigKeyUnique(any(SysConfig.class));
        verify(configService, never()).insertConfig(any(SysConfig.class));
    }

    @Test
    @DisplayName("add：参数键名已存在返回 500 提示且不落库")
    void add_duplicateKeyRejected() throws Exception {
        when(configService.checkConfigKeyUnique(any(SysConfig.class))).thenReturn(false);

        mockMvc.perform(post("/system/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configName\":\"皮肤\",\"configKey\":\"sys.index.skinName\",\"configValue\":\"skin-blue\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("新增参数'皮肤'失败，参数键名已存在"));

        verify(configService, never()).insertConfig(any(SysConfig.class));
    }

    @Test
    @DisplayName("add：合法参数回填 createBy 为登录用户后落库")
    void add_setsCreateByAndPersists() throws Exception {
        when(configService.checkConfigKeyUnique(any(SysConfig.class))).thenReturn(true);
        when(configService.insertConfig(any(SysConfig.class))).thenReturn(1);

        mockMvc.perform(post("/system/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configName\":\"机构主题色\",\"configKey\":\"sys.ui.primaryColor\",\"configValue\":\"#1E5BB8\",\"configType\":\"N\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"));

        ArgumentCaptor<SysConfig> captor = ArgumentCaptor.forClass(SysConfig.class);
        verify(configService).insertConfig(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getCreateBy());
        Assertions.assertEquals("sys.ui.primaryColor", captor.getValue().getConfigKey());
    }

    @Test
    @DisplayName("add：落库影响行数为 0 时 toAjax 降级为 500")
    void add_zeroRowsReturnsError() throws Exception {
        when(configService.checkConfigKeyUnique(any(SysConfig.class))).thenReturn(true);
        when(configService.insertConfig(any(SysConfig.class))).thenReturn(0);

        mockMvc.perform(post("/system/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configName\":\"空跑\",\"configKey\":\"sys.no.op\",\"configValue\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("操作失败"));
    }

    @Test
    @DisplayName("edit：键名冲突拦截；合法请求回填 updateBy")
    void edit_uniqueCheckAndUpdateBy() throws Exception {
        when(configService.checkConfigKeyUnique(any(SysConfig.class))).thenReturn(false);
        mockMvc.perform(put("/system/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configId\":3,\"configName\":\"皮肤\",\"configKey\":\"sys.index.skinName\",\"configValue\":\"skin-green\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("修改参数'皮肤'失败，参数键名已存在"));
        verify(configService, never()).updateConfig(any(SysConfig.class));

        when(configService.checkConfigKeyUnique(any(SysConfig.class))).thenReturn(true);
        when(configService.updateConfig(any(SysConfig.class))).thenReturn(1);
        mockMvc.perform(put("/system/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configId\":3,\"configName\":\"皮肤\",\"configKey\":\"sys.index.skinName\",\"configValue\":\"skin-green\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysConfig> captor = ArgumentCaptor.forClass(SysConfig.class);
        verify(configService).updateConfig(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
        Assertions.assertEquals(3L, captor.getValue().getConfigId());
    }

    @Test
    @DisplayName("remove：逗号路径变量绑定 Long[] 并返回操作成功")
    void remove_bindsCommaIds() throws Exception {
        mockMvc.perform(delete("/system/config/1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(configService).deleteConfigByIds(captor.capture());
        Assertions.assertArrayEquals(new Long[] {1L, 2L, 3L}, captor.getValue());
    }

    @Test
    @DisplayName("refreshCache：字面量路径优先于 /{configIds}，仅重置缓存不删数据")
    void refreshCache_takesPrecedenceOverDelete() throws Exception {
        mockMvc.perform(delete("/system/config/refreshCache"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(configService).resetConfigCache();
        verify(configService, never()).deleteConfigByIds(any(Long[].class));
    }
}
