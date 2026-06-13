package com.yu.web.controller.system;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.system.service.IAccountSyncService;

/**
 * 账号同步管理Controller
 */
@RestController
@RequestMapping("/system/account")
public class AccountSyncController extends BaseController
{
    @Autowired
    private IAccountSyncService accountSyncService;

    /**
     * 从BRM同步教师账号
     */
    @PreAuthorize("@ss.hasPermi('system:account:syncTeacher')")
    @Log(title = "账号同步", businessType = BusinessType.INSERT)
    @PostMapping("/sync/teachers")
    public AjaxResult syncTeachers(@RequestParam(required = false) Long deptId)
    {
        Map<String, Integer> result = accountSyncService.syncTeacherAccounts(deptId);
        return success(result);
    }

    /**
     * 从BRM同步学生账号
     */
    @PreAuthorize("@ss.hasPermi('system:account:syncStudent')")
    @Log(title = "账号同步", businessType = BusinessType.INSERT)
    @PostMapping("/sync/students")
    public AjaxResult syncStudents(@RequestParam(required = false) Long classId)
    {
        Map<String, Integer> result = accountSyncService.syncStudentAccounts(classId);
        return success(result);
    }
}
