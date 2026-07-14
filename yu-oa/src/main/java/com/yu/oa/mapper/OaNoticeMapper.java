package com.yu.oa.mapper;

import java.util.List;
import com.yu.oa.domain.OaNotice;

/**
 * 通知公告Mapper接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface OaNoticeMapper 
{
    public OaNotice selectOaNoticeByNoticeId(Long noticeId);
    public List<OaNotice> selectOaNoticeList(OaNotice oaNotice);
    public int insertOaNotice(OaNotice oaNotice);
    public int updateOaNotice(OaNotice oaNotice);
    public int deleteOaNoticeByNoticeId(Long noticeId);
    public int deleteOaNoticeByNoticeIds(Long[] noticeIds);
}
