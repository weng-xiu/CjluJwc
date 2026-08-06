package com.yu.brm.controller;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.brm.domain.BrmDepartment;
import com.yu.brm.service.IBrmDepartmentService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.utils.StringUtils;

@RestController
@RequestMapping("/brm/dept")
public class BrmDepartmentController extends BaseController
{
    @Autowired
    private IBrmDepartmentService brmDepartmentService;

    @PreAuthorize("@ss.hasPermi('brm:dept:list')")
    @GetMapping("/list")
    public AjaxResult list(BrmDepartment dept)
    {
        List<BrmDepartment> depts = brmDepartmentService.selectBrmDepartmentList(dept);
        return success(depts);
    }

    @PreAuthorize("@ss.hasPermi('brm:dept:export')")
    @Log(title = "院系", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BrmDepartment dept)
    {
        List<BrmDepartment> list = brmDepartmentService.selectBrmDepartmentList(dept);
        ExcelUtil<BrmDepartment> util = new ExcelUtil<BrmDepartment>(BrmDepartment.class);
        util.exportExcel(response, list, "院系数据");
    }

    @PreAuthorize("@ss.hasPermi('brm:dept:query')")
    @GetMapping(value = "/{deptId}")
    public AjaxResult getInfo(@PathVariable("deptId") Long deptId)
    {
        return success(brmDepartmentService.selectBrmDepartmentByDeptId(deptId));
    }

    @PreAuthorize("@ss.hasPermi('brm:dept:add')")
    @Log(title = "院系", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody BrmDepartment dept)
    {
        if ("0".equals(brmDepartmentService.checkDeptNameUnique(dept)))
        {
            return error("新增院系'" + dept.getDeptName() + "'失败，院系名称已存在");
        }
        return toAjax(brmDepartmentService.insertBrmDepartment(dept));
    }

    @PreAuthorize("@ss.hasPermi('brm:dept:edit')")
    @Log(title = "院系", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody BrmDepartment dept)
    {
        Long deptId = dept.getDeptId();
        if ("0".equals(brmDepartmentService.checkDeptNameUnique(dept)))
        {
            return error("修改院系'" + dept.getDeptName() + "'失败，院系名称已存在");
        }
        else if (dept.getParentId().equals(deptId))
        {
            return error("修改院系'" + dept.getDeptName() + "'失败，上级院系不能是自己");
        }
        else if (StringUtils.equals("1", dept.getStatus()) && brmDepartmentService.selectChildrenDeptById(deptId).size() > 0)
        {
            return error("该院系包含未停用的子院系！");
        }
        return toAjax(brmDepartmentService.updateBrmDepartment(dept));
    }

    @PreAuthorize("@ss.hasPermi('brm:dept:remove')")
    @Log(title = "院系", businessType = BusinessType.DELETE)
    @DeleteMapping("/{deptIds}")
    public AjaxResult remove(@PathVariable Long[] deptIds)
    {
        if (ArrayUtils.contains(deptIds, 100L))
        {
            return error("根节点不能删除");
        }
        for (Long deptId : deptIds)
        {
            if (brmDepartmentService.selectChildrenDeptById(deptId).size() > 0)
            {
                return error("存在下级院系,不允许删除");
            }
            if (brmDepartmentService.checkDeptExistUser(deptId))
            {
                return error("院系存在教师,不允许删除");
            }
        }
        return toAjax(brmDepartmentService.deleteBrmDepartmentByDeptIds(deptIds));
    }

    @PreAuthorize("@ss.hasPermi('brm:dept:add')")
    @Log(title = "院系", businessType = BusinessType.OTHER)
    @PostMapping("/syncFromSys")
    public AjaxResult syncFromSys()
    {
        int count = brmDepartmentService.syncFromSysDept();
        return success("已从部门管理同步 " + count + " 条院系数据");
    }
}
