package com.yu.aem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

import com.yu.aem.domain.AemSupervisionRecord;
import com.yu.aem.service.IAemSupervisionRecordService;

/**
 * 督导听课记录接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验 /aem/supervision 路由、@Validated 必填（courseId/teacherId/supervisor）、逗号数组批量删除契约。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AemSupervisionRecordControllerMockMvcTest {

    @Mock
    private IAemSupervisionRecordService aemSupervisionRecordService;

    @InjectMocks
    private AemSupervisionRecordController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定recordId并返回数据体")
    void getInfo_returnsRecord() throws Exception {
        AemSupervisionRecord r = new AemSupervisionRecord();
        r.setRecordId(3L);
        r.setCourseId(7L);
        when(aemSupervisionRecordService.selectAemSupervisionRecordByRecordId(eq(3L))).thenReturn(r);

        mockMvc.perform(get("/aem/supervision/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.recordId").value(3))
                .andExpect(jsonPath("$.data.courseId").value(7));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(aemSupervisionRecordService.insertAemSupervisionRecord(any(AemSupervisionRecord.class))).thenReturn(1);

        String body = "{\"courseId\":7,\"teacherId\":3,\"supervisor\":\"张督导\"}";
        mockMvc.perform(post("/aem/supervision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<AemSupervisionRecord> captor = ArgumentCaptor.forClass(AemSupervisionRecord.class);
        verify(aemSupervisionRecordService).insertAemSupervisionRecord(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(7L), captor.getValue().getCourseId());
    }

    @Test
    @DisplayName("add：supervisor空白被@NotBlank拦截返回400，不落库")
    void add_blankSupervisorRejected() throws Exception {
        String body = "{\"courseId\":7,\"teacherId\":3,\"supervisor\":\"\"}";
        mockMvc.perform(post("/aem/supervision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(aemSupervisionRecordService, never()).insertAemSupervisionRecord(any());
    }

    @Test
    @DisplayName("add：缺失必填teacherId被@NotNull拦截返回400，不落库")
    void add_missingTeacherIdRejected() throws Exception {
        String body = "{\"courseId\":7,\"supervisor\":\"张督导\"}";
        mockMvc.perform(post("/aem/supervision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(aemSupervisionRecordService, never()).insertAemSupervisionRecord(any());
    }

    @Test
    @DisplayName("edit：更新走服务方法并透传域对象")
    void edit_passthrough() throws Exception {
        when(aemSupervisionRecordService.updateAemSupervisionRecord(any(AemSupervisionRecord.class))).thenReturn(1);

        mockMvc.perform(put("/aem/supervision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordId\":3,\"courseId\":7,\"teacherId\":3,\"supervisor\":\"改后\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(aemSupervisionRecordService).updateAemSupervisionRecord(any(AemSupervisionRecord.class));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(aemSupervisionRecordService.deleteAemSupervisionRecordByRecordIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/aem/supervision/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(aemSupervisionRecordService).deleteAemSupervisionRecordByRecordIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(aemSupervisionRecordService.selectAemSupervisionRecordList(any(AemSupervisionRecord.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/aem/supervision/list").param("courseId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<AemSupervisionRecord> captor = ArgumentCaptor.forClass(AemSupervisionRecord.class);
        verify(aemSupervisionRecordService).selectAemSupervisionRecordList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(7L), captor.getValue().getCourseId());
    }
}
