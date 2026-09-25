package com.yu.portal.controller;

import java.util.Arrays;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.SecurityUtils;
import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.domain.TpmScheduleAdjustment;
import com.yu.tpm.service.ITpmCourseOfferingService;
import com.yu.tpm.service.ITpmScheduleAdjustmentService;
import com.yu.tpm.service.ITpmScheduleService;

/**
 * 调停课申请门户Controller（教师端，P6：申请—审批—进度查询—结果通知闭环）
 *
 * 防越权：列表/撤销强制绑定登录教师本人；申请仅可针对本人任课排课。
 *
 * @author ruoyi
 * @date 2026-05-20
 */
@RestController
@RequestMapping("/portal/adjustment")
public class PortalAdjustmentController extends BaseController
{
    /** 允许的调课类型（字典 tpm_adjust_type：1调课 2停课 3补课） */
    private static final List<String> ALLOWED_ADJUST_TYPES = Arrays.asList("1", "2", "3");

    @Autowired
    private ITpmScheduleAdjustmentService tpmScheduleAdjustmentService;

    @Autowired
    private ITpmScheduleService tpmScheduleService;

    @Autowired
    private ITpmCourseOfferingService tpmCourseOfferingService;

    /** 调停课审批人（与 TpmScheduleAdjustmentServiceImpl 同配置键，P5 移动端审批） */
    @Value("${tpm.adjust.approver:admin}")
    private String adjustApprover;

    /** 教师端：调停课申请列表查询（强制本人，进度含审批状态/意见/时间） */
    @PreAuthorize("@ss.hasPermi('portal:adjustment:list') and @ss.hasAnyRoles('admin,teacher')")
    @GetMapping("/list")
    public TableDataInfo list(TpmScheduleAdjustment tpmScheduleAdjustment)
    {
        if (!SecurityUtils.isAdmin(getUserId()))
        {
            tpmScheduleAdjustment.setCreateByExact(getUsername());
        }
        startPage();
        List<TpmScheduleAdjustment> list = tpmScheduleAdjustmentService.selectTpmScheduleAdjustmentList(tpmScheduleAdjustment);
        return getDataTable(list);
    }

    /** 教师端：本人任课排课列表（调停课申请选择源，按登录教师过滤） */
    @PreAuthorize("@ss.hasPermi('portal:adjustment:list') and @ss.hasAnyRoles('admin,teacher')")
    @GetMapping("/mySchedules")
    public TableDataInfo mySchedules(@RequestParam(required = false) Long semesterId)
    {
        TpmSchedule query = new TpmSchedule();
        query.setTeacherId(getUserId());
        query.setSemesterId(semesterId);
        query.setStatus("0");
        startPage();
        List<TpmSchedule> list = tpmScheduleService.selectTpmScheduleList(query);
        return getDataTable(list);
    }

    /** 教师端：提交调停课申请（绑定本人任课排课，提交即产生审批待办） */
    @PreAuthorize("@ss.hasPermi('portal:adjustment:add') and @ss.hasAnyRoles('admin,teacher')")
    @Log(title = "调停课申请", businessType = BusinessType.INSERT)
    @PostMapping("/apply")
    public AjaxResult add(@RequestBody TpmScheduleAdjustment tpmScheduleAdjustment)
    {
        checkScheduleOwned(tpmScheduleAdjustment.getScheduleId());
        String adjustType = tpmScheduleAdjustment.getAdjustType();
        if (adjustType == null || !ALLOWED_ADJUST_TYPES.contains(adjustType))
        {
            throw new ServiceException("调课类型不合法（1调课 2停课 3补课）");
        }
        if (tpmScheduleAdjustment.getReason() == null || tpmScheduleAdjustment.getReason().trim().isEmpty())
        {
            throw new ServiceException("申请原因不能为空");
        }
        tpmScheduleAdjustment.setAdjustId(null);
        tpmScheduleAdjustment.setApplicant(getLoginUser().getUser().getNickName());
        tpmScheduleAdjustment.setCreateBy(getUsername());
        tpmScheduleAdjustment.setApproveStatus("0");
        tpmScheduleAdjustmentService.insertTpmScheduleAdjustment(tpmScheduleAdjustment);
        return success(tpmScheduleAdjustmentService.selectTpmScheduleAdjustmentByAdjustId(tpmScheduleAdjustment.getAdjustId()));
    }

