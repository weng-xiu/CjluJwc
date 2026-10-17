package com.yu.web.controller.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.yu.system.domain.SysDataClass;
import com.yu.system.service.ISysDataClassService;

import java.util.Collections;

/**
 * 数据分类分级台账控制器契约测试（G5，K1 等保数据新规）。
 * 校验 list/getInfo 的域对象与路径绑定、add 中「表字段唯一性」拦截优先于落库并回填 createBy、
 * @Validated 必填（表名）缺失 400、remove 逗号路径绑定 Long[] 且影响行数 0 降级 500；
 * 视图层不感知台账与运行期脱敏/加密字段的印证逻辑。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysDataClassControllerMockMvcTest
{
    @Mock
    private ISysDataClassService dataClassService;

    @InjectMocks
    private SysDataClassController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
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

    private SysDataClass build(long id, String table, String column)
    {
        SysDataClass d = new SysDataClass();
        d.setClassId(id);
        d.setTableName(table);
        d.setColumnName(column);
        d.setDataCategory("PII");
        d.setSecurityLevel("L4");
        d.setStatus("0");
        return d;
    }

    @Test
    @DisplayName("list：绑定查询域对象并返回表格结构")
    void listReturnsTable() throws Exception
    {
        when(dataClassService.selectDataClassList(any(SysDataClass.class)))
                .thenReturn(List.of(build(1L, "sys_user", "phonenumber")));

        mockMvc.perform(get("/system/dataClass/list").param("tableName", "sys_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].tableName").value("sys_user"));

        ArgumentCaptor<SysDataClass> captor = ArgumentCaptor.forClass(SysDataClass.class);
        verify(dataClassService).selectDataClassList(captor.capture());
        Assertions.assertEquals("sys_user", captor.getValue().getTableName());
    }

    @Test
    @DisplayName("getInfo：路径变量绑定 classId 返回登记详情")
    void getInfoBindsId() throws Exception
    {
        when(dataClassService.selectDataClassById(5L)).thenReturn(build(5L, "sys_user", "id_card"));

        mockMvc.perform(get("/system/dataClass/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classId").value(5))
                .andExpect(jsonPath("$.data.columnName").value("id_card"));
    }

    @Test
    @DisplayName("add：表名缺失被 @Validated 拦截 400，唯一性校验不执行")
    void addMissingTableRejected() throws Exception
    {
        String body = "{\"columnName\":\"x\",\"dataCategory\":\"PII\",\"securityLevel\":\"L4\"}";
        mockMvc.perform(post("/system/dataClass")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(dataClassService, never()).checkColumnUnique(any(SysDataClass.class));
        verify(dataClassService, never()).insertDataClass(any(SysDataClass.class));
    }

    @Test
    @DisplayName("add：表字段已登记时拦截且不落库")
    void addDuplicateColumnRejected() throws Exception
    {
        when(dataClassService.checkColumnUnique(any(SysDataClass.class))).thenReturn(false);

        String body = "{\"tableName\":\"sys_user\",\"columnName\":\"phonenumber\",\"dataCategory\":\"CONTACT\",\"securityLevel\":\"L3\"}";
        mockMvc.perform(post("/system/dataClass")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("新增登记'sys_user.phonenumber'失败，该表字段已登记"));

        verify(dataClassService, never()).insertDataClass(any(SysDataClass.class));
    }

    @Test
    @DisplayName("add：唯一性通过则回填 createBy 并落库")
    void addSuccessSetsCreateBy() throws Exception
    {
        when(dataClassService.checkColumnUnique(any(SysDataClass.class))).thenReturn(true);
        when(dataClassService.insertDataClass(any(SysDataClass.class))).thenReturn(1);

        String body = "{\"tableName\":\"sys_user\",\"columnName\":\"email\",\"dataCategory\":\"CONTACT\",\"securityLevel\":\"L3\"}";
        mockMvc.perform(post("/system/dataClass")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysDataClass> captor = ArgumentCaptor.forClass(SysDataClass.class);
        verify(dataClassService).insertDataClass(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getCreateBy());
    }

    @Test
    @DisplayName("remove：逗号路径绑定 Long[]，影响行数 0 降级 500")
    void removeBindsIdsAndDegrades() throws Exception
    {
        when(dataClassService.deleteDataClassByIds(any(Long[].class))).thenReturn(0);

        mockMvc.perform(delete("/system/dataClass/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(dataClassService).deleteDataClassByIds(captor.capture());
        Assertions.assertArrayEquals(new Long[] {1L, 2L}, captor.getValue());
    }
}
