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

import com.yu.aem.domain.AemExamSeat;
import com.yu.aem.service.IAemExamSeatService;

/**
 * 考场座位编排接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验 /aem/examSeat 路由、@Validated 必填（examId/classroomId）、逗号数组批量删除契约；
 * 自动排座算法在 AemExamPlan 的 autoArrangeSeat 端点覆盖，Excel 导入依赖真实 POI 输入流不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AemExamSeatControllerMockMvcTest {

    @Mock
    private IAemExamSeatService aemExamSeatService;

    @InjectMocks
    private AemExamSeatController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定seatId并返回数据体")
    void getInfo_returnsSeat() throws Exception {
        AemExamSeat s = new AemExamSeat();
        s.setSeatId(3L);
        s.setExamId(5L);
        when(aemExamSeatService.selectAemExamSeatBySeatId(eq(3L))).thenReturn(s);

        mockMvc.perform(get("/aem/examSeat/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.seatId").value(3))
                .andExpect(jsonPath("$.data.examId").value(5));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(aemExamSeatService.insertAemExamSeat(any(AemExamSeat.class))).thenReturn(1);

        String body = "{\"examId\":1,\"classroomId\":2,\"seatNumber\":1}";
        mockMvc.perform(post("/aem/examSeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<AemExamSeat> captor = ArgumentCaptor.forClass(AemExamSeat.class);
        verify(aemExamSeatService).insertAemExamSeat(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(2L), captor.getValue().getClassroomId());
    }

    @Test
    @DisplayName("add：缺失必填examId被@NotNull拦截返回400，不落库")
    void add_missingExamIdRejected() throws Exception {
        String body = "{\"classroomId\":2,\"seatNumber\":1}";
        mockMvc.perform(post("/aem/examSeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(aemExamSeatService, never()).insertAemExamSeat(any());
    }

    @Test
    @DisplayName("edit：更新走服务方法并透传域对象")
    void edit_passthrough() throws Exception {
        when(aemExamSeatService.updateAemExamSeat(any(AemExamSeat.class))).thenReturn(1);

        mockMvc.perform(put("/aem/examSeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatId\":3,\"examId\":1,\"classroomId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(aemExamSeatService).updateAemExamSeat(any(AemExamSeat.class));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(aemExamSeatService.deleteAemExamSeatBySeatIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/aem/examSeat/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(aemExamSeatService).deleteAemExamSeatBySeatIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(aemExamSeatService.selectAemExamSeatList(any(AemExamSeat.class))).thenReturn(List.of());

        mockMvc.perform(get("/aem/examSeat/list").param("examId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<AemExamSeat> captor = ArgumentCaptor.forClass(AemExamSeat.class);
        verify(aemExamSeatService).selectAemExamSeatList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(5L), captor.getValue().getExamId());
    }
}
