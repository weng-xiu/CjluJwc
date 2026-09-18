package com.yu.portal.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.page.TableDataInfo;
import com.yu.aem.domain.AemExamPlan;
import com.yu.aem.domain.AemExamInvigilation;
import com.yu.aem.service.IAemExamPlanService;
import com.yu.aem.service.IAemExamInvigilationService;
import com.yu.brm.domain.BrmClassroom;
import com.yu.brm.service.IBrmClassroomService;

/**
 * 考试安排与监考安排门户Controller
 *
 * @author ruoyi
 * @date 2026-05-20
 */
@RestController
@RequestMapping("/portal/exam")
public class PortalExamController extends BaseController
{
    @Autowired
    private IAemExamPlanService aemExamPlanService;

    @Autowired
    private IAemExamInvigilationService aemExamInvigilationService;

    @Autowired
    private IBrmClassroomService brmClassroomService;

    /** 学生端：考试安排查询（门户专用，不套用部门数据范围） */
    @PreAuthorize("@ss.hasPermi('portal:exam:list')")
    @GetMapping("/list")
    public TableDataInfo list(AemExamPlan aemExamPlan)
    {
        startPage();
        List<AemExamPlan> list = aemExamPlanService.selectAemExamPlanListForPortal(aemExamPlan);
        return getDataTable(list);
    }

    /** 教师端：监考安排查询（附带考试名称、教室名称，供移动端卡片展示） */
    @PreAuthorize("@ss.hasPermi('portal:invigilation:list') and @ss.hasAnyRoles('admin,teacher')")
    @GetMapping("/invigilationList")
    public TableDataInfo invigilationList(AemExamInvigilation aemExamInvigilation)
    {
        startPage();
        List<AemExamInvigilation> list = aemExamInvigilationService.selectAemExamInvigilationList(aemExamInvigilation);
        // 逐页填充关联展示字段（分页后最多 pageSize 条，本地缓存去重避免重复查询）
        Map<Long, String> examNameCache = new HashMap<>();
        Map<Long, String> classroomNameCache = new HashMap<>();
        for (AemExamInvigilation item : list)
        {
            if (item.getExamId() != null)
            {
                item.setExamName(examNameCache.computeIfAbsent(item.getExamId(), id -> {
                    AemExamPlan plan = aemExamPlanService.selectAemExamPlanByExamId(id);
                    return plan != null ? plan.getExamName() : null;
                }));
            }
            if (item.getClassroomId() != null)
            {
                item.setClassroomName(classroomNameCache.computeIfAbsent(item.getClassroomId(), id -> {
                    BrmClassroom classroom = brmClassroomService.selectBrmClassroomByClassroomId(id);
                    return classroom != null ? classroom.getClassroomName() : null;
                }));
            }
        }
        return getDataTable(list);
    }
}
