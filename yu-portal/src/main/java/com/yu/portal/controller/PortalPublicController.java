package com.yu.portal.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Anonymous;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.portal.domain.PortalArticle;
import com.yu.portal.domain.PortalColumn;
import com.yu.portal.service.IPortalArticleService;
import com.yu.portal.service.IPortalBannerService;
import com.yu.portal.service.IPortalColumnService;

/**
 * 门户前台公开Controller（匿名访问）
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Anonymous
@RestController
@RequestMapping("/portal/public")
public class PortalPublicController extends BaseController
{
    @Autowired
    private IPortalArticleService portalArticleService;

    @Autowired
    private IPortalBannerService portalBannerService;

    @Autowired
    private IPortalColumnService portalColumnService;

    /**
     * 首页聚合数据
     */
    @GetMapping("/home")
    public AjaxResult home()
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("banners", portalBannerService.selectActiveBanners());
        ajax.put("featured", portalArticleService.selectFeaturedArticles());
        // 各栏目最新文章
        List<PortalArticle> newsList = portalArticleService.selectPublishedArticlesByColumnCode("news");
        ajax.put("news", newsList.size() > 5 ? newsList.subList(0, 5) : newsList);
        List<PortalArticle> academicList = portalArticleService.selectPublishedArticlesByColumnCode("academic");
        ajax.put("academic", academicList.size() > 5 ? academicList.subList(0, 5) : academicList);
        List<PortalArticle> noticeList = portalArticleService.selectPublishedArticlesByColumnCode("notice");
        ajax.put("notice", noticeList.size() > 5 ? noticeList.subList(0, 5) : noticeList);
        List<PortalArticle> campusList = portalArticleService.selectPublishedArticlesByColumnCode("campus");
        ajax.put("campus", campusList.size() > 4 ? campusList.subList(0, 4) : campusList);
        // 所有可见栏目
        PortalColumn visibleQuery = new PortalColumn();
        visibleQuery.setIsVisible("1");
        ajax.put("columns", portalColumnService.selectPortalColumnList(visibleQuery));
        return ajax;
    }

    /**
     * 按栏目编码查询已发布文章列表（分页）
     */
    @GetMapping("/column/{code}")
    public TableDataInfo column(@PathVariable("code") String code)
    {
        startPage();
        List<PortalArticle> list = portalArticleService.selectPublishedArticlesByColumnCode(code);
        return getDataTable(list);
    }

    /**
     * 文章详情（仅已发布），同时增加浏览次数，并返回同栏目上一篇/下一篇
     */
    @GetMapping("/article/{id}")
    public AjaxResult article(@PathVariable("id") Long id)
    {
        PortalArticle article = portalArticleService.selectPortalArticleById(id);
        if (article == null || !"2".equals(article.getPublishStatus()))
        {
            return error("文章不存在或未发布");
        }
        portalArticleService.incrementViewCount(id);
        // 浏览次数同步至返回结果
        article.setViewCount((article.getViewCount() == null ? 0 : article.getViewCount()) + 1);
        AjaxResult ajax = success(article);
        ajax.put("prev", portalArticleService.selectPrevArticle(article.getColumnId(), id));
        ajax.put("next", portalArticleService.selectNextArticle(article.getColumnId(), id));
        return ajax;
    }

    /**
     * 获取激活的轮播列表
     */
    @GetMapping("/banners")
    public AjaxResult banners()
    {
        return success(portalBannerService.selectActiveBanners());
    }

    /**
     * 搜索文章（仅已发布的）
     */
    @GetMapping("/search")
    public TableDataInfo search(@RequestParam("keyword") String keyword)
    {
        startPage();
        PortalArticle query = new PortalArticle();
        query.setTitle(keyword);
        query.setPublishStatus("2");
        List<PortalArticle> list = portalArticleService.selectPortalArticleList(query);
        return getDataTable(list);
    }
}
