package com.yu.tpm.controller;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yu.brm.service.IBrmClassroomBorrowService;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.tpm.service.IBrmBorrowFlowService;

/**
 * 教室借用审批流程控制器（B1：Flowable 两级审批 + 冲突校验 + 占用日历 + 全流程追溯）
 *
 * 借用数据 CRUD 仍归属 yu-brm 的 /brm/borrow；本控制器只做流程编排与只读分析。
 *
 * @author yu
 * @date 2026-09-21
 */
@RestController
@RequestMapping("/brm/borrowFlow")
public class BrmBorrowFlowController extends BaseController
{
    @Autowired
    private IBrmBorrowFlowService brmBorrowFlowService;

    @Autowired
    private IBrmClassroomBorrowService brmClassroomBorrowService;

    /** 提交借用申请，启动院系->教务处两级审批流程 */
    @PreAuthorize("@ss.hasPermi('brm:borrow:submit')")
    @Log(title = "教室借用提交审批", businessType = BusinessType.UPDATE)
    @PostMapping("/submit/{borrowId}")
    public AjaxResult submit(@PathVariable("borrowId") Long borrowId)
    {
        return success(brmBorrowFlowService.submit(borrowId));
    }

    /** 院系初审 */
    @PreAuthorize("@ss.hasPermi('brm:borrow:approve')")
    @Log(title = "教室借用院系审批", businessType = BusinessType.UPDATE)
    @PutMapping("/deptApprove/{borrowId}")
    public AjaxResult deptApprove(@PathVariable("borrowId") Long borrowId,
                                  @RequestBody Map<String, Object> body)
    {
        boolean approved = Boolean.parseBoolean(String.valueOf(body.get("approved")));
        String opinion = body.get("opinion") != null ? String.valueOf(body.get("opinion")) : null;
        brmBorrowFlowService.deptApprove(borrowId, approved, opinion);
        return success();
    }

    /** 教务处终审 */
    @PreAuthorize("@ss.hasPermi('brm:borrow:approve')")
    @Log(title = "教室借用教务处审批", businessType = BusinessType.UPDATE)
    @PutMapping("/aaApprove/{borrowId}")
    public AjaxResult aaApprove(@PathVariable("borrowId") Long borrowId,
                                @RequestBody Map<String, Object> body)
    {
        boolean approved = Boolean.parseBoolean(String.valueOf(body.get("approved")));
        String opinion = body.get("opinion") != null ? String.valueOf(body.get("opinion")) : null;
        brmBorrowFlowService.aaApprove(borrowId, approved, opinion);
        return success();
    }

    /** 撤销审批中的借用申请 */
    @PreAuthorize("@ss.hasPermi('brm:borrow:submit')")
    @Log(title = "教室借用撤销", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{borrowId}")
    public AjaxResult cancel(@PathVariable("borrowId") Long borrowId)
    {
        brmBorrowFlowService.cancel(borrowId);
        return success();
    }

    /** 借用冲突校验（提交前预检）：返回冲突描述列表，空表示可借 */
    @PreAuthorize("@ss.hasPermi('brm:borrow:list')")
    @GetMapping("/checkConflict")
    public AjaxResult checkConflict(@RequestParam("classroomId") Long classroomId,
                                    @RequestParam("borrowDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date borrowDate,
                                    @RequestParam(value = "startTime", required = false) String startTime,
                                    @RequestParam(value = "endTime", required = false) String endTime,
                                    @RequestParam(value = "excludeBorrowId", required = false) Long excludeBorrowId)
    {
        List<String> conflicts = brmClassroomBorrowService.checkConflict(classroomId, borrowDate, startTime, endTime, excludeBorrowId);
        return success(conflicts);
    }

    /** 教室占用日历：区间内借用单占用 + 每周固定排课占用 */
    @PreAuthorize("@ss.hasPermi('brm:borrow:list')")
    @GetMapping("/occupancy")
    public AjaxResult occupancy(@RequestParam("classroomId") Long classroomId,
                                @RequestParam(value = "beginDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date beginDate,
                                @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate)
    {
        return success(brmClassroomBorrowService.classroomOccupancy(classroomId, beginDate, endDate));
    }

    /** 全流程追溯：Flowable 历史任务链（含各节点办理人/时间/审批意见） */
    @PreAuthorize("@ss.hasPermi('brm:borrow:list')")
    @GetMapping("/trace/{borrowId}")
    public AjaxResult trace(@PathVariable("borrowId") Long borrowId)
    {
        return success(brmBorrowFlowService.trace(borrowId));
    }
}
