package com.yu.portal.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.tpm.domain.TpmScheduleAdjustment;
import com.yu.tpm.service.ITpmScheduleAdjustmentService;

/**
 * 调停课申请门户Controller（教师端）
 *
 * @author ruoyi
 * @date 2026-05-20
 */
@RestController
@RequestMapping("/portal/adjustment")
public class PortalAdjustmentController extends BaseController
{
    @Autowired
    private ITpmScheduleAdjustmentService tpmScheduleAdjustmentService;

    /** 教师端：调停课申请列表查询 */
    @PreAuthorize("@ss.hasPermi('portal:adjustment:list') and @ss.hasAnyRoles('admin,teacher')")
    @GetMapping("/list")
    public TableDataInfo list(TpmScheduleAdjustment tpmScheduleAdjustment)
    {
        startPage();
        List<TpmScheduleAdjustment> list = tpmScheduleAdjustmentService.selectTpmScheduleAdjustmentList(tpmScheduleAdjustment);
        return getDataTable(list);
    }

    /** 教师端：提交调停课申请 */
    @PreAuthorize("@ss.hasPermi('portal:adjustment:add') and @ss.hasAnyRoles('admin,teacher')")
    @Log(title = "调停课申请", businessType = BusinessType.INSERT)
    @PostMapping("/apply")
    public AjaxResult add(@Validated @RequestBody TpmScheduleAdjustment tpmScheduleAdjustment)
    {
        return toAjax(tpmScheduleAdjustmentService.insertTpmScheduleAdjustment(tpmScheduleAdjustment));
    }
}
