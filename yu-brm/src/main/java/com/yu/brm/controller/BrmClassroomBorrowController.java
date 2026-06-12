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
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.brm.domain.BrmClassroomBorrow;
import com.yu.brm.service.IBrmClassroomBorrowService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/brm/borrow")
public class BrmClassroomBorrowController extends BaseController
{
    @Autowired
    private IBrmClassroomBorrowService brmClassroomBorrowService;

    @PreAuthorize("@ss.hasPermi('brm:borrow:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrmClassroomBorrow brmClassroomBorrow)
    {
        startPage();
        List<BrmClassroomBorrow> list = brmClassroomBorrowService.selectBrmClassroomBorrowList(brmClassroomBorrow);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('brm:borrow:export')")
    @Log(title = "教室借用", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BrmClassroomBorrow brmClassroomBorrow)
    {
        List<BrmClassroomBorrow> list = brmClassroomBorrowService.selectBrmClassroomBorrowList(brmClassroomBorrow);
        ExcelUtil<BrmClassroomBorrow> util = new ExcelUtil<BrmClassroomBorrow>(BrmClassroomBorrow.class);
        util.exportExcel(response, list, "教室借用数据");
    }

    @PreAuthorize("@ss.hasPermi('brm:borrow:query')")
    @GetMapping(value = "/{borrowId}")
    public AjaxResult getInfo(@PathVariable("borrowId") Long borrowId)
    {
        return success(brmClassroomBorrowService.selectBrmClassroomBorrowByBorrowId(borrowId));
    }

    @PreAuthorize("@ss.hasPermi('brm:borrow:add')")
    @Log(title = "教室借用", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody BrmClassroomBorrow brmClassroomBorrow)
    {
        return toAjax(brmClassroomBorrowService.insertBrmClassroomBorrow(brmClassroomBorrow));
    }

    @PreAuthorize("@ss.hasPermi('brm:borrow:edit')")
    @Log(title = "教室借用", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody BrmClassroomBorrow brmClassroomBorrow)
    {
        return toAjax(brmClassroomBorrowService.updateBrmClassroomBorrow(brmClassroomBorrow));
    }

    @PreAuthorize("@ss.hasPermi('brm:borrow:remove')")
    @Log(title = "教室借用", businessType = BusinessType.DELETE)
    @DeleteMapping("/{borrowIds}")
    public AjaxResult remove(@PathVariable Long[] borrowIds)
    {
        return toAjax(brmClassroomBorrowService.deleteBrmClassroomBorrowByBorrowIds(borrowIds));
    }
}
