package com.yu.brm.controller;

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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.brm.domain.BrmClassroom;
import com.yu.brm.service.IBrmClassroomService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/brm/classroom")
public class BrmClassroomController extends BaseController
{
    @Autowired
    private IBrmClassroomService brmClassroomService;

    @PreAuthorize("@ss.hasPermi('brm:classroom:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrmClassroom brmClassroom)
    {
        startPage();
        List<BrmClassroom> list = brmClassroomService.selectBrmClassroomList(brmClassroom);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('brm:classroom:export')")
    @Log(title = "教室", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BrmClassroom brmClassroom)
    {
        List<BrmClassroom> list = brmClassroomService.selectBrmClassroomList(brmClassroom);
        ExcelUtil<BrmClassroom> util = new ExcelUtil<BrmClassroom>(BrmClassroom.class);
        util.exportExcel(response, list, "教室数据");
    }

    @PreAuthorize("@ss.hasPermi('brm:classroom:query')")
    @GetMapping(value = "/{classroomId}")
    public AjaxResult getInfo(@PathVariable("classroomId") Long classroomId)
    {
        return success(brmClassroomService.selectBrmClassroomByClassroomId(classroomId));
    }

    @PreAuthorize("@ss.hasPermi('brm:classroom:add')")
    @Log(title = "教室", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody BrmClassroom brmClassroom)
    {
        return toAjax(brmClassroomService.insertBrmClassroom(brmClassroom));
    }

    @PreAuthorize("@ss.hasPermi('brm:classroom:edit')")
    @Log(title = "教室", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody BrmClassroom brmClassroom)
    {
        return toAjax(brmClassroomService.updateBrmClassroom(brmClassroom));
    }

    @PreAuthorize("@ss.hasPermi('brm:classroom:remove')")
    @Log(title = "教室", businessType = BusinessType.DELETE)
    @DeleteMapping("/{classroomIds}")
    public AjaxResult remove(@PathVariable Long[] classroomIds)
    {
        return toAjax(brmClassroomService.deleteBrmClassroomByClassroomIds(classroomIds));
    }

    /** P7：下载教室导入模板 */
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<BrmClassroom> util = new ExcelUtil<BrmClassroom>(BrmClassroom.class);
        util.importTemplateExcel(response, "教室数据");
    }

    /** P7：教室 Excel 导入（逐行校验并返回校验报告） */
    @PreAuthorize("@ss.hasPermi('brm:classroom:import')")
    @Log(title = "教室", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<BrmClassroom> util = new ExcelUtil<BrmClassroom>(BrmClassroom.class);
        List<BrmClassroom> classroomList = util.importExcel(file.getInputStream());
        String message = brmClassroomService.importClassroom(classroomList, getUsername(), updateSupport);
        return success(message);
    }
}
