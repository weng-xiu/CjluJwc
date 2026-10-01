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

import com.yu.common.core.domain.entity.SysDept;
import com.yu.common.core.domain.entity.SysRole;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.framework.web.service.SysPermissionService;
import com.yu.framework.web.service.TokenService;
import com.yu.system.domain.SysUserRole;
import com.yu.system.service.ISysDeptService;
import com.yu.system.service.ISysRoleService;
import com.yu.system.service.ISysUserService;

/**
 * 角色管理接口层测试（Q1 第九批：MockMvc standalone）。
 * 校验 @Validated 必填字段（roleName/roleKey/roleSort）缺失的 400 拦截、名称/权限字符唯一性拦截契约、
 * 新增回填 createBy 与修改回填 updateBy（SecurityContext 登录用户）、修改成功后触发在线用户权限刷新、
 * dataScope/changeStatus 状态流转透传、逗号路径变量删除数组绑定、授权用户取消/批量授权与 selectAll 的数据范围校验、
 * deptTree 端点 checkedKeys/depts 平铺键契约；
 * 角色数据范围 SQL 拼接、菜单权限分配等业务逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysRoleControllerMockMvcTest {

    @Mock
    private ISysRoleService roleService;

    @Mock
    private TokenService tokenService;

    @Mock
    private SysPermissionService permissionService;

    @Mock
    private ISysUserService userService;

    @Mock
    private ISysDeptService deptService;

    @InjectMocks
    private SysRoleController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        // BaseController.getUsername() 依赖 SecurityContext，standalone 手动注入登录用户（userId=1）
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

    private SysRole role(long id, String name, String key) {
        SysRole r = new SysRole();
        r.setRoleId(id);
        r.setRoleName(name);
        r.setRoleKey(key);
        r.setRoleSort(1);
        r.setStatus("0");
        return r;
    }

    @Test
    @DisplayName("list：查询条件绑定域对象并返回分页表格结构")
    void list_bindsConditionAndReturnsTable() throws Exception {
        when(roleService.selectRoleList(any(SysRole.class)))
                .thenReturn(List.of(role(1L, "教务处", "academic")));

        mockMvc.perform(get("/system/role/list").param("roleName", "教务"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0].roleKey").value("academic"));

        ArgumentCaptor<SysRole> captor = ArgumentCaptor.forClass(SysRole.class);
        verify(roleService).selectRoleList(captor.capture());
        Assertions.assertEquals("教务", captor.getValue().getRoleName());
    }

    @Test
    @DisplayName("getInfo：路径变量绑定 roleId，先做数据范围校验再查详情")
    void getInfo_checksDataScopeThenQueries() throws Exception {
        when(roleService.selectRoleById(eq(9L))).thenReturn(role(9L, "院长", "dean"));

        mockMvc.perform(get("/system/role/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.roleId").value(9))
                .andExpect(jsonPath("$.data.roleName").value("院长"));

        verify(roleService).checkRoleDataScope(eq(9L));
    }

    @Test
    @DisplayName("add：roleName 缺失被 @Validated 拦截返回 400，不落库")
    void add_missingRoleNameRejected() throws Exception {
        String body = "{\"roleKey\":\"dean\",\"roleSort\":1}";
        mockMvc.perform(post("/system/role")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(roleService, never()).insertRole(any(SysRole.class));
    }

    @Test
    @DisplayName("add：roleSort 缺失（@NotNull）同样被拦截返回 400")
    void add_missingRoleSortRejected() throws Exception {
        String body = "{\"roleName\":\"新角色\",\"roleKey\":\"newkey\"}";
        mockMvc.perform(post("/system/role")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(roleService, never()).insertRole(any(SysRole.class));
    }

    @Test
    @DisplayName("add：角色名称已存在被唯一性拦截返回 500 且不落库")
    void add_duplicateNameRejected() throws Exception {
        when(roleService.checkRoleNameUnique(any(SysRole.class))).thenReturn(false);

        String body = "{\"roleName\":\"教务处\",\"roleKey\":\"academic\",\"roleSort\":1}";
        mockMvc.perform(post("/system/role")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("新增角色'教务处'失败，角色名称已存在"));

        verify(roleService, never()).insertRole(any(SysRole.class));
    }

    @Test
    @DisplayName("add：权限字符已存在被第二道唯一性拦截，名称校验通过后仍不落库")
    void add_duplicateKeyRejected() throws Exception {
        when(roleService.checkRoleNameUnique(any(SysRole.class))).thenReturn(true);
        when(roleService.checkRoleKeyUnique(any(SysRole.class))).thenReturn(false);

        String body = "{\"roleName\":\"新角色\",\"roleKey\":\"academic\",\"roleSort\":2}";
        mockMvc.perform(post("/system/role")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("新增角色'新角色'失败，角色权限已存在"));

        verify(roleService, never()).insertRole(any(SysRole.class));
    }

    @Test
    @DisplayName("add：合法角色回填 createBy 为登录用户后落库")
    void add_setsCreateByAndPersists() throws Exception {
        when(roleService.checkRoleNameUnique(any(SysRole.class))).thenReturn(true);
        when(roleService.checkRoleKeyUnique(any(SysRole.class))).thenReturn(true);
        when(roleService.insertRole(any(SysRole.class))).thenReturn(1);

        String body = "{\"roleName\":\"督导组\",\"roleKey\":\"supervision\",\"roleSort\":3}";
        mockMvc.perform(post("/system/role")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysRole> captor = ArgumentCaptor.forClass(SysRole.class);
        verify(roleService).insertRole(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getCreateBy());
    }

    @Test
    @DisplayName("edit：更新成功回填 updateBy 并触发在线用户权限刷新")
    void edit_successRefreshesOnlineTokens() throws Exception {
        when(roleService.checkRoleNameUnique(any(SysRole.class))).thenReturn(true);
        when(roleService.checkRoleKeyUnique(any(SysRole.class))).thenReturn(true);
        when(roleService.updateRole(any(SysRole.class))).thenReturn(1);

        String body = "{\"roleId\":5,\"roleName\":\"教务处\",\"roleKey\":\"academic\",\"roleSort\":1}";
        mockMvc.perform(put("/system/role")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysRole> captor = ArgumentCaptor.forClass(SysRole.class);
        verify(roleService).updateRole(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
        verify(tokenService).refreshPermissionByRoleId(eq(5L), eq(permissionService));
    }

    @Test
    @DisplayName("edit：更新影响行数为 0 返回 500 且不刷新在线令牌")
    void edit_zeroRowsFailsWithoutRefresh() throws Exception {
        when(roleService.checkRoleNameUnique(any(SysRole.class))).thenReturn(true);
        when(roleService.checkRoleKeyUnique(any(SysRole.class))).thenReturn(true);
        when(roleService.updateRole(any(SysRole.class))).thenReturn(0);

        String body = "{\"roleId\":5,\"roleName\":\"教务处\",\"roleKey\":\"academic\",\"roleSort\":1}";
        mockMvc.perform(put("/system/role")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(tokenService, never()).refreshPermissionByRoleId(any(), any());
    }

    @Test
    @DisplayName("dataScope：先校验角色合法性再透传授权数据权限")
    void dataScope_checksAllowedThenDelegates() throws Exception {
        when(roleService.authDataScope(any(SysRole.class))).thenReturn(1);

        String body = "{\"roleId\":6,\"dataScope\":\"3\"}";
        mockMvc.perform(put("/system/role/dataScope")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(roleService).checkRoleAllowed(any(SysRole.class));
        verify(roleService).authDataScope(any(SysRole.class));
    }

    @Test
    @DisplayName("changeStatus：回填 updateBy 并透传状态修改")
    void changeStatus_setsUpdateByAndDelegates() throws Exception {
        when(roleService.updateRoleStatus(any(SysRole.class))).thenReturn(1);

        String body = "{\"roleId\":7,\"status\":\"1\"}";
        mockMvc.perform(put("/system/role/changeStatus")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysRole> captor = ArgumentCaptor.forClass(SysRole.class);
        verify(roleService).updateRoleStatus(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
        Assertions.assertEquals("1", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("remove：逗号路径变量绑定为 Long[] 批量删除")
    void remove_bindsCommaPathIds() throws Exception {
        when(roleService.deleteRoleByIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/system/role/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(roleService).deleteRoleByIds(captor.capture());
        Assertions.assertArrayEquals(new Long[] {1L, 2L}, captor.getValue());
    }

    @Test
    @DisplayName("optionselect：返回全部角色列表")
    void optionselect_returnsAllRoles() throws Exception {
        when(roleService.selectRoleAll()).thenReturn(List.of(role(1L, "管理员", "admin")));

        mockMvc.perform(get("/system/role/optionselect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].roleKey").value("admin"));
    }

    @Test
    @DisplayName("allocatedList/unallocatedList：以域对象条件分别走已分配/未分配查询")
    void allocatedAndUnallocated_bindUserCondition() throws Exception {
        when(roleService.selectRoleList(any(SysRole.class))).thenReturn(Collections.emptyList());
        when(userService.selectAllocatedList(any(SysUser.class)))
                .thenReturn(List.of(new SysUser()));
        when(userService.selectUnallocatedList(any(SysUser.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/system/role/authUser/allocatedList")
                        .param("roleId", "3").param("userName", "stu01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).selectAllocatedList(captor.capture());
        Assertions.assertEquals(3L, captor.getValue().getRoleId());
        Assertions.assertEquals("stu01", captor.getValue().getUserName());

        mockMvc.perform(get("/system/role/authUser/unallocatedList").param("roleId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
        verify(userService).selectUnallocatedList(any(SysUser.class));
    }

    @Test
    @DisplayName("authUser/cancel：请求体 userId+roleId 反序列化为 SysUserRole 后取消授权")
    void cancelAuthUser_bindsBodyPair() throws Exception {
        when(roleService.deleteAuthUser(any(SysUserRole.class))).thenReturn(1);

        mockMvc.perform(put("/system/role/authUser/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":10,\"roleId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysUserRole> captor = ArgumentCaptor.forClass(SysUserRole.class);
        verify(roleService).deleteAuthUser(captor.capture());
        Assertions.assertEquals(10L, captor.getValue().getUserId());
        Assertions.assertEquals(3L, captor.getValue().getRoleId());
    }

    @Test
    @DisplayName("authUser/cancelAll：roleId 与 userIds 逗号串绑定为批量取消参数")
    void cancelAuthUserAll_bindsParams() throws Exception {
        when(roleService.deleteAuthUsers(eq(3L), any(Long[].class))).thenReturn(2);

        mockMvc.perform(put("/system/role/authUser/cancelAll")
                        .param("roleId", "3").param("userIds", "10,11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(roleService).deleteAuthUsers(eq(3L), captor.capture());
        Assertions.assertArrayEquals(new Long[] {10L, 11L}, captor.getValue());
    }

    @Test
    @DisplayName("authUser/selectAll：先做角色数据范围校验再批量授权")
    void selectAuthUserAll_checksScopeThenGrants() throws Exception {
        when(roleService.insertAuthUsers(eq(3L), any(Long[].class))).thenReturn(2);

        mockMvc.perform(put("/system/role/authUser/selectAll")
                        .param("roleId", "3").param("userIds", "10,12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(roleService).checkRoleDataScope(eq(3L));
        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(roleService).insertAuthUsers(eq(3L), captor.capture());
        Assertions.assertArrayEquals(new Long[] {10L, 12L}, captor.getValue());
    }

    @Test
    @DisplayName("deptTree：checkedKeys 与 depts 平铺在 AjaxResult 顶层键而非 data")
    void deptTree_flattensCheckedKeysAndDepts() throws Exception {
        when(deptService.selectDeptListByRoleId(eq(4L))).thenReturn(List.of(100L, 101L));
        when(deptService.selectDeptTreeList(any(SysDept.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/system/role/deptTree/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.checkedKeys.length()").value(2))
                .andExpect(jsonPath("$.depts").isArray())
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
