package com.yu.web.controller.system;

import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.system.ai.AiAnswer;
import com.yu.system.domain.SysAiKnowledge;
import com.yu.system.service.IAiLlmClient;
import com.yu.system.service.ISysAiChatService;
import com.yu.system.service.ISysAiKnowledgeService;

/**
 * 教务政策 AI 知识库控制器契约测试（G5，Phase34 AI 试点治理面）。
 * 校验治理统计（分类分布 + 热门条目）、引擎状态（大模型是否接入/阈值/启用数）、后台自测问答
 * 与推荐问法四类聚合接口的响应装配契约；ask 固定以 scene=admin 留痕、suggest 缺省 limit=6，
 * 视图层不感知检索/降级抽取与大模型调用细节。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysAiKnowledgeControllerMockMvcTest
{
    @Mock
    private ISysAiKnowledgeService sysAiKnowledgeService;

    @Mock
    private ISysAiChatService sysAiChatService;

    @Mock
    private IAiLlmClient aiLlmClient;

    @InjectMocks
    private SysAiKnowledgeController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /stat：分类分布与热门条目聚合为 categoryStat/hotKnowledge")
    void statAggregates() throws Exception
    {
        Map<String, Object> cat = new LinkedHashMap<>();
        cat.put("category", "学籍");
        when(sysAiKnowledgeService.selectCategoryStat()).thenReturn(List.of(cat));
        when(sysAiKnowledgeService.selectHotKnowledge(10)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/system/aiKnowledge/stat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryStat").isArray())
                .andExpect(jsonPath("$.data.hotKnowledge").isArray());

        verify(sysAiKnowledgeService).selectHotKnowledge(10);
    }

    @Test
    @DisplayName("GET /engine：回传大模型接入状态、口径说明与启用条目数")
    void engineStatus() throws Exception
    {
        when(aiLlmClient.isEnabled()).thenReturn(false);
        when(aiLlmClient.describeEngine()).thenReturn("未接入大模型，降级为本地知识库抽取");
        when(sysAiKnowledgeService.minScore()).thenReturn(0.3);
        when(sysAiKnowledgeService.listEnabled()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/system/aiKnowledge/engine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.llmEnabled").value(false))
                .andExpect(jsonPath("$.data.engineNote").value("未接入大模型，降级为本地知识库抽取"))
                .andExpect(jsonPath("$.data.enabledTotal").value(0));
    }

    @Test
    @DisplayName("POST /ask：自测问答固定以 scene=admin 留痕并回传回答")
    void askUsesAdminScene() throws Exception
    {
        AiAnswer ans = new AiAnswer();
        ans.setAnswer("按政策第三条，绩点 2.0 及以上可授予学位。");
        when(sysAiChatService.ask(any(), eq(0), eq("admin"))).thenReturn(ans);

        mockMvc.perform(post("/system/aiKnowledge/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"学位授予条件是什么\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value("按政策第三条，绩点 2.0 及以上可授予学位。"));

        verify(sysAiChatService).ask(any(), eq(0), eq("admin"));
    }

    @Test
    @DisplayName("GET /suggest：缺省 limit 时以 6 条推荐问法透传")
    void suggestDefaultLimit() throws Exception
    {
        when(sysAiChatService.suggestQuestions(6)).thenReturn(List.of("绩点怎么算"));

        mockMvc.perform(get("/system/aiKnowledge/suggest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("绩点怎么算"));

        verify(sysAiChatService).suggestQuestions(6);
    }

    @Test
    @DisplayName("GET /{knowledgeId}：路径变量绑定返回知识条目详情")
    void getInfoBindsId() throws Exception
    {
        when(sysAiKnowledgeService.selectSysAiKnowledgeByKnowledgeId(4L)).thenReturn(new SysAiKnowledge());

        mockMvc.perform(get("/system/aiKnowledge/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());

        verify(sysAiKnowledgeService).selectSysAiKnowledgeByKnowledgeId(4L);
    }
}
