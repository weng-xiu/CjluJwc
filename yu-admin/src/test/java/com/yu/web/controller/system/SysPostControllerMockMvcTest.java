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
import com.yu.system.domain.SysPost;
import com.yu.system.service.ISysPostService;

/**
 * 岗位信息接口层测试（Q1 第十批：MockMvc standalone）。
 * 校验 @Validated 必填字段（postCode/postName/postSort）缺失的 400 拦截、
 * add/edit 中「岗位名称唯一性」优先于「岗位编码唯一性」的双拦截顺序与各自 500 提示文案、
 * 新增回填 createBy 与修改回填 updateBy（SecurityContext 登录用户）、
 * deletePostByIds 影响行数为 0 时 toAjax 降级为 500、
 * remove 逗号路径变量绑定为 Long[] 透传、optionselect 与 getInfo 的绑定契约；
 * 岗位被用户占用时的删除保护（countUserPostById）在 remove 端点不参与，由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysPostControllerMockMvcTest {

    @Mock
    private ISysPostService postService;

    @InjectMocks
    private SysPostController controller;

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

    /** 构造岗位对象；命名避开 MockMvcRequestBuilders.post 静态导入的同名冲突 */
    private SysPost buildPost(long id, String code, String name) {
        SysPost p = new SysPost();
        p.setPostId(id);
        p.setPostCode(code);
        p.setPostName(name);
        p.setPostSort(1);
        p.setStatus("0");
        return p;
    }

    @Test
    @DisplayName("list：查询条件绑定域对象并返回表格结构")
    void list_bindsQueryAndReturnsTable() throws Exception {
        when(postService.selectPostList(any(SysPost.class)))
                .thenReturn(List.of(buildPost(1L, "ceo", "董事长")));

        mockMvc.perform(get("/system/post/list").param("postName", "董事"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].postCode").value("ceo"));

        ArgumentCaptor<SysPost> captor = ArgumentCaptor.forClass(SysPost.class);
        verify(postService).selectPostList(captor.capture());
        Assertions.assertEquals("董事", captor.getValue().getPostName());
    }

    @Test
    @DisplayName("getInfo：路径变量绑定 postId 并返回岗位详情")
    void getInfo_bindsPathId() throws Exception {
        when(postService.selectPostById(eq(7L))).thenReturn(buildPost(7L, "hr", "人事专员"));

        mockMvc.perform(get("/system/post/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.postId").value(7))
                .andExpect(jsonPath("$.data.postName").value("人事专员"));
    }

    @Test
    @DisplayName("add：postSort 缺失被 @Validated 拦截返回 400，唯一性校验不执行")
    void add_missingSortRejected() throws Exception {
        String body = "{\"postCode\":\"hr\",\"postName\":\"人事专员\"}";
        mockMvc.perform(post("/system/post")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(postService, never()).checkPostNameUnique(any(SysPost.class));
        verify(postService, never()).insertPost(any(SysPost.class));
    }

    @Test
    @DisplayName("add：postName 空白被 @Validated 拦截返回 400")
    void add_blankNameRejected() throws Exception {
        String body = "{\"postCode\":\"hr\",\"postName\":\"\",\"postSort\":1}";
        mockMvc.perform(post("/system/post")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(postService, never()).insertPost(any(SysPost.class));
    }

    @Test
    @DisplayName("add：岗位名称重复优先于编码重复被拦截，返回 500 且不落库")
    void add_duplicateNameRejectedFirst() throws Exception {
        when(postService.checkPostNameUnique(any(SysPost.class))).thenReturn(false);

        String body = "{\"postCode\":\"hr\",\"postName\":\"人事专员\",\"postSort\":1}";
        mockMvc.perform(post("/system/post")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("新增岗位'人事专员'失败，岗位名称已存在"));

        verify(postService, never()).checkPostCodeUnique(any(SysPost.class));
        verify(postService, never()).insertPost(any(SysPost.class));
    }

    @Test
    @DisplayName("add：名称唯一但编码重复走第二道拦截")
    void add_duplicateCodeRejected() throws Exception {
        when(postService.checkPostNameUnique(any(SysPost.class))).thenReturn(true);
        when(postService.checkPostCodeUnique(any(SysPost.class))).thenReturn(false);

        mockMvc.perform(post("/system/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postCode\":\"hr\",\"postName\":\"新岗位\",\"postSort\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("新增岗位'新岗位'失败，岗位编码已存在"));

        verify(postService, never()).insertPost(any(SysPost.class));
    }

    @Test
    @DisplayName("add：双唯一性通过则回填 createBy 并落库")
    void add_setsCreateByAndPersists() throws Exception {
        when(postService.checkPostNameUnique(any(SysPost.class))).thenReturn(true);
        when(postService.checkPostCodeUnique(any(SysPost.class))).thenReturn(true);
        when(postService.insertPost(any(SysPost.class))).thenReturn(1);

        mockMvc.perform(post("/system/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postCode\":\"teach\",\"postName\":\"教师\",\"postSort\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"));

        ArgumentCaptor<SysPost> captor = ArgumentCaptor.forClass(SysPost.class);
        verify(postService).insertPost(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getCreateBy());
        Assertions.assertEquals(3, captor.getValue().getPostSort());
    }

    @Test
    @DisplayName("edit：编码重复拦截；合法请求回填 updateBy")
    void edit_uniqueCheckAndUpdateBy() throws Exception {
        when(postService.checkPostNameUnique(any(SysPost.class))).thenReturn(true);
        when(postService.checkPostCodeUnique(any(SysPost.class))).thenReturn(false);
        mockMvc.perform(put("/system/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\":5,\"postCode\":\"hr\",\"postName\":\"人事经理\",\"postSort\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("修改岗位'人事经理'失败，岗位编码已存在"));
        verify(postService, never()).updatePost(any(SysPost.class));

        when(postService.checkPostCodeUnique(any(SysPost.class))).thenReturn(true);
        when(postService.updatePost(any(SysPost.class))).thenReturn(1);
        mockMvc.perform(put("/system/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\":5,\"postCode\":\"hr\",\"postName\":\"人事经理\",\"postSort\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysPost> captor = ArgumentCaptor.forClass(SysPost.class);
        verify(postService).updatePost(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
        Assertions.assertEquals(5L, captor.getValue().getPostId());
    }

    @Test
    @DisplayName("remove：逗号路径变量绑定 Long[]，影响行数 0 降级为 500")
    void remove_bindsCommaIdsAndMapsZeroRowsToError() throws Exception {
        when(postService.deletePostByIds(any(Long[].class))).thenReturn(0);
        mockMvc.perform(delete("/system/post/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("操作失败"));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(postService).deletePostByIds(captor.capture());
        Assertions.assertArrayEquals(new Long[] {1L, 2L}, captor.getValue());

        when(postService.deletePostByIds(any(Long[].class))).thenReturn(1);
        mockMvc.perform(delete("/system/post/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("optionselect：返回全量岗位列表供选择框使用")
    void optionselect_returnsAllPosts() throws Exception {
        when(postService.selectPostAll())
                .thenReturn(List.of(buildPost(1L, "ceo", "董事长"), buildPost(2L, "hr", "人事专员")));

        mockMvc.perform(get("/system/post/optionselect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].postCode").value("ceo"));
    }
}
