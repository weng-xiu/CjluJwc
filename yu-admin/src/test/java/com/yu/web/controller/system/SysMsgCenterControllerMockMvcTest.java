package com.yu.web.controller.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
 * 统一消息与待办中心接口层测试（Q1 第十二批：MockMvc standalone）。
 * 校验消息/待办列表把当前登录用户 ID 强制回填到查询条件的 receiverId（越权隔离契约）、
 * markRead/markAllRead/completeTodo 依影响行数 toAjax、unreadCount/pendingCount 将 int 计数经
 * {@code success(Object)} 落 data 而非 msg；
 * 登录态由 standalone 注入 SecurityContext 的 LoginUser（userId=1）提供，读写落库逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysMsgCenterControllerMockMvcTest {

    @Mock
    private ISysMessageService sysMessageService;

    @Mock
    private ISysTodoService sysTodoService;

    @InjectMocks
    private SysMsgCenterController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
        sysUser.setUserId(1L);
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
    @DisplayName("messageList：强制把登录用户 ID 回填为查询 receiverId")
    void messageList_injectsReceiverId() throws Exception {
        when(sysMessageService.selectMessageList(any(SysMessage.class))).thenReturn(List.of(new SysMessage()));

        mockMvc.perform(get("/system/msgCenter/message/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows.length()").value(1));

        ArgumentCaptor<SysMessage> captor = ArgumentCaptor.forClass(SysMessage.class);
        verify(sysMessageService).selectMessageList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(1L, captor.getValue().getReceiverId());
    }

    @Test
    @DisplayName("markRead：影响行数 1 返回操作成功并透传当前用户")
    void markRead_ok() throws Exception {
        when(sysMessageService.markRead(eq(5L), eq(1L))).thenReturn(1);

        mockMvc.perform(put("/system/msgCenter/message/read/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("markAllRead：按当前用户批量置已读")
    void markAllRead_ok() throws Exception {
        when(sysMessageService.markAllRead(eq(1L))).thenReturn(3);

        mockMvc.perform(put("/system/msgCenter/message/readAll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("unreadCount：int 计数经 success(Object) 落 data")
    void unreadCount_toData() throws Exception {
        when(sysMessageService.countUnread(eq(1L))).thenReturn(7);

        mockMvc.perform(get("/system/msgCenter/message/unreadCount"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(7));
    }

    @Test
    @DisplayName("todoList：强制把登录用户 ID 回填为查询 receiverId")
    void todoList_injectsReceiverId() throws Exception {
        when(sysTodoService.selectTodoList(any(SysTodo.class))).thenReturn(List.of(new SysTodo()));

        mockMvc.perform(get("/system/msgCenter/todo/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows.length()").value(1));

        ArgumentCaptor<SysTodo> captor = ArgumentCaptor.forClass(SysTodo.class);
        verify(sysTodoService).selectTodoList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(1L, captor.getValue().getReceiverId());
    }

    @Test
    @DisplayName("completeTodo：影响行数 0 经 toAjax 降级 500")
    void completeTodo_zeroRowsReturnsError() throws Exception {
        when(sysTodoService.completeTodo(eq(3L), eq(1L))).thenReturn(0);

        mockMvc.perform(post("/system/msgCenter/todo/complete/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("操作失败"));
    }

    @Test
    @DisplayName("pendingCount：int 计数落 data")
    void pendingCount_toData() throws Exception {
        when(sysTodoService.countPending(eq(1L))).thenReturn(4);

        mockMvc.perform(get("/system/msgCenter/todo/pendingCount"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(4));
    }
}
