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

import com.yu.aem.domain.AemExamInvigilation;
import com.yu.aem.service.IAemExamInvigilationService;

/**
 * 监考教师分配接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验 /aem/invigilation 路由、@Validated 必填（examId/classroomId/teacherId）、逗号数组批量删除契约；
 * Excel 导入（importData 依赖真实 POI 输入流与登录上下文）不属纯接口层契约，故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AemExamInvigilationControllerMockMvcTest {

    @Mock
    private IAemExamInvigilationService aemExamInvigilationService;

    @InjectMocks
    private AemExamInvigilationController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定invigilationId并返回数据体")
    void getInfo_returnsRecord() throws Exception {
        AemExamInvigilation r = new AemExamInvigilation();
        r.setInvigilationId(4L);
        r.setTeacherId(9L);
        when(aemExamInvigilationService.selectAemExamInvigilationByInvigilationId(eq(4L))).thenReturn(r);

        mockMvc.perform(get("/aem/invigilation/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.invigilationId").value(4))
                .andExpect(jsonPath("$.data.teacherId").value(9));
    }

    @Test
    @DisplayName("add：合法三元必填请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(aemExamInvigilationService.insertAemExamInvigilation(any(AemExamInvigilation.class))).thenReturn(1);

        String body = "{\"examId\":1,\"classroomId\":2,\"teacherId\":3}";
        mockMvc.perform(post("/aem/invigilation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<AemExamInvigilation> captor = ArgumentCaptor.forClass(AemExamInvigilation.class);
        verify(aemExamInvigilationService).insertAemExamInvigilation(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(3L), captor.getValue().getTeacherId());
    }

    @Test
    @DisplayName("add：缺失必填teacherId被@NotNull拦截返回400，不落库")
    void add_missingTeacherIdRejected() throws Exception {
        String body = "{\"examId\":1,\"classroomId\":2}";
        mockMvc.perform(post("/aem/invigilation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(aemExamInvigilationService, never()).insertAemExamInvigilation(any());
    }

    @Test
    @DisplayName("edit：更新走服务方法并透传域对象")
    void edit_passthrough() throws Exception {
        when(aemExamInvigilationService.updateAemExamInvigilation(any(AemExamInvigilation.class))).thenReturn(1);

        mockMvc.perform(put("/aem/invigilation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invigilationId\":4,\"examId\":1,\"classroomId\":2,\"teacherId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(aemExamInvigilationService).updateAemExamInvigilation(any(AemExamInvigilation.class));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(aemExamInvigilationService.deleteAemExamInvigilationByInvigilationIds(any(Long[].class))).thenReturn(3);

        mockMvc.perform(delete("/aem/invigilation/1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(aemExamInvigilationService).deleteAemExamInvigilationByInvigilationIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(3, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(aemExamInvigilationService.selectAemExamInvigilationList(any(AemExamInvigilation.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/aem/invigilation/list").param("examId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<AemExamInvigilation> captor = ArgumentCaptor.forClass(AemExamInvigilation.class);
        verify(aemExamInvigilationService).selectAemExamInvigilationList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getExamId());
    }
}
