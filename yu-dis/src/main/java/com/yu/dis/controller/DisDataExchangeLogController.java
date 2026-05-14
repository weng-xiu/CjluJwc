package com.yu.dis.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.dis.domain.DisDataExchangeLog;
import com.yu.dis.service.IDisDataExchangeLogService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/dis/exchange")
public class DisDataExchangeLogController extends BaseController
{
    @Autowired
    private IDisDataExchangeLogService disDataExchangeLogService;

    @PreAuthorize("@ss.hasPermi('dis:exchangeLog:list')")
    @GetMapping("/list")
    public TableDataInfo list(DisDataExchangeLog disDataExchangeLog)
    {
        startPage();
        List<DisDataExchangeLog> list = disDataExchangeLogService.selectDisDataExchangeLogList(disDataExchangeLog);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('dis:exchangeLog:export')")
    @Log(title = "交换日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DisDataExchangeLog disDataExchangeLog)
    {
        List<DisDataExchangeLog> list = disDataExchangeLogService.selectDisDataExchangeLogList(disDataExchangeLog);
        ExcelUtil<DisDataExchangeLog> util = new ExcelUtil<DisDataExchangeLog>(DisDataExchangeLog.class);
        util.exportExcel(response, list, "数据交换日志");
    }

    @PreAuthorize("@ss.hasPermi('dis:exchangeLog:query')")
    @GetMapping(value = "/{logId}")
    public AjaxResult getInfo(@PathVariable("logId") Long logId)
    {
        return success(disDataExchangeLogService.selectDisDataExchangeLogByLogId(logId));
    }

    @PreAuthorize("@ss.hasPermi('dis:exchangeLog:remove')")
    @Log(title = "交换日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{logIds}")
    public AjaxResult remove(@PathVariable Long[] logIds)
    {
        return toAjax(disDataExchangeLogService.deleteDisDataExchangeLogByLogIds(logIds));
    }

    @PreAuthorize("@ss.hasPermi('dis:exchangeLog:clean')")
    @Log(title = "交换日志", businessType = BusinessType.CLEAN)
    @DeleteMapping("/clean")
    public AjaxResult clean()
    {
        return toAjax(disDataExchangeLogService.cleanDisDataExchangeLog());
    }
}
