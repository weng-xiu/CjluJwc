package com.yu.tpm.controller;

import java.util.List;
import java.util.Map;
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
import com.yu.tpm.domain.TpmSelectionEnrollment;
import com.yu.tpm.domain.dto.ConflictWarning;
import com.yu.tpm.domain.dto.CourseSuggestion;
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
     * 执行抽签
     * T6：可选传入随机种子（seed），传入相同种子可复现同一抽签结果用于审计；不传则自动生成并记录。
     */
    @PreAuthorize("@ss.hasPermi('tpm:enroll:edit')")
    // V4.0 §7.3/A1：抽签属全量重算，全局限 3 次/分（集群多实例下的竞态由 A3 分布式锁收口）
    @RateLimiter(time = 60, count = 3)
    @PostMapping("/lottery/{roundId}")
    @Log(title = "选课抽签", businessType = BusinessType.UPDATE)
    public AjaxResult lottery(@PathVariable Long roundId,
                              @RequestParam(required = false) Long seed)
    {
        // A3：抽签为轮次级全量重算，加分布式锁防多实例并发重复抽取；锁在事务提交后（控制器层）释放
        String lockKey = "selection:lottery:" + roundId;
        String token = distributedLock.tryLock(lockKey, 0L, 30000L);
        if (token == null)
        {
            return error("该轮次抽签正在进行中，请稍后重试");
        }
        try
        {
            Map<String, Object> result = tpmSelectionEnrollmentService.runLottery(roundId, seed);
            return success(result);
        }
        finally
        {
            distributedLock.unlock(lockKey, token);
        }
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
        // A3：同一开课的候补递补需串行，加分布式锁避免并发超卖/重复递补
        String lockKey = "selection:promote:" + offeringId;
        String token = distributedLock.tryLock(lockKey, 0L, 20000L);
        if (token == null)
        {
            return error("该开课递补正在进行中，请稍后重试");
        }
        try
        {
            return success(tpmSelectionEnrollmentService.promoteWaitlist(offeringId));
        }
        finally
        {
            distributedLock.unlock(lockKey, token);
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
