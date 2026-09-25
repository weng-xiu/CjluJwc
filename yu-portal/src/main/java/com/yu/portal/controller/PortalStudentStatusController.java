package com.yu.portal.controller;

import java.util.Arrays;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.SecurityUtils;
import com.yu.sam.domain.SamStatusChange;
import com.yu.sam.domain.SamStudent;
import com.yu.sam.service.ISamStatusChangeService;
import com.yu.sam.service.ISamStudentService;

/**
 * 学生学籍门户Controller（P6：申请—审批—进度查询—结果通知闭环）
 *
 * 防越权：studentId 强制绑定登录用户；查询/撤销/追溯均校验归属。
 *
 * @author ruoyi
 * @date 2026-05-20
 */
@RestController
@RequestMapping("/portal/studentStatus")
public class PortalStudentStatusController extends BaseController
{
    /** 门户端允许的异动类型（0休学 1复学 3退学），其余类型由管理员在管理端登记 */
    private static final List<String> PORTAL_CHANGE_TYPES = Arrays.asList("0", "1", "3");

    @Autowired
    private ISamStudentService samStudentService;

    @Autowired
    private ISamStatusChangeService samStatusChangeService;

    /** 学生端：学籍信息查询（强制本人，附院系/专业/班级名称） */
    @PreAuthorize("@ss.hasPermi('portal:status:list') and @ss.hasAnyRoles('admin,student')")
    @GetMapping("/list")
    public TableDataInfo list(SamStudent samStudent)
    {
        SamStudent mine = resolveMyStudent();
        if (mine == null)
        {
            return getDataTable(java.util.Collections.emptyList());
        }
        return getDataTable(Arrays.asList(mine));
    }

    /** 学生端：学籍信息查询（前端别名） */
    @PreAuthorize("@ss.hasPermi('portal:status:list') and @ss.hasAnyRoles('admin,student')")
    @GetMapping("/info")
    public AjaxResult info()
    {
        SamStudent mine = resolveMyStudent();
        return success(mine != null ? mine : new SamStudent());
    }

    /** 学生端：学籍异动申请（登记并即时提交进入院系->教务处多级审批流程） */
    @PreAuthorize("@ss.hasPermi('portal:status:change') and @ss.hasAnyRoles('admin,student')")
    @Log(title = "学籍异动申请", businessType = BusinessType.INSERT)
    @PostMapping("/change")
    public AjaxResult change(@RequestBody SamStatusChange samStatusChange)
    {
        return doApply(samStatusChange);
    }

    /** 学生端：学籍异动申请（前端别名） */
    @PreAuthorize("@ss.hasPermi('portal:status:change') and @ss.hasAnyRoles('admin,student')")
    @Log(title = "学籍异动申请", businessType = BusinessType.INSERT)
    @PostMapping("/apply")
    public AjaxResult apply(@RequestBody SamStatusChange samStatusChange)
    {
        return doApply(samStatusChange);
    }

    /** 学生端：学籍异动记录查询（强制本人，不走数据权限过滤） */
    @PreAuthorize("@ss.hasPermi('portal:status:list') and @ss.hasAnyRoles('admin,student')")
    @GetMapping("/changeList")
    public TableDataInfo changeList(SamStatusChange samStatusChange)
    {
        SamStudent mine = resolveMyStudent();
        if (mine == null)
        {
            return getDataTable(java.util.Collections.emptyList());
        }
        samStatusChange.setStudentId(mine.getStudentId());
        // 门户端仅展示本人提交的申请
        samStatusChange.setCreateByExact(getUsername());
        startPage();
        List<SamStatusChange> list = samStatusChangeService.selectMyStatusChangeList(samStatusChange);
        return getDataTable(list);
    }

    /** 学生端：我的异动申请审批进度追溯（Flowable 历史任务链） */
    @PreAuthorize("@ss.hasPermi('portal:status:list') and @ss.hasAnyRoles('admin,student')")
    @GetMapping("/trace/{changeId}")
    public AjaxResult trace(@PathVariable("changeId") Long changeId)
    {
        checkOwned(changeId);
        return success(samStatusChangeService.traceChange(changeId));
    }

    /** 学生端：撤销本人审批中的异动申请（含流程实例撤销） */
    @PreAuthorize("@ss.hasPermi('portal:status:change') and @ss.hasAnyRoles('admin,student')")
    @Log(title = "学籍异动撤销", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{changeId}")
    public AjaxResult cancel(@PathVariable("changeId") Long changeId)
    {
        checkOwned(changeId);
        return toAjax(samStatusChangeService.cancelByApplicant(changeId, getUsername()));
    }

    /** 申请登记 + 提交流程（S5 已接入 Flowable，门户提交即入流程） */
    private AjaxResult doApply(SamStatusChange samStatusChange)
    {
        SamStudent mine = resolveMyStudent();
        if (mine == null)
        {
            throw new ServiceException("未找到您的学籍信息，请联系教务处");
        }
        String changeType = samStatusChange.getChangeType();
        if (changeType == null || !PORTAL_CHANGE_TYPES.contains(changeType))
        {
            throw new ServiceException("门户端仅支持休学/复学/退学申请，其他异动请联系教务办办理");
        }
        if (samStatusChange.getReason() == null || samStatusChange.getReason().trim().isEmpty())
        {
            throw new ServiceException("申请原因不能为空");
        }
        samStatusChange.setChangeId(null);
        samStatusChange.setStudentId(mine.getStudentId());
        samStatusChange.setOriginalStatus(mine.getStudentStatus());
        samStatusChange.setNewStatus(null);
        samStatusChange.setApplicant(getLoginUser().getUser().getNickName());
        samStatusChange.setCreateBy(getUsername());
        if (samStatusChange.getChangeDate() == null)
        {
            samStatusChange.setChangeDate(DateUtils.getNowDate());
        }
        samStatusChangeService.insertSamStatusChange(samStatusChange);
        // 启动院系->教务处多级审批流程并向首级审批人推送待办（校验失败抛异常，申请保留为待审）
        samStatusChangeService.submitForApproval(samStatusChange.getChangeId());
        return success(samStatusChangeService.selectSamStatusChangeByChangeId(samStatusChange.getChangeId()));
    }

    /** 归属校验：只能操作本人的异动申请（admin 可代管） */
    private void checkOwned(Long changeId)
    {
        SamStatusChange change = samStatusChangeService.selectSamStatusChangeByChangeId(changeId);
        if (change == null)
        {
            throw new ServiceException("异动申请不存在");
        }
        SamStudent mine = resolveMyStudent();
        boolean owned = mine != null && mine.getStudentId().equals(change.getStudentId());
        if (!owned && !SecurityUtils.isAdmin(getUserId()))
        {
            throw new ServiceException("无权操作他人的异动申请");
        }
    }

    /** 解析本人学籍：先按 user_id 关联，再兼容 student_id==user_id 口径 */
    private SamStudent resolveMyStudent()
    {
        SamStudent mine = samStudentService.selectSamStudentByUserId(getUserId());
        if (mine == null)
        {
            mine = samStudentService.selectSamStudentByStudentId(getUserId());
        }
        return mine;
    }
}
