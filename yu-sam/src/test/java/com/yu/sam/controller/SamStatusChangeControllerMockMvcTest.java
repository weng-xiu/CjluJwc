package com.yu.sam.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.sam.domain.SamStatusChange;
import com.yu.sam.service.ISamStatusChangeService;

/**
 * 学籍异动接口层测试（Q1 第十四批：MockMvc standalone）。
 * 校验路由 /sam/statusChange、@Validated 必填校验（studentId @NotNull、changeType @NotBlank）、
 * 多级审批链契约：submit/{changeId} 提交审批、approve/{changeId} 与 reject/{changeId} 从请求体解析
 * taskId/comment 并透传服务层（含回写学籍状态联动的入口），逗号数组批量删除、list 查询条件绑定；
 * 审批通过后的下游联动业务逻辑由服务层与 StatusChangeApprovalHandlerTest 覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamStatusChangeControllerMockMvcTest {

    @Mock
    private ISamStatusChangeService samStatusChangeService;

    @InjectMocks
    private SamStatusChangeController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定changeId并返回异动数据体")
    void getInfo_returnsChange() throws Exception {
        SamStatusChange change = new SamStatusChange();
        change.setChangeId(6L);
        change.setStudentId(11L);
        when(samStatusChangeService.selectSamStatusChangeByChangeId(eq(6L))).thenReturn(change);

        mockMvc.perform(get("/sam/statusChange/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.changeId").value(6))
                .andExpect(jsonPath("$.data.studentId").value(11));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(samStatusChangeService.insertSamStatusChange(any(SamStatusChange.class))).thenReturn(1);

        String body = "{\"studentId\":11,\"changeType\":\"1\"}";
        mockMvc.perform(post("/sam/statusChange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamStatusChange> captor = ArgumentCaptor.forClass(SamStatusChange.class);
        verify(samStatusChangeService).insertSamStatusChange(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(11L), captor.getValue().getStudentId());
        org.junit.jupiter.api.Assertions.assertEquals("1", captor.getValue().getChangeType());
    }

    @Test
    @DisplayName("add：缺失必填studentId被@NotNull拦截返回400，不落库")
    void add_missingStudentIdRejected() throws Exception {
        String body = "{\"changeType\":\"1\"}";
        mockMvc.perform(post("/sam/statusChange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(samStatusChangeService, never()).insertSamStatusChange(any());
    }

    @Test
    @DisplayName("add：缺失必填changeType被@NotBlank拦截返回400，不落库")
    void add_missingChangeTypeRejected() throws Exception {
        String body = "{\"studentId\":11}";
        mockMvc.perform(post("/sam/statusChange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(samStatusChangeService, never()).insertSamStatusChange(any());
    }

    @Test
    @DisplayName("submit：提交审批路径变量绑定changeId，成功影响行数→200")
    void submit_bindsIdSuccess() throws Exception {
        when(samStatusChangeService.submitForApproval(eq(6L))).thenReturn(1);

        mockMvc.perform(post("/sam/statusChange/submit/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samStatusChangeService).submitForApproval(eq(6L));
    }

    @Test
    @DisplayName("approve：从请求体解析taskId/comment透传服务层，0行toAjax降级500")
    void approve_parsesBodyAndPassesThrough() throws Exception {
        when(samStatusChangeService.approveChange(eq(6L), eq("T-1"), eq("同意"))).thenReturn(0);

        String body = "{\"taskId\":\"T-1\",\"comment\":\"同意\"}";
        mockMvc.perform(post("/sam/statusChange/approve/6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(samStatusChangeService).approveChange(eq(6L), eq("T-1"), eq("同意"));
    }

    @Test
    @DisplayName("reject：从请求体解析taskId/comment透传服务层")
    void reject_parsesBodyAndPassesThrough() throws Exception {
        when(samStatusChangeService.rejectChange(eq(6L), eq("T-2"), eq("材料不全"))).thenReturn(1);

        String body = "{\"taskId\":\"T-2\",\"comment\":\"材料不全\"}";
        mockMvc.perform(post("/sam/statusChange/reject/6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samStatusChangeService).rejectChange(eq(6L), eq("T-2"), eq("材料不全"));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(samStatusChangeService.deleteSamStatusChangeByChangeIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/sam/statusChange/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(samStatusChangeService).deleteSamStatusChangeByChangeIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(samStatusChangeService.selectSamStatusChangeList(any(SamStatusChange.class))).thenReturn(List.of());

        mockMvc.perform(get("/sam/statusChange/list").param("changeType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SamStatusChange> captor = ArgumentCaptor.forClass(SamStatusChange.class);
        verify(samStatusChangeService).selectSamStatusChangeList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("1", captor.getValue().getChangeType());
    }
}
