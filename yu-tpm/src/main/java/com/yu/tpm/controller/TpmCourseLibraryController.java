package com.yu.tpm.controller;

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
import org.springframework.web.multipart.MultipartFile;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.tpm.domain.TpmCourseLibrary;
import com.yu.tpm.service.ITpmCourseLibraryService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 课程库Controller
 *
 * @author ruoyi
 * @date 2026-05-09
 */
@RestController
@RequestMapping("/tpm/courseLib")
public class TpmCourseLibraryController extends BaseController
{
    @Autowired
    private ITpmCourseLibraryService tpmCourseLibraryService;

    @PreAuthorize("@ss.hasPermi('tpm:course:list')")
    @GetMapping("/list")
    public TableDataInfo list(TpmCourseLibrary tpmCourseLibrary)
    {
        startPage();
        List<TpmCourseLibrary> list = tpmCourseLibraryService.selectTpmCourseLibraryList(tpmCourseLibrary);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('tpm:course:export')")
    @Log(title = "课程库", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TpmCourseLibrary tpmCourseLibrary)
    {
        List<TpmCourseLibrary> list = tpmCourseLibraryService.selectTpmCourseLibraryList(tpmCourseLibrary);
        ExcelUtil<TpmCourseLibrary> util = new ExcelUtil<TpmCourseLibrary>(TpmCourseLibrary.class);
        util.exportExcel(response, list, "课程库数据");
    }

    @PreAuthorize("@ss.hasPermi('tpm:course:query')")
    @GetMapping(value = "/{courseId}")
    public AjaxResult getInfo(@PathVariable("courseId") Long courseId)
    {
        return success(tpmCourseLibraryService.selectTpmCourseLibraryByCourseId(courseId));
    }

    @PreAuthorize("@ss.hasPermi('tpm:course:add')")
    @Log(title = "课程库", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TpmCourseLibrary tpmCourseLibrary)
    {
        return toAjax(tpmCourseLibraryService.insertTpmCourseLibrary(tpmCourseLibrary));
    }

    @PreAuthorize("@ss.hasPermi('tpm:course:edit')")
    @Log(title = "课程库", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TpmCourseLibrary tpmCourseLibrary)
    {
        return toAjax(tpmCourseLibraryService.updateTpmCourseLibrary(tpmCourseLibrary));
    }

    @PreAuthorize("@ss.hasPermi('tpm:course:remove')")
    @Log(title = "课程库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{courseIds}")
    public AjaxResult remove(@PathVariable Long[] courseIds)
    {
        return toAjax(tpmCourseLibraryService.deleteTpmCourseLibraryByCourseIds(courseIds));
    }

    /**
     * P7：下载课程导入模板
     */
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<TpmCourseLibrary> util = new ExcelUtil<TpmCourseLibrary>(TpmCourseLibrary.class);
        util.importTemplateExcel(response, "课程数据");
    }

    /**
     * P7：批量导入课程（Excel）
     *
     * @param file          Excel 文件
     * @param updateSupport 编码已存在时是否更新
     */
    @PreAuthorize("@ss.hasPermi('tpm:course:import')")
    @Log(title = "课程库", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<TpmCourseLibrary> util = new ExcelUtil<TpmCourseLibrary>(TpmCourseLibrary.class);
        List<TpmCourseLibrary> courseList = util.importExcel(file.getInputStream());
        String message = tpmCourseLibraryService.importCourse(courseList, getUsername(), updateSupport);
        return success(message);
    }
}
