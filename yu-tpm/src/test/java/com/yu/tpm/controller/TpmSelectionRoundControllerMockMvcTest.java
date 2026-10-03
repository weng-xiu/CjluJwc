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

import com.yu.tpm.domain.TpmSelectionRound;
import com.yu.tpm.service.ITpmSelectionRoundService;

/**
 * 选课轮次接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验 /tpm/round 路由、@Validated 必填（semesterId/roundName/startTime/endTime）、
 * 逗号数组批量删除、start/finish 状态流转端点走 toAjax(int)。轮次编排业务规则由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TpmSelectionRoundControllerMockMvcTest {

    @Mock
    private ITpmSelectionRoundService tpmSelectionRoundService;

    @InjectMocks
    private TpmSelectionRoundController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定roundId并返回轮次数据体")
    void getInfo_returnsRound() throws Exception {
        TpmSelectionRound r = new TpmSelectionRound();
        r.setRoundId(4L);
        r.setRoundName("第一轮选课");
        when(tpmSelectionRoundService.selectTpmSelectionRoundByRoundId(eq(4L))).thenReturn(r);

        mockMvc.perform(get("/tpm/round/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.roundId").value(4))
                .andExpect(jsonPath("$.data.roundName").value("第一轮选课"));
    }

    @Test
    @DisplayName("add：合法请求体（含时间戳Date）通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(tpmSelectionRoundService.insertTpmSelectionRound(any(TpmSelectionRound.class))).thenReturn(1);

        String body = "{\"semesterId\":1,\"roundName\":\"第一轮\",\"startTime\":1735660800000,\"endTime\":1735747200000}";
        mockMvc.perform(post("/tpm/round")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmSelectionRound> captor = ArgumentCaptor.forClass(TpmSelectionRound.class);
        verify(tpmSelectionRoundService).insertTpmSelectionRound(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getSemesterId());
        org.junit.jupiter.api.Assertions.assertNotNull(captor.getValue().getStartTime());
    }

    @Test
    @DisplayName("add：roundName空白被@NotBlank拦截返回400，不落库")
    void add_blankRoundNameRejected() throws Exception {
        String body = "{\"semesterId\":1,\"roundName\":\"\",\"startTime\":1735660800000,\"endTime\":1735747200000}";
        mockMvc.perform(post("/tpm/round")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(tpmSelectionRoundService, never()).insertTpmSelectionRound(any());
    }

    @Test
    @DisplayName("add：缺失必填startTime被@NotNull拦截返回400，不落库")
    void add_missingStartTimeRejected() throws Exception {
        String body = "{\"semesterId\":1,\"roundName\":\"第一轮\",\"endTime\":1735747200000}";
        mockMvc.perform(post("/tpm/round")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(tpmSelectionRoundService, never()).insertTpmSelectionRound(any());
    }

    @Test
    @DisplayName("start：开启轮次走startRound并绑定roundId")
    void start_bindsRoundId() throws Exception {
        when(tpmSelectionRoundService.startRound(eq(2L))).thenReturn(1);

        mockMvc.perform(put("/tpm/round/start/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tpmSelectionRoundService).startRound(eq(2L));
    }

    @Test
    @DisplayName("finish：结束轮次走finishRound并绑定roundId")
    void finish_bindsRoundId() throws Exception {
        when(tpmSelectionRoundService.finishRound(eq(3L))).thenReturn(1);

        mockMvc.perform(put("/tpm/round/finish/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tpmSelectionRoundService).finishRound(eq(3L));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(tpmSelectionRoundService.deleteTpmSelectionRoundByRoundIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/tpm/round/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(tpmSelectionRoundService).deleteTpmSelectionRoundByRoundIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(tpmSelectionRoundService.selectTpmSelectionRoundList(any(TpmSelectionRound.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/tpm/round/list").param("semesterId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<TpmSelectionRound> captor = ArgumentCaptor.forClass(TpmSelectionRound.class);
        verify(tpmSelectionRoundService).selectTpmSelectionRoundList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getSemesterId());
    }
}
