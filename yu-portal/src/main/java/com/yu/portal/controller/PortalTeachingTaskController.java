package com.yu.portal.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.page.TableDataInfo;
import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.service.ITpmCourseOfferingService;

/**
 * 教学任务查询门户Controller（教师端）
 *
 * @author ruoyi
 * @date 2026-05-20
 */
@RestController
@RequestMapping("/portal/teachingTask")
public class PortalTeachingTaskController extends BaseController
{
    @Autowired
    private ITpmCourseOfferingService tpmCourseOfferingService;

    /** 教师端：教学任务查询 */
    @PreAuthorize("@ss.hasPermi('portal:teachingTask:list') and @ss.hasAnyRoles('admin,teacher')")
    @GetMapping("/list")
    public TableDataInfo list(TpmCourseOffering tpmCourseOffering)
    {
        startPage();
        List<TpmCourseOffering> list = tpmCourseOfferingService.selectTpmCourseOfferingList(tpmCourseOffering);
        return getDataTable(list);
    }
}
