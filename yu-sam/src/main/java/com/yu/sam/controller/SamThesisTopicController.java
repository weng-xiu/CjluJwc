package com.yu.sam.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.sam.domain.SamThesisTopic;
import com.yu.sam.service.ISamThesisTopicService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 毕业论文选题库Controller
 *
 * @author ruoyi
 * @date 2026-09-26
 */
@RestController
@RequestMapping("/sam/thesisTopic")
public class SamThesisTopicController extends BaseController
{
    @Autowired
    private ISamThesisTopicService samThesisTopicService;

    @PreAuthorize("@ss.hasPermi('sam:thesisTopic:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamThesisTopic samThesisTopic)
    {
        startPage();
        List<SamThesisTopic> list = samThesisTopicService.selectSamThesisTopicList(samThesisTopic);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('sam:thesisTopic:export')")
    @Log(title = "毕业论文选题库", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SamThesisTopic samThesisTopic)
    {
        List<SamThesisTopic> list = samThesisTopicService.selectSamThesisTopicList(samThesisTopic);
        ExcelUtil<SamThesisTopic> util = new ExcelUtil<SamThesisTopic>(SamThesisTopic.class);
        util.exportExcel(response, list, "毕业论文选题数据");
    }

    @PreAuthorize("@ss.hasPermi('sam:thesisTopic:query')")
    @GetMapping(value = "/{topicId}")
    public AjaxResult getInfo(@PathVariable("topicId") Long topicId)
    {
        return success(samThesisTopicService.selectSamThesisTopicByTopicId(topicId));
    }

    @PreAuthorize("@ss.hasPermi('sam:thesisTopic:add')")
    @Log(title = "毕业论文选题库", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SamThesisTopic samThesisTopic)
    {
        samThesisTopic.setCreateBy(getUsername());
        return toAjax(samThesisTopicService.insertSamThesisTopic(samThesisTopic));
    }

    @PreAuthorize("@ss.hasPermi('sam:thesisTopic:edit')")
    @Log(title = "毕业论文选题库", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SamThesisTopic samThesisTopic)
    {
        samThesisTopic.setUpdateBy(getUsername());
        return toAjax(samThesisTopicService.updateSamThesisTopic(samThesisTopic));
    }

    /** 题目审核（通过=可选题，不通过=下架） */
    @PreAuthorize("@ss.hasPermi('sam:thesisTopic:edit')")
    @Log(title = "毕业论文选题审核", businessType = BusinessType.UPDATE)
    @PutMapping("/audit/{topicId}")
    public AjaxResult audit(@PathVariable("topicId") Long topicId, @RequestParam("pass") boolean pass,
            @RequestParam(value = "opinion", required = false) String opinion)
    {
        return toAjax(samThesisTopicService.auditTopic(topicId, pass, opinion, getUsername()));
    }

    /** 题目上架/下架 */
    @PreAuthorize("@ss.hasPermi('sam:thesisTopic:edit')")
    @Log(title = "毕业论文选题状态", businessType = BusinessType.UPDATE)
    @PutMapping("/status/{topicId}")
    public AjaxResult changeStatus(@PathVariable("topicId") Long topicId, @RequestParam("status") String status)
    {
        return toAjax(samThesisTopicService.changeStatus(topicId, status, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('sam:thesisTopic:remove')")
    @Log(title = "毕业论文选题库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{topicIds}")
    public AjaxResult remove(@PathVariable Long[] topicIds)
    {
        return toAjax(samThesisTopicService.deleteSamThesisTopicByTopicIds(topicIds));
    }
}
