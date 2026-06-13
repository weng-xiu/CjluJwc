package com.yu.portal.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.tpm.domain.TpmSelectionEnrollment;
import com.yu.tpm.domain.TpmSelectionRound;
import com.yu.tpm.service.ITpmSelectionEnrollmentService;
import com.yu.tpm.service.ITpmSelectionRoundService;

/**
 * 选课中心门户Controller
 *
 * @author ruoyi
 * @date 2026-05-20
 */
@RestController
@RequestMapping("/portal/selection")
public class PortalSelectionController extends BaseController
{
    @Autowired
    private ITpmSelectionRoundService tpmSelectionRoundService;

    @Autowired
    private ITpmSelectionEnrollmentService tpmSelectionEnrollmentService;

    /** 选课轮次列表 */
    @PreAuthorize("@ss.hasPermi('portal:selection:list') and @ss.hasAnyRoles('admin,student')")
    @GetMapping("/roundList")
    public TableDataInfo roundList(TpmSelectionRound tpmSelectionRound)
    {
        startPage();
        List<TpmSelectionRound> list = tpmSelectionRoundService.selectTpmSelectionRoundList(tpmSelectionRound);
        return getDataTable(list);
    }

    /** 可选课程列表（选课名单） */
    @PreAuthorize("@ss.hasPermi('portal:selection:query') and @ss.hasAnyRoles('admin,student')")
    @GetMapping("/courseList")
    public TableDataInfo courseList(TpmSelectionEnrollment tpmSelectionEnrollment)
    {
        startPage();
        List<TpmSelectionEnrollment> list = tpmSelectionEnrollmentService.selectTpmSelectionEnrollmentList(tpmSelectionEnrollment);
        return getDataTable(list);
    }

    /** 学生选课 */
    @PreAuthorize("@ss.hasPermi('portal:selection:enroll') and @ss.hasAnyRoles('admin,student')")
    @Log(title = "选课操作", businessType = BusinessType.INSERT)
    @PostMapping("/enroll")
    public AjaxResult enroll(@RequestBody TpmSelectionEnrollment tpmSelectionEnrollment)
    {
        return toAjax(tpmSelectionEnrollmentService.insertTpmSelectionEnrollment(tpmSelectionEnrollment));
    }

    /** 选课结果导出 */
    @PreAuthorize("@ss.hasPermi('portal:selection:export') and @ss.hasAnyRoles('admin,student')")
    @Log(title = "选课记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TpmSelectionEnrollment tpmSelectionEnrollment)
    {
        List<TpmSelectionEnrollment> list = tpmSelectionEnrollmentService.selectTpmSelectionEnrollmentList(tpmSelectionEnrollment);
        ExcelUtil<TpmSelectionEnrollment> util = new ExcelUtil<TpmSelectionEnrollment>(TpmSelectionEnrollment.class);
        util.exportExcel(response, list, "选课记录数据");
    }

    /** 学生退课 */
    @PreAuthorize("@ss.hasPermi('portal:selection:drop') and @ss.hasAnyRoles('admin,student')")
    @Log(title = "退课操作", businessType = BusinessType.DELETE)
    @DeleteMapping("/drop/{enrollmentId}")
    public AjaxResult drop(@PathVariable Long enrollmentId)
    {
        return toAjax(tpmSelectionEnrollmentService.deleteTpmSelectionEnrollmentByEnrollId(enrollmentId));
    }

    /** 选课结果查询 */
    @PreAuthorize("@ss.hasPermi('portal:selection:result') and @ss.hasAnyRoles('admin,student')")
    @GetMapping("/result")
    public TableDataInfo result()
    {
        startPage();
        List<TpmSelectionEnrollment> list = tpmSelectionEnrollmentService.selectTpmSelectionEnrollmentList(new TpmSelectionEnrollment());
        return getDataTable(list);
    }
}
