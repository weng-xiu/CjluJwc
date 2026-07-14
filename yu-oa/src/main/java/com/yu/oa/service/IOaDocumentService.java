package com.yu.oa.service;

import java.util.List;
import java.util.Map;
import com.yu.oa.domain.OaDocument;

/**
 * 公文Service接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface IOaDocumentService 
{
    public OaDocument selectOaDocumentByDocumentId(Long documentId);
    public List<OaDocument> selectOaDocumentList(OaDocument oaDocument);
    public int insertOaDocument(OaDocument oaDocument);
    public int updateOaDocument(OaDocument oaDocument);
    public int deleteOaDocumentByDocumentId(Long documentId);
    public int deleteOaDocumentByDocumentIds(Long[] documentIds);

    /**
     * 提交公文审批
     */
    public int submitDocument(Long documentId);

    /**
     * 审批通过
     */
    public int approveDocument(Long documentId, String taskId, String comment);

    /**
     * 驳回公文
     */
    public int rejectDocument(Long documentId, String taskId, String comment);

    /**
     * 撤回公文
     */
    public int cancelDocument(Long documentId);

    /**
     * 查询待办公文
     */
    public List<OaDocument> selectTodoList(OaDocument oaDocument);

    /**
     * 查询已办公文
     */
    public List<OaDocument> selectDoneList(OaDocument oaDocument);
}
