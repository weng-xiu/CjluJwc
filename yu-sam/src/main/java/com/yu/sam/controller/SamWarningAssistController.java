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
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.sam.domain.SamWarningAssist;
import com.yu.sam.domain.SamWarningAssistRecord;
import com.yu.sam.service.ISamWarningAssistService;

/**
 * 学业预警帮扶任务 Controller（S6 帮扶闭环）
 */
@RestController
@RequestMapping("/sam/warningAssist")
public class SamWarningAssistController extends BaseController
{
    @Autowired
    private ISamWarningAssistService samWarningAssistService;

    @PreAuthorize("@ss.hasPermi('sam:warningAssist:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamWarningAssist query)
    {
        startPage();
        List<SamWarningAssist> list = samWarningAssistService.selectSamWarningAssistList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('sam:warningAssist:export')")
    @Log(title = "预警帮扶", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SamWarningAssist query)
    {
        List<SamWarningAssist> list = samWarningAssistService.selectSamWarningAssistList(query);
        ExcelUtil<SamWarningAssist> util = new ExcelUtil<SamWarningAssist>(SamWarningAssist.class);
        util.exportExcel(response, list, "预警帮扶数据");
    }

    @PreAuthorize("@ss.hasPermi('sam:warningAssist:query')")
    @GetMapping(value = "/{assistId}")
    public AjaxResult getInfo(@PathVariable("assistId") Long assistId)
    {
        return success(samWarningAssistService.selectSamWarningAssistByAssistId(assistId));
    }

    /** 手动派发帮扶任务 */
    @PreAuthorize("@ss.hasPermi('sam:warningAssist:dispatch')")
    @Log(title = "预警帮扶派发", businessType = BusinessType.INSERT)
    @PostMapping("/dispatch")
    public AjaxResult dispatch(@Validated @RequestBody SamWarningAssist assist)
    {
        assist.setCreateBy(getUsername());
        return toAjax(samWarningAssistService.dispatch(assist));
    }

    /** 认领帮扶任务 */
    @PreAuthorize("@ss.hasPermi('sam:warningAssist:claim')")
    @Log(title = "预警帮扶认领", businessType = BusinessType.UPDATE)
    @PutMapping("/claim/{assistId}")
    public AjaxResult claim(@PathVariable("assistId") Long assistId)
    {
        return toAjax(samWarningAssistService.claim(assistId, getUserId(), getLoginUser().getUser().getNickName()));
    }

    /** 登记帮扶跟踪记录 */
    @PreAuthorize("@ss.hasPermi('sam:warningAssist:follow')")
    @Log(title = "预警帮扶跟踪", businessType = BusinessType.INSERT)
    @PostMapping("/follow/{assistId}")
    public AjaxResult follow(@PathVariable("assistId") Long assistId, @RequestBody SamWarningAssistRecord record)
    {
        record.setOperatorId(getUserId());
        record.setOperatorName(getLoginUser().getUser().getNickName());
        record.setCreateBy(getUsername());
        return toAjax(samWarningAssistService.follow(assistId, record));
    }

    /** 完结帮扶（可选同步解除预警） */
    @PreAuthorize("@ss.hasPermi('sam:warningAssist:finish')")
    @Log(title = "预警帮扶完结", businessType = BusinessType.UPDATE)
    @PutMapping("/finish/{assistId}")
    public AjaxResult finish(@PathVariable("assistId") Long assistId,
                             @RequestParam(required = false) String finishRemark,
                             @RequestParam(defaultValue = "false") boolean resolveWarning)
    {
        return toAjax(samWarningAssistService.finish(assistId, finishRemark, resolveWarning));
    }

    /** 关闭帮扶任务 */
    @PreAuthorize("@ss.hasPermi('sam:warningAssist:finish')")
    @Log(title = "预警帮扶关闭", businessType = BusinessType.UPDATE)
    @PutMapping("/close/{assistId}")
    public AjaxResult close(@PathVariable("assistId") Long assistId,
                            @RequestParam(required = false) String finishRemark)
    {
        return toAjax(samWarningAssistService.closeAssist(assistId, finishRemark));
    }

    @PreAuthorize("@ss.hasPermi('sam:warningAssist:remove')")
    @Log(title = "预警帮扶", businessType = BusinessType.DELETE)
    @DeleteMapping("/{assistIds}")
    public AjaxResult remove(@PathVariable Long[] assistIds)
    {
        return toAjax(samWarningAssistService.deleteSamWarningAssistByAssistIds(assistIds));
    }
}
