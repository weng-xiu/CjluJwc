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
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.brm.domain.BrmTeacherPosition;
import com.yu.brm.service.IBrmTeacherPositionService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/brm/position")
public class BrmTeacherPositionController extends BaseController
{
    @Autowired
    private IBrmTeacherPositionService brmTeacherPositionService;

    @PreAuthorize("@ss.hasPermi('brm:position:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrmTeacherPosition brmTeacherPosition)
    {
        startPage();
        List<BrmTeacherPosition> list = brmTeacherPositionService.selectBrmTeacherPositionList(brmTeacherPosition);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('brm:position:export')")
    @Log(title = "教师任职信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BrmTeacherPosition brmTeacherPosition)
    {
        List<BrmTeacherPosition> list = brmTeacherPositionService.selectBrmTeacherPositionList(brmTeacherPosition);
        ExcelUtil<BrmTeacherPosition> util = new ExcelUtil<BrmTeacherPosition>(BrmTeacherPosition.class);
        util.exportExcel(response, list, "教师任职信息数据");
    }

    @PreAuthorize("@ss.hasPermi('brm:position:query')")
    @GetMapping(value = "/{posId}")
    public AjaxResult getInfo(@PathVariable("posId") Long posId)
    {
        return success(brmTeacherPositionService.selectBrmTeacherPositionByPosId(posId));
    }

    @PreAuthorize("@ss.hasPermi('brm:position:add')")
    @Log(title = "教师任职信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BrmTeacherPosition brmTeacherPosition)
    {
        return toAjax(brmTeacherPositionService.insertBrmTeacherPosition(brmTeacherPosition));
    }

    @PreAuthorize("@ss.hasPermi('brm:position:edit')")
    @Log(title = "教师任职信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BrmTeacherPosition brmTeacherPosition)
    {
        return toAjax(brmTeacherPositionService.updateBrmTeacherPosition(brmTeacherPosition));
    }

    @PreAuthorize("@ss.hasPermi('brm:position:remove')")
    @Log(title = "教师任职信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{posIds}")
    public AjaxResult remove(@PathVariable Long[] posIds)
    {
        return toAjax(brmTeacherPositionService.deleteBrmTeacherPositionByPosIds(posIds));
    }
}
