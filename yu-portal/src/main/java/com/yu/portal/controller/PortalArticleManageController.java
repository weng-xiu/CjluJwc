package com.yu.portal.controller;

import java.util.List;
import java.util.Map;
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
import com.yu.common.utils.SecurityUtils;
import com.yu.portal.domain.PortalArticle;
import com.yu.portal.service.IPortalArticleService;
import com.yu.common.core.page.TableDataInfo;

/**
 * 门户文章管理Controller（后台管理）
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@RestController
@RequestMapping("/portal/articleManage")
public class PortalArticleManageController extends BaseController
{
    @Autowired
    private IPortalArticleService portalArticleService;

    @PreAuthorize("@ss.hasPermi('portal:article:list')")
    @GetMapping("/list")
    public TableDataInfo list(PortalArticle portalArticle)
    {
        startPage();
        List<PortalArticle> list = portalArticleService.selectPortalArticleList(portalArticle);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('portal:article:query')")
    @GetMapping(value = "/{articleId}")
    public AjaxResult getInfo(@PathVariable("articleId") Long articleId)
    {
        return success(portalArticleService.selectPortalArticleById(articleId));
    }

    @PreAuthorize("@ss.hasPermi('portal:article:add')")
    @Log(title = "门户文章", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalArticle portalArticle)
    {
        return toAjax(portalArticleService.insertPortalArticle(portalArticle));
    }

    @PreAuthorize("@ss.hasPermi('portal:article:edit')")
    @Log(title = "门户文章", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalArticle portalArticle)
    {
        return toAjax(portalArticleService.updatePortalArticle(portalArticle));
    }

    @PreAuthorize("@ss.hasPermi('portal:article:remove')")
    @Log(title = "门户文章", businessType = BusinessType.DELETE)
    @DeleteMapping("/{articleIds}")
    public AjaxResult remove(@PathVariable Long[] articleIds)
    {
        return toAjax(portalArticleService.deletePortalArticleByIds(articleIds));
    }

    @PreAuthorize("@ss.hasPermi('portal:article:publish')")
    @Log(title = "门户文章", businessType = BusinessType.UPDATE)
    @PutMapping("/publish/{articleId}")
    public AjaxResult publish(@PathVariable("articleId") Long articleId)
    {
        return toAjax(portalArticleService.publishArticle(articleId));
    }

    @PreAuthorize("@ss.hasPermi('portal:article:publish')")
    @Log(title = "门户文章", businessType = BusinessType.UPDATE)
    @PutMapping("/submit/{articleId}")
    public AjaxResult submit(@PathVariable("articleId") Long articleId)
    {
        return toAjax(portalArticleService.submitForReview(articleId));
    }

    @PreAuthorize("@ss.hasPermi('portal:article:review')")
    @Log(title = "门户文章", businessType = BusinessType.UPDATE)
    @PutMapping("/approve/{articleId}")
    public AjaxResult approve(@PathVariable("articleId") Long articleId, @RequestBody Map<String, String> body)
    {
        Long reviewerId = SecurityUtils.getUserId();
        String comment = body.get("reviewComment");
        return toAjax(portalArticleService.approveArticle(articleId, reviewerId, comment));
    }

    @PreAuthorize("@ss.hasPermi('portal:article:review')")
    @Log(title = "门户文章", businessType = BusinessType.UPDATE)
    @PutMapping("/reject/{articleId}")
    public AjaxResult reject(@PathVariable("articleId") Long articleId, @RequestBody Map<String, String> body)
    {
        Long reviewerId = SecurityUtils.getUserId();
        String comment = body.get("reviewComment");
        return toAjax(portalArticleService.rejectArticle(articleId, reviewerId, comment));
    }

    @PreAuthorize("@ss.hasPermi('portal:article:publish')")
    @Log(title = "门户文章", businessType = BusinessType.UPDATE)
    @PutMapping("/withdraw/{articleId}")
    public AjaxResult withdraw(@PathVariable("articleId") Long articleId)
    {
        return toAjax(portalArticleService.withdrawArticle(articleId));
    }
}
