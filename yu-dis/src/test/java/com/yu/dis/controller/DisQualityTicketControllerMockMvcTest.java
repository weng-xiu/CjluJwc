package com.yu.dis.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
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
import com.yu.dis.domain.DisQualityTicket;
import com.yu.dis.service.IDisQualityTicketService;

/**
 * 质量异常工单接口层测试（F5，MockMvc standalone）。
 * 校验路由 /dis/qualityTicket、@Validated 必填（anomalyType @NotBlank）、
 * add/edit 回填登录操作人、逗号数组批量删除、list 查询绑定与表格结构、
 * detect 异常检测阈值参数绑定与生成条数返回。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 *
 * @author F5
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DisQualityTicketControllerMockMvcTest
{
    @Mock
    private IDisQualityTicketService disQualityTicketService;

    @InjectMocks
    private DisQualityTicketController controller;

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
    @DisplayName("list：status/severity 查询绑定并返回表格结构")
    void list_bindsQuery() throws Exception
    {
        DisQualityTicket row = new DisQualityTicket();
        row.setTicketId(1L);
        row.setTaskName("学籍同步任务");
        row.setAnomalyType("0");
        row.setSeverity("2");
        row.setStatus("0");
        ArrayList<DisQualityTicket> rows = new ArrayList<>();
        rows.add(row);
        when(disQualityTicketService.selectDisQualityTicketList(any(DisQualityTicket.class)))
                .thenReturn(rows);

        mockMvc.perform(get("/dis/qualityTicket/list")
                        .param("status", "0").param("severity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0].taskName").value("学籍同步任务"));

        ArgumentCaptor<DisQualityTicket> captor = ArgumentCaptor.forClass(DisQualityTicket.class);
        verify(disQualityTicketService).selectDisQualityTicketList(captor.capture());
        Assertions.assertEquals("0", captor.getValue().getStatus());
        Assertions.assertEquals("2", captor.getValue().getSeverity());
    }

    @Test
    @DisplayName("getInfo：路径变量绑定ticketId并返回工单")
    void getInfo_returnsTicket() throws Exception
    {
        DisQualityTicket t = new DisQualityTicket();
        t.setTicketId(9L);
        t.setTaskName("成绩同步任务");
        when(disQualityTicketService.selectDisQualityTicketByTicketId(eq(9L))).thenReturn(t);

        mockMvc.perform(get("/dis/qualityTicket/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskName").value("成绩同步任务"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并回填createBy")
    void add_validBodyInjectsCreateBy() throws Exception
    {
        when(disQualityTicketService.insertDisQualityTicket(any(DisQualityTicket.class))).thenReturn(1);

        String body = "{\"taskName\":\"一卡通同步任务\",\"anomalyType\":\"3\",\"severity\":\"1\",\"assignee\":\"ops\"}";
        mockMvc.perform(post("/dis/qualityTicket")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<DisQualityTicket> captor = ArgumentCaptor.forClass(DisQualityTicket.class);
        verify(disQualityTicketService).insertDisQualityTicket(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getCreateBy());
        Assertions.assertEquals("3", captor.getValue().getAnomalyType());
    }

    @Test
    @DisplayName("add：缺失必填anomalyType被@NotBlank拦截返回400，不落库")
    void add_missingAnomalyTypeRejected() throws Exception
    {
        mockMvc.perform(post("/dis/qualityTicket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskName\":\"一卡通同步任务\",\"severity\":\"1\"}"))
                .andExpect(status().isBadRequest());

        verify(disQualityTicketService, never()).insertDisQualityTicket(any());
    }

    @Test
    @DisplayName("edit：推进处置状态并注入updateBy")
    void edit_updatesStatusAndInjectsUpdateBy() throws Exception
    {
        when(disQualityTicketService.updateDisQualityTicket(any(DisQualityTicket.class))).thenReturn(1);

        String body = "{\"ticketId\":9,\"anomalyType\":\"0\",\"status\":\"2\",\"resolution\":\"已修复源系统连接\"}";
        mockMvc.perform(put("/dis/qualityTicket")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<DisQualityTicket> captor = ArgumentCaptor.forClass(DisQualityTicket.class);
        verify(disQualityTicketService).updateDisQualityTicket(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
        Assertions.assertEquals("2", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("remove：逗号数组批量删除")
    void remove_batchIds() throws Exception
    {
        when(disQualityTicketService.deleteDisQualityTicketByTicketIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/dis/qualityTicket/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(disQualityTicketService).deleteDisQualityTicketByTicketIds(eq(new Long[]{1L, 2L}));
    }

    @Test
    @DisplayName("detect：阈值参数透传并返回生成条数")
    void detect_bindsThreshold() throws Exception
    {
        when(disQualityTicketService.detectAndGenerate(anyDouble(), any(String.class))).thenReturn(3);

        mockMvc.perform(post("/dis/qualityTicket/detect").param("threshold", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(3));

        verify(disQualityTicketService).detectAndGenerate(eq(30.0), eq("tester"));
    }

    @Test
    @DisplayName("detect：缺省阈值20")
    void detect_defaultThreshold() throws Exception
    {
        when(disQualityTicketService.detectAndGenerate(anyDouble(), any(String.class))).thenReturn(0);

        mockMvc.perform(post("/dis/qualityTicket/detect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));

        verify(disQualityTicketService).detectAndGenerate(eq(20.0), eq("tester"));
    }
}
