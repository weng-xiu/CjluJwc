package com.yu.oa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.yu.oa.domain.OaDocument;
import com.yu.oa.service.IOaDocumentService;

/**
 * OA 公文接口层测试（Q1 第五批：MockMvc standalone）。
 * 校验公文 CRUD 与审批链路的 HTTP 路由、@RequestParam/@PathVariable 绑定、
 * toAjax 成败双分支（影响行数 0 → code 500）、待办/已办分页返回契约；
 * @PreAuthorize 权限字符与 Flowable 流程推进由服务层/集成测试覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OaDocumentControllerMockMvcTest {

    @Mock
    private IOaDocumentService oaDocumentService;

    @InjectMocks
    private OaDocumentController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("list：分页返回 rows/total/code")
    void list_returnsTable() throws Exception {
        when(oaDocumentService.selectOaDocumentList(any(OaDocument.class))).thenReturn(List.of());

        mockMvc.perform(get("/oa/document/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @DisplayName("getInfo：按 documentId 查询详情落 data")
    void getInfo_returnsData() throws Exception {
        OaDocument doc = new OaDocument();
        doc.setDocumentId(3L);
        when(oaDocumentService.selectOaDocumentByDocumentId(3L)).thenReturn(doc);

        mockMvc.perform(get("/oa/document/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.documentId").value(3));
    }

    @Test
    @DisplayName("add：请求体绑定后新增，影响行数 1 返回 200")
    void add_success() throws Exception {
        when(oaDocumentService.insertOaDocument(any(OaDocument.class))).thenReturn(1);

        mockMvc.perform(post("/oa/document")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"通知\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(oaDocumentService).insertOaDocument(any(OaDocument.class));
    }

    @Test
    @DisplayName("edit：更新影响行数为 0 时 toAjax 返回 500")
    void edit_zeroRowsReturnsError() throws Exception {
        when(oaDocumentService.updateOaDocument(any(OaDocument.class))).thenReturn(0);

        mockMvc.perform(put("/oa/document")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"documentId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号分隔路径绑定为 Long[] 批量删除")
    void remove_bindsIdArray() throws Exception {
        when(oaDocumentService.deleteOaDocumentByDocumentIds(any())).thenReturn(2);

        mockMvc.perform(delete("/oa/document/1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(oaDocumentService).deleteOaDocumentByDocumentIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(3, captor.getValue().length);
    }

    @Test
    @DisplayName("submit：提交审批透传 documentId")
    void submit_success() throws Exception {
        when(oaDocumentService.submitDocument(5L)).thenReturn(1);

        mockMvc.perform(post("/oa/document/submit/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(oaDocumentService).submitDocument(5L);
    }

    @Test
    @DisplayName("approve：审批通过透传 documentId/taskId/comment")
    void approve_passesAllParams() throws Exception {
        when(oaDocumentService.approveDocument(eq(5L), eq("task-1"), eq("同意"))).thenReturn(1);

        mockMvc.perform(post("/oa/document/approve")
                        .param("documentId", "5").param("taskId", "task-1").param("comment", "同意"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(oaDocumentService).approveDocument(5L, "task-1", "同意");
    }

    @Test
    @DisplayName("approve：comment 可选，缺省传 null")
    void approve_commentOptional() throws Exception {
        when(oaDocumentService.approveDocument(eq(6L), eq("task-2"), isNullComment())).thenReturn(1);

        mockMvc.perform(post("/oa/document/approve")
                        .param("documentId", "6").param("taskId", "task-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private static String isNullComment() {
        return org.mockito.ArgumentMatchers.isNull();
    }

    @Test
    @DisplayName("reject：审批驳回透传参数")
    void reject_success() throws Exception {
        when(oaDocumentService.rejectDocument(eq(7L), eq("task-3"), eq("材料不全"))).thenReturn(1);

        mockMvc.perform(post("/oa/document/reject")
                        .param("documentId", "7").param("taskId", "task-3").param("comment", "材料不全"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(oaDocumentService).rejectDocument(7L, "task-3", "材料不全");
    }

    @Test
    @DisplayName("cancel：撤回公文透传 documentId")
    void cancel_success() throws Exception {
        when(oaDocumentService.cancelDocument(8L)).thenReturn(1);

        mockMvc.perform(post("/oa/document/cancel/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("todo：待办公文分页返回")
    void todo_returnsTable() throws Exception {
        when(oaDocumentService.selectTodoList(any(OaDocument.class))).thenReturn(List.of());

        mockMvc.perform(get("/oa/document/todo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());
    }

    @Test
    @DisplayName("done：已办公文分页返回")
    void done_returnsTable() throws Exception {
        when(oaDocumentService.selectDoneList(any(OaDocument.class))).thenReturn(List.of());

        mockMvc.perform(get("/oa/document/done"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());
    }
}
