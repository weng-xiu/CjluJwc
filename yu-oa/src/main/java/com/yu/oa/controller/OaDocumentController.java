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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;
import com.yu.oa.domain.OaDocument;
import com.yu.oa.service.IOaDocumentService;

/**
 * 公文Controller
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@RestController
@RequestMapping("/oa/document")
public class OaDocumentController extends BaseController
{
    @Autowired
    private IOaDocumentService oaDocumentService;

    /**
     * 查询公文列表
     */
    @PreAuthorize("@ss.hasPermi('oa:document:list')")
    @GetMapping("/list")
    public TableDataInfo list(OaDocument oaDocument)
    {
        startPage();
        List<OaDocument> list = oaDocumentService.selectOaDocumentList(oaDocument);
        return getDataTable(list);
    }

    /**
     * 导出公文列表
     */
    @PreAuthorize("@ss.hasPermi('oa:document:export')")
    @Log(title = "公文", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OaDocument oaDocument)
    {
        List<OaDocument> list = oaDocumentService.selectOaDocumentList(oaDocument);
        ExcelUtil<OaDocument> util = new ExcelUtil<OaDocument>(OaDocument.class);
        util.exportExcel(response, list, "公文数据");
    }

    /**
     * 获取公文详细信息
     */
    @PreAuthorize("@ss.hasPermi('oa:document:query')")
    @GetMapping(value = "/{documentId}")
    public AjaxResult getInfo(@PathVariable("documentId") Long documentId)
    {
        return success(oaDocumentService.selectOaDocumentByDocumentId(documentId));
    }

    /**
     * 新增公文
     */
    @PreAuthorize("@ss.hasPermi('oa:document:add')")
    @Log(title = "公文", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OaDocument oaDocument)
    {
        return toAjax(oaDocumentService.insertOaDocument(oaDocument));
    }

    /**
     * 修改公文
     */
    @PreAuthorize("@ss.hasPermi('oa:document:edit')")
    @Log(title = "公文", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OaDocument oaDocument)
    {
        return toAjax(oaDocumentService.updateOaDocument(oaDocument));
    }

    /**
     * 删除公文
     */
    @PreAuthorize("@ss.hasPermi('oa:document:remove')")
    @Log(title = "公文", businessType = BusinessType.DELETE)
    @DeleteMapping("/{documentIds}")
    public AjaxResult remove(@PathVariable Long[] documentIds)
    {
        return toAjax(oaDocumentService.deleteOaDocumentByDocumentIds(documentIds));
    }

    /**
     * 提交审批
     */
    @PreAuthorize("@ss.hasPermi('oa:document:submit')")
    @Log(title = "公文", businessType = BusinessType.UPDATE)
    @PostMapping("/submit/{documentId}")
    public AjaxResult submit(@PathVariable("documentId") Long documentId)
    {
        return toAjax(oaDocumentService.submitDocument(documentId));
    }

    /**
     * 审批通过
     */
    @PreAuthorize("@ss.hasPermi('oa:document:approve')")
    @Log(title = "公文", businessType = BusinessType.UPDATE)
    @PostMapping("/approve")
    public AjaxResult approve(@RequestParam("documentId") Long documentId,
                              @RequestParam("taskId") String taskId,
                              @RequestParam(value = "comment", required = false) String comment)
    {
        return toAjax(oaDocumentService.approveDocument(documentId, taskId, comment));
    }

    /**
     * 审批驳回
     */
    @PreAuthorize("@ss.hasPermi('oa:document:approve')")
    @Log(title = "公文", businessType = BusinessType.UPDATE)
    @PostMapping("/reject")
    public AjaxResult reject(@RequestParam("documentId") Long documentId,
                             @RequestParam("taskId") String taskId,
                             @RequestParam(value = "comment", required = false) String comment)
    {
        return toAjax(oaDocumentService.rejectDocument(documentId, taskId, comment));
    }

    /**
     * 撤回公文
     */
    @PreAuthorize("@ss.hasPermi('oa:document:edit')")
    @Log(title = "公文", businessType = BusinessType.UPDATE)
    @PostMapping("/cancel/{documentId}")
    public AjaxResult cancel(@PathVariable("documentId") Long documentId)
    {
        return toAjax(oaDocumentService.cancelDocument(documentId));
    }

    /**
     * 待办公文
     */
    @PreAuthorize("@ss.hasPermi('oa:document:todo')")
    @GetMapping("/todo")
    public TableDataInfo todoList(OaDocument oaDocument)
    {
        startPage();
        List<OaDocument> list = oaDocumentService.selectTodoList(oaDocument);
        return getDataTable(list);
    }

    /**
     * 已办公文
     */
    @PreAuthorize("@ss.hasPermi('oa:document:query')")
    @GetMapping("/done")
    public TableDataInfo doneList(OaDocument oaDocument)
    {
        startPage();
        List<OaDocument> list = oaDocumentService.selectDoneList(oaDocument);
        return getDataTable(list);
    }
}
