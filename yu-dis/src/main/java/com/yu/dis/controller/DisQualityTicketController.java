package com.yu.dis.controller;

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
import com.yu.dis.domain.DisQualityTicket;
import com.yu.dis.service.IDisQualityTicketService;

/**
 * 数据对接质量异常工单 Controller（F5 质量看板异常工单化）。
 *
 * <p>质量看板（N2）异常处置闭环：工单 CRUD + 异常检测一键生成待处理工单。
 *
 * @author F5
 */
@RestController
@RequestMapping("/dis/qualityTicket")
public class DisQualityTicketController extends BaseController
{
    @Autowired
    private IDisQualityTicketService disQualityTicketService;

    @PreAuthorize("@ss.hasPermi('dis:qualityTicket:list')")
    @GetMapping("/list")
    public TableDataInfo list(DisQualityTicket disQualityTicket)
    {
        startPage();
        List<DisQualityTicket> list = disQualityTicketService.selectDisQualityTicketList(disQualityTicket);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('dis:qualityTicket:export')")
    @Log(title = "质量异常工单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DisQualityTicket disQualityTicket)
    {
        List<DisQualityTicket> list = disQualityTicketService.selectDisQualityTicketList(disQualityTicket);
        ExcelUtil<DisQualityTicket> util = new ExcelUtil<DisQualityTicket>(DisQualityTicket.class);
        util.exportExcel(response, list, "质量异常工单数据");
    }

    @PreAuthorize("@ss.hasPermi('dis:qualityTicket:query')")
    @GetMapping(value = "/{ticketId}")
    public AjaxResult getInfo(@PathVariable("ticketId") Long ticketId)
    {
        return success(disQualityTicketService.selectDisQualityTicketByTicketId(ticketId));
    }

    @PreAuthorize("@ss.hasPermi('dis:qualityTicket:add')")
    @Log(title = "质量异常工单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody DisQualityTicket disQualityTicket)
    {
        disQualityTicket.setCreateBy(getUsername());
        return toAjax(disQualityTicketService.insertDisQualityTicket(disQualityTicket));
    }

    @PreAuthorize("@ss.hasPermi('dis:qualityTicket:edit')")
    @Log(title = "质量异常工单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody DisQualityTicket disQualityTicket)
    {
        disQualityTicket.setUpdateBy(getUsername());
        return toAjax(disQualityTicketService.updateDisQualityTicket(disQualityTicket));
    }

    @PreAuthorize("@ss.hasPermi('dis:qualityTicket:remove')")
    @Log(title = "质量异常工单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ticketIds}")
    public AjaxResult remove(@PathVariable Long[] ticketIds)
    {
        return toAjax(disQualityTicketService.deleteDisQualityTicketByTicketIds(ticketIds));
    }

    /**
     * 异常检测：扫描全部同步任务，对失败率达到阈值的任务自动生成待处理工单（幂等）。
     *
     * @param threshold 失败率阈值（百分比），默认 20
     * @return 本次新生成的工单数量
     */
    @PreAuthorize("@ss.hasPermi('dis:qualityTicket:generate')")
    @Log(title = "质量异常工单", businessType = BusinessType.INSERT)
    @PostMapping("/detect")
    public AjaxResult detect(@RequestParam(defaultValue = "20") double threshold)
    {
        int generated = disQualityTicketService.detectAndGenerate(threshold, getUsername());
        return AjaxResult.success("异常检测完成，新增工单 " + generated + " 条", generated);
    }
}
