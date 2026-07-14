package com.yu.oa.service;

import java.util.List;
import com.yu.oa.domain.OaNotice;

/**
 * 通知公告Service接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface IOaNoticeService 
{
    /**
     * 查询通知公告
     * 
     * @param noticeId 通知公告ID
     * @return 通知公告
     */
    public OaNotice selectOaNoticeByNoticeId(Long noticeId);

    /**
     * 查询通知公告列表
     * 
     * @param oaNotice 通知公告
     * @return 通知公告集合
     */
    public List<OaNotice> selectOaNoticeList(OaNotice oaNotice);

    /**
     * 新增通知公告
     * 
     * @param oaNotice 通知公告
     * @return 结果
     */
    public int insertOaNotice(OaNotice oaNotice);

    /**
     * 修改通知公告
     * 
     * @param oaNotice 通知公告
     * @return 结果
     */
    public int updateOaNotice(OaNotice oaNotice);

    /**
     * 批量删除通知公告
     * 
     * @param noticeIds 需要删除的通知公告ID
     * @return 结果
     */
    public int deleteOaNoticeByNoticeIds(Long[] noticeIds);

    /**
     * 删除通知公告信息
     * 
     * @param noticeId 通知公告ID
     * @return 结果
     */
    public int deleteOaNoticeByNoticeId(Long noticeId);

    /**
     * 发布公告
     * 
     * @param noticeId 通知公告ID
     * @return 结果
     */
    public int publishOaNotice(Long noticeId);

    /**
     * 撤回公告
     * 
     * @param noticeId 通知公告ID
     * @return 结果
     */
    public int revokeOaNotice(Long noticeId);

    /**
     * 记录已读
     * 
     * @param noticeId 通知公告ID
     * @return 结果
     */
    public int readOaNotice(Long noticeId);
}
