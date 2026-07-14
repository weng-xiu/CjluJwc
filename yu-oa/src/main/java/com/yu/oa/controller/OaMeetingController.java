package com.yu.oa.controller;

import java.util.List;
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
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;
import com.yu.oa.domain.OaMeeting;
import com.yu.oa.domain.OaMeetingMinutes;
import com.yu.oa.domain.OaMeetingParticipant;
import com.yu.oa.service.IOaMeetingService;

/**
 * 会议管理Controller
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@RestController
@RequestMapping("/oa/meeting")
public class OaMeetingController extends BaseController
{
    @Autowired
    private IOaMeetingService oaMeetingService;

    /**
     * 查询会议管理列表
     */
    @PreAuthorize("@ss.hasPermi('oa:meeting:list')")
    @GetMapping("/list")
    public TableDataInfo list(OaMeeting oaMeeting)
    {
        startPage();
        List<OaMeeting> list = oaMeetingService.selectOaMeetingList(oaMeeting);
        return getDataTable(list);
    }

    /**
     * 导出会议管理列表
     */
    @PreAuthorize("@ss.hasPermi('oa:meeting:export')")
    @Log(title = "会议管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OaMeeting oaMeeting)
    {
        List<OaMeeting> list = oaMeetingService.selectOaMeetingList(oaMeeting);
        ExcelUtil<OaMeeting> util = new ExcelUtil<OaMeeting>(OaMeeting.class);
        util.exportExcel(response, list, "会议管理数据");
    }

    /**
     * 获取会议管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('oa:meeting:query')")
    @GetMapping(value = "/{meetingId}")
    public AjaxResult getInfo(@PathVariable("meetingId") Long meetingId)
    {
        return success(oaMeetingService.selectOaMeetingByMeetingId(meetingId));
    }

    /**
     * 新增会议管理
     */
    @PreAuthorize("@ss.hasPermi('oa:meeting:add')")
    @Log(title = "会议管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OaMeeting oaMeeting)
    {
        return toAjax(oaMeetingService.insertOaMeeting(oaMeeting));
    }

    /**
     * 修改会议管理
     */
    @PreAuthorize("@ss.hasPermi('oa:meeting:edit')")
    @Log(title = "会议管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OaMeeting oaMeeting)
    {
        return toAjax(oaMeetingService.updateOaMeeting(oaMeeting));
    }

    /**
     * 删除会议管理
     */
    @PreAuthorize("@ss.hasPermi('oa:meeting:remove')")
    @Log(title = "会议管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{meetingIds}")
    public AjaxResult remove(@PathVariable Long[] meetingIds)
    {
        return toAjax(oaMeetingService.deleteOaMeetingByMeetingIds(meetingIds));
    }

    /**
     * 检测会议室冲突
     */
    @PreAuthorize("@ss.hasPermi('oa:meeting:add')")
    @GetMapping("/checkConflict")
    public AjaxResult checkConflict(OaMeeting oaMeeting)
    {
        return success(oaMeetingService.checkMeetingConflict(oaMeeting));
    }

    /**
     * 保存会议纪要
     */
    @PreAuthorize("@ss.hasPermi('oa:meeting:edit')")
    @Log(title = "会议纪要", businessType = BusinessType.UPDATE)
    @PostMapping("/minutes")
    public AjaxResult saveMinutes(@RequestBody OaMeetingMinutes minutes)
    {
        return toAjax(oaMeetingService.saveMinutes(minutes));
    }

    /**
     * 更新参会状态
     */
    @PreAuthorize("@ss.hasPermi('oa:meeting:edit')")
    @Log(title = "参会人员", businessType = BusinessType.UPDATE)
    @PutMapping("/participant")
    public AjaxResult updateParticipant(@RequestBody OaMeetingParticipant participant)
    {
        return toAjax(oaMeetingService.updateParticipantStatus(participant));
    }

    /**
     * 会议签到
     */
    @PreAuthorize("@ss.hasPermi('oa:meeting:edit')")
    @Log(title = "会议签到", businessType = BusinessType.UPDATE)
    @PostMapping("/sign/{participantId}")
    public AjaxResult sign(@PathVariable("participantId") Long participantId)
    {
        return toAjax(oaMeetingService.signMeeting(participantId));
    }
}
