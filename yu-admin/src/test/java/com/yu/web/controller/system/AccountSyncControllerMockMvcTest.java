package com.yu.web.controller.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.system.service.IAccountSyncService;

/**
 * 账号同步控制器契约测试（G5）：锁定教师/学生账号从 BRM 反向同步的两条 POST 接口契约。
 * 校验同步结果 {created/updated/...} 计数经统一响应格式序列化，且可选的部门/班级范围参数
 * 原样透传给服务层（视图层不感知账号生成的幂等与去重逻辑）。
 */
@ExtendWith(MockitoExtension.class)
class AccountSyncControllerMockMvcTest
{
    @Mock
    private IAccountSyncService accountSyncService;

    @InjectMocks
    private AccountSyncController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static Map<String, Integer> counts(int created, int updated)
    {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("created", created);
        m.put("updated", updated);
        return m;
    }

    @Test
    @DisplayName("POST /system/account/sync/teachers：按院系同步并回传统计")
    void syncTeachersWithDept() throws Exception
    {
        when(accountSyncService.syncTeacherAccounts(100L)).thenReturn(counts(5, 2));

        mockMvc.perform(post("/system/account/sync/teachers").param("deptId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.created").value(5))
                .andExpect(jsonPath("$.data.updated").value(2));

        verify(accountSyncService).syncTeacherAccounts(100L);
    }

    @Test
    @DisplayName("POST /system/account/sync/teachers：缺省院系时全量同步（透传 null）")
    void syncTeachersWithoutDept() throws Exception
    {
        when(accountSyncService.syncTeacherAccounts(any())).thenReturn(counts(1, 0));

        mockMvc.perform(post("/system/account/sync/teachers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.created").value(1));

        verify(accountSyncService).syncTeacherAccounts(null);
    }

    @Test
    @DisplayName("POST /system/account/sync/students：按班级同步并回传统计")
    void syncStudentsWithClass() throws Exception
    {
        when(accountSyncService.syncStudentAccounts(7L)).thenReturn(counts(30, 4));

        mockMvc.perform(post("/system/account/sync/students").param("classId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.created").value(30))
                .andExpect(jsonPath("$.data.updated").value(4));

        verify(accountSyncService).syncStudentAccounts(7L);
    }
}
