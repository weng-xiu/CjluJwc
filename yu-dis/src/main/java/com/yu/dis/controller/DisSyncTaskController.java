package com.yu.dis.controller;

import java.util.List;
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
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.dis.domain.DisSyncTask;
import com.yu.dis.service.IDisSyncTaskService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/dis/task")
public class DisSyncTaskController extends BaseController
{
    @Autowired
    private IDisSyncTaskService disSyncTaskService;

    @PreAuthorize("@ss.hasPermi('dis:syncTask:list')")
    @GetMapping("/list")
    public TableDataInfo list(DisSyncTask disSyncTask)
    {
        startPage();
        List<DisSyncTask> list = disSyncTaskService.selectDisSyncTaskList(disSyncTask);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('dis:syncTask:export')")
    @Log(title = "同步任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DisSyncTask disSyncTask)
    {
        List<DisSyncTask> list = disSyncTaskService.selectDisSyncTaskList(disSyncTask);
        ExcelUtil<DisSyncTask> util = new ExcelUtil<DisSyncTask>(DisSyncTask.class);
        util.exportExcel(response, list, "数据同步任务");
    }

    @PreAuthorize("@ss.hasPermi('dis:syncTask:query')")
    @GetMapping(value = "/{taskId}")
    public AjaxResult getInfo(@PathVariable("taskId") Long taskId)
    {
        return success(disSyncTaskService.selectDisSyncTaskByTaskId(taskId));
    }

    @PreAuthorize("@ss.hasPermi('dis:syncTask:add')")
    @Log(title = "同步任务", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody DisSyncTask disSyncTask)
    {
        return toAjax(disSyncTaskService.insertDisSyncTask(disSyncTask));
    }

    @PreAuthorize("@ss.hasPermi('dis:syncTask:edit')")
    @Log(title = "同步任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody DisSyncTask disSyncTask)
    {
        return toAjax(disSyncTaskService.updateDisSyncTask(disSyncTask));
    }

    @PreAuthorize("@ss.hasPermi('dis:syncTask:remove')")
    @Log(title = "同步任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{taskIds}")
    public AjaxResult remove(@PathVariable Long[] taskIds)
    {
        return toAjax(disSyncTaskService.deleteDisSyncTaskByTaskIds(taskIds));
    }

    /**
     * 手动执行同步任务（D1：调用—解析—落库—留痕）
     */
    @PreAuthorize("@ss.hasPermi('dis:syncTask:execute')")
    @Log(title = "同步任务", businessType = BusinessType.OTHER)
    @PostMapping("/execute/{taskId}")
    public AjaxResult execute(@PathVariable("taskId") Long taskId)
    {
        return success(disSyncTaskService.executeSyncTask(taskId));
    }
}
