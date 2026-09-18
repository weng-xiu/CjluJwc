package com.yu.portal.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.sam.domain.SamWarning;
import com.yu.sam.service.ISamWarningService;

/**
 * 学业预警门户Controller（学生查看本人预警）
 *
 * @author ruoyi
 * @date 2026-05-20
 */
@RestController
@RequestMapping("/portal/warning")
public class PortalWarningController extends BaseController
{
    @Autowired
    private ISamWarningService samWarningService;

    /** 学生端：查询本人学业预警（强制绑定当前登录用户，防越权） */
    @PreAuthorize("@ss.hasPermi('portal:warning:list') and @ss.hasAnyRoles('admin,student')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) Long semesterId)
    {
        SamWarning query = new SamWarning();
        query.setStudentId(getUserId());
        query.setSemesterId(semesterId);
        startPage();
        List<SamWarning> list = samWarningService.selectSamWarningList(query);
        return getDataTable(list);
    }

    /** 学生端：本人预警统计（按类型/级别汇总） */
    @PreAuthorize("@ss.hasPermi('portal:warning:list') and @ss.hasAnyRoles('admin,student')")
    @GetMapping("/statistics")
    public AjaxResult statistics(@RequestParam(required = false) Long semesterId)
    {
        SamWarning query = new SamWarning();
        query.setStudentId(getUserId());
        query.setSemesterId(semesterId);
        List<SamWarning> list = samWarningService.selectSamWarningList(query);

        Map<String, Object> result = new HashMap<>();
        // 按类型统计（0GPA/1学分/2出勤/3综合）
        result.put("gpaCount", list.stream().filter(w -> "0".equals(w.getWarningType())).count());
        result.put("creditCount", list.stream().filter(w -> "1".equals(w.getWarningType())).count());
        result.put("attendanceCount", list.stream().filter(w -> "2".equals(w.getWarningType())).count());
        result.put("comprehensiveCount", list.stream().filter(w -> "3".equals(w.getWarningType())).count());
        result.put("totalCount", list.size());
        // 按级别统计（0一般/1严重/2高危）
        result.put("normalCount", list.stream().filter(w -> "0".equals(w.getWarningLevel())).count());
        result.put("seriousCount", list.stream().filter(w -> "1".equals(w.getWarningLevel())).count());
        result.put("highRiskCount", list.stream().filter(w -> "2".equals(w.getWarningLevel())).count());
        // 未解除数量
        result.put("unresolvedCount", list.stream().filter(w -> "0".equals(w.getIsResolved()) || w.getIsResolved() == null).count());
        return success(result);
    }
}
