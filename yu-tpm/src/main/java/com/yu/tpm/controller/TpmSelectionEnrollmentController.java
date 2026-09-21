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
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.tpm.domain.TpmSelectionEnrollment;
import com.yu.tpm.domain.dto.ConflictWarning;
import com.yu.tpm.domain.dto.CourseSuggestion;
import com.yu.tpm.service.ITpmSelectionEnrollmentService;
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
    @PostMapping("/enrollWithValidation")
    @Log(title = "带验证选课", businessType = BusinessType.INSERT)
    public AjaxResult enrollWithValidation(@RequestBody TpmSelectionEnrollment enrollment)
    {
        return tpmSelectionEnrollmentService
                .enrollWithValidation(enrollment.getStudentId(), enrollment.getCourseOfferingId(), enrollment.getRoundId());
    }

    /**
     * 执行抽签
     * T6：可选传入随机种子（seed），传入相同种子可复现同一抽签结果用于审计；不传则自动生成并记录。
     */
    @PreAuthorize("@ss.hasPermi('tpm:enroll:edit')")
    @PostMapping("/lottery/{roundId}")
    @Log(title = "选课抽签", businessType = BusinessType.UPDATE)
    public AjaxResult lottery(@PathVariable Long roundId,
                              @RequestParam(required = false) Long seed)
    {
        Map<String, Object> result = tpmSelectionEnrollmentService.runLottery(roundId, seed);
        return success(result);
    }

    /**
     * T6：候补递补——按候补排名将落选学生递补至空余容量
     */
    @PreAuthorize("@ss.hasPermi('tpm:enroll:edit')")
    @PostMapping("/promoteWaitlist/{offeringId}")
    @Log(title = "选课候补递补", businessType = BusinessType.UPDATE)
    public AjaxResult promoteWaitlist(@PathVariable Long offeringId)
    {
        return success(tpmSelectionEnrollmentService.promoteWaitlist(offeringId));
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
