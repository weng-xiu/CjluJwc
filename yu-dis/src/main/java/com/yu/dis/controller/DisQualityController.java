package com.yu.dis.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.dis.quality.DisQualityService;

/**
 * 数据对接质量看板 Controller（N2 质量看板）。
 *
 * @author N2
 */
@RestController
@RequestMapping("/dis/quality")
public class DisQualityController extends BaseController
{
    @Autowired
    private DisQualityService disQualityService;

    /** 数据同步质量看板 */
    @PreAuthorize("@ss.hasPermi('dis:quality:view')")
    @GetMapping("/dashboard")
    public AjaxResult dashboard()
    {
        return success(disQualityService.dashboard());
    }
}