    /** 教师端：撤销本人待审的调停课申请 */
    @PreAuthorize("@ss.hasPermi('portal:adjustment:add') and @ss.hasAnyRoles('admin,teacher')")
    @Log(title = "调停课撤销", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{adjustId}")
    public AjaxResult cancel(@PathVariable("adjustId") Long adjustId)
    {
        checkOwned(adjustId);
        return toAjax(tpmScheduleAdjustmentService.cancelByApplicant(adjustId, getUsername()));
    }

    /** 审批人：待审调停课列表（P5 移动端审批，仅配置的审批人/admin 可用） */
    @GetMapping("/pendingList")
    public TableDataInfo pendingList(TpmScheduleAdjustment query)
    {
        checkApprover();
        query.setApproveStatus("0");
        startPage();
        List<TpmScheduleAdjustment> list = tpmScheduleAdjustmentService.selectTpmScheduleAdjustmentList(query);
        return getDataTable(list);
    }

    /** 审批人：通过（同步回写待办/消息通知申请人） */
    @Log(title = "调停课审批通过", businessType = BusinessType.UPDATE)
    @PostMapping("/approve/{adjustId}")
    public AjaxResult approve(@PathVariable("adjustId") Long adjustId,
                              @RequestBody(required = false) TpmScheduleAdjustment body)
    {
        checkApprover();
        tpmScheduleAdjustmentService.approve(adjustId, body != null ? body.getApproveComment() : null);
        return success();
    }

    /** 审批人：驳回 */
    @Log(title = "调停课审批驳回", businessType = BusinessType.UPDATE)
    @PostMapping("/reject/{adjustId}")
    public AjaxResult reject(@PathVariable("adjustId") Long adjustId,
                             @RequestBody(required = false) TpmScheduleAdjustment body)
    {
        checkApprover();
        tpmScheduleAdjustmentService.reject(adjustId, body != null ? body.getApproveComment() : null);
        return success();
    }

    /** 审批人校验：仅配置的 tpm.adjust.approver 或 admin 可执行审批（不用权限字符，避免门户角色 403） */
    private void checkApprover()
    {
        if (!SecurityUtils.isAdmin(getUserId()) && !getUsername().equals(adjustApprover))
        {
            throw new ServiceException("仅调停课审批人可执行审批操作");
        }
    }

    /** 归属校验：只能操作本人的调停课申请（admin 可代管） */
    private void checkOwned(Long adjustId)
    {
        TpmScheduleAdjustment adjustment = tpmScheduleAdjustmentService.selectTpmScheduleAdjustmentByAdjustId(adjustId);
        if (adjustment == null)
        {
            throw new ServiceException("调停课申请不存在");
        }
        if (!getUsername().equals(adjustment.getCreateBy()) && !SecurityUtils.isAdmin(getUserId()))
        {
            throw new ServiceException("无权操作他人的调停课申请");
        }
    }

    /** 排课归属校验：仅可针对本人任课的排课发起调停课（admin 豁免） */
    private void checkScheduleOwned(Long scheduleId)
    {
        if (SecurityUtils.isAdmin(getUserId()))
        {
            return;
        }
        if (scheduleId == null)
        {
            throw new ServiceException("排课不能为空");
        }
        TpmSchedule schedule = tpmScheduleService.selectTpmScheduleByScheduleId(scheduleId);
        if (schedule == null)
        {
            throw new ServiceException("排课记录不存在");
        }
        TpmCourseOffering offering = tpmCourseOfferingService.selectTpmCourseOfferingByOfferingId(schedule.getOfferingId());
        if (offering == null || !getUserId().equals(offering.getTeacherId()))
        {
            throw new ServiceException("只能对本人任课的排课发起调停课申请");
        }
    }
}
