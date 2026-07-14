package com.yu.oa.mapper;

import java.util.List;
import com.yu.oa.domain.OaNoticeRead;

/**
 * 通知公告已读记录Mapper接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface OaNoticeReadMapper 
{
    public OaNoticeRead selectOaNoticeReadByReadId(Long readId);
    public OaNoticeRead selectOaNoticeReadByUser(Long noticeId, Long userId);
    public List<OaNoticeRead> selectOaNoticeReadList(OaNoticeRead oaNoticeRead);
    public int insertOaNoticeRead(OaNoticeRead oaNoticeRead);
    public int updateOaNoticeRead(OaNoticeRead oaNoticeRead);
    public int deleteOaNoticeReadByNoticeId(Long noticeId);
    public int deleteOaNoticeReadByReadIds(Long[] readIds);
}
