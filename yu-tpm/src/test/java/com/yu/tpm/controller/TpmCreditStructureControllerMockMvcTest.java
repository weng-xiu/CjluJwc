package com.yu.tpm.controller;

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

import com.yu.tpm.domain.TpmCreditStructure;
import com.yu.tpm.service.ITpmCreditStructureService;

/**
 * 学分结构接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验 /tpm/creditStruct 路由、@Validated 必填（planId/creditType/creditTypeName）、逗号数组批量删除契约。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TpmCreditStructureControllerMockMvcTest {

    @Mock
    private ITpmCreditStructureService tpmCreditStructureService;

    @InjectMocks
    private TpmCreditStructureController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定structId并返回数据体")
    void getInfo_returnsStruct() throws Exception {
        TpmCreditStructure s = new TpmCreditStructure();
        s.setStructId(2L);
        s.setPlanId(7L);
        when(tpmCreditStructureService.selectTpmCreditStructureByStructId(eq(2L))).thenReturn(s);

        mockMvc.perform(get("/tpm/creditStruct/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.structId").value(2))
                .andExpect(jsonPath("$.data.planId").value(7));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(tpmCreditStructureService.insertTpmCreditStructure(any(TpmCreditStructure.class))).thenReturn(1);

        String body = "{\"planId\":7,\"creditType\":\"REQUIRED\",\"creditTypeName\":\"必修\"}";
        mockMvc.perform(post("/tpm/creditStruct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmCreditStructure> captor = ArgumentCaptor.forClass(TpmCreditStructure.class);
        verify(tpmCreditStructureService).insertTpmCreditStructure(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(7L), captor.getValue().getPlanId());
    }

    @Test
    @DisplayName("add：缺失必填planId被@NotNull拦截返回400，不落库")
    void add_missingPlanIdRejected() throws Exception {
        String body = "{\"creditType\":\"REQUIRED\",\"creditTypeName\":\"必修\"}";
        mockMvc.perform(post("/tpm/creditStruct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(tpmCreditStructureService, never()).insertTpmCreditStructure(any());
    }

    @Test
    @DisplayName("add：creditTypeName空白被@NotBlank拦截返回400，不落库")
    void add_blankCreditTypeNameRejected() throws Exception {
        String body = "{\"planId\":7,\"creditType\":\"REQUIRED\",\"creditTypeName\":\"\"}";
        mockMvc.perform(post("/tpm/creditStruct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(tpmCreditStructureService, never()).insertTpmCreditStructure(any());
    }

    @Test
    @DisplayName("edit：更新走服务方法并透传域对象")
    void edit_passthrough() throws Exception {
        when(tpmCreditStructureService.updateTpmCreditStructure(any(TpmCreditStructure.class))).thenReturn(1);

        mockMvc.perform(put("/tpm/creditStruct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"structId\":2,\"planId\":7,\"creditType\":\"ELECTIVE\",\"creditTypeName\":\"选修\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tpmCreditStructureService).updateTpmCreditStructure(any(TpmCreditStructure.class));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(tpmCreditStructureService.deleteTpmCreditStructureByStructIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/tpm/creditStruct/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(tpmCreditStructureService).deleteTpmCreditStructureByStructIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(tpmCreditStructureService.selectTpmCreditStructureList(any(TpmCreditStructure.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/tpm/creditStruct/list").param("planId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<TpmCreditStructure> captor = ArgumentCaptor.forClass(TpmCreditStructure.class);
        verify(tpmCreditStructureService).selectTpmCreditStructureList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(7L), captor.getValue().getPlanId());
    }
}
