package com.yu.oa.mapper;

import java.util.List;
import com.yu.oa.domain.OaProcessCategory;

/**
 * 流程分类Mapper接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface OaProcessCategoryMapper 
{
    public OaProcessCategory selectOaProcessCategoryByCategoryId(Long categoryId);
    public List<OaProcessCategory> selectOaProcessCategoryList(OaProcessCategory oaProcessCategory);
    public int insertOaProcessCategory(OaProcessCategory oaProcessCategory);
    public int updateOaProcessCategory(OaProcessCategory oaProcessCategory);
    public int deleteOaProcessCategoryByCategoryId(Long categoryId);
    public int deleteOaProcessCategoryByCategoryIds(Long[] categoryIds);
}
