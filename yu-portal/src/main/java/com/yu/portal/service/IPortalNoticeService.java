package com.yu.portal.service;

import java.util.List;
import com.yu.portal.domain.PortalNotice;

/**
 * 教务通知Service接口
 *
 * @author ruoyi
 * @date 2026-05-20
 */
public interface IPortalNoticeService
{
    public PortalNotice selectPortalNoticeByNoticeId(Long noticeId);
    public List<PortalNotice> selectPortalNoticeList(PortalNotice portalNotice);
    public int insertPortalNotice(PortalNotice portalNotice);
    public int updatePortalNotice(PortalNotice portalNotice);
    public int deletePortalNoticeByNoticeIds(Long[] noticeIds);
    public int deletePortalNoticeByNoticeId(Long noticeId);
}
