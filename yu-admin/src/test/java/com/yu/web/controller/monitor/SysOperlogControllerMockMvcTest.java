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

import com.yu.system.domain.SysOperLog;
import com.yu.system.service.ISysOperLogService;

/**
 * 操作日志接口层测试（Q1 第六批：MockMvc standalone）。
 * 校验 monitor 域操作日志的列表查询、批量删除（逗号路径绑 Long[]）、清空日志端点契约；
 * 日志切面写入与持久化由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysOperlogControllerMockMvcTest {

    @Mock
    private ISysOperLogService operLogService;

    @InjectMocks
    private SysOperlogController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("list：操作日志分页返回")
    void list_returnsTable() throws Exception {
        when(operLogService.selectOperLogList(any(SysOperLog.class))).thenReturn(List.of());

        mockMvc.perform(get("/monitor/operlog/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());
    }

    @Test
    @DisplayName("remove：逗号分隔路径绑定为 Long[] 批量删除")
    void remove_bindsIdArray() throws Exception {
        when(operLogService.deleteOperLogByIds(any())).thenReturn(2);

        mockMvc.perform(delete("/monitor/operlog/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(operLogService).deleteOperLogByIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("clean：清空操作日志并返回成功")
    void clean_success() throws Exception {
        mockMvc.perform(delete("/monitor/operlog/clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(operLogService).cleanOperLog();
    }
}
