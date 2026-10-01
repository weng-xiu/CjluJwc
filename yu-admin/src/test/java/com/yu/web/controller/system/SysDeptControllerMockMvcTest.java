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

import java.util.ArrayList;
import java.util.Arrays;
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

import com.yu.common.core.domain.entity.SysDept;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.system.service.ISysDeptService;

/**
 * 部门管理接口层测试（Q1 第九批：MockMvc standalone）。
 * 校验 @Validated 必填字段（deptName/orderNum）缺失的 400 拦截、部门名称唯一性拦截、
 * 新增回填 createBy 与修改回填 updateBy（SecurityContext 登录用户）、
 * 修改时上级部门不能是自己（自环）与停用部门含未停用子部门的双拦截分支、
 * list/exclude 端点在控制器内按 deptId 与 ancestors 链剔除自身及子孙的过滤契约、
 * 删除时下级部门/存在用户两道 warn(601) 前置拦截后才走数据范围校验、updateSort 逗号串拆分透传；
 * 祖级列表重算、数据范围 SQL 过滤等业务逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysDeptControllerMockMvcTest {

    @Mock
    private ISysDeptService deptService;

    @InjectMocks
    private SysDeptController controller;

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

    private SysDept dept(long id, String name, String ancestors, String status) {
        SysDept d = new SysDept();
        d.setDeptId(id);
        d.setDeptName(name);
        d.setAncestors(ancestors);
        d.setStatus(status);
        return d;
    }

    @Test
    @DisplayName("list：查询条件绑定域对象并返回部门列表落 data")
    void list_bindsCondition() throws Exception {
        when(deptService.selectDeptList(any(SysDept.class)))
                .thenReturn(List.of(dept(100L, "教务处", "0,1", "0")));

        mockMvc.perform(get("/system/dept/list").param("deptName", "教务"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].deptId").value(100));

        ArgumentCaptor<SysDept> captor = ArgumentCaptor.forClass(SysDept.class);
        verify(deptService).selectDeptList(captor.capture());
        Assertions.assertEquals("教务", captor.getValue().getDeptName());
    }

    @Test
    @DisplayName("list/exclude：剔除等于 deptId 或祖级链含 deptId 的部门及其子孙")
    void excludeChild_filtersSelfAndDescendants() throws Exception {
        // 200 为被剔除节点自身；300 祖级链含 200（子孙）；100 保留
        List<SysDept> depts = new ArrayList<>(Arrays.asList(
                dept(100L, "长江大学", "0", "0"),
                dept(200L, "农学院", "0,100", "0"),
                dept(300L, "农学系", "0,100,200", "0")));
        when(deptService.selectDeptList(any(SysDept.class))).thenReturn(depts);

        mockMvc.perform(get("/system/dept/list/exclude/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].deptId").value(100));
    }

    @Test
    @DisplayName("getInfo：路径变量绑定 deptId，先做数据范围校验再查详情")
    void getInfo_checksDataScopeThenQueries() throws Exception {
        when(deptService.selectDeptById(eq(7L))).thenReturn(dept(7L, "教务科", "0,1,2", "0"));

        mockMvc.perform(get("/system/dept/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deptId").value(7))
                .andExpect(jsonPath("$.data.deptName").value("教务科"));

        verify(deptService).checkDeptDataScope(eq(7L));
    }

    @Test
    @DisplayName("add：deptName 缺失被 @Validated 拦截返回 400，不落库")
    void add_missingDeptNameRejected() throws Exception {
        String body = "{\"orderNum\":1,\"parentId\":100}";
        mockMvc.perform(post("/system/dept")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(deptService, never()).insertDept(any(SysDept.class));
    }

    @Test
    @DisplayName("add：orderNum 缺失（@NotNull）同样被拦截返回 400")
    void add_missingOrderNumRejected() throws Exception {
        String body = "{\"deptName\":\"新部门\",\"parentId\":100}";
        mockMvc.perform(post("/system/dept")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(deptService, never()).insertDept(any(SysDept.class));
    }

    @Test
    @DisplayName("add：部门名称已存在被唯一性拦截返回 500 且不落库")
    void add_duplicateNameRejected() throws Exception {
        when(deptService.checkDeptNameUnique(any(SysDept.class))).thenReturn(false);

        String body = "{\"deptName\":\"教务处\",\"orderNum\":1,\"parentId\":100}";
        mockMvc.perform(post("/system/dept")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("新增部门'教务处'失败，部门名称已存在"));

        verify(deptService, never()).insertDept(any(SysDept.class));
    }

    @Test
    @DisplayName("add：合法部门回填 createBy 为登录用户后落库")
    void add_setsCreateByAndPersists() throws Exception {
        when(deptService.checkDeptNameUnique(any(SysDept.class))).thenReturn(true);
        when(deptService.insertDept(any(SysDept.class))).thenReturn(1);

        String body = "{\"deptName\":\"实践科\",\"orderNum\":2,\"parentId\":100}";
        mockMvc.perform(post("/system/dept")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysDept> captor = ArgumentCaptor.forClass(SysDept.class);
        verify(deptService).insertDept(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getCreateBy());
    }

    @Test
    @DisplayName("edit：上级部门等于自身被自环拦截且不更新")
    void edit_selfParentRejected() throws Exception {
        when(deptService.checkDeptNameUnique(any(SysDept.class))).thenReturn(true);

        String body = "{\"deptId\":5,\"parentId\":5,\"deptName\":\"教学部\",\"orderNum\":1}";
        mockMvc.perform(put("/system/dept")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("修改部门'教学部'失败，上级部门不能是自己"));

        verify(deptService, never()).updateDept(any(SysDept.class));
    }

    @Test
    @DisplayName("edit：停用部门仍含未停用子部门被拦截")
    void edit_disableWithNormalChildrenRejected() throws Exception {
        when(deptService.checkDeptNameUnique(any(SysDept.class))).thenReturn(true);
        when(deptService.selectNormalChildrenDeptById(eq(5L))).thenReturn(2);

        String body = "{\"deptId\":5,\"parentId\":1,\"deptName\":\"教学部\",\"orderNum\":1,\"status\":\"1\"}";
        mockMvc.perform(put("/system/dept")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("该部门包含未停用的子部门！"));

        verify(deptService, never()).updateDept(any(SysDept.class));
    }

    @Test
    @DisplayName("edit：合法部门回填 updateBy 后更新")
    void edit_setsUpdateByAndUpdates() throws Exception {
        when(deptService.checkDeptNameUnique(any(SysDept.class))).thenReturn(true);
        when(deptService.updateDept(any(SysDept.class))).thenReturn(1);

        String body = "{\"deptId\":5,\"parentId\":1,\"deptName\":\"改名科\",\"orderNum\":1,\"status\":\"0\"}";
        mockMvc.perform(put("/system/dept")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(deptService).checkDeptDataScope(eq(5L));
        ArgumentCaptor<SysDept> captor = ArgumentCaptor.forClass(SysDept.class);
        verify(deptService).updateDept(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
    }

    @Test
    @DisplayName("updateSort：deptIds/orderNums 逗号串拆分为数组透传")
    void updateSort_splitsCommaStrings() throws Exception {
        String body = "{\"deptIds\":\"3,1,2\",\"orderNums\":\"1,2,3\"}";
        mockMvc.perform(put("/system/dept/updateSort")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(deptService).updateDeptSort(eq(new String[] {"3", "1", "2"}), eq(new String[] {"1", "2", "3"}));
    }

    @Test
    @DisplayName("remove：存在下级部门返回 warn(601) 且不删除")
    void remove_withChildrenWarns() throws Exception {
        when(deptService.hasChildByDeptId(eq(8L))).thenReturn(true);

        mockMvc.perform(delete("/system/dept/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(601))
                .andExpect(jsonPath("$.msg").value("存在下级部门,不允许删除"));

        verify(deptService, never()).deleteDeptById(any());
    }

    @Test
    @DisplayName("remove：部门存在用户返回 warn(601) 且不删除")
    void remove_withUsersWarns() throws Exception {
        when(deptService.hasChildByDeptId(eq(8L))).thenReturn(false);
        when(deptService.checkDeptExistUser(eq(8L))).thenReturn(true);

        mockMvc.perform(delete("/system/dept/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(601))
                .andExpect(jsonPath("$.msg").value("部门存在用户,不允许删除"));

        verify(deptService, never()).deleteDeptById(any());
    }

    @Test
    @DisplayName("remove：无下级无用户时先数据范围校验再删除")
    void remove_cleanDeptDeleted() throws Exception {
        when(deptService.hasChildByDeptId(eq(8L))).thenReturn(false);
        when(deptService.checkDeptExistUser(eq(8L))).thenReturn(false);
        when(deptService.deleteDeptById(eq(8L))).thenReturn(1);

        mockMvc.perform(delete("/system/dept/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(deptService).checkDeptDataScope(eq(8L));
        verify(deptService).deleteDeptById(eq(8L));
    }
}
