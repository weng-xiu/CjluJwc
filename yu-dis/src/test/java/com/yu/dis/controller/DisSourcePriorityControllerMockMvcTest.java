package com.yu.dis.controller;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.dis.arbitration.ArbitrationPreviewRequest;
import com.yu.dis.domain.DisSourcePriority;
import com.yu.dis.service.IDisSourcePriorityService;

/**
 * 主数据裁决矩阵接口层测试（F5，MockMvc standalone）。
 * 校验路由 /dis/priority、@Validated 必填（sourceCode @NotBlank、priority @NotNull）、
 * add/edit 回填登录操作人、逗号数组批量删除、list 查询绑定与表格结构、试裁决 preview 返回权威版本。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 *
 * @author F5
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DisSourcePriorityControllerMockMvcTest
{
    @Mock
    private IDisSourcePriorityService disSourcePriorityService;

    @InjectMocks
    private DisSourcePriorityController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
        sysUser.setUserId(1L);
        sysUser.setUserName("tester");
        LoginUser loginUser = new LoginUser(1L, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("list：sourceCode/status 查询绑定并返回表格结构")
    void list_bindsQuery() throws Exception
    {
        DisSourcePriority row = new DisSourcePriority();
        row.setPriorityId(1L);
        row.setSourceCode("GRAD");
        row.setPriority(100);
        ArrayList<DisSourcePriority> rows = new ArrayList<>();
        rows.add(row);
        when(disSourcePriorityService.selectDisSourcePriorityList(any(DisSourcePriority.class)))
                .thenReturn(rows);

        mockMvc.perform(get("/dis/priority/list").param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0].sourceCode").value("GRAD"));

        ArgumentCaptor<DisSourcePriority> captor = ArgumentCaptor.forClass(DisSourcePriority.class);
        verify(disSourcePriorityService).selectDisSourcePriorityList(captor.capture());
        Assertions.assertEquals("0", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("getInfo：路径变量绑定priorityId并返回配置")
    void getInfo_returnsPriority() throws Exception
    {
        DisSourcePriority p = new DisSourcePriority();
        p.setPriorityId(9L);
        p.setSourceName("研究生学籍主系统");
        when(disSourcePriorityService.selectDisSourcePriorityByPriorityId(eq(9L))).thenReturn(p);

        mockMvc.perform(get("/dis/priority/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceName").value("研究生学籍主系统"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并回填createBy")
    void add_validBodyInjectsCreateBy() throws Exception
    {
        when(disSourcePriorityService.insertDisSourcePriority(any(DisSourcePriority.class))).thenReturn(1);

        String body = "{\"sourceCode\":\"LIB\",\"sourceName\":\"图书馆系统\",\"priority\":50,\"status\":\"0\"}";
        mockMvc.perform(post("/dis/priority")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<DisSourcePriority> captor = ArgumentCaptor.forClass(DisSourcePriority.class);
        verify(disSourcePriorityService).insertDisSourcePriority(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getCreateBy());
        Assertions.assertEquals("LIB", captor.getValue().getSourceCode());
    }

    @Test
    @DisplayName("add：缺失必填priority被@NotNull拦截返回400，不落库")
    void add_missingPriorityRejected() throws Exception
    {
        mockMvc.perform(post("/dis/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceCode\":\"LIB\",\"sourceName\":\"图书馆系统\"}"))
                .andExpect(status().isBadRequest());

        verify(disSourcePriorityService, never()).insertDisSourcePriority(any());
    }

    @Test
    @DisplayName("add：缺失必填sourceCode被@NotBlank拦截返回400，不落库")
    void add_missingSourceCodeRejected() throws Exception
    {
        mockMvc.perform(post("/dis/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceName\":\"图书馆系统\",\"priority\":50}"))
                .andExpect(status().isBadRequest());

        verify(disSourcePriorityService, never()).insertDisSourcePriority(any());
    }

    @Test
    @DisplayName("edit：调整优先级并注入updateBy")
    void edit_updatesPriorityAndInjectsUpdateBy() throws Exception
    {
        when(disSourcePriorityService.updateDisSourcePriority(any(DisSourcePriority.class))).thenReturn(1);

        String body = "{\"priorityId\":9,\"sourceCode\":\"GRAD\",\"priority\":120,\"status\":\"0\"}";
        mockMvc.perform(put("/dis/priority")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<DisSourcePriority> captor = ArgumentCaptor.forClass(DisSourcePriority.class);
        verify(disSourcePriorityService).updateDisSourcePriority(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
        Assertions.assertEquals(120, captor.getValue().getPriority());
    }

    @Test
    @DisplayName("remove：逗号数组批量删除")
    void remove_batchIds() throws Exception
    {
        when(disSourcePriorityService.deleteDisSourcePriorityByPriorityIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/dis/priority/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(disSourcePriorityService).deleteDisSourcePriorityByPriorityIds(eq(new Long[]{1L, 2L}));
    }

    @Test
    @DisplayName("preview：试裁决返回权威胜出源")
    void preview_returnsWinner() throws Exception
    {
        Map<String, Object> winner = new LinkedHashMap<>();
        winner.put("sourceCode", "GRAD");
        winner.put("priority", 100);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("businessKey", "2024001");
        data.put("winner", winner);
        when(disSourcePriorityService.preview(any(ArbitrationPreviewRequest.class))).thenReturn(data);

        String body = "{\"businessKey\":\"2024001\",\"candidates\":[{\"sourceCode\":\"GRAD\",\"updateTime\":\"2026-01-01 10:00:00\"},{\"sourceCode\":\"CARD\",\"updateTime\":\"2026-02-01 10:00:00\"}]}";
        mockMvc.perform(post("/dis/priority/preview")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.winner.sourceCode").value("GRAD"));

        ArgumentCaptor<ArbitrationPreviewRequest> captor = ArgumentCaptor.forClass(ArbitrationPreviewRequest.class);
        verify(disSourcePriorityService).preview(captor.capture());
        Assertions.assertEquals(2, captor.getValue().getCandidates().size());
    }
}
