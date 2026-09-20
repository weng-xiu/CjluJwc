package com.yu.oa.controller;

import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;
import com.yu.oa.domain.OaMeeting;
import com.yu.oa.domain.OaMeetingRoom;
import com.yu.oa.service.IOaMeetingRoomService;
import com.yu.oa.service.IOaMeetingService;

/**
 * 会议室Controller
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@RestController
@RequestMapping("/oa/meetingRoom")
public class OaMeetingRoomController extends BaseController
{
    @Autowired
    private IOaMeetingRoomService oaMeetingRoomService;

    @Autowired
    private IOaMeetingService oaMeetingService;

    /**
     * O2：会议室占用日历——查询某会议室某天的会议安排。
     *
     * @param roomId 会议室ID
     * @param date   日期（yyyy-MM-dd，默认今天）
     */
    @PreAuthorize("@ss.hasPermi('oa:meetingRoom:list')")
    @GetMapping("/occupancy/{roomId}")
    public AjaxResult occupancy(@PathVariable("roomId") Long roomId,
                                @RequestParam(required = false) String date)
    {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        if (date != null && !date.isEmpty())
        {
            try
            {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                cal.setTime(sdf.parse(date));
            }
            catch (Exception e)
            {
                return error("日期格式不正确，应为 yyyy-MM-dd");
            }
        }
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        Date beginTime = cal.getTime();
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
        Date endTime = cal.getTime();
        List<OaMeeting> list = oaMeetingService.roomOccupancy(roomId, beginTime, endTime);
        return success(list);
    }

    /**
     * 查询会议室列表
     */
    @PreAuthorize("@ss.hasPermi('oa:meetingRoom:list')")
    @GetMapping("/list")
    public TableDataInfo list(OaMeetingRoom oaMeetingRoom)
    {
        startPage();
        List<OaMeetingRoom> list = oaMeetingRoomService.selectOaMeetingRoomList(oaMeetingRoom);
        return getDataTable(list);
    }

    /**
     * 导出会议室列表
     */
    @PreAuthorize("@ss.hasPermi('oa:meetingRoom:export')")
    @Log(title = "会议室", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OaMeetingRoom oaMeetingRoom)
    {
        List<OaMeetingRoom> list = oaMeetingRoomService.selectOaMeetingRoomList(oaMeetingRoom);
        ExcelUtil<OaMeetingRoom> util = new ExcelUtil<OaMeetingRoom>(OaMeetingRoom.class);
        util.exportExcel(response, list, "会议室数据");
    }

    /**
     * 获取会议室详细信息
     */
    @PreAuthorize("@ss.hasPermi('oa:meetingRoom:query')")
    @GetMapping(value = "/{roomId}")
    public AjaxResult getInfo(@PathVariable("roomId") Long roomId)
    {
        return success(oaMeetingRoomService.selectOaMeetingRoomByRoomId(roomId));
    }

    /**
     * 新增会议室
     */
    @PreAuthorize("@ss.hasPermi('oa:meetingRoom:add')")
    @Log(title = "会议室", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OaMeetingRoom oaMeetingRoom)
    {
        return toAjax(oaMeetingRoomService.insertOaMeetingRoom(oaMeetingRoom));
    }

    /**
     * 修改会议室
     */
    @PreAuthorize("@ss.hasPermi('oa:meetingRoom:edit')")
    @Log(title = "会议室", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OaMeetingRoom oaMeetingRoom)
    {
        return toAjax(oaMeetingRoomService.updateOaMeetingRoom(oaMeetingRoom));
    }

    /**
     * 删除会议室
     */
    @PreAuthorize("@ss.hasPermi('oa:meetingRoom:remove')")
    @Log(title = "会议室", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roomIds}")
    public AjaxResult remove(@PathVariable Long[] roomIds)
    {
        return toAjax(oaMeetingRoomService.deleteOaMeetingRoomByRoomIds(roomIds));
    }
}
