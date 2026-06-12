package com.yu.portal.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.portal.mapper.PortalNoticeMapper;
import com.yu.portal.domain.PortalNotice;
import com.yu.portal.service.IPortalNoticeService;

/**
 * 教务通知Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-20
 */
@Service
public class PortalNoticeServiceImpl implements IPortalNoticeService
{
    @Autowired
    private PortalNoticeMapper portalNoticeMapper;

    @Override
    public PortalNotice selectPortalNoticeByNoticeId(Long noticeId)
    {
        return portalNoticeMapper.selectPortalNoticeByNoticeId(noticeId);
    }

    @Override
    public List<PortalNotice> selectPortalNoticeList(PortalNotice portalNotice)
    {
        return portalNoticeMapper.selectPortalNoticeList(portalNotice);
    }

    @Transactional
    @Override
    public int insertPortalNotice(PortalNotice portalNotice)
    {
        portalNotice.setCreateTime(DateUtils.getNowDate());
        return portalNoticeMapper.insertPortalNotice(portalNotice);
    }

    @Transactional
    @Override
    public int updatePortalNotice(PortalNotice portalNotice)
    {
        portalNotice.setUpdateTime(DateUtils.getNowDate());
        return portalNoticeMapper.updatePortalNotice(portalNotice);
    }

    @Transactional
    @Override
    public int deletePortalNoticeByNoticeId(Long noticeId)
    {
        return portalNoticeMapper.deletePortalNoticeByNoticeId(noticeId);
    }

    @Transactional
    @Override
    public int deletePortalNoticeByNoticeIds(Long[] noticeIds)
    {
        return portalNoticeMapper.deletePortalNoticeByNoticeIds(noticeIds);
    }
}
