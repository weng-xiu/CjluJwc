package com.yu.portal.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.portal.domain.PortalArticle;

/**
 * 门户文章Mapper接口
 *
 * @author ruoyi
 * @date 2026-07-13
 */
public interface PortalArticleMapper
{
    /**
     * 查询门户文章列表
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
     * 根据栏目编码查询已发布文章
     *
     * @param columnCode 栏目编码
     * @return 门户文章集合
     */
    public List<PortalArticle> selectPublishedArticlesByColumnCode(String columnCode);

    /**
     * 查询推荐到首页的文章
     *
     * @return 门户文章集合
     */
    public List<PortalArticle> selectFeaturedArticles();

    /**
     * 查询同栏目上一篇已发布文章
     *
     * @param columnId 栏目ID
     * @param articleId 当前文章ID
     * @return 门户文章
     */
    public PortalArticle selectPrevArticle(@Param("columnId") Long columnId, @Param("articleId") Long articleId);

    /**
     * 查询同栏目下一篇已发布文章
     *
     * @param columnId 栏目ID
     * @param articleId 当前文章ID
     * @return 门户文章
     */
    public PortalArticle selectNextArticle(@Param("columnId") Long columnId, @Param("articleId") Long articleId);

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
     * 删除门户文章
     *
     * @param articleId 文章ID
     * @return 结果
     */
    public int deletePortalArticleById(Long articleId);

    /**
     * 批量删除门户文章
     *
     * @param articleIds 文章ID数组
     * @return 结果
     */
    public int deletePortalArticleByIds(Long[] articleIds);

    /**
     * 增加浏览次数
     *
     * @param articleId 文章ID
     * @return 结果
     */
    public int incrementViewCount(Long articleId);
}
