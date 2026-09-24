package com.yu.sam.controller;

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
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.sam.domain.SamRegistration;
import com.yu.sam.service.ISamRegistrationService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 学期注册与报到管理Controller（S8）
 *
 * @author ruoyi
 * @date 2026-09-23
 */
@RestController
@RequestMapping("/sam/registration")
public class SamRegistrationController extends BaseController
{
    @Autowired
    private ISamRegistrationService samRegistrationService;

    @PreAuthorize("@ss.hasPermi('sam:registration:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamRegistration samRegistration)
    {
        startPage();
        List<SamRegistration> list = samRegistrationService.selectSamRegistrationList(samRegistration);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('sam:registration:export')")
    @Log(title = "学期注册", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SamRegistration samRegistration)
    {
        List<SamRegistration> list = samRegistrationService.selectSamRegistrationList(samRegistration);
        ExcelUtil<SamRegistration> util = new ExcelUtil<SamRegistration>(SamRegistration.class);
        util.exportExcel(response, list, "学期注册数据");
    }

    @PreAuthorize("@ss.hasPermi('sam:registration:query')")
    @GetMapping(value = "/{registrationId}")
    public AjaxResult getInfo(@PathVariable("registrationId") Long registrationId)
    {
        return success(samRegistrationService.selectSamRegistrationByRegistrationId(registrationId));
    }

    /** 当前学期ID（供前端默认选中） */
    @PreAuthorize("@ss.hasPermi('sam:registration:list')")
    @GetMapping("/currentSemester")
    public AjaxResult currentSemester()
    {
        return success(samRegistrationService.getCurrentSemesterId());
    }

    /** 报到初始化：为指定学期全部在读学生生成未注册记录（幂等） */
    @PreAuthorize("@ss.hasPermi('sam:registration:init')")
    @Log(title = "学期注册", businessType = BusinessType.INSERT)
    @PostMapping("/init")
    public AjaxResult init(@RequestParam(required = false) Long semesterId)
    {
        int rows = samRegistrationService.initRegistration(semesterId, getUsername());
        return AjaxResult.success("报到初始化完成，新增 " + rows + " 条未注册记录", rows);
    }

    /** 批量注册办理：status=1已注册 / 2延迟注册 / 0撤销为未注册 */
    @PreAuthorize("@ss.hasPermi('sam:registration:register')")
    @Log(title = "学期注册", businessType = BusinessType.UPDATE)
    @PutMapping("/batchRegister")
    public AjaxResult batchRegister(@RequestBody RegisterForm form)
    {
        int rows = samRegistrationService.batchRegister(
                form.getRegistrationIds(), form.getStatus(), form.getDeferReason(), getUsername());
        return toAjax(rows);
    }

    @PreAuthorize("@ss.hasPermi('sam:registration:edit')")
    @Log(title = "学期注册", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SamRegistration samRegistration)
    {
        samRegistration.setUpdateBy(getUsername());
        return toAjax(samRegistrationService.updateSamRegistration(samRegistration));
    }

    @PreAuthorize("@ss.hasPermi('sam:registration:remove')")
    @Log(title = "学期注册", businessType = BusinessType.DELETE)
    @DeleteMapping("/{registrationIds}")
    public AjaxResult remove(@PathVariable Long[] registrationIds)
    {
        return toAjax(samRegistrationService.deleteSamRegistrationByRegistrationIds(registrationIds));
    }

    /** 注册情况总览 */
    @PreAuthorize("@ss.hasPermi('sam:registration:list')")
    @GetMapping("/stat/overview")
    public AjaxResult statOverview(@RequestParam(required = false) Long semesterId)
    {
        return success(samRegistrationService.statOverview(semesterId));
    }

    /** 按院系注册率统计 */
    @PreAuthorize("@ss.hasPermi('sam:registration:list')")
    @GetMapping("/stat/byDept")
    public AjaxResult statByDept(@RequestParam(required = false) Long semesterId)
    {
        return success(samRegistrationService.statByDept(semesterId));
    }

    /** 批量注册请求体 */
    public static class RegisterForm
    {
        private Long[] registrationIds;
        private String status;
        private String deferReason;

        public Long[] getRegistrationIds() { return registrationIds; }
        public void setRegistrationIds(Long[] registrationIds) { this.registrationIds = registrationIds; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getDeferReason() { return deferReason; }
        public void setDeferReason(String deferReason) { this.deferReason = deferReason; }
    }
}
