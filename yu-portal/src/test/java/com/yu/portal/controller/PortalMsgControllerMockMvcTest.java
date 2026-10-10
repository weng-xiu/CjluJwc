package com.yu.portal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.system.domain.SysMessage;
import com.yu.system.domain.SysTodo;
import com.yu.system.service.ISysMessageService;
import com.yu.system.service.ISysTodoService;

/**
 * 门户消息与待办接口层测试（Q1 第四批：MockMvc standalone）。
 * 校验 P5 本人专用端点契约：list/todoList 强制 receiverId=登录用户，
 * todoList 缺省 status 补 "0"（仅未完成）、显式传入则保留；
 * markRead/markAllRead/completeTodo 透传登录用户且 toAjax 成败双分支；
 * unreadCount/pendingCount 直接返回计数。归属校验业务逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalMsgControllerMockMvcTest {

    @Mock
    private ISysMessageService sysMessageService;

    @Mock
    private ISysTodoService sysTodoService;

    @InjectMocks
    private PortalMsgController controller;

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
    @DisplayName("list：查询参数 receiverId 被强制覆盖为登录用户")
    void list_forcesReceiverId() throws Exception {
        when(sysMessageService.selectMessageList(any(SysMessage.class))).thenReturn(List.of());

        mockMvc.perform(get("/portal/msg/list").param("receiverId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SysMessage> captor = ArgumentCaptor.forClass(SysMessage.class);
        verify(sysMessageService).selectMessageList(captor.capture());
        Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getReceiverId());
    }

    @Test
    @DisplayName("markRead：透传 messageId 与登录用户，影响行数 1 返回 200")
    void markRead_success() throws Exception {
        when(sysMessageService.markRead(eq(7L), eq(1L))).thenReturn(1);

        mockMvc.perform(put("/portal/msg/read/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysMessageService).markRead(7L, 1L);
    }

    @Test
    @DisplayName("markRead：影响行数为 0 时 toAjax 返回 500")
    void markRead_zeroRowsReturnsError() throws Exception {
        when(sysMessageService.markRead(eq(8L), eq(1L))).thenReturn(0);

        mockMvc.perform(put("/portal/msg/read/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("markAllRead：按登录用户全部标记已读")
    void markAllRead_success() throws Exception {
        when(sysMessageService.markAllRead(1L)).thenReturn(3);

        mockMvc.perform(put("/portal/msg/readAll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysMessageService).markAllRead(1L);
        verify(sysMessageService, never()).markRead(any(), any());
    }

    @Test
    @DisplayName("unreadCount：返回登录用户未读数（$.data）")
    void unreadCount_returnsData() throws Exception {
        when(sysMessageService.countUnread(1L)).thenReturn(5);

        mockMvc.perform(get("/portal/msg/unreadCount"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    @DisplayName("todoList：缺省 status 补 '0'（仅未完成）并强制 receiverId")
    void todoList_defaultsStatusZero() throws Exception {
        when(sysTodoService.selectTodoList(any(SysTodo.class))).thenReturn(List.of());

        mockMvc.perform(get("/portal/msg/todoList").param("receiverId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SysTodo> captor = ArgumentCaptor.forClass(SysTodo.class);
        verify(sysTodoService).selectTodoList(captor.capture());
        Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getReceiverId());
        Assertions.assertEquals("0", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("todoList：显式传入 status 时保留该状态过滤")
    void todoList_keepsExplicitStatus() throws Exception {
        when(sysTodoService.selectTodoList(any(SysTodo.class))).thenReturn(List.of());

        mockMvc.perform(get("/portal/msg/todoList").param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysTodo> captor = ArgumentCaptor.forClass(SysTodo.class);
        verify(sysTodoService).selectTodoList(captor.capture());
        Assertions.assertEquals("1", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("completeTodo：透传 todoId 与登录用户办结")
    void completeTodo_success() throws Exception {
        when(sysTodoService.completeTodo(eq(9L), eq(1L))).thenReturn(1);

        mockMvc.perform(post("/portal/msg/todo/complete/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysTodoService).completeTodo(9L, 1L);
    }

    @Test
    @DisplayName("pendingCount：返回登录用户未完成待办数")
    void pendingCount_returnsData() throws Exception {
        when(sysTodoService.countPending(1L)).thenReturn(2);

        mockMvc.perform(get("/portal/msg/todo/pendingCount"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(2));
    }
}
