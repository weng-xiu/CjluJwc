package com.yu.portal.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.yu.portal.service.IPortalAiService;
import com.yu.system.ai.AiAnswer;
import com.yu.system.service.IAiLlmClient;
import com.yu.system.service.ISysAiChatService;

/**
 * 门户 AI 服务接口层测试（Q1 第四批：MockMvc standalone）。
 * 校验 Phase34 试点契约：ask 以 scene="portal"、topK=0 调用问答服务并透传问题；
 * suggest 缺省 limit 兜底 6、显式传入生效；engine 披露 llmEnabled/engineNote；
 * recommend/portrait 以 getUserId() 作为学生ID口径（无法越权查询他人）。
 * 问答检索、推荐打分、画像计算等业务逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalAiControllerMockMvcTest {

    @Mock
    private ISysAiChatService sysAiChatService;

    @Mock
    private IAiLlmClient aiLlmClient;

    @Mock
    private IPortalAiService portalAiService;

    @InjectMocks
    private PortalAiController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
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
    @DisplayName("ask：问题透传问答服务，scene=portal、topK=0，答案落 data")
    void ask_passesQuestionAndScene() throws Exception {
        AiAnswer answer = new AiAnswer();
        answer.setAnswer("按政策第三条");
        when(sysAiChatService.ask(eq("选课时间"), eq(0), eq("portal"))).thenReturn(answer);

        mockMvc.perform(post("/portal/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"选课时间\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.answer").value("按政策第三条"));

        verify(sysAiChatService).ask("选课时间", 0, "portal");
    }

    @Test
    @DisplayName("suggest：未传 limit 时兜底 6 条推荐问法")
    void suggest_defaultsLimitSix() throws Exception {
        when(sysAiChatService.suggestQuestions(6)).thenReturn(List.of("如何选课", "成绩在哪查"));

        mockMvc.perform(get("/portal/ai/suggest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(sysAiChatService).suggestQuestions(6);
    }

    @Test
    @DisplayName("suggest：显式传入 limit 时按该值查询")
    void suggest_honorsExplicitLimit() throws Exception {
        when(sysAiChatService.suggestQuestions(3)).thenReturn(List.of("a"));

        mockMvc.perform(get("/portal/ai/suggest").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysAiChatService).suggestQuestions(3);
    }

    @Test
    @DisplayName("engine：披露当前回答引擎开关与说明")
    void engine_returnsLlmStatus() throws Exception {
        when(aiLlmClient.isEnabled()).thenReturn(true);
        when(aiLlmClient.describeEngine()).thenReturn("大模型已接入");

        mockMvc.perform(get("/portal/ai/engine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.llmEnabled").value(true))
                .andExpect(jsonPath("$.data.engineNote").value("大模型已接入"));
    }

    @Test
    @DisplayName("recommend：以登录用户为 studentId 口径返回推荐")
    void recommend_bindsLoginUserId() throws Exception {
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("count", 5);
        when(portalAiService.recommendCourses(1L)).thenReturn(rec);

        mockMvc.perform(get("/portal/ai/recommend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.count").value(5));

        verify(portalAiService).recommendCourses(1L);
    }

    @Test
    @DisplayName("portrait：以登录用户为 studentId 口径返回学业画像")
    void portrait_bindsLoginUserId() throws Exception {
        Map<String, Object> portrait = new LinkedHashMap<>();
        portrait.put("overall", 82);
        when(portalAiService.buildPortrait(1L)).thenReturn(portrait);

        mockMvc.perform(get("/portal/ai/portrait"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.overall").value(82));

        verify(portalAiService).buildPortrait(1L);
    }
}
