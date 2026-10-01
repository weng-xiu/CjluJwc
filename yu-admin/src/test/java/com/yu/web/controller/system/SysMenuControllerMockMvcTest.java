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

import com.yu.common.core.domain.TreeSelect;
import com.yu.common.core.domain.entity.SysMenu;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.system.service.ISysMenuService;

/**
 * 菜单管理接口层测试（Q1 第九批：MockMvc standalone）。
 * 校验 @Validated 必填字段（menuName/menuType/orderNum）缺失的 400 拦截、外链菜单（isFrame=0）地址必须 http(s):// 开头、
 * 名称/路由配置唯一性三重拦截顺序契约、新增回填 createBy 与修改回填 updateBy、
 * 修改时上级菜单不能选择自身的自环拦截、treeselect/roleMenuTreeselect 的 platform 参数有无双分支分流、
 * updateSort 逗号串拆分、删除时子菜单与已分配角色的 warn(601) 拦截分支；
 * 菜单树构建、角色路由权限计算等业务逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysMenuControllerMockMvcTest {

    @Mock
    private ISysMenuService menuService;

    @InjectMocks
    private SysMenuController controller;

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

    private SysMenu menu(long id, String name) {
        SysMenu m = new SysMenu();
        m.setMenuId(id);
        m.setMenuName(name);
        m.setMenuType("C");
        m.setOrderNum(1);
        return m;
    }

    private TreeSelect tree(long id, String label) {
        TreeSelect t = new TreeSelect();
        t.setId(id);
        t.setLabel(label);
        return t;
    }

    @Test
    @DisplayName("list：查询条件绑定域对象、以登录用户为查询主体并返回列表落 data")
    void list_bindsConditionAndUserId() throws Exception {
        when(menuService.selectMenuList(any(SysMenu.class), eq(1L)))
                .thenReturn(List.of(menu(1L, "成绩管理")));

        mockMvc.perform(get("/system/menu/list").param("menuName", "成绩"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].menuName").value("成绩管理"));

        ArgumentCaptor<SysMenu> captor = ArgumentCaptor.forClass(SysMenu.class);
        verify(menuService).selectMenuList(captor.capture(), eq(1L));
        Assertions.assertEquals("成绩", captor.getValue().getMenuName());
    }

    @Test
    @DisplayName("getInfo：路径变量绑定 menuId 返回菜单详情")
    void getInfo_bindsPathId() throws Exception {
        when(menuService.selectMenuById(eq(15L))).thenReturn(menu(15L, "用户管理"));

        mockMvc.perform(get("/system/menu/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.menuId").value(15))
                .andExpect(jsonPath("$.data.menuName").value("用户管理"));
    }

    @Test
    @DisplayName("treeselect：不带 platform 走条件查询分支并以登录用户为上下文")
    void treeselect_withoutPlatform_usesConditionBranch() throws Exception {
        when(menuService.selectMenuList(any(SysMenu.class), eq(1L)))
                .thenReturn(List.of(menu(2L, "系统管理")));
        when(menuService.buildMenuTreeSelect(any())).thenReturn(List.of(tree(2L, "系统管理")));

        mockMvc.perform(get("/system/menu/treeselect").param("menuName", "系统"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].label").value("系统管理"));

        verify(menuService, never()).selectMenuListByPlatform(any(), any());
    }

    @Test
    @DisplayName("treeselect：带 platform 参数分流到按平台查询分支")
    void treeselect_withPlatform_usesPlatformBranch() throws Exception {
        when(menuService.selectMenuListByPlatform(eq(1L), eq("portal")))
                .thenReturn(List.of(menu(3L, "门户首页")));
        when(menuService.buildMenuTreeSelect(any())).thenReturn(List.of(tree(3L, "门户首页")));

        mockMvc.perform(get("/system/menu/treeselect").param("platform", "portal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(3));

        verify(menuService, never()).selectMenuList(any(SysMenu.class), eq(1L));
    }

    @Test
    @DisplayName("roleMenuTreeselect：无 platform 走 selectMenuList(userId)+roleId 已选键分支")
    void roleMenuTreeselect_defaultBranch() throws Exception {
        when(menuService.selectMenuList(eq(1L))).thenReturn(List.of(menu(4L, "排课管理")));
        when(menuService.selectMenuListByRoleId(eq(7L))).thenReturn(List.of(4L));
        when(menuService.buildMenuTreeSelect(any())).thenReturn(List.of(tree(4L, "排课管理")));

        mockMvc.perform(get("/system/menu/roleMenuTreeselect/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedKeys[0]").value(4))
                .andExpect(jsonPath("$.menus[0].id").value(4));

        verify(menuService, never()).selectMenuListByRoleIdAndPlatform(any(), any());
    }

    @Test
    @DisplayName("roleMenuTreeselect：带 platform 走平台菜单与平台已选键双分支")
    void roleMenuTreeselect_platformBranch() throws Exception {
        when(menuService.selectMenuListByPlatform(eq(1L), eq("admin")))
                .thenReturn(List.of(menu(5L, "公告管理")));
        when(menuService.selectMenuListByRoleIdAndPlatform(eq(7L), eq("admin")))
                .thenReturn(List.of(5L, 6L));
        when(menuService.buildMenuTreeSelect(any())).thenReturn(List.of(tree(5L, "公告管理")));

        mockMvc.perform(get("/system/menu/roleMenuTreeselect/7").param("platform", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedKeys.length()").value(2))
                .andExpect(jsonPath("$.menus[0].id").value(5));

        verify(menuService, never()).selectMenuListByRoleId(any());
    }

    @Test
    @DisplayName("add：menuName/menuType 缺失被 @Validated 拦截返回 400，不落库")
    void add_missingRequiredFieldsRejected() throws Exception {
        String body = "{\"orderNum\":1,\"path\":\"grade\"}";
        mockMvc.perform(post("/system/menu")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(menuService, never()).insertMenu(any(SysMenu.class));
    }

    @Test
    @DisplayName("add：orderNum 缺失（@NotNull）同样被拦截返回 400")
    void add_missingOrderNumRejected() throws Exception {
        String body = "{\"menuName\":\"新菜单\",\"menuType\":\"C\"}";
        mockMvc.perform(post("/system/menu")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(menuService, never()).insertMenu(any(SysMenu.class));
    }

    @Test
    @DisplayName("add：菜单名称已存在被第一道唯一性拦截且不落库")
    void add_duplicateNameRejected() throws Exception {
        when(menuService.checkMenuNameUnique(any(SysMenu.class))).thenReturn(false);

        String body = "{\"menuName\":\"用户管理\",\"menuType\":\"C\",\"orderNum\":1,\"isFrame\":\"1\"}";
        mockMvc.perform(post("/system/menu")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("新增菜单'用户管理'失败，菜单名称已存在"));

        verify(menuService, never()).insertMenu(any(SysMenu.class));
    }

    @Test
    @DisplayName("add：外链菜单（isFrame=0）地址非 http(s):// 被拦截")
    void add_frameMenuWithNonHttpPathRejected() throws Exception {
        when(menuService.checkMenuNameUnique(any(SysMenu.class))).thenReturn(true);

        String body = "{\"menuName\":\"外部链接\",\"menuType\":\"C\",\"orderNum\":1,\"isFrame\":\"0\",\"path\":\"/bad/url\"}";
        mockMvc.perform(post("/system/menu")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("新增菜单'外部链接'失败，地址必须以http(s)://开头"));

        verify(menuService, never()).insertMenu(any(SysMenu.class));
    }

    @Test
    @DisplayName("add：路由名称或地址已存在被第三道 checkRouteConfigUnique 拦截")
    void add_duplicateRouteConfigRejected() throws Exception {
        when(menuService.checkMenuNameUnique(any(SysMenu.class))).thenReturn(true);
        when(menuService.checkRouteConfigUnique(any(SysMenu.class))).thenReturn(false);

        String body = "{\"menuName\":\"成绩\",\"menuType\":\"C\",\"orderNum\":1,\"isFrame\":\"1\",\"path\":\"grade\"}";
        mockMvc.perform(post("/system/menu")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("新增菜单'成绩'失败，路由名称或地址已存在"));

        verify(menuService, never()).insertMenu(any(SysMenu.class));
    }

    @Test
    @DisplayName("add：合法菜单回填 createBy 后落库")
    void add_setsCreateByAndPersists() throws Exception {
        when(menuService.checkMenuNameUnique(any(SysMenu.class))).thenReturn(true);
        when(menuService.checkRouteConfigUnique(any(SysMenu.class))).thenReturn(true);
        when(menuService.insertMenu(any(SysMenu.class))).thenReturn(1);

        String body = "{\"menuName\":\"选课名单\",\"menuType\":\"C\",\"orderNum\":2,\"isFrame\":\"1\",\"path\":\"enroll\"}";
        mockMvc.perform(post("/system/menu")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysMenu> captor = ArgumentCaptor.forClass(SysMenu.class);
        verify(menuService).insertMenu(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getCreateBy());
    }

    @Test
    @DisplayName("edit：上级菜单选择自己（menuId==parentId）被自环拦截")
    void edit_selfParentRejected() throws Exception {
        when(menuService.checkMenuNameUnique(any(SysMenu.class))).thenReturn(true);

        String body = "{\"menuId\":9,\"parentId\":9,\"menuName\":\"目录\",\"menuType\":\"M\",\"orderNum\":1,\"isFrame\":\"1\"}";
        mockMvc.perform(put("/system/menu")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("修改菜单'目录'失败，上级菜单不能选择自己"));

        verify(menuService, never()).updateMenu(any(SysMenu.class));
    }

    @Test
    @DisplayName("edit：合法菜单回填 updateBy 后更新")
    void edit_setsUpdateByAndUpdates() throws Exception {
        when(menuService.checkMenuNameUnique(any(SysMenu.class))).thenReturn(true);
        when(menuService.checkRouteConfigUnique(any(SysMenu.class))).thenReturn(true);
        when(menuService.updateMenu(any(SysMenu.class))).thenReturn(1);

        String body = "{\"menuId\":9,\"parentId\":1,\"menuName\":\"改名\",\"menuType\":\"C\",\"orderNum\":1,\"isFrame\":\"1\",\"path\":\"renamed\"}";
        mockMvc.perform(put("/system/menu")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysMenu> captor = ArgumentCaptor.forClass(SysMenu.class);
        verify(menuService).updateMenu(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
    }

    @Test
    @DisplayName("updateSort：menuIds/orderNums 逗号串拆分为数组透传")
    void updateSort_splitsCommaStrings() throws Exception {
        String body = "{\"menuIds\":\"3,1,2\",\"orderNums\":\"1,2,3\"}";
        mockMvc.perform(put("/system/menu/updateSort")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(menuService).updateMenuSort(eq(new String[] {"3", "1", "2"}), eq(new String[] {"1", "2", "3"}));
    }

    @Test
    @DisplayName("remove：存在子菜单返回 warn(601) 且不删除")
    void remove_withChildrenWarns() throws Exception {
        when(menuService.hasChildByMenuId(eq(8L))).thenReturn(true);

        mockMvc.perform(delete("/system/menu/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(601))
                .andExpect(jsonPath("$.msg").value("存在子菜单,不允许删除"));

        verify(menuService, never()).deleteMenuById(any());
    }

    @Test
    @DisplayName("remove：菜单已分配角色返回 warn(601) 且不删除")
    void remove_assignedToRoleWarns() throws Exception {
        when(menuService.hasChildByMenuId(eq(8L))).thenReturn(false);
        when(menuService.checkMenuExistRole(eq(8L))).thenReturn(true);

        mockMvc.perform(delete("/system/menu/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(601))
                .andExpect(jsonPath("$.msg").value("菜单已分配,不允许删除"));

        verify(menuService, never()).deleteMenuById(any());
    }

    @Test
    @DisplayName("remove：无子菜单且未分配时正常删除返回 200")
    void remove_cleanMenuDeleted() throws Exception {
        when(menuService.hasChildByMenuId(eq(8L))).thenReturn(false);
        when(menuService.checkMenuExistRole(eq(8L))).thenReturn(false);
        when(menuService.deleteMenuById(eq(8L))).thenReturn(1);

        mockMvc.perform(delete("/system/menu/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(menuService).deleteMenuById(eq(8L));
    }
}
