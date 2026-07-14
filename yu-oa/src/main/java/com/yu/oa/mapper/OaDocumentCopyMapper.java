package com.yu.oa.mapper;

import java.util.List;
import com.yu.oa.domain.OaDocumentCopy;

/**
 * 公文抄送Mapper接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface OaDocumentCopyMapper 
{
    public OaDocumentCopy selectOaDocumentCopyByCopyId(Long copyId);
    public List<OaDocumentCopy> selectOaDocumentCopyByDocumentId(Long documentId);
    public int insertOaDocumentCopy(OaDocumentCopy oaDocumentCopy);
    public int updateOaDocumentCopy(OaDocumentCopy oaDocumentCopy);
    public int deleteOaDocumentCopyByDocumentId(Long documentId);
    public int deleteOaDocumentCopyByCopyIds(Long[] copyIds);
}
