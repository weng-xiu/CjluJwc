package com.yu.tpm.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.tpm.domain.TpmSelectionRound;
import com.yu.tpm.service.ITpmSelectionRoundService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 选课轮次Controller
 *
 * @author ruoyi
 * @date 2026-05-09
 */
@RestController
@RequestMapping("/tpm/round")
public class TpmSelectionRoundController extends BaseController
{
    @Autowired
    private ITpmSelectionRoundService tpmSelectionRoundService;

    @PreAuthorize("@ss.hasPermi('tpm:round:list')")
    @GetMapping("/list")
    public TableDataInfo list(TpmSelectionRound tpmSelectionRound)
    {
        startPage();
        List<TpmSelectionRound> list = tpmSelectionRoundService.selectTpmSelectionRoundList(tpmSelectionRound);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('tpm:round:export')")
    @Log(title = "选课轮次", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TpmSelectionRound tpmSelectionRound)
    {
        List<TpmSelectionRound> list = tpmSelectionRoundService.selectTpmSelectionRoundList(tpmSelectionRound);
        ExcelUtil<TpmSelectionRound> util = new ExcelUtil<TpmSelectionRound>(TpmSelectionRound.class);
        util.exportExcel(response, list, "选课轮次数据");
    }

    @PreAuthorize("@ss.hasPermi('tpm:round:query')")
    @GetMapping(value = "/{roundId}")
    public AjaxResult getInfo(@PathVariable("roundId") Long roundId)
    {
        return success(tpmSelectionRoundService.selectTpmSelectionRoundByRoundId(roundId));
    }

    @PreAuthorize("@ss.hasPermi('tpm:round:add')")
    @Log(title = "选课轮次", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TpmSelectionRound tpmSelectionRound)
    {
        return toAjax(tpmSelectionRoundService.insertTpmSelectionRound(tpmSelectionRound));
    }

    @PreAuthorize("@ss.hasPermi('tpm:round:edit')")
    @Log(title = "选课轮次", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TpmSelectionRound tpmSelectionRound)
    {
        return toAjax(tpmSelectionRoundService.updateTpmSelectionRound(tpmSelectionRound));
    }

    @PreAuthorize("@ss.hasPermi('tpm:round:remove')")
    @Log(title = "选课轮次", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roundIds}")
    public AjaxResult remove(@PathVariable Long[] roundIds)
    {
        return toAjax(tpmSelectionRoundService.deleteTpmSelectionRoundByRoundIds(roundIds));
    }

    /**
     * 开启选课轮次
     */
    @PreAuthorize("@ss.hasPermi('tpm:round:edit')")
    @Log(title = "开启选课轮次", businessType = BusinessType.UPDATE)
    @PutMapping("/start/{roundId}")
    public AjaxResult start(@PathVariable Long roundId)
    {
        return toAjax(tpmSelectionRoundService.startRound(roundId));
    }

    /**
     * 结束选课轮次
     */
    @PreAuthorize("@ss.hasPermi('tpm:round:edit')")
    @Log(title = "结束选课轮次", businessType = BusinessType.UPDATE)
    @PutMapping("/finish/{roundId}")
    public AjaxResult finish(@PathVariable Long roundId)
    {
        return toAjax(tpmSelectionRoundService.finishRound(roundId));
    }
}
