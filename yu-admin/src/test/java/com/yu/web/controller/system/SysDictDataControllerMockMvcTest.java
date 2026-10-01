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

import com.yu.common.core.domain.entity.SysDictData;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.system.service.ISysDictDataService;
import com.yu.system.service.ISysDictTypeService;

/**
 * 字典数据接口层测试（Q1 第十一批：MockMvc standalone）。
 * 校验 @Validated 必填字段（dictLabel/dictValue/dictType）缺失返回 400 且不落库、
 * getInfo 路径变量绑定 dictCode、dictType 端点在服务返回 null 时降级为空数组（无 @PreAuthorize 的公开下拉），
 * 新增回填 createBy / 修改回填 updateBy（SecurityContext 登录用户），
 * insert/update 影响行数为 0 时 toAjax 降级为 500、
 * remove 逗号路径变量绑定为 Long[]；缓存重载、字典联动等业务逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysDictDataControllerMockMvcTest {

    @Mock
    private ISysDictDataService dictDataService;

    @Mock
    private ISysDictTypeService dictTypeService;

    @InjectMocks
    private SysDictDataController controller;

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

    private SysDictData dictData(long code, String type, String label, String value) {
        SysDictData d = new SysDictData();
        d.setDictCode(code);
        d.setDictType(type);
        d.setDictLabel(label);
        d.setDictValue(value);
        d.setStatus("0");
        return d;
    }

    @Test
    @DisplayName("list：查询条件绑定域对象并返回表格结构")
    void list_bindsQueryAndReturnsTable() throws Exception {
        when(dictDataService.selectDictDataList(any(SysDictData.class)))
                .thenReturn(List.of(dictData(1L, "sys_user_sex", "男", "0")));

        mockMvc.perform(get("/system/dict/data/list").param("dictType", "sys_user_sex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].dictValue").value("0"));

        ArgumentCaptor<SysDictData> captor = ArgumentCaptor.forClass(SysDictData.class);
        verify(dictDataService).selectDictDataList(captor.capture());
        Assertions.assertEquals("sys_user_sex", captor.getValue().getDictType());
    }

    @Test
    @DisplayName("getInfo：路径变量绑定 dictCode 并返回字典数据详情")
    void getInfo_bindsPathCode() throws Exception {
        when(dictDataService.selectDictDataById(eq(9L))).thenReturn(dictData(9L, "sys_yes_no", "是", "Y"));

        mockMvc.perform(get("/system/dict/data/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dictCode").value(9))
                .andExpect(jsonPath("$.data.dictValue").value("Y"));
    }

    @Test
    @DisplayName("dictType：按类型返回字典数据列表（公开下拉，无权限注解）")
    void dictType_returnsList() throws Exception {
        when(dictTypeService.selectDictDataByType(eq("sys_user_sex")))
                .thenReturn(List.of(dictData(1L, "sys_user_sex", "男", "0"), dictData(2L, "sys_user_sex", "女", "1")));

        mockMvc.perform(get("/system/dict/data/type/sys_user_sex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("dictType：服务返回 null 时降级为空数组而非 null")
    void dictType_nullDegradesToEmptyArray() throws Exception {
        when(dictTypeService.selectDictDataByType(eq("sys_missing"))).thenReturn(null);

        mockMvc.perform(get("/system/dict/data/type/sys_missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("add：dictLabel 缺失被 @Validated 拦截返回 400，不落库")
    void add_blankLabelRejected() throws Exception {
        String body = "{\"dictType\":\"sys_user_sex\",\"dictValue\":\"0\"}";
        mockMvc.perform(post("/system/dict/data")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(dictDataService, never()).insertDictData(any(SysDictData.class));
    }

    @Test
    @DisplayName("add：dictValue 缺失被 @Validated 拦截返回 400，不落库")
    void add_blankValueRejected() throws Exception {
        String body = "{\"dictType\":\"sys_user_sex\",\"dictLabel\":\"男\"}";
        mockMvc.perform(post("/system/dict/data")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(dictDataService, never()).insertDictData(any(SysDictData.class));
    }

    @Test
    @DisplayName("add：合法字典数据回填 createBy 为登录用户后落库")
    void add_setsCreateByAndPersists() throws Exception {
        when(dictDataService.insertDictData(any(SysDictData.class))).thenReturn(1);

        String body = "{\"dictType\":\"sys_grade\",\"dictLabel\":\"优秀\",\"dictValue\":\"A\"}";
        mockMvc.perform(post("/system/dict/data")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"));

        ArgumentCaptor<SysDictData> captor = ArgumentCaptor.forClass(SysDictData.class);
        verify(dictDataService).insertDictData(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getCreateBy());
        Assertions.assertEquals("A", captor.getValue().getDictValue());
    }

    @Test
    @DisplayName("add：落库影响行数为 0 时 toAjax 降级为 500")
    void add_zeroRowsReturnsError() throws Exception {
        when(dictDataService.insertDictData(any(SysDictData.class))).thenReturn(0);

        mockMvc.perform(post("/system/dict/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dictType\":\"sys_grade\",\"dictLabel\":\"空跑\",\"dictValue\":\"X\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("操作失败"));
    }

    @Test
    @DisplayName("edit：合法请求回填 updateBy")
    void edit_setsUpdateBy() throws Exception {
        when(dictDataService.updateDictData(any(SysDictData.class))).thenReturn(1);

        mockMvc.perform(put("/system/dict/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dictCode\":3,\"dictType\":\"sys_user_sex\",\"dictLabel\":\"男\",\"dictValue\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysDictData> captor = ArgumentCaptor.forClass(SysDictData.class);
        verify(dictDataService).updateDictData(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
        Assertions.assertEquals(3L, captor.getValue().getDictCode());
    }

    @Test
    @DisplayName("edit：影响行数为 0 时 toAjax 降级为 500")
    void edit_zeroRowsReturnsError() throws Exception {
        when(dictDataService.updateDictData(any(SysDictData.class))).thenReturn(0);

        mockMvc.perform(put("/system/dict/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dictCode\":3,\"dictType\":\"sys_user_sex\",\"dictLabel\":\"男\",\"dictValue\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("操作失败"));
    }

    @Test
    @DisplayName("remove：逗号路径变量绑定 Long[] 并返回操作成功")
    void remove_bindsCommaCodes() throws Exception {
        mockMvc.perform(delete("/system/dict/data/1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(dictDataService).deleteDictDataByIds(captor.capture());
        Assertions.assertArrayEquals(new Long[] {1L, 2L, 3L}, captor.getValue());
    }
}
