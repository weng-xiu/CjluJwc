package com.yu.web.controller.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import com.yu.brm.domain.BrmTeacher;
import com.yu.brm.service.IBrmTeacherService;
import com.yu.common.core.domain.TreeSelect;
import com.yu.common.core.domain.entity.SysDept;
import com.yu.common.core.domain.entity.SysRole;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.system.service.ISysDeptService;
import com.yu.system.service.ISysPostService;
import com.yu.system.service.ISysRoleService;
import com.yu.system.service.ISysUserService;

/**
 * 用户管理接口层测试（Q1 第七批：MockMvc standalone）。
 * 校验 getInfo 无参（新增初始化：roles/posts）与带 userId（data/postIds/roleIds，教师补 teacherInfo）两分支、
 * add 的教师工号必填/账号唯一性拦截/合法时 BCrypt 加密并回填 createBy、edit 唯一性拦截与 updateBy 回填、
 * remove 的"当前用户不能删除"自我保护、resetPwd 密码加密、changeStatus/changeLifecycle 状态流转、
 * authRole/insertAuthRole 授权契约、deptTree 部门树；数据权限校验、真实落库由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysUserControllerMockMvcTest {

    @Mock
    private ISysUserService userService;

    @Mock
    private ISysRoleService roleService;

    @Mock
    private ISysDeptService deptService;

    @Mock
    private ISysPostService postService;

    @Mock
    private IBrmTeacherService brmTeacherService;

    @InjectMocks
    private SysUserController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        // BaseController.getUsername()/getUserId() 依赖 SecurityContext，standalone 手动注入登录用户（userId=1）
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

    private SysRole role(long id) {
        SysRole r = new SysRole();
        r.setRoleId(id);
        return r;
    }

    @Test
    @DisplayName("list：查询条件绑定域对象并返回表格分页结构")
    void list_bindsQuery() throws Exception {
        SysUser u = new SysUser();
        u.setUserId(5L);
        u.setUserName("student01");
        when(userService.selectUserList(any(SysUser.class))).thenReturn(List.of(u));

        mockMvc.perform(get("/system/user/list").param("userName", "student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].userName").value("student01"));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).selectUserList(captor.capture());
        Assertions.assertEquals("student", captor.getValue().getUserName());
    }

    @Test
    @DisplayName("getInfo：无 userId 时返回可选角色与岗位（新增初始化），不含 data")
    void getInfo_withoutUserId_returnsRolesAndPosts() throws Exception {
        when(roleService.selectRoleAll()).thenReturn(List.of(role(2L)));
        when(postService.selectPostAll()).thenReturn(List.of());

        mockMvc.perform(get("/system/user/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("getInfo：带 userId 返回用户 data 与 postIds/roleIds（非教师不含 teacherInfo）")
    void getInfo_withUserId_returnsDataAndIds() throws Exception {
        SysUser u = new SysUser();
        u.setUserId(5L);
        u.setUserCategory("student");
        u.setRoles(List.of());
        when(userService.selectUserById(eq(5L))).thenReturn(u);
        when(postService.selectPostListByUserId(eq(5L))).thenReturn(List.of(1L, 2L));
        when(roleService.selectRoleAll()).thenReturn(List.of(role(2L)));

        mockMvc.perform(get("/system/user/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(5))
                .andExpect(jsonPath("$.postIds").isArray())
                .andExpect(jsonPath("$.roleIds").isArray())
                .andExpect(jsonPath("$.teacherInfo").doesNotExist());

        verify(userService).checkUserDataScope(eq(5L));
    }

    @Test
    @DisplayName("getInfo：教师用户补充 teacherInfo（按 userId 查教师详情）")
    void getInfo_teacher_addsTeacherInfo() throws Exception {
        SysUser u = new SysUser();
        u.setUserId(6L);
        u.setUserCategory("teacher");
        u.setRoles(List.of());
        BrmTeacher teacher = new BrmTeacher();
        when(userService.selectUserById(eq(6L))).thenReturn(u);
        when(postService.selectPostListByUserId(eq(6L))).thenReturn(List.of());
        when(roleService.selectRoleAll()).thenReturn(List.of());
        when(brmTeacherService.selectBrmTeacherByUserId(eq(6L))).thenReturn(teacher);

        mockMvc.perform(get("/system/user/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(6))
                .andExpect(jsonPath("$.teacherInfo").exists());

        verify(brmTeacherService).selectBrmTeacherByUserId(eq(6L));
    }

    @Test
    @DisplayName("add：教师用户缺少工号被拦截，不落库")
    void add_teacherWithoutCode_rejected() throws Exception {
        String body = "{\"userName\":\"t01\",\"password\":\"abc123\",\"userCategory\":\"teacher\"}";
        mockMvc.perform(post("/system/user")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("教师工号不能为空")));

        verify(userService, never()).insertUser(any(SysUser.class));
    }

    @Test
    @DisplayName("add：登录账号已存在被拦截，不落库")
    void add_duplicateUserName_rejected() throws Exception {
        when(userService.checkUserNameUnique(any(SysUser.class))).thenReturn(false);

        String body = "{\"userName\":\"dup\",\"password\":\"abc123\"}";
        mockMvc.perform(post("/system/user")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("登录账号已存在")));

        verify(userService, never()).insertUser(any(SysUser.class));
    }

    @Test
    @DisplayName("add：合法用户 BCrypt 加密密码并回填 createBy 后落库")
    void add_valid_encryptsPasswordAndSetsCreateBy() throws Exception {
        when(userService.checkUserNameUnique(any(SysUser.class))).thenReturn(true);
        when(userService.insertUser(any(SysUser.class))).thenReturn(1);

        String body = "{\"userName\":\"newstu\",\"password\":\"abc123\",\"nickName\":\"新生\"}";
        mockMvc.perform(post("/system/user")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).insertUser(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getCreateBy());
        Assertions.assertNotEquals("abc123", captor.getValue().getPassword());
        Assertions.assertTrue(captor.getValue().getPassword().startsWith("$2"), "密码应为 BCrypt 密文");
    }

    @Test
    @DisplayName("add：userName 空白被 @Validated 拦截返回 400")
    void add_blankUserName_rejected() throws Exception {
        mockMvc.perform(post("/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"abc123\"}"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).insertUser(any(SysUser.class));
    }

    @Test
    @DisplayName("edit：合法用户回填 updateBy 并更新，账号冲突分支返回 error")
    void edit_validSetsUpdateBy() throws Exception {
        when(userService.checkUserNameUnique(any(SysUser.class))).thenReturn(true);
        when(userService.updateUser(any(SysUser.class))).thenReturn(1);

        mockMvc.perform(put("/system/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":5,\"userName\":\"u5\",\"nickName\":\"改\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).updateUser(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
    }

    @Test
    @DisplayName("remove：待删集合含当前登录用户时自我保护返回 error，不调删除")
    void remove_containsSelf_rejected() throws Exception {
        mockMvc.perform(delete("/system/user/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("当前用户不能删除"));

        verify(userService, never()).deleteUserByIds(any(Long[].class));
    }

    @Test
    @DisplayName("remove：删除他人集合正常透传数组并落删除")
    void remove_others_succeeds() throws Exception {
        when(userService.deleteUserByIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/system/user/5,6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(userService).deleteUserByIds(captor.capture());
        Assertions.assertArrayEquals(new Long[] {5L, 6L}, captor.getValue());
    }

    @Test
    @DisplayName("resetPwd：新密码 BCrypt 加密并回填 updateBy 后重置")
    void resetPwd_encryptsAndSetsUpdateBy() throws Exception {
        when(userService.resetPwd(any(SysUser.class))).thenReturn(1);

        mockMvc.perform(put("/system/user/resetPwd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":5,\"password\":\"newpass1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).resetPwd(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
        Assertions.assertTrue(captor.getValue().getPassword().startsWith("$2"));
    }

    @Test
    @DisplayName("changeStatus：状态变更回填 updateBy 并调用 updateUserStatus")
    void changeStatus_callsUpdateUserStatus() throws Exception {
        when(userService.updateUserStatus(any(SysUser.class))).thenReturn(1);

        mockMvc.perform(put("/system/user/changeStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":5,\"status\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).updateUserStatus(captor.capture());
        Assertions.assertEquals("1", captor.getValue().getStatus());
        Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
    }

    @Test
    @DisplayName("changeLifecycle：生命周期以 userId+accountStatus 透传服务层")
    void changeLifecycle_passesAccountStatus() throws Exception {
        when(userService.changeLifecycleStatus(eq(5L), eq("2"))).thenReturn(1);

        mockMvc.perform(put("/system/user/lifecycle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":5,\"accountStatus\":\"2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userService).changeLifecycleStatus(eq(5L), eq("2"));
    }

    @Test
    @DisplayName("authRole：返回用户与其可申请角色（非管理员过滤 admin 角色）")
    void authRole_returnsUserAndRoles() throws Exception {
        SysUser u = new SysUser();
        u.setUserId(5L);
        when(userService.selectUserById(eq(5L))).thenReturn(u);
        when(roleService.selectRolesByUserId(eq(5L))).thenReturn(List.of(role(2L), role(1L)));

        mockMvc.perform(get("/system/user/authRole/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.userId").value(5))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    @DisplayName("insertAuthRole：授权以 userId+roleIds 透传并返回成功")
    void insertAuthRole_grantsAndReturnsSuccess() throws Exception {
        mockMvc.perform(put("/system/user/authRole")
                        .param("userId", "5").param("roleIds", "1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(userService).insertUserAuth(eq(5L), captor.capture());
        Assertions.assertArrayEquals(new Long[] {1L, 2L, 3L}, captor.getValue());
    }

    @Test
    @DisplayName("deptTree：部门树下拉数据落 data")
    void deptTree_returnsTree() throws Exception {
        TreeSelect node = new TreeSelect();
        node.setId(100L);
        node.setLabel("教务处");
        when(deptService.selectDeptTreeList(any(SysDept.class)))
                .thenReturn(List.of(node));

        mockMvc.perform(get("/system/user/deptTree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(100))
                .andExpect(jsonPath("$.data[0].label").value("教务处"));
    }
}
