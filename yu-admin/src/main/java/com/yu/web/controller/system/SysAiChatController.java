package com.yu.web.controller.system;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.system.domain.SysAiChatRecord;
import com.yu.system.service.ISysAiChatService;

/**
 * AI问答留痕与效果统计Controller（Phase34 AI应用试点）
 *
 * 留痕是审计数据，本 Controller 只开放查询与导出，不提供删除接口。
 * 统计口径用于回答两个问题：AI 到底命中了多少真实提问，以及哪些提问还没被知识库覆盖。
 *
 * @author yu
 * @date 2026-09-26
 */
@RestController
@RequestMapping("/system/aiChat")
public class SysAiChatController extends BaseController
{
    @Autowired
    private ISysAiChatService sysAiChatService;

    /**
     * 查询问答留痕列表
     */
    @PreAuthorize("@ss.hasPermi('system:aiChat:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysAiChatRecord sysAiChatRecord)
    {
        startPage();
        List<SysAiChatRecord> list = sysAiChatService.selectSysAiChatRecordList(sysAiChatRecord);
        return getDataTable(list);
    }

    /**
     * 导出问答留痕列表
     */
    @PreAuthorize("@ss.hasPermi('system:aiChat:export')")
    @Log(title = "AI问答留痕", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysAiChatRecord sysAiChatRecord)
    {
        List<SysAiChatRecord> list = sysAiChatService.selectSysAiChatRecordList(sysAiChatRecord);
        ExcelUtil<SysAiChatRecord> util = new ExcelUtil<SysAiChatRecord>(SysAiChatRecord.class);
        util.exportExcel(response, list, "AI问答留痕数据");
    }

    /**
     * 问答效果统计（总览、来源分布、场景分布、按日趋势、未命中问题、热门条目）
     */
    @PreAuthorize("@ss.hasPermi('system:aiChat:query')")
    @GetMapping("/stat")
    public AjaxResult stat(SysAiChatRecord sysAiChatRecord, Integer days, Integer unmatchedLimit)
    {
        return success(sysAiChatService.selectChatStat(sysAiChatRecord,
                days == null ? 14 : days, unmatchedLimit == null ? 10 : unmatchedLimit));
    }

    /**
     * 查看单次问答详情（含完整回答与引用条目）
     */
    @PreAuthorize("@ss.hasPermi('system:aiChat:query')")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId)
    {
        return success(sysAiChatService.selectSysAiChatRecordByRecordId(recordId));
    }
}
