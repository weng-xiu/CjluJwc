package com.yu.tpm.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.annotation.RateLimiter;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.common.enums.LimitType;
import com.yu.tpm.domain.TpmLotteryTask;
import com.yu.tpm.domain.TpmSelectionEnrollment;
import com.yu.tpm.domain.dto.ConflictWarning;
import com.yu.tpm.domain.dto.CourseSuggestion;
import com.yu.tpm.service.ITpmLotteryTaskService;
import com.yu.tpm.service.ITpmSelectionEnrollmentService;
import com.yu.framework.cache.DistributedLock;
import com.yu.framework.cache.SelectionAdmission;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 选课名单Controller
 *
 * @author ruoyi
 * @date 2026-05-09
 */
@RestController
@RequestMapping("/tpm/enroll")
public class TpmSelectionEnrollmentController extends BaseController
{
    @Autowired
    private ITpmSelectionEnrollmentService tpmSelectionEnrollmentService;

    /** A3：多实例下保护抽签/递补等全量重算写路径的竞态 */
    @Autowired
    private DistributedLock distributedLock;

    /** T-5：抽签任务表（提交异步任务、查询进度） */
    @Autowired
    private ITpmLotteryTaskService tpmLotteryTaskService;

    /** A1：选课尖峰削峰与排队号发号 */
    @Autowired
    private SelectionAdmission selectionAdmission;

