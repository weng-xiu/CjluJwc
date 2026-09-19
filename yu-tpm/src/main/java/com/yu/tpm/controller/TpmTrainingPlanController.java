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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.common.utils.SecurityUtils;
import com.yu.tpm.domain.TpmTrainingPlan;
import com.yu.tpm.domain.TpmCourseLibrary;
import com.yu.tpm.domain.TpmCreditStructure;
import com.yu.tpm.service.ITpmTrainingPlanService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 培养方案Controller
 *
 * @author ruoyi
 * @date 2026-05-09
 */
@RestController
@RequestMapping("/tpm/plan")
public class TpmTrainingPlanController extends BaseController
{
    @Autowired
    private ITpmTrainingPlanService tpmTrainingPlanService;

    /** Jackson 对象映射（静态实例，无需 Spring Bean） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @PreAuthorize("@ss.hasPermi('tpm:plan:list')")
    @GetMapping("/list")
    public TableDataInfo list(TpmTrainingPlan tpmTrainingPlan)
    {
        startPage();
        List<TpmTrainingPlan> list = tpmTrainingPlanService.selectTpmTrainingPlanList(tpmTrainingPlan);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('tpm:plan:export')")
    @Log(title = "培养方案", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TpmTrainingPlan tpmTrainingPlan)
    {
        List<TpmTrainingPlan> list = tpmTrainingPlanService.selectTpmTrainingPlanList(tpmTrainingPlan);
        ExcelUtil<TpmTrainingPlan> util = new ExcelUtil<TpmTrainingPlan>(TpmTrainingPlan.class);
        util.exportExcel(response, list, "培养方案数据");
    }

    @PreAuthorize("@ss.hasPermi('tpm:plan:query')")
    @GetMapping(value = "/{planId}")
    public AjaxResult getInfo(@PathVariable("planId") Long planId)
    {
        return success(tpmTrainingPlanService.selectTpmTrainingPlanByPlanId(planId));
    }

    @PreAuthorize("@ss.hasPermi('tpm:plan:add')")
    @Log(title = "培养方案", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TpmTrainingPlan tpmTrainingPlan)
    {
        return toAjax(tpmTrainingPlanService.insertTpmTrainingPlan(tpmTrainingPlan));
    }

    @PreAuthorize("@ss.hasPermi('tpm:plan:edit')")
    @Log(title = "培养方案", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TpmTrainingPlan tpmTrainingPlan)
    {
        return toAjax(tpmTrainingPlanService.updateTpmTrainingPlan(tpmTrainingPlan));
    }

    @PreAuthorize("@ss.hasPermi('tpm:plan:remove')")
    @Log(title = "培养方案", businessType = BusinessType.DELETE)
    @DeleteMapping("/{planIds}")
    public AjaxResult remove(@PathVariable Long[] planIds)
    {
        return toAjax(tpmTrainingPlanService.deleteTpmTrainingPlanByPlanIds(planIds));
    }

    @PreAuthorize("@ss.hasPermi('tpm:plan:edit')")
    @Log(title = "培养方案", businessType = BusinessType.UPDATE)
    @PutMapping("/publish/{planId}")
    public AjaxResult publish(@PathVariable Long planId)
    {
        return toAjax(tpmTrainingPlanService.publishTrainingPlan(planId));
    }

    @PreAuthorize("@ss.hasPermi('tpm:plan:edit')")
    @Log(title = "培养方案", businessType = BusinessType.UPDATE)
    @PutMapping("/deprecate/{planId}")
    public AjaxResult deprecate(@PathVariable Long planId)
    {
        return toAjax(tpmTrainingPlanService.deprecateTrainingPlan(planId));
    }

    /** T3：复制培养方案为新草稿版本（含课程与学分结构子表） */
    @PreAuthorize("@ss.hasPermi('tpm:plan:add')")
    @Log(title = "培养方案", businessType = BusinessType.INSERT)
    @PostMapping("/copy/{planId}")
    public AjaxResult copy(@PathVariable Long planId)
    {
        Long newPlanId = tpmTrainingPlanService.copyTrainingPlan(planId);
        AjaxResult result = success("复制成功，已生成新草稿版本");
        result.put("planId", newPlanId);
        return result;
    }

    @PreAuthorize("@ss.hasAnyPermi('tpm:plan:add,tpm:plan:edit')")
    @Log(title = "培养方案", businessType = BusinessType.UPDATE)
    @PostMapping("/saveWithChildren")
    public AjaxResult saveWithChildren(@RequestBody Map<String, Object> body) throws Exception
    {
        TpmTrainingPlan plan = OBJECT_MAPPER.convertValue(body.get("plan"), TpmTrainingPlan.class);
        List<TpmCourseLibrary> courseList = OBJECT_MAPPER.convertValue(body.get("courseList"),
                new TypeReference<List<TpmCourseLibrary>>() {});
        List<TpmCreditStructure> creditList = OBJECT_MAPPER.convertValue(body.get("creditList"),
                new TypeReference<List<TpmCreditStructure>>() {});
        String username = SecurityUtils.getUsername();
        if (plan.getPlanId() == null)
        {
            plan.setCreateBy(username);
        }
        else
        {
            plan.setUpdateBy(username);
        }
        if (courseList != null)
        {
            for (TpmCourseLibrary course : courseList)
            {
                if (course.getCourseId() == null)
                {
                    course.setCreateBy(username);
                }
                else
                {
                    course.setUpdateBy(username);
                }
            }
        }
        if (creditList != null)
        {
            for (TpmCreditStructure credit : creditList)
            {
                if (credit.getStructId() == null)
                {
                    credit.setCreateBy(username);
                }
                else
                {
                    credit.setUpdateBy(username);
                }
            }
        }
        return toAjax(tpmTrainingPlanService.savePlanWithChildren(plan, courseList, creditList));
    }
}
