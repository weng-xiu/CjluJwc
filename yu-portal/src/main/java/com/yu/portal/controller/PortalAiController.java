package com.yu.portal.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.portal.service.IPortalAiService;
import com.yu.system.ai.AiAnswer;
import com.yu.system.domain.SysAiChatRecord;
import com.yu.system.service.IAiLlmClient;
import com.yu.system.service.ISysAiChatService;

/**
 * 门户 AI 服务Controller（Phase34 AI应用试点）
 *
 * 覆盖三类师生自助能力：教务政策智能问答、个性化选课推荐、学业画像。
 * 问答对师生开放，推荐与画像需要学籍成绩数据，仅对学生（及管理员代查）开放；
 * 所有分析均以 getUserId() 作为学生ID口径，与门户选课、成绩接口保持一致，用户无法越权查询他人数据。
 *
 * @author yu
 * @date 2026-09-26
 */
@RestController
@RequestMapping("/portal/ai")
public class PortalAiController extends BaseController
{
    @Autowired
    private ISysAiChatService sysAiChatService;

    @Autowired
    private IAiLlmClient aiLlmClient;

    @Autowired
    private IPortalAiService portalAiService;

    /** 教务政策智能问答（回答口径与引用依据随答案返回，未命中时如实告知） */
    @PreAuthorize("@ss.hasPermi('portal:ai:ask') and @ss.hasAnyRoles('admin,student,teacher')")
    @Log(title = "门户AI问答", businessType = BusinessType.OTHER)
    @PostMapping("/ask")
    public AjaxResult ask(@RequestBody SysAiChatRecord req)
    {
        AiAnswer answer = sysAiChatService.ask(req.getQuestion(), 0, "portal");
        return success(answer);
    }

    /** 推荐问法（取自知识库真实条目，供首页与提问页引导） */
    @PreAuthorize("@ss.hasPermi('portal:ai:chat') and @ss.hasAnyRoles('admin,student,teacher')")
    @GetMapping("/suggest")
    public AjaxResult suggest(Integer limit)
    {
        return success(sysAiChatService.suggestQuestions(limit == null ? 6 : limit));
    }

    /** 当前回答引擎说明（供前端披露是否已接入大模型） */
    @PreAuthorize("@ss.hasPermi('portal:ai:chat') and @ss.hasAnyRoles('admin,student,teacher')")
    @GetMapping("/engine")
    public AjaxResult engine()
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("llmEnabled", aiLlmClient.isEnabled());
        data.put("engineNote", aiLlmClient.describeEngine());
        return success(data);
    }

    /** 个性化选课推荐（硬约束复用选课校验，多信号加权打分） */
    @PreAuthorize("@ss.hasPermi('portal:ai:recommend') and @ss.hasAnyRoles('admin,student')")
    @GetMapping("/recommend")
    public AjaxResult recommend()
    {
        return success(portalAiService.recommendCourses(getUserId()));
    }

    /** 学业画像（六维评分 + 模块达成 + 建议） */
    @PreAuthorize("@ss.hasPermi('portal:ai:portrait') and @ss.hasAnyRoles('admin,student')")
    @GetMapping("/portrait")
    public AjaxResult portrait()
    {
        return success(portalAiService.buildPortrait(getUserId()));
    }
}
