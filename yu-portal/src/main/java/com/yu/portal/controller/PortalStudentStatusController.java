package com.yu.portal.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.sam.domain.SamStudent;
import com.yu.sam.domain.SamStatusChange;
import com.yu.sam.service.ISamStudentService;
import com.yu.sam.service.ISamStatusChangeService;

/**
 * 学籍服务门户Controller
 *
 * @author ruoyi
 * @date 2026-05-20
 */
@RestController
@RequestMapping("/portal/studentStatus")
public class PortalStudentStatusController extends BaseController
{
    @Autowired
    private ISamStudentService samStudentService;

    @Autowired
    private ISamStatusChangeService samStatusChangeService;

    /** 学生端：学籍信息查询 */
    @PreAuthorize("@ss.hasPermi('portal:status:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamStudent samStudent)
    {
        startPage();
        List<SamStudent> list = samStudentService.selectSamStudentList(samStudent);
        return getDataTable(list);
    }

    /** 学生端：学籍信息查询（前端别名） */
    @PreAuthorize("@ss.hasPermi('portal:status:list')")
    @GetMapping("/info")
    public AjaxResult info()
    {
        SamStudent query = new SamStudent();
        List<SamStudent> list = samStudentService.selectSamStudentList(query);
        if (list != null && !list.isEmpty()) {
            return success(list.get(0));
        }
        return success(new SamStudent());
    }

    /** 学生端：学籍异动申请 */
    @PreAuthorize("@ss.hasPermi('portal:status:change')")
    @Log(title = "学籍异动申请", businessType = BusinessType.INSERT)
    @PostMapping("/change")
    public AjaxResult change(@RequestBody SamStatusChange samStatusChange)
    {
        return toAjax(samStatusChangeService.insertSamStatusChange(samStatusChange));
    }

    /** 学生端：学籍异动申请（前端别名） */
    @PreAuthorize("@ss.hasPermi('portal:status:change')")
    @Log(title = "学籍异动申请", businessType = BusinessType.INSERT)
    @PostMapping("/apply")
    public AjaxResult apply(@RequestBody SamStatusChange samStatusChange)
    {
        return toAjax(samStatusChangeService.insertSamStatusChange(samStatusChange));
    }

    /** 学生端：学籍异动记录查询 */
    @PreAuthorize("@ss.hasPermi('portal:status:list')")
    @GetMapping("/changeList")
    public TableDataInfo changeList(SamStatusChange samStatusChange)
    {
        startPage();
        List<SamStatusChange> list = samStatusChangeService.selectSamStatusChangeList(samStatusChange);
        return getDataTable(list);
    }
}
