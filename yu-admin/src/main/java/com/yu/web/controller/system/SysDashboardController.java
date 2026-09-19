package com.yu.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.web.service.IDashboardStatService;

/**
 * 教务数据驾驶舱 Controller（P2）
 * 登录即可查看，供管理端首页看板调用。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@RestController
@RequestMapping("/system/dashboard")
public class SysDashboardController extends BaseController
{
    @Autowired
    private IDashboardStatService dashboardStatService;

    /**
     * 驾驶舱总览：当前学期 + 核心指标 + 预警分布 + 低通过率课程TOP + 选课结果分布
     */
    @GetMapping("/overview")
    public AjaxResult overview()
    {
        return success(dashboardStatService.selectOverview());
    }
}
