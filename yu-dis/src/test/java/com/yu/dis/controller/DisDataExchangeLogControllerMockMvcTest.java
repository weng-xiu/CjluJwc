package com.yu.dis.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.yu.dis.domain.DisDataExchangeLog;
import com.yu.dis.service.IDisDataExchangeLogService;

/**
 * 数据交换日志接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-dis 长尾日志控制器）。
 * 只读日志型控制器，无 add/edit。校验路由 /dis/exchange 下 list/getInfo 查询、
 * remove 逗号数组批量删除、clean 一键清空（DELETE /clean → toAjax(cleanDisDataExchangeLog())，
 * 影响行数0时降级500）。Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DisDataExchangeLogControllerMockMvcTest {

    @Mock
    private IDisDataExchangeLogService disDataExchangeLogService;

    @InjectMocks
    private DisDataExchangeLogController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定logId并返回日志明细")
    void getInfo_returnsLog() throws Exception {
        DisDataExchangeLog log = new DisDataExchangeLog();
        log.setLogId(5L);
        when(disDataExchangeLogService.selectDisDataExchangeLogByLogId(eq(5L))).thenReturn(log);

        mockMvc.perform(get("/dis/exchange/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.logId").value(5));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(disDataExchangeLogService.deleteDisDataExchangeLogByLogIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/dis/exchange/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(disDataExchangeLogService).deleteDisDataExchangeLogByLogIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("clean：一键清空日志，影响行数0时toAjax降级500")
    void clean_contract() throws Exception {
        when(disDataExchangeLogService.cleanDisDataExchangeLog()).thenReturn(1);
        mockMvc.perform(delete("/dis/exchange/clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        when(disDataExchangeLogService.cleanDisDataExchangeLog()).thenReturn(0);
        mockMvc.perform(delete("/dis/exchange/clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("list：返回表格分页结构并透传查询条件")
    void list_returnsTable() throws Exception {
        when(disDataExchangeLogService.selectDisDataExchangeLogList(any(DisDataExchangeLog.class))).thenReturn(List.of());

        mockMvc.perform(get("/dis/exchange/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        verify(disDataExchangeLogService).selectDisDataExchangeLogList(any(DisDataExchangeLog.class));
    }
}