    @PreAuthorize("@ss.hasPermi('tpm:enroll:list')")
    @GetMapping("/list")
    public TableDataInfo list(TpmSelectionEnrollment tpmSelectionEnrollment)
    {
        startPage();
        List<TpmSelectionEnrollment> list = tpmSelectionEnrollmentService.selectTpmSelectionEnrollmentList(tpmSelectionEnrollment);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('tpm:enroll:export')")
    @Log(title = "选课名单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TpmSelectionEnrollment tpmSelectionEnrollment)
    {
        List<TpmSelectionEnrollment> list = tpmSelectionEnrollmentService.selectTpmSelectionEnrollmentList(tpmSelectionEnrollment);
        ExcelUtil<TpmSelectionEnrollment> util = new ExcelUtil<TpmSelectionEnrollment>(TpmSelectionEnrollment.class);
        util.exportExcel(response, list, "选课名单数据");
    }

    @PreAuthorize("@ss.hasPermi('tpm:enroll:query')")
    @GetMapping(value = "/{enrollId}")
    public AjaxResult getInfo(@PathVariable("enrollId") Long enrollId)
    {
        return success(tpmSelectionEnrollmentService.selectTpmSelectionEnrollmentByEnrollId(enrollId));
    }

    @PreAuthorize("@ss.hasPermi('tpm:enroll:add')")
    @Log(title = "选课名单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TpmSelectionEnrollment tpmSelectionEnrollment)
    {
        return toAjax(tpmSelectionEnrollmentService.insertTpmSelectionEnrollment(tpmSelectionEnrollment));
    }

    @PreAuthorize("@ss.hasPermi('tpm:enroll:edit')")
    @Log(title = "选课名单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TpmSelectionEnrollment tpmSelectionEnrollment)
    {
        return toAjax(tpmSelectionEnrollmentService.updateTpmSelectionEnrollment(tpmSelectionEnrollment));
    }

    @PreAuthorize("@ss.hasPermi('tpm:enroll:remove')")
    @Log(title = "选课名单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{enrollIds}")
    public AjaxResult remove(@PathVariable Long[] enrollIds)
    {
        return toAjax(tpmSelectionEnrollmentService.deleteTpmSelectionEnrollmentByEnrollIds(enrollIds));
    }

    /**
     * 选课冲突检测
     */
    @PreAuthorize("@ss.hasPermi('tpm:selection:validate')")
    @PostMapping("/validate")
    @Log(title = "选课冲突检测", businessType = BusinessType.OTHER)
    public AjaxResult validateSelection(@RequestBody TpmSelectionEnrollment enrollment)
    {
        List<ConflictWarning> warnings = tpmSelectionEnrollmentService
                .checkSelectionConflicts(enrollment.getStudentId(), enrollment.getCourseOfferingId(), enrollment.getRoundId());
        return success(warnings);
    }

    /**
     * 获取替代课程建议
     */
    @PreAuthorize("@ss.hasPermi('tpm:selection:suggest')")
    @GetMapping("/suggestions/{courseOfferingId}")
    public AjaxResult getAlternatives(@PathVariable Long courseOfferingId, @RequestParam Long studentId, @RequestParam Long roundId)
    {
        List<CourseSuggestion> suggestions = tpmSelectionEnrollmentService
                .getAlternativeCourses(studentId, courseOfferingId, roundId);
        return success(suggestions);
    }

    /**
     * 带验证的选课（含冲突检测+Redis并发控制）
     */
    @PreAuthorize("@ss.hasPermi('tpm:selection:enroll')")
    // V4.0 §7.3/A1：选课尖峰入站限流（IP 维度 30 次/分）
    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @PostMapping("/enrollWithValidation")
    @Log(title = "带验证选课", businessType = BusinessType.INSERT)
    public AjaxResult enrollWithValidation(@RequestBody TpmSelectionEnrollment enrollment)
    {
        // A1：削峰闸门——超过平滑速率的请求在此被快速拒绝并领取排队号，不触达 DB（容量超卖仍由下游原子扣减+分布式锁兜底）
        SelectionAdmission.Admission admission = selectionAdmission.tryAdmit(enrollment.getRoundId());
        if (!admission.isAllowed())
        {
            AjaxResult busy = error("当前选课人数较多，已进入削峰排队，您的排队号为 " + admission.getQueueNumber() + "，请稍后重试");
            busy.put("queueNumber", admission.getQueueNumber());
            busy.put("queued", true);
            return busy;
        }
        // F2-2：支持携带志愿优先级（priority），供轮次 weighted 抽签模式按志愿权重中签
        AjaxResult result = tpmSelectionEnrollmentService
                .enrollWithValidation(enrollment.getStudentId(), enrollment.getCourseOfferingId(),
                        enrollment.getRoundId(), enrollment.getPriority());
        result.put("queueNumber", admission.getQueueNumber());
        return result;
    }

    /**
     * F2-2 退改选窗口：将已选课程改选为另一开课（需轮次开放退改选且在窗口时间内）。
     */
    @PreAuthorize("@ss.hasPermi('tpm:selection:enroll')")
    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @PostMapping("/swap")
    @Log(title = "选课退改选", businessType = BusinessType.UPDATE)
    public AjaxResult swapCourse(@RequestParam Long enrollId, @RequestParam Long newOfferingId)
    {
        return tpmSelectionEnrollmentService.swapCourse(enrollId, newOfferingId);
    }

    /**
     * 执行抽签（T-5：同步请求 → 异步任务）
     *
     * <p>契约变更：本端不再在请求内跑完抽签（万级记录的批量落库会长时间挂起连接），
     * 而是落一条任务行并立即返回 taskId；执行结果与进度请查
     * {@code GET /tpm/enroll/lottery/task/{taskId}} 或 {@code /lottery/task/round/{roundId}}。</p>
     *
     * <p>T6：可选传入随机种子（seed），传入相同种子可复现同一抽签结果用于审计；不传则执行阶段自动生成。</p>
     */
    @PreAuthorize("@ss.hasPermi('tpm:enroll:edit')")
    // V4.0 §7.3/A1：抽签属全量重算，全局限 3 次/分（重复提交另由任务表唯一索引挡住）
    @RateLimiter(time = 60, count = 3)
    @PostMapping("/lottery/{roundId}")
    @Log(title = "选课抽签", businessType = BusinessType.UPDATE)
    public AjaxResult lottery(@PathVariable Long roundId,
                              @RequestParam(required = false) Long seed)
    {
        TpmLotteryTask task = tpmLotteryTaskService.submitLotteryTask(roundId, seed);
        if (TpmLotteryTask.STATUS_FAILED.equals(task.getTaskStatus()))
        {
            // 提交即失败（线程池拒绝）：任务行已置失败，把原因直接给前端，避免“看起来提交成功却永远不动”
            return AjaxResult.error(task.getErrorMsg());
        }
        return AjaxResult.success(
                "抽签任务已提交（任务号 " + task.getTaskId() + "），可在当前页查看进度", task);
    }

    /**
     * T-5：按任务号查询抽签进度
     */
    @PreAuthorize("@ss.hasPermi('tpm:enroll:list') or @ss.hasPermi('tpm:enroll:edit')")
    @GetMapping("/lottery/task/{taskId}")
    public AjaxResult lotteryTask(@PathVariable Long taskId)
    {
        TpmLotteryTask task = tpmLotteryTaskService.selectTpmLotteryTaskByTaskId(taskId);
        if (task == null)
        {
            return error("抽签任务不存在");
        }
        return success(task);
    }

    /**
     * T-5：查询轮次最近一次抽签任务（前端刷新页面后仍能续上进度）
     */
    @PreAuthorize("@ss.hasPermi('tpm:enroll:list') or @ss.hasPermi('tpm:enroll:edit')")
    @GetMapping("/lottery/task/round/{roundId}")
    public AjaxResult latestLotteryTask(@PathVariable Long roundId)
    {
        // 从未提交过时返回 null，前端据此判断“还没抽过签”
        return success(tpmLotteryTaskService.selectLatestTaskByRoundId(roundId));
    }

    /**
     * T6：候补递补——按候补排名将落选学生递补至空余容量
     */
    @PreAuthorize("@ss.hasPermi('tpm:enroll:edit')")
    @RateLimiter(time = 60, count = 6)
    @PostMapping("/promoteWaitlist/{offeringId}")
    @Log(title = "选课候补递补", businessType = BusinessType.UPDATE)
    public AjaxResult promoteWaitlist(@PathVariable Long offeringId)
    {
        // A3：同一开课的候补递补需串行；T-5：改用带看门狗的句柄，递补耗时超过租约也不会丢锁
        String lockKey = "selection:promote:" + offeringId;
        try (DistributedLock.LockHandle handle = distributedLock.tryAcquire(lockKey, 0L, 20000L))
        {
            if (handle == null)
            {
                return error("该开课递补正在进行中，请稍后重试");
            }
            return success(tpmSelectionEnrollmentService.promoteWaitlist(offeringId));
        }
    }

    /**
     * 学生退课
     */
    @PreAuthorize("@ss.hasPermi('tpm:enroll:edit')")
    @PostMapping("/drop/{enrollId}")
    @Log(title = "学生退课", businessType = BusinessType.UPDATE)
    public AjaxResult drop(@PathVariable Long enrollId)
    {
        return tpmSelectionEnrollmentService.dropCourse(enrollId);
    }
}
