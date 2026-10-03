package com.yu.aem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

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

import com.yu.aem.domain.AemExamPlan;
import com.yu.aem.service.IAemExamInvigilationService;
import com.yu.aem.service.IAemExamPlanService;
import com.yu.aem.service.IAemExamSeatService;

/**
 * 考试安排接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验 /aem/examPlan 主子表联动与三类自动编排端点：@Validated 必填（examName/semesterId/examType）、
 * detail 独立路由、座位/监考/整卷编排委托到各自服务、detectConflicts 必填 semesterId。
 * 编排算法与冲突规避逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AemExamPlanControllerMockMvcTest {

    @Mock
    private IAemExamPlanService aemExamPlanService;

    @Mock
    private IAemExamSeatService aemExamSeatService;

    @Mock
    private IAemExamInvigilationService aemExamInvigilationService;

    @InjectMocks
    private AemExamPlanController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定examId并返回考试计划数据体")
    void getInfo_returnsPlan() throws Exception {
        AemExamPlan p = new AemExamPlan();
        p.setExamId(5L);
        p.setExamName("期末统一考试");
        when(aemExamPlanService.selectAemExamPlanByExamId(eq(5L))).thenReturn(p);

        mockMvc.perform(get("/aem/examPlan/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.examId").value(5))
                .andExpect(jsonPath("$.data.examName").value("期末统一考试"));
    }

    @Test
    @DisplayName("getDetail：走detail独立服务方法（含座位/监考子表），与getInfo区分路由")
    void getDetail_usesDetailServiceMethod() throws Exception {
        AemExamPlan p = new AemExamPlan();
        p.setExamId(6L);
        when(aemExamPlanService.selectAemExamPlanDetail(eq(6L))).thenReturn(p);

        mockMvc.perform(get("/aem/examPlan/detail/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.examId").value(6));

        verify(aemExamPlanService).selectAemExamPlanDetail(eq(6L));
        verify(aemExamPlanService, never()).selectAemExamPlanByExamId(any());
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(aemExamPlanService.insertAemExamPlan(any(AemExamPlan.class))).thenReturn(1);

        String body = "{\"examName\":\"期中考试\",\"semesterId\":1,\"examType\":\"1\"}";
        mockMvc.perform(post("/aem/examPlan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<AemExamPlan> captor = ArgumentCaptor.forClass(AemExamPlan.class);
        verify(aemExamPlanService).insertAemExamPlan(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getSemesterId());
    }

    @Test
    @DisplayName("add：缺失必填semesterId被@NotNull拦截返回400，不落库")
    void add_missingSemesterIdRejected() throws Exception {
        String body = "{\"examName\":\"期中考试\",\"examType\":\"1\"}";
        mockMvc.perform(post("/aem/examPlan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(aemExamPlanService, never()).insertAemExamPlan(any());
    }

    @Test
    @DisplayName("add：examName空白被@NotBlank拦截返回400，不落库")
    void add_blankExamNameRejected() throws Exception {
        String body = "{\"examName\":\"\",\"semesterId\":1,\"examType\":\"1\"}";
        mockMvc.perform(post("/aem/examPlan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(aemExamPlanService, never()).insertAemExamPlan(any());
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(aemExamPlanService.deleteAemExamPlanByExamIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/aem/examPlan/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(aemExamPlanService).deleteAemExamPlanByExamIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("autoArrangeSeat：仅examId时classroomId以null透传座位服务")
    void autoArrangeSeat_nullClassroomPassthrough() throws Exception {
        when(aemExamSeatService.autoArrangeSeats(eq(7L), isNull())).thenReturn(Map.of("arranged", 30));

        mockMvc.perform(post("/aem/examPlan/autoArrangeSeat/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.arranged").value(30));

        verify(aemExamSeatService).autoArrangeSeats(eq(7L), isNull());
    }

    @Test
    @DisplayName("autoArrangeSeat：显式classroomId透传座位服务")
    void autoArrangeSeat_withClassroom() throws Exception {
        when(aemExamSeatService.autoArrangeSeats(eq(7L), eq(9L))).thenReturn(Map.of("arranged", 25));

        mockMvc.perform(post("/aem/examPlan/autoArrangeSeat/7").param("classroomId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.arranged").value(25));

        verify(aemExamSeatService).autoArrangeSeats(eq(7L), eq(9L));
    }

    @Test
    @DisplayName("autoDispatch：派监考委托监考服务并返回结果Map")
    void autoDispatch_delegatesInvigilation() throws Exception {
        when(aemExamInvigilationService.autoDispatchInvigilators(eq(8L))).thenReturn(Map.of("dispatched", 4));

        mockMvc.perform(post("/aem/examPlan/autoDispatch/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dispatched").value(4));

        verify(aemExamInvigilationService).autoDispatchInvigilators(eq(8L));
    }

    @Test
    @DisplayName("autoArrange：整卷自动编排委托计划服务")
    void autoArrange_delegatesPlan() throws Exception {
        when(aemExamPlanService.autoArrangeExam(eq(10L))).thenReturn(Map.of("rooms", 2));

        mockMvc.perform(post("/aem/examPlan/autoArrange/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rooms").value(2));

        verify(aemExamPlanService).autoArrangeExam(eq(10L));
    }

    @Test
    @DisplayName("detectConflicts：必填semesterId，List结果落data数组")
    void detectConflicts_returnsList() throws Exception {
        when(aemExamPlanService.detectExamConflicts(eq(3L))).thenReturn(List.of(Map.of("examId", 1)));

        mockMvc.perform(get("/aem/examPlan/detectConflicts").param("semesterId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].examId").value(1));
    }

    @Test
    @DisplayName("detectConflicts：缺失semesterId返回400")
    void detectConflicts_missingParamRejected() throws Exception {
        mockMvc.perform(get("/aem/examPlan/detectConflicts"))
                .andExpect(status().isBadRequest());

        verify(aemExamPlanService, never()).detectExamConflicts(any());
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(aemExamPlanService.selectAemExamPlanList(any(AemExamPlan.class))).thenReturn(List.of());

        mockMvc.perform(get("/aem/examPlan/list").param("semesterId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<AemExamPlan> captor = ArgumentCaptor.forClass(AemExamPlan.class);
        verify(aemExamPlanService).selectAemExamPlanList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getSemesterId());
    }
}
