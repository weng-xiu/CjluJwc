package com.yu.oa.mapper;

import java.util.List;
import com.yu.oa.domain.OaDocumentAttach;

/**
 * 公文附件Mapper接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface OaDocumentAttachMapper 
{
    public OaDocumentAttach selectOaDocumentAttachByAttachId(Long attachId);
    public List<OaDocumentAttach> selectOaDocumentAttachByDocumentId(Long documentId);
    public int insertOaDocumentAttach(OaDocumentAttach oaDocumentAttach);
    public int updateOaDocumentAttach(OaDocumentAttach oaDocumentAttach);
    public int deleteOaDocumentAttachByDocumentId(Long documentId);
    public int deleteOaDocumentAttachByAttachIds(Long[] attachIds);
}
