package com.yu.web.controller.monitor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.framework.web.service.SysPasswordService;
import com.yu.system.domain.SysLogininfor;
import com.yu.system.service.ISysLogininforService;

/**
 * 登录日志接口层测试（Q1 第六批：MockMvc standalone）。
 * 校验 monitor 域登录访问记录的列表查询、批量删除（逗号路径绑 Long[]）、
 * 清空日志、账户解锁（透传用户名清理密码错误计数缓存）四类端点契约；
 * 日志落库与解锁缓存实现由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysLogininforControllerMockMvcTest {

    @Mock
    private ISysLogininforService logininforService;

    @Mock
    private SysPasswordService passwordService;

    @InjectMocks
    private SysLogininforController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("list：登录日志分页返回")
    void list_returnsTable() throws Exception {
        when(logininforService.selectLogininforList(any(SysLogininfor.class))).thenReturn(List.of());

        mockMvc.perform(get("/monitor/logininfor/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());
    }

    @Test
    @DisplayName("remove：逗号分隔路径绑定为 Long[] 批量删除")
    void remove_bindsIdArray() throws Exception {
        when(logininforService.deleteLogininforByIds(any())).thenReturn(2);

        mockMvc.perform(delete("/monitor/logininfor/1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(logininforService).deleteLogininforByIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(3, captor.getValue().length);
    }

    @Test
    @DisplayName("clean：清空登录日志并返回成功")
    void clean_success() throws Exception {
        mockMvc.perform(delete("/monitor/logininfor/clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(logininforService).cleanLogininfor();
    }

    @Test
    @DisplayName("unlock：按用户名清理登录锁定缓存")
    void unlock_clearsLoginRecordCache() throws Exception {
        mockMvc.perform(get("/monitor/logininfor/unlock/zhangsan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(passwordService).clearLoginRecordCache("zhangsan");
    }
}
