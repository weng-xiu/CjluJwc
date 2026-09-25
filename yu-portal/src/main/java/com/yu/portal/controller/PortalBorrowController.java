package com.yu.portal.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.brm.domain.BrmClassroomBorrow;
import com.yu.brm.service.IBrmClassroomBorrowService;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.SecurityUtils;
import com.yu.tpm.service.IBrmBorrowFlowService;
import jakarta.validation.constraints.NotNull;

/**
 * 门户端教室借用申请控制器（B1）
 *
 * 防越权：applicantUserId 强制绑定登录用户；查询/撤销/追溯均校验归属。
 *
 * @author yu
 * @date 2026-09-21
 */
@RestController
@RequestMapping("/portal/borrow")
public class PortalBorrowController extends BaseController
{
    @Autowired
    private IBrmClassroomBorrowService brmClassroomBorrowService;

    @Autowired
    private IBrmBorrowFlowService brmBorrowFlowService;

    @Autowired
    private com.yu.brm.service.IBrmClassroomService brmClassroomService;

    /** 可选教室列表（正常状态，供申请表单下拉，避免门户直连管理端接口403） */
    @PreAuthorize("@ss.hasPermi('portal:borrow:list') and @ss.hasAnyRoles('admin,teacher')")
    @GetMapping("/classrooms")
    public TableDataInfo classrooms(com.yu.brm.domain.BrmClassroom query)
    {
        query.setStatus("0");
        startPage();
        List<com.yu.brm.domain.BrmClassroom> list = brmClassroomService.selectBrmClassroomList(query);
        return getDataTable(list);
    }

    /** 教室占用查询（排课+在用借用，供申请前查看空闲） */
    @PreAuthorize("@ss.hasPermi('portal:borrow:list') and @ss.hasAnyRoles('admin,teacher')")
    @GetMapping("/occupancy")
    public AjaxResult occupancy(@org.springframework.web.bind.annotation.RequestParam("classroomId") Long classroomId,
                                @org.springframework.web.bind.annotation.RequestParam(value = "beginDate", required = false)
                                @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") java.util.Date beginDate,
                                @org.springframework.web.bind.annotation.RequestParam(value = "endDate", required = false)
                                @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") java.util.Date endDate)
    {
        return success(brmClassroomBorrowService.classroomOccupancy(classroomId, beginDate, endDate));
    }

    /** 我的借用申请列表（按登录用户过滤） */
    @PreAuthorize("@ss.hasPermi('portal:borrow:list') and @ss.hasAnyRoles('admin,teacher')")
    @GetMapping("/myList")
    public TableDataInfo myList(BrmClassroomBorrow query)
    {
        query.setApplicantUserId(getUserId());
        query.setApplicant(null);
        startPage();
        List<BrmClassroomBorrow> list = brmClassroomBorrowService.selectBrmClassroomBorrowList(query);
        return getDataTable(list);
    }

    /** 提交教室借用申请：登记 + 即时启动院系->教务处两级审批（含冲突校验） */
    @PreAuthorize("@ss.hasPermi('portal:borrow:add') and @ss.hasAnyRoles('admin,teacher')")
    @Log(title = "教室借用申请", businessType = BusinessType.INSERT)
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/apply")
    public AjaxResult apply(@RequestBody BrmClassroomBorrow borrow)
    {
        if (borrow.getClassroomId() == null || borrow.getBorrowDate() == null)
        {
            throw new ServiceException("教室与借用日期不能为空");
        }
        borrow.setBorrowId(null);
        borrow.setApplicantUserId(getUserId());
        borrow.setApplicant(getLoginUser().getUser().getNickName());
        borrow.setApplicantDept(getLoginUser().getUser().getDept() != null
                ? getLoginUser().getUser().getDept().getDeptName() : null);
        borrow.setApproveStatus("0");
        borrow.setCreateBy(getUsername());
        brmClassroomBorrowService.insertBrmClassroomBorrow(borrow);
        // 冲突校验 + 启动流程（冲突时抛异常，申请不落流程）
        brmBorrowFlowService.submit(borrow.getBorrowId());
        return success(brmClassroomBorrowService.selectBrmClassroomBorrowByBorrowId(borrow.getBorrowId()));
    }

    /** 撤销我的审批中申请 */
    @PreAuthorize("@ss.hasPermi('portal:borrow:add') and @ss.hasAnyRoles('admin,teacher')")
    @Log(title = "教室借用撤销", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{borrowId}")
    public AjaxResult cancel(@PathVariable("borrowId") Long borrowId)
    {
        checkOwned(borrowId);
        brmBorrowFlowService.cancel(borrowId);
        return success();
    }

    /** 我的申请审批进度/全流程追溯 */
    @PreAuthorize("@ss.hasPermi('portal:borrow:list') and @ss.hasAnyRoles('admin,teacher')")
    @GetMapping("/trace/{borrowId}")
    public AjaxResult trace(@PathVariable("borrowId") Long borrowId)
    {
        checkOwned(borrowId);
        return success(brmBorrowFlowService.trace(borrowId));
    }

    /** 提交前冲突预检（教室占用日历同端点复用管理版，只读） */
    @PreAuthorize("@ss.hasPermi('portal:borrow:add') and @ss.hasAnyRoles('admin,teacher')")
    @GetMapping("/checkConflict")
    public AjaxResult checkConflict(@NotNull @org.springframework.web.bind.annotation.RequestParam("classroomId") Long classroomId,
                                    @org.springframework.web.bind.annotation.RequestParam("borrowDate")
                                    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") java.util.Date borrowDate,
                                    @org.springframework.web.bind.annotation.RequestParam(value = "startTime", required = false) String startTime,
                                    @org.springframework.web.bind.annotation.RequestParam(value = "endTime", required = false) String endTime)
    {
        return success(brmClassroomBorrowService.checkConflict(classroomId, borrowDate, startTime, endTime, null));
    }

    /** 归属校验：只能操作本人提交的申请（admin 可代管） */
    private void checkOwned(Long borrowId)
    {
        BrmClassroomBorrow borrow = brmClassroomBorrowService.selectBrmClassroomBorrowByBorrowId(borrowId);
        if (borrow == null)
        {
            throw new ServiceException("借用申请不存在");
        }
        if (!getUserId().equals(borrow.getApplicantUserId()) && !SecurityUtils.isAdmin(getUserId()))
        {
            throw new ServiceException("无权操作他人的借用申请");
        }
    }
}
