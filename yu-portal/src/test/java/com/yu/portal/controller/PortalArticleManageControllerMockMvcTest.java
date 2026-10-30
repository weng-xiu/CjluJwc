package com.yu.portal.controller;

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
import com.yu.portal.domain.PortalArticle;
import com.yu.portal.service.IPortalArticleService;

import java.util.Collections;

/**
 * 门户文章管理接口层测试（Q1 第十七批：MockMvc standalone）。
 * 覆盖 CMS 文章 CRUD 与发布工作流契约：list/getInfo 透传服务；add/edit 受 @Validated 拦截
 * （缺标题 400、服务不被调用）与 toAjax 成败双分支；remove 走逗号串数组绑定；
 * publish/submit/withdraw 各自委派对应服务并回 toAjax；approve/reject 以登录用户为审核人
 * 且从请求体取 reviewComment。发布/审核的状态机与校验业务逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalArticleManageControllerMockMvcTest {

    @Mock
    private IPortalArticleService portalArticleService;

    @InjectMocks
    private PortalArticleManageController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
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

    @Test
    @DisplayName("list：透传查询条件并返回分页行")
    void list_returnsPage() throws Exception {
        PortalArticle a = new PortalArticle();
        a.setArticleId(1L);
        when(portalArticleService.selectPortalArticleList(any(PortalArticle.class)))
                .thenReturn(List.of(a));

        mockMvc.perform(get("/portal/articleManage/list").param("columnId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].articleId").value(1));
    }

    @Test
    @DisplayName("getInfo：按主键返回文章详情")
    void getInfo_returnsArticle() throws Exception {
        PortalArticle a = new PortalArticle();
        a.setArticleId(5L);
        a.setTitle("通知");
        when(portalArticleService.selectPortalArticleById(5L)).thenReturn(a);

        mockMvc.perform(get("/portal/articleManage/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("通知"));
    }

    @Test
    @DisplayName("add：合法请求体落库成功")
    void add_validBodySucceeds() throws Exception {
        when(portalArticleService.insertPortalArticle(any(PortalArticle.class))).thenReturn(1);

        mockMvc.perform(post("/portal/articleManage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"开学通知\",\"columnId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<PortalArticle> captor = ArgumentCaptor.forClass(PortalArticle.class);
        verify(portalArticleService).insertPortalArticle(captor.capture());
        Assertions.assertEquals("开学通知", captor.getValue().getTitle());
    }

    @Test
    @DisplayName("add：富文本 XSS 载荷在 Controller 入口被白名单清洗")
    void add_sanitizesXssPayload() throws Exception {
        when(portalArticleService.insertPortalArticle(any(PortalArticle.class))).thenReturn(1);

        String body = "{\"title\":\"开学通知\",\"columnId\":3,\"content\":"
                + "\"<p>正文</p><script>alert(1)</script><img src=x onerror='alert(2)'>\"}";
        mockMvc.perform(post("/portal/articleManage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<PortalArticle> captor = ArgumentCaptor.forClass(PortalArticle.class);
        verify(portalArticleService).insertPortalArticle(captor.capture());
        Assertions.assertEquals("<p>正文</p><img src=\"x\">", captor.getValue().getContent());
    }

    @Test
    @DisplayName("add：缺标题被 @Validated 拦截返回 400 且不落库")
    void add_missingTitleReturns400() throws Exception {
        mockMvc.perform(post("/portal/articleManage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columnId\":3}"))
                .andExpect(status().isBadRequest());

        verify(portalArticleService, never()).insertPortalArticle(any());
    }

    @Test
    @DisplayName("edit：更新影响行数为 0 时返回 500")
    void edit_zeroRowsReturnsError() throws Exception {
        when(portalArticleService.updatePortalArticle(any(PortalArticle.class))).thenReturn(0);

        mockMvc.perform(put("/portal/articleManage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\":5,\"title\":\"改\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号串绑定为 ID 数组批量删除")
    void remove_bindsIdArray() throws Exception {
        when(portalArticleService.deletePortalArticleByIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/portal/articleManage/1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(portalArticleService).deletePortalArticleByIds(captor.capture());
        Assertions.assertArrayEquals(new Long[] {1L, 2L, 3L}, captor.getValue());
    }

    @Test
    @DisplayName("publish：委派发布服务并回成功")
    void publish_delegates() throws Exception {
        when(portalArticleService.publishArticle(9L)).thenReturn(1);

        mockMvc.perform(put("/portal/articleManage/publish/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(portalArticleService).publishArticle(9L);
    }

    @Test
    @DisplayName("submit：委派提交审核服务")
    void submit_delegates() throws Exception {
        when(portalArticleService.submitForReview(9L)).thenReturn(1);

        mockMvc.perform(put("/portal/articleManage/submit/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(portalArticleService).submitForReview(9L);
    }

    @Test
    @DisplayName("approve：审核人取登录用户、意见取请求体")
    void approve_bindsReviewerAndComment() throws Exception {
        when(portalArticleService.approveArticle(eq(9L), eq(1L), eq("通过"))).thenReturn(1);

        mockMvc.perform(put("/portal/articleManage/approve/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewComment\":\"通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(portalArticleService).approveArticle(eq(9L), eq(1L), eq("通过"));
    }

    @Test
    @DisplayName("reject：审核人取登录用户、驳回意见透传")
    void reject_bindsReviewerAndComment() throws Exception {
        when(portalArticleService.rejectArticle(eq(9L), eq(1L), eq("驳回"))).thenReturn(1);

        mockMvc.perform(put("/portal/articleManage/reject/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewComment\":\"驳回\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(portalArticleService).rejectArticle(eq(9L), eq(1L), eq("驳回"));
    }

    @Test
    @DisplayName("withdraw：委派撤回服务")
    void withdraw_delegates() throws Exception {
        when(portalArticleService.withdrawArticle(9L)).thenReturn(1);

        mockMvc.perform(put("/portal/articleManage/withdraw/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(portalArticleService).withdrawArticle(9L);
    }
}
