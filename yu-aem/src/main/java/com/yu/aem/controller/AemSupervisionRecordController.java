package com.yu.aem.controller;

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
import com.yu.aem.domain.AemSupervisionRecord;
import com.yu.aem.service.IAemSupervisionRecordService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 督导听课记录Controller
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@RestController
@RequestMapping("/aem/supervision")
public class AemSupervisionRecordController extends BaseController
{
    @Autowired
    private IAemSupervisionRecordService aemSupervisionRecordService;

    @PreAuthorize("@ss.hasPermi('aem:supervision:list')")
    @GetMapping("/list")
    public TableDataInfo list(AemSupervisionRecord aemSupervisionRecord)
    {
        startPage();
        List<AemSupervisionRecord> list = aemSupervisionRecordService.selectAemSupervisionRecordList(aemSupervisionRecord);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aem:supervision:export')")
    @Log(title = "督导听课记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AemSupervisionRecord aemSupervisionRecord)
    {
        List<AemSupervisionRecord> list = aemSupervisionRecordService.selectAemSupervisionRecordList(aemSupervisionRecord);
        ExcelUtil<AemSupervisionRecord> util = new ExcelUtil<AemSupervisionRecord>(AemSupervisionRecord.class);
        util.exportExcel(response, list, "督导听课记录数据");
    }

    @PreAuthorize("@ss.hasPermi('aem:supervision:query')")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId)
    {
        return success(aemSupervisionRecordService.selectAemSupervisionRecordByRecordId(recordId));
    }

    @PreAuthorize("@ss.hasPermi('aem:supervision:add')")
    @Log(title = "督导听课记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AemSupervisionRecord aemSupervisionRecord)
    {
        return toAjax(aemSupervisionRecordService.insertAemSupervisionRecord(aemSupervisionRecord));
    }

    @PreAuthorize("@ss.hasPermi('aem:supervision:edit')")
    @Log(title = "督导听课记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AemSupervisionRecord aemSupervisionRecord)
    {
        return toAjax(aemSupervisionRecordService.updateAemSupervisionRecord(aemSupervisionRecord));
    }

    @PreAuthorize("@ss.hasPermi('aem:supervision:remove')")
    @Log(title = "督导听课记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds)
    {
        return toAjax(aemSupervisionRecordService.deleteAemSupervisionRecordByRecordIds(recordIds));
    }
}
