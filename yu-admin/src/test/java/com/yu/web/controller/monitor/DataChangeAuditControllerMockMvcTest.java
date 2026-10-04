package com.yu.web.controller.monitor;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

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

import com.yu.system.domain.DataChangeAudit;
import com.yu.system.service.IDataChangeAuditService;

/**
 * 字段级数据变更流水查询接口层测试（V4.0 K1 合规③，MockMvc standalone）。
 *
 * <p>校验 monitor 域只读审计接口的路由与参数绑定契约：/list 条件查询表格结构、
 * /biz/{entityType}/{bizId} 双路径变量透传；流水写入由服务层单测覆盖。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataChangeAuditControllerMockMvcTest {

    @Mock
    private IDataChangeAuditService dataChangeAuditService;

    @InjectMocks
    private DataChangeAuditController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private DataChangeAudit row() {
        DataChangeAudit a = new DataChangeAudit();
        a.setAuditId(1L);
        a.setEntityType("aem_grade_record");
        a.setBizId("1");
        a.setFieldName("totalScore");
        a.setOldValue("85.0");
        a.setNewValue("90.0");
        a.setOperName("admin");
        return a;
    }

    @Test
    @DisplayName("/list 返回分页表格结构，条件透传服务层")
    void listReturnsTable() throws Exception {
        List<DataChangeAudit> list = Collections.singletonList(row());
        when(dataChangeAuditService.selectDataChangeAuditList(org.mockito.ArgumentMatchers.any(DataChangeAudit.class)))
                .thenReturn(list);

        mockMvc.perform(get("/monitor/dataAudit/list").param("entityType", "aem_grade_record"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0].fieldName").value("totalScore"))
                .andExpect(jsonPath("$.rows[0].oldValue").value("85.0"));
    }

    @Test
    @DisplayName("/biz/{entityType}/{bizId} 双路径变量透传并回历史")
    void bizHistoryPathVariables() throws Exception {
        when(dataChangeAuditService.selectByBiz(eq("aem_grade_record"), eq("1")))
                .thenReturn(Collections.singletonList(row()));

        mockMvc.perform(get("/monitor/dataAudit/biz/aem_grade_record/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].entityType").value("aem_grade_record"))
                .andExpect(jsonPath("$.rows[0].newValue").value("90.0"));

        verify(dataChangeAuditService).selectByBiz("aem_grade_record", "1");
    }
}
