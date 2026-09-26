package com.yu.web.controller.system;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.system.ai.AiAnswer;
import com.yu.system.domain.SysAiChatRecord;
import com.yu.system.domain.SysAiKnowledge;
import com.yu.system.service.IAiLlmClient;
import com.yu.system.service.ISysAiChatService;
import com.yu.system.service.ISysAiKnowledgeService;

/**
 * 教务政策AI知识库Controller（Phase34 AI应用试点）
 *
 * 除常规维护接口外，另提供后台自测问答与引擎状态查询：
 * 管理员可先用真实问题验证知识库效果，再发布给师生使用，避免把低质量回答直接暴露给用户。
 *
 * @author yu
 * @date 2026-09-26
 */
@RestController
@RequestMapping("/system/aiKnowledge")
public class SysAiKnowledgeController extends BaseController
{
    @Autowired
    private ISysAiKnowledgeService sysAiKnowledgeService;

    @Autowired
    private ISysAiChatService sysAiChatService;

    @Autowired
    private IAiLlmClient aiLlmClient;

    /**
     * 查询知识条目列表
     */
    @PreAuthorize("@ss.hasPermi('system:aiKnowledge:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysAiKnowledge sysAiKnowledge)
    {
        startPage();
        List<SysAiKnowledge> list = sysAiKnowledgeService.selectSysAiKnowledgeList(sysAiKnowledge);
        return getDataTable(list);
    }

    /**
     * 导出知识条目列表
     */
    @PreAuthorize("@ss.hasPermi('system:aiKnowledge:export')")
    @Log(title = "AI知识库", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysAiKnowledge sysAiKnowledge)
    {
        List<SysAiKnowledge> list = sysAiKnowledgeService.selectSysAiKnowledgeList(sysAiKnowledge);
        ExcelUtil<SysAiKnowledge> util = new ExcelUtil<SysAiKnowledge>(SysAiKnowledge.class);
        util.exportExcel(response, list, "AI知识库数据");
    }

    /**
     * 知识库治理统计（分类分布与热门条目）
     */
    @PreAuthorize("@ss.hasPermi('system:aiKnowledge:list')")
    @GetMapping("/stat")
    public AjaxResult stat()
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("categoryStat", sysAiKnowledgeService.selectCategoryStat());
        data.put("hotKnowledge", sysAiKnowledgeService.selectHotKnowledge(10));
        return success(data);
    }

    /**
     * 回答引擎状态（大模型是否接入、检索阈值与召回条数、启用条目数）
     */
    @PreAuthorize("@ss.hasPermi('system:aiKnowledge:query')")
    @GetMapping("/engine")
    public AjaxResult engine()
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("llmEnabled", aiLlmClient.isEnabled());
        data.put("engineNote", aiLlmClient.describeEngine());
        data.put("minScore", sysAiKnowledgeService.minScore());
        data.put("enabledTotal", sysAiKnowledgeService.listEnabled().size());
        return success(data);
    }

    /**
     * 后台自测问答（留痕场景记为 admin，便于与门户真实提问区分）
     */
    @PreAuthorize("@ss.hasPermi('system:aiKnowledge:ask')")
    @Log(title = "AI问答自测", businessType = BusinessType.OTHER)
    @PostMapping("/ask")
    public AjaxResult ask(@RequestBody SysAiChatRecord req)
    {
        AiAnswer answer = sysAiChatService.ask(req.getQuestion(), 0, "admin");
        return success(answer);
    }

    /**
     * 推荐问法（取自知识库，供自测页与门户引导）
     */
    @PreAuthorize("@ss.hasPermi('system:aiKnowledge:list')")
    @GetMapping("/suggest")
    public AjaxResult suggest(Integer limit)
    {
        List<String> questions = sysAiChatService.suggestQuestions(limit == null ? 6 : limit);
        return success(questions);
    }

    /**
     * 获取知识条目详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:aiKnowledge:query')")
    @GetMapping(value = "/{knowledgeId}")
    public AjaxResult getInfo(@PathVariable("knowledgeId") Long knowledgeId)
    {
        return success(sysAiKnowledgeService.selectSysAiKnowledgeByKnowledgeId(knowledgeId));
    }

    /**
     * 新增知识条目
     */
    @PreAuthorize("@ss.hasPermi('system:aiKnowledge:add')")
    @Log(title = "AI知识库", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysAiKnowledge sysAiKnowledge)
    {
        sysAiKnowledge.setCreateBy(getUsername());
        return toAjax(sysAiKnowledgeService.insertSysAiKnowledge(sysAiKnowledge));
    }

    /**
     * 修改知识条目
     */
    @PreAuthorize("@ss.hasPermi('system:aiKnowledge:edit')")
    @Log(title = "AI知识库", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysAiKnowledge sysAiKnowledge)
    {
        sysAiKnowledge.setUpdateBy(getUsername());
        return toAjax(sysAiKnowledgeService.updateSysAiKnowledge(sysAiKnowledge));
    }

    /**
     * 删除知识条目
     */
    @PreAuthorize("@ss.hasPermi('system:aiKnowledge:remove')")
    @Log(title = "AI知识库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{knowledgeIds}")
    public AjaxResult remove(@PathVariable Long[] knowledgeIds)
    {
        return toAjax(sysAiKnowledgeService.deleteSysAiKnowledgeByKnowledgeIds(knowledgeIds));
    }
}
