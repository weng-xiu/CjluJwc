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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.sam.domain.SamThesis;
import com.yu.sam.service.ISamThesisService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 毕业论文（设计）全过程Controller
 *
 * <p>管理端提供档案维护、环节审核、查重登记、成绩归档、抽检管理与过程统计；
 * 学生与指导教师的过程性操作由门户端 /portal/thesis 承载。</p>
 *
 * @author ruoyi
 * @date 2026-09-26
 */
@RestController
@RequestMapping("/sam/thesis")
public class SamThesisController extends BaseController
{
    @Autowired
    private ISamThesisService samThesisService;

    @PreAuthorize("@ss.hasPermi('sam:thesis:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamThesis samThesis)
    {
        startPage();
        List<SamThesis> list = samThesisService.selectSamThesisList(samThesis);
        return getDataTable(list);
    }

    /** 过程统计（汇总指标 + 各环节进度） */
    @PreAuthorize("@ss.hasPermi('sam:thesis:list')")
    @GetMapping("/stat")
    public AjaxResult stat(SamThesis samThesis)
    {
        return success(samThesisService.statSummary(samThesis));
    }

    @PreAuthorize("@ss.hasPermi('sam:thesis:export')")
    @Log(title = "毕业论文", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SamThesis samThesis)
    {
        List<SamThesis> list = samThesisService.selectSamThesisList(samThesis);
        ExcelUtil<SamThesis> util = new ExcelUtil<SamThesis>(SamThesis.class);
        util.exportExcel(response, list, "毕业论文数据");
    }

    /** 论文档案详情（含环节留痕） */
    @PreAuthorize("@ss.hasPermi('sam:thesis:query')")
    @GetMapping(value = "/{thesisId}")
    public AjaxResult getInfo(@PathVariable("thesisId") Long thesisId)
    {
        return success(samThesisService.selectDetailWithProcess(thesisId));
    }

    @PreAuthorize("@ss.hasPermi('sam:thesis:add')")
    @Log(title = "毕业论文", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SamThesis samThesis)
    {
        samThesis.setCreateBy(getUsername());
        return toAjax(samThesisService.insertSamThesis(samThesis));
    }

    @PreAuthorize("@ss.hasPermi('sam:thesis:edit')")
    @Log(title = "毕业论文", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SamThesis samThesis)
    {
        samThesis.setUpdateBy(getUsername());
        return toAjax(samThesisService.updateSamThesis(samThesis));
    }

    /** 环节审核（开题、中期检查、答辩） */
    @PreAuthorize("@ss.hasPermi('sam:thesis:audit')")
    @Log(title = "毕业论文环节审核", businessType = BusinessType.UPDATE)
    @PostMapping("/stage/audit")
    public AjaxResult audit(@RequestBody ThesisAction action)
    {
        return toAjax(samThesisService.auditStage(action.getThesisId(), action.getStage(),
                Boolean.TRUE.equals(action.getPass()), action.getScore(), action.getOpinion(), getUsername()));
    }

    /** 代学生提交环节材料（补录场景） */
    @PreAuthorize("@ss.hasPermi('sam:thesis:edit')")
    @Log(title = "毕业论文材料补录", businessType = BusinessType.INSERT)
    @PostMapping("/stage/submit")
    public AjaxResult submit(@RequestBody ThesisAction action)
    {
        return toAjax(samThesisService.submitStage(action.getThesisId(), action.getStage(), action.getTitle(),
                action.getContent(), action.getAttachment(), getUsername()));
    }

    /** 查重结果登记 */
    @PreAuthorize("@ss.hasPermi('sam:thesis:audit')")
    @Log(title = "毕业论文查重登记", businessType = BusinessType.INSERT)
    @PostMapping("/check")
    public AjaxResult check(@RequestBody ThesisAction action)
    {
        return toAjax(samThesisService.recordCheck(action.getThesisId(), action.getScore(),
                action.getAttachment(), action.getOpinion(), getUsername()));
    }

    /** 成绩归档 */
    @PreAuthorize("@ss.hasPermi('sam:thesis:audit')")
    @Log(title = "毕业论文成绩归档", businessType = BusinessType.INSERT)
    @PostMapping("/archive")
    public AjaxResult archive(@RequestBody ThesisAction action)
    {
        return toAjax(samThesisService.archiveGrade(action.getThesisId(), action.getTotalScore(),
                action.getDefenseScore(), action.getOpinion(), getUsername()));
    }

    /** 抽检状态维护（送检与结果回填） */
    @PreAuthorize("@ss.hasPermi('sam:thesis:sample')")
    @Log(title = "毕业论文抽检", businessType = BusinessType.UPDATE)
    @PutMapping("/sample")
    public AjaxResult sample(@RequestBody ThesisAction action)
    {
        return toAjax(samThesisService.markSample(action.getThesisId(), action.getSampleStatus(),
                action.getOpinion(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('sam:thesis:remove')")
    @Log(title = "毕业论文", businessType = BusinessType.DELETE)
    @DeleteMapping("/{thesisIds}")
    public AjaxResult remove(@PathVariable Long[] thesisIds)
    {
        return toAjax(samThesisService.deleteSamThesisByThesisIds(thesisIds));
    }

    /**
     * 环节动作入参（不同动作取用不同字段，避免为每个动作单独定义 DTO）
     */
    public static class ThesisAction
    {
        private Long thesisId;
        private String stage;
        private String title;
        private String content;
        private String attachment;
        private Boolean pass;
        private Double score;
        private Double defenseScore;
        private Double totalScore;
        private String opinion;
        private String sampleStatus;

        public Long getThesisId() { return thesisId; }
        public void setThesisId(Long thesisId) { this.thesisId = thesisId; }
        public String getStage() { return stage; }
        public void setStage(String stage) { this.stage = stage; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getAttachment() { return attachment; }
        public void setAttachment(String attachment) { this.attachment = attachment; }
        public Boolean getPass() { return pass; }
        public void setPass(Boolean pass) { this.pass = pass; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        public Double getDefenseScore() { return defenseScore; }
        public void setDefenseScore(Double defenseScore) { this.defenseScore = defenseScore; }
        public Double getTotalScore() { return totalScore; }
        public void setTotalScore(Double totalScore) { this.totalScore = totalScore; }
        public String getOpinion() { return opinion; }
        public void setOpinion(String opinion) { this.opinion = opinion; }
        public String getSampleStatus() { return sampleStatus; }
        public void setSampleStatus(String sampleStatus) { this.sampleStatus = sampleStatus; }
    }
}
