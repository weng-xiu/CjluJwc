package com.yu.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.system.servicedesk.IServiceDeskService;

/**
 * 一站式服务大厅 Controller（N3）。
 *
 * <p>对管理端开放：事项目录、时效看板、超时催办。催办提醒复用 A2 事件通道下发。
 *
 * @author N3
 */
@RestController
@RequestMapping("/system/serviceDesk")
public class ServiceDeskController extends BaseController
{
    @Autowired
    private IServiceDeskService serviceDeskService;

    /** 事项目录 */
    @PreAuthorize("@ss.hasPermi('system:serviceDesk:view')")
    @GetMapping("/catalog")
    public AjaxResult catalog()
    {
        return success(serviceDeskService.catalog());
    }

    /** 时效看板 */
    @PreAuthorize("@ss.hasPermi('system:serviceDesk:view')")
    @GetMapping("/dashboard")
    public AjaxResult dashboard()
    {
        return success(serviceDeskService.dashboard());
    }

    /** 触发超时催办，返回被催办事项数量 */
    @PreAuthorize("@ss.hasPermi('system:serviceDesk:urge')")
    @PostMapping("/urge")
    public AjaxResult urge()
    {
        return AjaxResult.success("催办完成", serviceDeskService.urgeOverdue());
    }
}
