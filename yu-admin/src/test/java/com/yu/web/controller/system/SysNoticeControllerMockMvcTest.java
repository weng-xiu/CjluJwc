package com.yu.web.controller.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import java.util.Map;

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
import com.yu.system.domain.SysNotice;
import com.yu.system.service.ISysNoticeReadService;
import com.yu.system.service.ISysNoticeService;

/**
 * 公告接口层测试（Q1 第七批：MockMvc standalone）。
 * 校验 @Validated noticeTitle 缺失的 400 拦截、新增/修改的 createBy/updateBy 回填（SecurityContext 登录用户）、
 * listTop 的已读标记聚合与 unreadCount 计算、markRead 的 userId 取自登录上下文、
 * markReadAll 的逗号串经 Convert.toLongArray 解析为 Long[]、remove 先删已读记录再删公告的双 service 联动契约；
 * 公告正文 XSS 清洗、已读状态持久化等业务逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysNoticeControllerMockMvcTest {

    @Mock
    private ISysNoticeService noticeService;

    @Mock
    private ISysNoticeReadService noticeReadService;

    @InjectMocks
    private SysNoticeController controller;

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

    private SysNotice notice(long id, boolean read) {
        SysNotice n = new SysNotice();
        n.setNoticeId(id);
        n.setNoticeTitle("公告" + id);
        n.setNoticeType("1");
        n.setIsRead(read);
        return n;
    }

    @Test
    @DisplayName("list：查询条件绑定域对象并返回表格分页结构")
    void list_bindsQueryAndReturnsTable() throws Exception {
        when(noticeService.selectNoticeList(any(SysNotice.class)))
                .thenReturn(List.of(notice(1L, false)));

        mockMvc.perform(get("/system/notice/list").param("noticeTitle", "公告"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].noticeId").value(1));

        ArgumentCaptor<SysNotice> captor = ArgumentCaptor.forClass(SysNotice.class);
        verify(noticeService).selectNoticeList(captor.capture());
        Assertions.assertEquals("公告", captor.getValue().getNoticeTitle());
    }

    @Test
    @DisplayName("getInfo：路径变量绑定 noticeId 并返回公告详情")
    void getInfo_bindsPathId() throws Exception {
        when(noticeService.selectNoticeById(eq(9L))).thenReturn(notice(9L, false));

        mockMvc.perform(get("/system/notice/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.noticeId").value(9))
                .andExpect(jsonPath("$.data.noticeTitle").value("公告9"));
    }

    @Test
    @DisplayName("add：noticeTitle 缺失被 @Validated 拦截返回 400，不落库")
    void add_blankTitleRejected() throws Exception {
        String body = "{\"noticeType\":\"1\",\"noticeContent\":\"内容\"}";
        mockMvc.perform(post("/system/notice")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(noticeService, never()).insertNotice(any(SysNotice.class));
    }

    @Test
    @DisplayName("add：合法公告回填 createBy 为登录用户后落库")
    void add_setsCreateByAndPersists() throws Exception {
        when(noticeService.insertNotice(any(SysNotice.class))).thenReturn(1);

        String body = "{\"noticeTitle\":\"期末通知\",\"noticeType\":\"1\",\"noticeContent\":\"正文\"}";
        mockMvc.perform(post("/system/notice")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysNotice> captor = ArgumentCaptor.forClass(SysNotice.class);
        verify(noticeService).insertNotice(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getCreateBy());
    }

    @Test
    @DisplayName("edit：合法公告回填 updateBy，标题缺失仍被 @Validated 拦截")
    void edit_setsUpdateByAndValidates() throws Exception {
        when(noticeService.updateNotice(any(SysNotice.class))).thenReturn(1);
        mockMvc.perform(put("/system/notice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noticeId\":3,\"noticeTitle\":\"改后标题\",\"noticeContent\":\"x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        ArgumentCaptor<SysNotice> captor = ArgumentCaptor.forClass(SysNotice.class);
        verify(noticeService).updateNotice(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getUpdateBy());

        mockMvc.perform(put("/system/notice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noticeId\":3,\"noticeContent\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("listTop：按登录用户取带已读标记列表并计算 unreadCount")
    void listTop_computesUnreadCount() throws Exception {
        when(noticeReadService.selectNoticeListWithReadStatus(eq(1L), anyInt()))
                .thenReturn(List.of(notice(1L, true), notice(2L, false), notice(3L, false)));

        mockMvc.perform(get("/system/notice/listTop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.unreadCount").value(2));

        verify(noticeReadService).selectNoticeListWithReadStatus(eq(1L), eq(5));
    }

    @Test
    @DisplayName("markRead：noticeId 走请求参数，userId 取登录上下文")
    void markRead_usesLoggedInUser() throws Exception {
        mockMvc.perform(post("/system/notice/markRead").param("noticeId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(noticeReadService).markRead(eq(5L), eq(1L));
    }

    @Test
    @DisplayName("markReadAll：ids 逗号串经 Convert.toLongArray 解析为 Long[] 批量已读")
    void markReadAll_parsesCommaIds() throws Exception {
        mockMvc.perform(post("/system/notice/markReadAll").param("ids", "1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(noticeReadService).markReadBatch(eq(1L), captor.capture());
        Assertions.assertArrayEquals(new Long[] {1L, 2L, 3L}, captor.getValue());
    }

    @Test
    @DisplayName("readUsersList：按 noticeId+searchValue 查询已读用户表格")
    void readUsersList_bindsParams() throws Exception {
        when(noticeReadService.selectReadUsersByNoticeId(eq(7L), eq("张")))
                .thenReturn(List.of(Map.of("userId", 100, "nickName", "张三")));

        mockMvc.perform(get("/system/notice/readUsers/list")
                        .param("noticeId", "7").param("searchValue", "张"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].nickName").value("张三"));

        verify(noticeReadService).selectReadUsersByNoticeId(eq(7L), eq("张"));
    }

    @Test
    @DisplayName("remove：先删已读记录再删公告，逗号路径变量绑定为数组")
    void remove_cleansReadRecordsThenDeletes() throws Exception {
        when(noticeService.deleteNoticeByIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/system/notice/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(noticeReadService).deleteByNoticeIds(captor.capture());
        Assertions.assertArrayEquals(new Long[] {1L, 2L}, captor.getValue());
        verify(noticeService).deleteNoticeByIds(eq(new Long[] {1L, 2L}));
    }
}
