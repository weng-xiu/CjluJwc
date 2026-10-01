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

import com.yu.common.core.domain.entity.SysDictType;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.system.service.ISysDictTypeService;

/**
 * 字典类型接口层测试（Q1 第十批：MockMvc standalone）。
 * 校验 @Validated 必填字段（dictName/dictType）缺失与 dictType 不符合 {@code ^[a-z][a-z0-9_]*$} 格式的 400 拦截、
 * 字典类型唯一性冲突在 add/edit 分别返回 500 且不落库、新增回填 createBy 与修改回填 updateBy（SecurityContext 登录用户）、
 * insert/update 影响行数为 0 时 toAjax 降级为 500、
 * remove 逗号路径变量绑定为 Long[] 且返回 void service 调用后的 success 契约、
 * refreshCache 与 /{dictIds} 删除端点的路径优先级消歧（字面量优先于路径变量）；
 * 字典缓存重载、字典数据联动删除等业务逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysDictTypeControllerMockMvcTest {

    @Mock
    private ISysDictTypeService dictTypeService;

    @InjectMocks
    private SysDictTypeController controller;

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

    private SysDictType dictType(long id, String type) {
        SysDictType d = new SysDictType();
        d.setDictId(id);
        d.setDictName("性别" + id);
        d.setDictType(type);
        d.setStatus("0");
        return d;
    }

    @Test
    @DisplayName("list：查询条件绑定域对象并返回表格结构")
    void list_bindsQueryAndReturnsTable() throws Exception {
        when(dictTypeService.selectDictTypeList(any(SysDictType.class)))
                .thenReturn(List.of(dictType(1L, "sys_user_sex")));

        mockMvc.perform(get("/system/dict/type/list").param("dictName", "性别"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].dictType").value("sys_user_sex"));

        ArgumentCaptor<SysDictType> captor = ArgumentCaptor.forClass(SysDictType.class);
        verify(dictTypeService).selectDictTypeList(captor.capture());
        Assertions.assertEquals("性别", captor.getValue().getDictName());
    }

    @Test
    @DisplayName("getInfo：路径变量绑定 dictId 并返回字典类型详情")
    void getInfo_bindsPathId() throws Exception {
        when(dictTypeService.selectDictTypeById(eq(9L))).thenReturn(dictType(9L, "sys_yes_no"));

        mockMvc.perform(get("/system/dict/type/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dictId").value(9))
                .andExpect(jsonPath("$.data.dictType").value("sys_yes_no"));
    }

    @Test
    @DisplayName("add：dictName 缺失被 @Validated 拦截返回 400，唯一性校验与落库均不执行")
    void add_blankNameRejected() throws Exception {
        String body = "{\"dictType\":\"sys_new_type\"}";
        mockMvc.perform(post("/system/dict/type")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(dictTypeService, never()).checkDictTypeUnique(any(SysDictType.class));
        verify(dictTypeService, never()).insertDictType(any(SysDictType.class));
    }

    @Test
    @DisplayName("add：dictType 含大写字母违反 @Pattern 返回 400")
    void add_illegalDictTypePatternRejected() throws Exception {
        String body = "{\"dictName\":\"测试字典\",\"dictType\":\"Sys_New_Type\"}";
        mockMvc.perform(post("/system/dict/type")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(dictTypeService, never()).insertDictType(any(SysDictType.class));
    }

    @Test
    @DisplayName("add：字典类型已存在返回 500 提示且不落库")
    void add_duplicateTypeRejected() throws Exception {
        when(dictTypeService.checkDictTypeUnique(any(SysDictType.class))).thenReturn(false);

        String body = "{\"dictName\":\"性别\",\"dictType\":\"sys_user_sex\"}";
        mockMvc.perform(post("/system/dict/type")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("新增字典'性别'失败，字典类型已存在"));

        verify(dictTypeService, never()).insertDictType(any(SysDictType.class));
    }

    @Test
    @DisplayName("add：合法字典回填 createBy 为登录用户后落库")
    void add_setsCreateByAndPersists() throws Exception {
        when(dictTypeService.checkDictTypeUnique(any(SysDictType.class))).thenReturn(true);
        when(dictTypeService.insertDictType(any(SysDictType.class))).thenReturn(1);

        String body = "{\"dictName\":\"成绩等级\",\"dictType\":\"sys_grade_level\",\"status\":\"0\"}";
        mockMvc.perform(post("/system/dict/type")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"));

        ArgumentCaptor<SysDictType> captor = ArgumentCaptor.forClass(SysDictType.class);
        verify(dictTypeService).insertDictType(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getCreateBy());
        Assertions.assertEquals("sys_grade_level", captor.getValue().getDictType());
    }

    @Test
    @DisplayName("add：落库影响行数为 0 时 toAjax 降级为 500")
    void add_zeroRowsReturnsError() throws Exception {
        when(dictTypeService.checkDictTypeUnique(any(SysDictType.class))).thenReturn(true);
        when(dictTypeService.insertDictType(any(SysDictType.class))).thenReturn(0);

        mockMvc.perform(post("/system/dict/type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dictName\":\"空跑\",\"dictType\":\"sys_no_op\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("操作失败"));
    }

    @Test
    @DisplayName("edit：唯一性冲突返回 500；合法请求回填 updateBy")
    void edit_uniqueCheckAndUpdateBy() throws Exception {
        when(dictTypeService.checkDictTypeUnique(any(SysDictType.class))).thenReturn(false);
        mockMvc.perform(put("/system/dict/type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dictId\":3,\"dictName\":\"性别\",\"dictType\":\"sys_user_sex\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("修改字典'性别'失败，字典类型已存在"));
        verify(dictTypeService, never()).updateDictType(any(SysDictType.class));

        when(dictTypeService.checkDictTypeUnique(any(SysDictType.class))).thenReturn(true);
        when(dictTypeService.updateDictType(any(SysDictType.class))).thenReturn(1);
        mockMvc.perform(put("/system/dict/type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dictId\":3,\"dictName\":\"性别字典\",\"dictType\":\"sys_user_sex\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysDictType> captor = ArgumentCaptor.forClass(SysDictType.class);
        verify(dictTypeService).updateDictType(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
        Assertions.assertEquals(3L, captor.getValue().getDictId());
    }

    @Test
    @DisplayName("remove：逗号路径变量绑定 Long[] 并返回操作成功")
    void remove_bindsCommaIds() throws Exception {
        mockMvc.perform(delete("/system/dict/type/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(dictTypeService).deleteDictTypeByIds(captor.capture());
        Assertions.assertArrayEquals(new Long[] {1L, 2L}, captor.getValue());
    }

    @Test
    @DisplayName("refreshCache：字面量路径优先于 /{dictIds}，仅重置缓存不删数据")
    void refreshCache_takesPrecedenceOverDelete() throws Exception {
        mockMvc.perform(delete("/system/dict/type/refreshCache"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(dictTypeService).resetDictCache();
        verify(dictTypeService, never()).deleteDictTypeByIds(any(Long[].class));
    }

    @Test
    @DisplayName("optionselect：无权限注解端点返回全量字典类型列表")
    void optionselect_returnsAllTypes() throws Exception {
        when(dictTypeService.selectDictTypeAll())
                .thenReturn(List.of(dictType(1L, "sys_user_sex"), dictType(2L, "sys_yes_no")));

        mockMvc.perform(get("/system/dict/type/optionselect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].dictType").value("sys_yes_no"));
    }
}
