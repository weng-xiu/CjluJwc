package com.yu.web.controller.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.system.domain.SysAiChatRecord;
import com.yu.system.service.ISysAiChatService;

/**
 * AI 问答留痕与效果统计接口层测试（Q1 第十二批：MockMvc standalone）。
 * 留痕只读——本控制器无删除端点，测试覆盖 list 表格结构、getInfo 路径变量绑定 recordId、
 * stat 的 days/unmatchedLimit 默认值回填（缺省 14/10，显式则透传）与统计 Map 落 data；
 * 同时钉住 {@code /stat} 字面量路径优先于 {@code /{recordId}}（recordId 为 Long，"stat" 不可转换）的消歧契约；
 * 问答命中、留痕落库等业务逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysAiChatControllerMockMvcTest {

    @Mock
    private ISysAiChatService sysAiChatService;

    @InjectMocks
    private SysAiChatController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private SysAiChatRecord record(long id) {
        SysAiChatRecord r = new SysAiChatRecord();
        r.setRecordId(id);
        return r;
    }

    @Test
    @DisplayName("list：返回留痕表格结构")
    void list_returnsTable() throws Exception {
        when(sysAiChatService.selectSysAiChatRecordList(any(SysAiChatRecord.class)))
                .thenReturn(List.of(record(1L)));

        mockMvc.perform(get("/system/aiChat/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows.length()").value(1))
                .andExpect(jsonPath("$.rows[0].recordId").value(1));
    }

    @Test
    @DisplayName("getInfo：路径变量绑定 recordId 返回详情")
    void getInfo_bindsRecordId() throws Exception {
        when(sysAiChatService.selectSysAiChatRecordByRecordId(eq(9L))).thenReturn(record(9L));

        mockMvc.perform(get("/system/aiChat/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.recordId").value(9));
    }

    @Test
    @DisplayName("stat：days/unmatchedLimit 缺省时回填默认 14/10")
    void stat_appliesDefaults() throws Exception {
        Map<String, Object> stat = new HashMap<>();
        stat.put("total", 100);
        when(sysAiChatService.selectChatStat(any(SysAiChatRecord.class), eq(14), eq(10))).thenReturn(stat);

        mockMvc.perform(get("/system/aiChat/stat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(100));

        verify(sysAiChatService).selectChatStat(any(SysAiChatRecord.class), eq(14), eq(10));
    }

    @Test
    @DisplayName("stat：显式 days/unmatchedLimit 原样透传（/stat 字面量优先于 /{recordId}）")
    void stat_passesExplicitParams() throws Exception {
        when(sysAiChatService.selectChatStat(any(SysAiChatRecord.class), eq(7), eq(5)))
                .thenReturn(new HashMap<>());

        mockMvc.perform(get("/system/aiChat/stat").param("days", "7").param("unmatchedLimit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysAiChatService).selectChatStat(any(SysAiChatRecord.class), eq(7), eq(5));
    }
}
