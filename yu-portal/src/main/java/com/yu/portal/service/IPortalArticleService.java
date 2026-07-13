package com.yu.portal.service;

import java.util.List;
import com.yu.portal.domain.PortalArticle;

/**
 * 门户文章Service接口
 *
 * @author ruoyi
 * @date 2026-07-13
 */
public interface IPortalArticleService
{
    /**
     * 查询门户文章列表（后台管理）
     *
     * @param portalArticle 门户文章
     * @return 门户文章集合
     */
    public List<PortalArticle> selectPortalArticleList(PortalArticle portalArticle);

    /**
     * 通过文章ID查询门户文章
     *
     * @param articleId 文章ID
     * @return 门户文章
     */
    public PortalArticle selectPortalArticleById(Long articleId);

    /**
     * 前台按栏目编码查询已发布文章
     *
     * @param columnCode 栏目编码
     * @return 门户文章集合
     */
    public List<PortalArticle> selectPublishedArticlesByColumnCode(String columnCode);

    /**
     * 前台首页推荐文章
     *
     * @return 门户文章集合
     */
    public List<PortalArticle> selectFeaturedArticles();

    /**
     * 新增门户文章
     *
     * @param portalArticle 门户文章
     * @return 结果
     */
    public int insertPortalArticle(PortalArticle portalArticle);

    /**
     * 修改门户文章
     *
     * @param portalArticle 门户文章
     * @return 结果
     */
    public int updatePortalArticle(PortalArticle portalArticle);

    /**
     * 批量删除门户文章
     *
     * @param articleIds 文章ID数组
     * @return 结果
     */
    public int deletePortalArticleByIds(Long[] articleIds);

    /**
     * 提交审核（status 0→1）
     *
     * @param articleId 文章ID
     * @return 结果
     */
    public int submitForReview(Long articleId);

    /**
     * 审批通过（status 1→2），设置publishDate=NOW()
     *
     * @param articleId 文章ID
     * @param reviewerId 审核人ID
     * @param comment 审核意见
     * @return 结果
     */
    public int approveArticle(Long articleId, Long reviewerId, String comment);

    /**
     * 驳回（status 1→0）
     *
     * @param articleId 文章ID
     * @param reviewerId 审核人ID
     * @param comment 审核意见
     * @return 结果
     */
    public int rejectArticle(Long articleId, Long reviewerId, String comment);

    /**
     * 直接发布（→2）
     *
     * @param articleId 文章ID
     * @return 结果
     */
    public int publishArticle(Long articleId);

    /**
     * 撤回（2→3）
     *
     * @param articleId 文章ID
     * @return 结果
     */
    public int withdrawArticle(Long articleId);

    /**
     * 增加浏览次数
     *
     * @param articleId 文章ID
     * @return 结果
     */
    public int incrementViewCount(Long articleId);
}
