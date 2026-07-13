package com.yu.portal.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.portal.mapper.PortalArticleMapper;
import com.yu.portal.domain.PortalArticle;
import com.yu.portal.service.IPortalArticleService;

/**
 * 门户文章Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Service
public class PortalArticleServiceImpl implements IPortalArticleService
{
    @Autowired
    private PortalArticleMapper portalArticleMapper;

    @Override
    public List<PortalArticle> selectPortalArticleList(PortalArticle portalArticle)
    {
        return portalArticleMapper.selectPortalArticleList(portalArticle);
    }

    @Override
    public PortalArticle selectPortalArticleById(Long articleId)
    {
        return portalArticleMapper.selectPortalArticleById(articleId);
    }

    @Override
    public List<PortalArticle> selectPublishedArticlesByColumnCode(String columnCode)
    {
        return portalArticleMapper.selectPublishedArticlesByColumnCode(columnCode);
    }

    @Override
    public List<PortalArticle> selectFeaturedArticles()
    {
        return portalArticleMapper.selectFeaturedArticles();
    }

    @Transactional
    @Override
    public int insertPortalArticle(PortalArticle portalArticle)
    {
        portalArticle.setCreateTime(DateUtils.getNowDate());
        return portalArticleMapper.insertPortalArticle(portalArticle);
    }

    @Transactional
    @Override
    public int updatePortalArticle(PortalArticle portalArticle)
    {
        portalArticle.setUpdateTime(DateUtils.getNowDate());
        return portalArticleMapper.updatePortalArticle(portalArticle);
    }

    @Transactional
    @Override
    public int deletePortalArticleByIds(Long[] articleIds)
    {
        return portalArticleMapper.deletePortalArticleByIds(articleIds);
    }

    @Transactional
    @Override
    public int submitForReview(Long articleId)
    {
        PortalArticle article = portalArticleMapper.selectPortalArticleById(articleId);
        if (article == null)
        {
            throw new ServiceException("文章不存在");
        }
        if (!"0".equals(article.getPublishStatus()))
        {
            throw new ServiceException("仅草稿状态的文章可以提交审核");
        }
        PortalArticle update = new PortalArticle();
        update.setArticleId(articleId);
        update.setPublishStatus("1");
        update.setUpdateTime(DateUtils.getNowDate());
        return portalArticleMapper.updatePortalArticle(update);
    }

    @Transactional
    @Override
    public int approveArticle(Long articleId, Long reviewerId, String comment)
    {
        PortalArticle article = portalArticleMapper.selectPortalArticleById(articleId);
        if (article == null)
        {
            throw new ServiceException("文章不存在");
        }
        if (!"1".equals(article.getPublishStatus()))
        {
            throw new ServiceException("仅待审核状态的文章可以审批通过");
        }
        PortalArticle update = new PortalArticle();
        update.setArticleId(articleId);
        update.setPublishStatus("2");
        update.setPublishDate(DateUtils.getNowDate());
        update.setReviewerId(reviewerId);
        update.setReviewComment(comment);
        update.setReviewTime(DateUtils.getNowDate());
        update.setUpdateTime(DateUtils.getNowDate());
        return portalArticleMapper.updatePortalArticle(update);
    }

    @Transactional
    @Override
    public int rejectArticle(Long articleId, Long reviewerId, String comment)
    {
        PortalArticle article = portalArticleMapper.selectPortalArticleById(articleId);
        if (article == null)
        {
            throw new ServiceException("文章不存在");
        }
        if (!"1".equals(article.getPublishStatus()))
        {
            throw new ServiceException("仅待审核状态的文章可以驳回");
        }
        PortalArticle update = new PortalArticle();
        update.setArticleId(articleId);
        update.setPublishStatus("0");
        update.setReviewerId(reviewerId);
        update.setReviewComment(comment);
        update.setReviewTime(DateUtils.getNowDate());
        update.setUpdateTime(DateUtils.getNowDate());
        return portalArticleMapper.updatePortalArticle(update);
    }

    @Transactional
    @Override
    public int publishArticle(Long articleId)
    {
        PortalArticle article = portalArticleMapper.selectPortalArticleById(articleId);
        if (article == null)
        {
            throw new ServiceException("文章不存在");
        }
        PortalArticle update = new PortalArticle();
        update.setArticleId(articleId);
        update.setPublishStatus("2");
        update.setPublishDate(DateUtils.getNowDate());
        update.setUpdateTime(DateUtils.getNowDate());
        return portalArticleMapper.updatePortalArticle(update);
    }

    @Transactional
    @Override
    public int withdrawArticle(Long articleId)
    {
        PortalArticle article = portalArticleMapper.selectPortalArticleById(articleId);
        if (article == null)
        {
            throw new ServiceException("文章不存在");
        }
        if (!"2".equals(article.getPublishStatus()))
        {
            throw new ServiceException("仅已发布的文章可以撤回");
        }
        PortalArticle update = new PortalArticle();
        update.setArticleId(articleId);
        update.setPublishStatus("3");
        update.setUpdateTime(DateUtils.getNowDate());
        return portalArticleMapper.updatePortalArticle(update);
    }

    @Override
    public int incrementViewCount(Long articleId)
    {
        return portalArticleMapper.incrementViewCount(articleId);
    }
}
