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
import com.yu.aem.domain.AemExamSeat;
import com.yu.aem.service.IAemExamSeatService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 考场座位编排Controller
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@RestController
@RequestMapping("/aem/examSeat")
public class AemExamSeatController extends BaseController
{
    @Autowired
    private IAemExamSeatService aemExamSeatService;

    @PreAuthorize("@ss.hasPermi('aem:examSeat:list')")
    @GetMapping("/list")
    public TableDataInfo list(AemExamSeat aemExamSeat)
    {
        startPage();
        List<AemExamSeat> list = aemExamSeatService.selectAemExamSeatList(aemExamSeat);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aem:examSeat:export')")
    @Log(title = "考场座位编排", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AemExamSeat aemExamSeat)
    {
        List<AemExamSeat> list = aemExamSeatService.selectAemExamSeatList(aemExamSeat);
        ExcelUtil<AemExamSeat> util = new ExcelUtil<AemExamSeat>(AemExamSeat.class);
        util.exportExcel(response, list, "考场座位编排数据");
    }

    @PreAuthorize("@ss.hasPermi('aem:examSeat:query')")
    @GetMapping(value = "/{seatId}")
    public AjaxResult getInfo(@PathVariable("seatId") Long seatId)
    {
        return success(aemExamSeatService.selectAemExamSeatBySeatId(seatId));
    }

    @PreAuthorize("@ss.hasPermi('aem:examSeat:add')")
    @Log(title = "考场座位编排", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AemExamSeat aemExamSeat)
    {
        return toAjax(aemExamSeatService.insertAemExamSeat(aemExamSeat));
    }

    @PreAuthorize("@ss.hasPermi('aem:examSeat:edit')")
    @Log(title = "考场座位编排", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AemExamSeat aemExamSeat)
    {
        return toAjax(aemExamSeatService.updateAemExamSeat(aemExamSeat));
    }

    @PreAuthorize("@ss.hasPermi('aem:examSeat:remove')")
    @Log(title = "考场座位编排", businessType = BusinessType.DELETE)
    @DeleteMapping("/{seatIds}")
    public AjaxResult remove(@PathVariable Long[] seatIds)
    {
        return toAjax(aemExamSeatService.deleteAemExamSeatBySeatIds(seatIds));
    }
}
