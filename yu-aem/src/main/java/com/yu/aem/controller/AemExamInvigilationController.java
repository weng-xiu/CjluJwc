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
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.aem.domain.AemExamInvigilation;
import com.yu.aem.service.IAemExamInvigilationService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 监考教师分配Controller
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@RestController
@RequestMapping("/aem/invigilation")
public class AemExamInvigilationController extends BaseController
{
    @Autowired
    private IAemExamInvigilationService aemExamInvigilationService;

    @PreAuthorize("@ss.hasPermi('aem:invigilation:list')")
    @GetMapping("/list")
    public TableDataInfo list(AemExamInvigilation aemExamInvigilation)
    {
        startPage();
        List<AemExamInvigilation> list = aemExamInvigilationService.selectAemExamInvigilationList(aemExamInvigilation);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aem:invigilation:export')")
    @Log(title = "监考教师分配", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AemExamInvigilation aemExamInvigilation)
    {
        List<AemExamInvigilation> list = aemExamInvigilationService.selectAemExamInvigilationList(aemExamInvigilation);
        ExcelUtil<AemExamInvigilation> util = new ExcelUtil<AemExamInvigilation>(AemExamInvigilation.class);
        util.exportExcel(response, list, "监考教师分配数据");
    }

    @PreAuthorize("@ss.hasPermi('aem:invigilation:query')")
    @GetMapping(value = "/{invigilationId}")
    public AjaxResult getInfo(@PathVariable("invigilationId") Long invigilationId)
    {
        return success(aemExamInvigilationService.selectAemExamInvigilationByInvigilationId(invigilationId));
    }

    @PreAuthorize("@ss.hasPermi('aem:invigilation:add')")
    @Log(title = "监考教师分配", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AemExamInvigilation aemExamInvigilation)
    {
        return toAjax(aemExamInvigilationService.insertAemExamInvigilation(aemExamInvigilation));
    }

    @PreAuthorize("@ss.hasPermi('aem:invigilation:edit')")
    @Log(title = "监考教师分配", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AemExamInvigilation aemExamInvigilation)
    {
        return toAjax(aemExamInvigilationService.updateAemExamInvigilation(aemExamInvigilation));
    }

    @PreAuthorize("@ss.hasPermi('aem:invigilation:remove')")
    @Log(title = "监考教师分配", businessType = BusinessType.DELETE)
    @DeleteMapping("/{invigilationIds}")
    public AjaxResult remove(@PathVariable Long[] invigilationIds)
    {
        return toAjax(aemExamInvigilationService.deleteAemExamInvigilationByInvigilationIds(invigilationIds));
    }
}
