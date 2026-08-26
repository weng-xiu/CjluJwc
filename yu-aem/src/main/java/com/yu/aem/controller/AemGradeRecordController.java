package com.yu.aem.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.aem.domain.AemGradeRecord;
import com.yu.aem.service.IAemGradeRecordService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 成绩记录Controller
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@RestController
@RequestMapping("/aem/gradeRecord")
public class AemGradeRecordController extends BaseController
{
    @Autowired
    private IAemGradeRecordService aemGradeRecordService;

    @PreAuthorize("@ss.hasPermi('aem:gradeRecord:list')")
    @GetMapping("/list")
    public TableDataInfo list(AemGradeRecord aemGradeRecord)
    {
        startPage();
        List<AemGradeRecord> list = aemGradeRecordService.selectAemGradeRecordList(aemGradeRecord);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aem:gradeRecord:export')")
    @Log(title = "成绩记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AemGradeRecord aemGradeRecord)
    {
        List<AemGradeRecord> list = aemGradeRecordService.selectAemGradeRecordList(aemGradeRecord);
        ExcelUtil<AemGradeRecord> util = new ExcelUtil<AemGradeRecord>(AemGradeRecord.class);
        util.exportExcel(response, list, "成绩记录数据");
    }

    @PreAuthorize("@ss.hasPermi('aem:gradeRecord:query')")
    @GetMapping(value = "/{gradeId}")
    public AjaxResult getInfo(@PathVariable("gradeId") Long gradeId)
    {
        return success(aemGradeRecordService.selectAemGradeRecordByGradeId(gradeId));
    }

    /**
     * 查询成绩记录明细（含复核记录子表），用于主子表联动展开
     */
    @PreAuthorize("@ss.hasPermi('aem:gradeRecord:query')")
    @GetMapping(value = "/detail/{gradeId}")
    public AjaxResult getDetail(@PathVariable("gradeId") Long gradeId)
    {
        return success(aemGradeRecordService.selectAemGradeRecordDetail(gradeId));
    }

    @PreAuthorize("@ss.hasPermi('aem:gradeRecord:add')")
    @Log(title = "成绩记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AemGradeRecord aemGradeRecord)
    {
        return toAjax(aemGradeRecordService.insertAemGradeRecord(aemGradeRecord));
    }

    @PreAuthorize("@ss.hasPermi('aem:gradeRecord:edit')")
    @Log(title = "成绩记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AemGradeRecord aemGradeRecord)
    {
        return toAjax(aemGradeRecordService.updateAemGradeRecord(aemGradeRecord));
    }

    @PreAuthorize("@ss.hasPermi('aem:gradeRecord:remove')")
    @Log(title = "成绩记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{gradeIds}")
    public AjaxResult remove(@PathVariable Long[] gradeIds)
    {
        return toAjax(aemGradeRecordService.deleteAemGradeRecordByGradeIds(gradeIds));
    }

    /**
     * 批量导入成绩（Excel）
     * @param file          Excel文件
     * @param algorithmCode GPA算法代码（可选，空则使用默认算法）
     */
    @PreAuthorize("@ss.hasPermi('aem:gradeRecord:import')")
    @Log(title = "成绩记录", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file,
                                 @RequestParam(required = false) String algorithmCode) throws Exception
    {
        ExcelUtil<AemGradeRecord> util = new ExcelUtil<AemGradeRecord>(AemGradeRecord.class);
        List<AemGradeRecord> list = util.importExcel(file.getInputStream());
        int count = aemGradeRecordService.importGrade(list, getUsername(), algorithmCode);
        return success("导入成功，共处理 " + count + " 条成绩");
    }

    /**
     * 下载成绩导入模板
     */
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<AemGradeRecord> util = new ExcelUtil<AemGradeRecord>(AemGradeRecord.class);
        util.importTemplateExcel(response, "成绩数据");
    }
}
