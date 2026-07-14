package com.yu.oa.mapper;

import java.util.List;
import com.yu.oa.domain.OaDocument;

/**
 * 公文Mapper接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface OaDocumentMapper 
{
    public OaDocument selectOaDocumentByDocumentId(Long documentId);
    public List<OaDocument> selectOaDocumentList(OaDocument oaDocument);
    public int insertOaDocument(OaDocument oaDocument);
    public int updateOaDocument(OaDocument oaDocument);
    public int deleteOaDocumentByDocumentId(Long documentId);
    public int deleteOaDocumentByDocumentIds(Long[] documentIds);
}
