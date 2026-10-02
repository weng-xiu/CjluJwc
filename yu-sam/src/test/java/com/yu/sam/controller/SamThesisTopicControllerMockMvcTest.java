package com.yu.sam.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
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
import com.yu.sam.domain.SamThesisTopic;
import com.yu.sam.service.ISamThesisTopicService;

/**
 * 毕业论文选题库接口层测试（Q1 第十五批：MockMvc standalone，覆盖 yu-sam S9 选题域控制器）。
 * 校验 CRUD 路由与 @Validated 必填校验（planYear/topicName @NotBlank）、
 * 题目审核 audit 的 pass 必填与可选 opinion 透传、上架/下架状态变更绑定、
 * add/edit 回填登录操作人、逗号数组批量删除；
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamThesisTopicControllerMockMvcTest {

    @Mock
    private ISamThesisTopicService samThesisTopicService;

    @InjectMocks
    private SamThesisTopicController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
        sysUser.setUserId(1L);
        sysUser.setUserName("tester");
        LoginUser loginUser = new LoginUser(1L, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定topicId并返回题目记录")
    void getInfo_returnsTopic() throws Exception {
        SamThesisTopic topic = new SamThesisTopic();
        topic.setTopicId(3L);
        topic.setTopicName("排课算法优化");
        when(samThesisTopicService.selectSamThesisTopicByTopicId(eq(3L))).thenReturn(topic);

        mockMvc.perform(get("/sam/thesisTopic/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.topicName").value("排课算法优化"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并回填createBy")
    void add_validBodyInjectsCreateBy() throws Exception {
        when(samThesisTopicService.insertSamThesisTopic(any(SamThesisTopic.class))).thenReturn(1);

        String body = "{\"planYear\":\"2026\",\"topicName\":\"教务数据治理\",\"capacity\":5}";
        mockMvc.perform(post("/sam/thesisTopic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamThesisTopic> captor = ArgumentCaptor.forClass(SamThesisTopic.class);
        verify(samThesisTopicService).insertSamThesisTopic(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("2026", captor.getValue().getPlanYear());
        org.junit.jupiter.api.Assertions.assertEquals("tester", captor.getValue().getCreateBy());
    }

    @Test
    @DisplayName("add：缺失必填topicName被@NotBlank拦截返回400，不落库")
    void add_missingTopicNameRejected() throws Exception {
        String body = "{\"planYear\":\"2026\",\"capacity\":5}";
        mockMvc.perform(post("/sam/thesisTopic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(samThesisTopicService, never()).insertSamThesisTopic(any());
    }

    @Test
    @DisplayName("add：缺失必填planYear被@NotBlank拦截返回400，不落库")
    void add_missingPlanYearRejected() throws Exception {
        String body = "{\"topicName\":\"教务数据治理\"}";
        mockMvc.perform(post("/sam/thesisTopic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(samThesisTopicService, never()).insertSamThesisTopic(any());
    }

    @Test
    @DisplayName("edit：合法请求体回填updateBy，影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(samThesisTopicService.updateSamThesisTopic(any(SamThesisTopic.class))).thenReturn(0);

        String body = "{\"topicId\":3,\"planYear\":\"2026\",\"topicName\":\"排课算法优化\"}";
        mockMvc.perform(put("/sam/thesisTopic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        ArgumentCaptor<SamThesisTopic> captor = ArgumentCaptor.forClass(SamThesisTopic.class);
        verify(samThesisTopicService).updateSamThesisTopic(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
    }

    @Test
    @DisplayName("audit：pass必填缺失返回400，携带时透传审核签名（含登录操作人）")
    void audit_passRequired() throws Exception {
        mockMvc.perform(put("/sam/thesisTopic/audit/3"))
                .andExpect(status().isBadRequest());

        when(samThesisTopicService.auditTopic(eq(3L), eq(true), eq("选题合理"), anyString())).thenReturn(1);
        mockMvc.perform(put("/sam/thesisTopic/audit/3")
                        .param("pass", "true").param("opinion", "选题合理"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("status：上架/下架变更绑定topicId与status并透传")
    void changeStatus_bindsParams() throws Exception {
        when(samThesisTopicService.changeStatus(eq(3L), eq("0"), anyString())).thenReturn(1);

        mockMvc.perform(put("/sam/thesisTopic/status/3").param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samThesisTopicService).changeStatus(eq(3L), eq("0"), eq("tester"));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(samThesisTopicService.deleteSamThesisTopicByTopicIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/sam/thesisTopic/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(samThesisTopicService).deleteSamThesisTopicByTopicIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(samThesisTopicService.selectSamThesisTopicList(any(SamThesisTopic.class))).thenReturn(List.of());

        mockMvc.perform(get("/sam/thesisTopic/list").param("topicName", "排课"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SamThesisTopic> captor = ArgumentCaptor.forClass(SamThesisTopic.class);
        verify(samThesisTopicService).selectSamThesisTopicList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("排课", captor.getValue().getTopicName());
    }
}
